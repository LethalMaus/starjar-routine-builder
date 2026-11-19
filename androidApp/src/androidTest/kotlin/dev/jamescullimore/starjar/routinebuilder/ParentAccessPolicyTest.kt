package dev.jamescullimore.starjar.routinebuilder

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import dev.jamescullimore.starjar.routinebuilder.ai.ParentGenerator
import dev.jamescullimore.starjar.routinebuilder.core.CloudAllowanceExceeded
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ParentAccessPolicyTest {
    @Test
    fun judgingAccessPreservesAnExhaustedStandardAllowance() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.cacheDir, "access-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context =
            object : ContextWrapper(base) {
                override fun getNoBackupFilesDir() = directory
            }
        try {
            val standard = ParentGenerator(context, judgingAccess = false)
            assertNull(standard.reserveCloudAttempt(false))
            repeat(10) { assertNotNull(standard.reserveCloudAttempt(true)) }
            try {
                standard.reserveCloudAttempt(true)
                fail("Expected exhausted allowance")
            } catch (_: CloudAllowanceExceeded) {}
            val ledger = File(directory, "cloud-usage.json")
            val before = ledger.readText()
            val judging = ParentGenerator(context, judgingAccess = true)
            repeat(20) { assertNull(judging.reserveCloudAttempt(true)) }
            assertEquals(before, ledger.readText())
            try {
                standard.reserveCloudAttempt(true)
                fail("Standard allowance must remain exhausted")
            } catch (_: CloudAllowanceExceeded) {}
        } finally {
            directory.deleteRecursively()
        }
    }
}

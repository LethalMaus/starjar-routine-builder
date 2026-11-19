package dev.jamescullimore.starjar.routinebuilder.shared

import java.io.File
import kotlin.test.*

class WebTransferTest {
    @Test
    fun webFileImportsAndProducesReturnFiles() {
        val plan = PlanTransfer.read(WEB_TRANSFER_FIXTURE, { "new-id" })
        assertEquals(20, plan.items.first().badges.size)
        assertEquals("suggested", plan.items.last().state)
        assertEquals("Alte Idee", plan.discarded.single().name)
        File("build/transfer-contract").mkdirs()
        File("build/transfer-contract/editable.json").writeText(PlanTransfer.editable(plan))
        File("build/transfer-contract/templates.json").writeText(PlanTransfer.templates(plan))
    }
}

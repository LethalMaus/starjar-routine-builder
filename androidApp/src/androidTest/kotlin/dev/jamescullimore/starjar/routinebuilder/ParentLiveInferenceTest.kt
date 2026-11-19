package dev.jamescullimore.starjar.routinebuilder

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import dev.jamescullimore.starjar.routinebuilder.ai.ParentGenerator
import dev.jamescullimore.starjar.routinebuilder.shared.*
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** One synthetic cloud call, explicitly opted in; never part of ordinary CI. */
class ParentLiveInferenceTest {
    @Test
    fun protectedCloudUsesTheCurrentParentPrompt() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveParentAi") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
        ActivityScenario.launch(MainActivity::class.java).use {
            val token =
                Tasks.await(
                    FirebaseAppCheck.getInstance().getAppCheckToken(true),
                    30,
                    TimeUnit.SECONDS,
                )
            assertTrue(token.token.isNotBlank()) // Never persist or log tokens.
            val features =
                FeatureChoices(
                    badgeCount = 10,
                    frequency = true,
                    streaks = true,
                    reminders = true,
                    urgency = false,
                )
            val request =
                ParentRequest(
                    "Make school mornings calmer: getting dressed, eating breakfast and leaving on time. Suggest practical tasks and free shared-time rewards.",
                    ageBand = "primary",
                    availableMinutes = 30,
                    constraints = listOf("no_purchases", "no_screens"),
                    features = features,
                )
            val started = System.currentTimeMillis()
            val (text, source) =
                runBlocking {
                    ParentGenerator(context)
                        .generate(PlanEngine.generationPrompt(request, true), true, true)
                }
            // Retain synthetic response on failure so invalid output is not silently discarded.
            File(output, "parent-cloud-response.json").writeText(text)
            val plan =
                PlanEngine.merge(
                    ParentPlan("synthetic-validation", "School mornings"),
                    text,
                    features,
                ) {
                    UUID.randomUUID().toString()
                }
            val tasks = plan.items.filter { it.kind == "task" }
            val rewards = plan.items.filter { it.kind == "reward" }
            assertEquals("IN_CLOUD", source)
            assertTrue(tasks.isNotEmpty())
            assertTrue(rewards.isNotEmpty())
            assertTrue(tasks.all { it.badges.size == 10 })
            val selected = plan.copy(items = plan.items.map { it.copy(state = "kept") })
            val exported = PlanTransfer.templates(selected)
            assertEquals(
                plan.items.size,
                PlanTransfer.read(exported, { UUID.randomUUID().toString() }).items.size,
            )
            File(output, "parent-cloud-verified.json")
                .writeText(
                    buildJsonObject {
                        put("source", source)
                        put("device", android.os.Build.MODEL)
                        put("model", BuildConfig.CLOUD_MODEL)
                        put("judgingAccess", BuildConfig.JUDGING_ACCESS)
                        put("appCheckTokenObtained", true)
                        put("releaseBuild", !BuildConfig.DEBUG)
                        put("durationMs", System.currentTimeMillis() - started)
                        put("tasks", tasks.size)
                        put("rewards", rewards.size)
                        put("badgesPerTask", 10)
                        put("exportRoundTrip", true)
                    }
                        .toString()
                )
        }
    }
}

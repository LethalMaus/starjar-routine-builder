package dev.jamescullimore.starjar.routinebuilder

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.jamescullimore.starjar.routinebuilder.ai.ParentGenerator
import dev.jamescullimore.starjar.routinebuilder.shared.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real local-only parent planning; opt-in, synthetic inputs, no cloud fallback. */
class ParentLocalInferenceTest {
    @Test fun englishPlanningAndFollowUp() = verify("en")
    @Test fun germanPlanningAndFollowUp() = verify("de")

    private fun verify(language: String) {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveParentLocal") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
        val features = FeatureChoices(badgeCount = 0, frequency = false, streaks = false, reminders = false, urgency = false)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            runBlocking {
                val generator = ParentGenerator(context)
                assertTrue(generator.localAvailable())
                val goal = if (language == "de") "Morgens vor der Schule fällt es schwer, Kleidung auszuwählen und sich anzuziehen. Schlage kleine praktische Schritte und kostenlose gemeinsame Aktivitäten als Belohnungen vor." else "Make choosing clothes and getting dressed before school easier. Suggest practical small actions and free shared-time rewards."
                var plan = ParentPlan("local-test", "School mornings", language = language)
                for (phase in listOf("initial", "followup")) {
                    val request = PlanEngine.request(plan, goal,
                        if (phase != "followup") "" else if (language == "de") "Die Etiketten in der Kleidung kratzen. Schlage neue sanfte Möglichkeiten vor, damit das Anziehen angenehm wird, ohne Zeitdruck." else "Clothes tags feel uncomfortable. Suggest new gentle ways to make getting dressed comfortable, without rushing.",
                        "primary", "30", features, listOf("no_purchases", "no_screens"))
                    val (text, source) = generator.generate(PlanEngine.generationPrompt(request, false), false, false)
                    File(output, "parent-local-$language-$phase.json").writeText(buildJsonObject {
                        put("source", source); put("model", generator.lastLocalModelName); put("cloudAllowed", false)
                        put("response", Json.parseToJsonElement(text))
                    }.toString())
                    assertEquals("ON_DEVICE", source)
                    val merged = PlanEngine.merge(plan, text, features) { UUID.randomUUID().toString() }
                    assertTrue(merged.items.any { it.kind == "task" })
                    assertTrue(merged.items.any { it.kind == "reward" })
                    assertTrue(merged.items.all { it.pointAmount in if (it.kind == "task") 3..10 else 10..30 })
                    assertTrue(merged.items.all { it.limitType == "NONE" && it.badges.isEmpty() && it.requireApproval })
                    assertEquals(plan.items, merged.items.take(plan.items.size))
                    val selected = merged.copy(items = merged.items.map { it.copy(state = "kept") })
                    assertEquals(selected.items.size, PlanTransfer.read(PlanTransfer.templates(selected), { UUID.randomUUID().toString() }).items.size)
                    if (phase == "initial") {
                        val edited = selected.copy(items = selected.items.mapIndexed { i, item -> if (i == 0) item.copy(pointAmount = 9) else item })
                        plan = PlanEngine.discard(edited, edited.items.first { it.kind == "task" && it.id != edited.items.first().id }.id)
                    }
                }
                File(output, "parent-local-$language-verified.json").writeText("{\"passed\":true,\"cloudRequests\":0,\"exportRoundTrip\":true,\"keptEditsPreserved\":true}")
            }
        }
    }
}

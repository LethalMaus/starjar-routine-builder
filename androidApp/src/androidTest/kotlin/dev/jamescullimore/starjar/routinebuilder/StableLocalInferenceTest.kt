package dev.jamescullimore.starjar.routinebuilder

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.ai.*
import com.google.firebase.ai.type.*
import java.io.File
import java.util.UUID
import dev.jamescullimore.starjar.routinebuilder.ai.AiRewardRoutineDraft
import dev.jamescullimore.starjar.routinebuilder.shared.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in: downloads the supported stable model and never uses cloud inference. */
@OptIn(PublicPreviewAPI::class)
class StableLocalInferenceTest {
    @Test fun downloadAndVerifyStableLocalRoute() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("downloadStableLocal") == "true")
        val structuredProbe = InstrumentationRegistry.getArguments().getString("structuredProbe") == "true"
        val fullPrompt = InstrumentationRegistry.getArguments().getString("fullParentPrompt") == "true"
        val features = FeatureChoices(badgeCount = 0, frequency = false, streaks = false, reminders = false, urgency = false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
        val evidence = linkedMapOf<String, JsonElement>("category" to JsonPrimitive("STABLE"), "cloudRequests" to JsonPrimitive(0))
        fun save() = File(output, if (structuredProbe) "stable-structured.json" else if (fullPrompt) "stable-parent.json" else "stable-local.json").writeText(Json { prettyPrint = true }.encodeToString(JsonObject(evidence)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            try {
                runBlocking {
                    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                        modelName = BuildConfig.CLOUD_MODEL,
                        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE, modelOption = OnDeviceModelOption.STABLE, maxOutputTokens = if (fullPrompt) 2048 else 512),
                    )
                    val extension = requireNotNull(model.onDeviceExtension)
                    val before = withTimeout(20000) { extension.checkStatus() }
                    evidence["before"] = JsonPrimitive(before.toString()); save()
                    if (before == OnDeviceModelStatus.DOWNLOADABLE) {
                        withTimeout(300000) { extension.download().collect { status ->
                            evidence["downloadStatus"] = JsonPrimitive(status.toString().take(500)); save()
                        } }
                    }
                    val after = withTimeout(20000) { extension.checkStatus() }
                    evidence["after"] = JsonPrimitive(after.toString()); save()
                    assertEquals(OnDeviceModelStatus.AVAILABLE, after)
                    evidence["model"] = JsonPrimitive(withTimeout(20000) { extension.getModelName() }); save()
                    val prompt = if (fullPrompt) PlanEngine.generationPrompt(ParentRequest(
                        "Make choosing clothes and getting dressed before school easier. Suggest child-friendly tasks and free shared-time rewards.",
                        ageBand = "primary", availableMinutes = 30,
                        constraints = listOf("no_purchases", "no_screens"), features = features,
                    ), false) else "Suggest one short child-friendly task for choosing clothes before school and one free shared-game reward. No personal details."
                    val response = withTimeout(120000) {
                        if (structuredProbe) {
                            val result = model.generateObject(JsonSchema.obj(clazz = AiRewardRoutineDraft::class, properties = mapOf(
                                "tasks" to JsonSchema.array(items = JsonSchema.string(), maxItems = 3),
                                "reward" to JsonSchema.string(),
                            )), "Suggest two short child-friendly tasks for getting dressed before school and one free shared-game reward.")
                            val value = requireNotNull(result.getObject())
                            assertTrue(value.tasks.isNotEmpty()); assertTrue(value.reward.isNotBlank())
                            evidence["structuredOutputValidated"] = JsonPrimitive(true)
                            evidence["typedResponse"] = buildJsonObject { put("tasks", JsonArray(value.tasks.map { JsonPrimitive(it) })); put("reward", value.reward) }; save()
                            result.response
                        } else model.generateContent(prompt)
                    }
                    evidence["source"] = JsonPrimitive(response.inferenceSource.toString())
                    evidence["syntheticResponse"] = JsonPrimitive(response.text ?: ""); save()
                    assertEquals(InferenceSource.ON_DEVICE, response.inferenceSource)
                    if (!structuredProbe) assertFalse(response.text.isNullOrBlank())
                    if (fullPrompt) {
                        val plan = PlanEngine.merge(ParentPlan("synthetic-local", "School mornings"), requireNotNull(response.text), features) { UUID.randomUUID().toString() }
                        assertTrue(plan.items.any { it.kind == "task" })
                        assertTrue(plan.items.any { it.kind == "reward" })
                        val exported = PlanTransfer.templates(plan.copy(items = plan.items.map { it.copy(state = "kept") }))
                        assertEquals(plan.items.size, PlanTransfer.read(exported, { UUID.randomUUID().toString() }).items.size)
                        evidence["currentPromptValidated"] = JsonPrimitive(true)
                        evidence["exportRoundTrip"] = JsonPrimitive(true)
                        evidence["items"] = JsonPrimitive(plan.items.size); save()
                    }
                }
            } catch (failure: Throwable) {
                evidence["errorType"] = JsonPrimitive(failure.javaClass.simpleName)
                evidence["error"] = JsonPrimitive(failure.message?.take(600) ?: "Unknown error"); save()
                throw failure
            }
        }
    }
}

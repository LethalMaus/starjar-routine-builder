package dev.jamescullimore.starjar.routinebuilder.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.*
import com.google.firebase.ai.type.*
import dev.jamescullimore.starjar.routinebuilder.BuildConfig
import dev.jamescullimore.starjar.routinebuilder.core.CloudAllowance
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Each response carries the SDK's actual inference source. Consent is required before any cloud
 * route.
 */
@OptIn(PublicPreviewAPI::class)
class ParentGenerator(
    context: Context,
    private val judgingAccess: Boolean = BuildConfig.JUDGING_ACCESS,
) {
    private val allowance = CloudAllowance(File(context.noBackupFilesDir, "cloud-usage.json"))

    internal suspend fun reserveCloudAttempt(cloudAllowed: Boolean): String? =
        if (cloudAllowed && !judgingAccess) withContext(Dispatchers.IO) { allowance.reserve() }
        else null

    internal var lastLocalModelName: String? = null
        private set

    private suspend fun availableLocalOption(): OnDeviceModelOption? {
        if (!FirebaseRoutineGenerator.configured) return null
        return withTimeout(15000) {
            listOf(OnDeviceModelOption.PREVIEW_FAST, OnDeviceModelOption.PREVIEW, OnDeviceModelOption.STABLE).firstOrNull { option ->
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = BuildConfig.CLOUD_MODEL,
                    onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE, modelOption = option),
                )
                model.onDeviceExtension?.checkStatus() == OnDeviceModelStatus.AVAILABLE
            }
        }
    }

    suspend fun localAvailable(): Boolean = availableLocalOption() != null

    suspend fun generate(
        prompt: String,
        cloudAllowed: Boolean,
        advanced: Boolean,
    ): Pair<String, String> {
        check(FirebaseRoutineGenerator.configured)
        require(!advanced || cloudAllowed)
        lastLocalModelName = null
        if (!advanced) {
            try {
                val option = availableLocalOption()
                if (option != null) {
                    val local = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                        modelName = BuildConfig.CLOUD_MODEL,
                        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE, modelOption = option, maxOutputTokens = 2048, temperature = 0.2f, topK = 3),
                    )
                    val result = withTimeout(120000) { local.generateObject(LocalParentPlanning.schema, LocalParentPlanning.prompt(prompt)) }
                    check(result.response.inferenceSource == InferenceSource.ON_DEVICE)
                    val text = LocalParentPlanning.encode(requireNotNull(result.getObject()))
                    lastLocalModelName = local.onDeviceExtension?.getModelName()
                    return text to "ON_DEVICE"
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (!cloudAllowed) throw failure
            }
        }
        check(cloudAllowed) { "On-device generation is unavailable" }
        reserveCloudAttempt(true)
        val model =
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = BuildConfig.CLOUD_MODEL,
                    generationConfig =
                        generationConfig {
                            maxOutputTokens = 8192
                            temperature = 0.2f
                            responseMimeType = "application/json"
                        },
                )
        val response = withTimeout(120000) { model.generateContent(prompt) }
        val source =
            when (response.inferenceSource) {
                InferenceSource.ON_DEVICE -> "ON_DEVICE"
                InferenceSource.IN_CLOUD -> "IN_CLOUD"
                else -> error("Unknown inference route")
            }
        check(cloudAllowed || source == "ON_DEVICE")
        return requireNotNull(response.text) to source
    }
}

package dev.jamescullimore.starjar.routinebuilder.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.*
import com.google.firebase.ai.annotations.Generable
import com.google.firebase.ai.type.*
import dev.jamescullimore.starjar.routinebuilder.BuildConfig
import dev.jamescullimore.starjar.routinebuilder.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Generable(description = "Short routine names for an adult to review")
data class AiRoutineDraft(val tasks: List<String>, val reward: String? = null) { companion object }

@Serializable
@Generable(description = "Routine with the requested free practical reward; empty tasks and empty reward mean clarification is needed")
data class AiRewardRoutineDraft(val tasks: List<String>, val reward: String) { companion object }

@Serializable
@Generable(description = "Routine task names only; empty tasks mean clarification is needed")
data class AiTaskRoutineDraft(val tasks: List<String>) { companion object }

data class GenerationOutput(val draft: GeneratedRoutineDraft, val receipt: GenerationReceipt)
@OptIn(PublicPreviewAPI::class)
interface RoutineGenerator {
 suspend fun readiness(): OnDeviceModelStatus
 fun download(): Flow<DownloadStatus>
 suspend fun generate(request: RoutineRequest, policy: GenerationPolicy, cloudDiagnostic: Boolean = false): GenerationOutput
}
class ConfigurationRequired : IllegalStateException("Dedicated Firebase project configuration is missing")

@OptIn(PublicPreviewAPI::class)
class FirebaseRoutineGenerator(private val instructions: String) : RoutineGenerator {
 companion object { val configured get() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_API_KEY.isNotBlank() }
 private fun model(policy: GenerationPolicy, cloudDiagnostic: Boolean = false): GenerativeModel {
  if (!configured) throw ConfigurationRequired()
  require(!cloudDiagnostic || policy == GenerationPolicy.CLOUD_ALLOWED)
  val mode = when {
   cloudDiagnostic -> InferenceMode.ONLY_IN_CLOUD
   policy == GenerationPolicy.LOCAL_ONLY -> InferenceMode.ONLY_ON_DEVICE
   else -> InferenceMode.PREFER_ON_DEVICE
  }
  return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
   modelName = BuildConfig.CLOUD_MODEL,
   generationConfig = generationConfig { maxOutputTokens = 256; temperature = 0.2f },
   onDeviceConfig = OnDeviceConfig(mode = mode, modelOption = OnDeviceModelOption.PREVIEW_FAST, maxOutputTokens = 256, temperature = 0.2f, topK = 3)
  )
 }
 override suspend fun readiness(): OnDeviceModelStatus = localExtension().checkStatus()
 override fun download(): Flow<DownloadStatus> = localExtension().download()
 private fun localExtension(): OnDeviceExtension = requireNotNull(model(GenerationPolicy.LOCAL_ONLY).onDeviceExtension)
 override suspend fun generate(request: RoutineRequest, policy: GenerationPolicy, cloudDiagnostic: Boolean): GenerationOutput {
  RoutineValidation.request(request)
  val selected = model(policy, cloudDiagnostic)
  val prompt = instructions + "\nREQUEST_DATA:\n" + RoutineRequestEncoding.encode(request) + "\nOUTPUT LANGUAGE: " + if (request.language == "de") "German. Use natural German with correct umlauts." else "English. Use English for every task and reward."
  val start = android.os.SystemClock.elapsedRealtime()
  // The requested shape is enforced by the schema, not just the prompt.
  val (value, response) = if (request.includeReward) {
   val result = selected.generateObject(JsonSchema.obj(clazz = AiRewardRoutineDraft::class, properties = mapOf("tasks" to JsonSchema.array(items = JsonSchema.string(), maxItems = request.taskCount), "reward" to JsonSchema.string())), prompt)
   val generated = result.getObject() ?: throw IllegalArgumentException("No structured response")
   AiRoutineDraft(generated.tasks, generated.reward.takeUnless { generated.tasks.isEmpty() && it.isBlank() }) to result.response
  } else {
   val result = selected.generateObject(JsonSchema.obj(clazz = AiTaskRoutineDraft::class, properties = mapOf("tasks" to JsonSchema.array(items = JsonSchema.string(), maxItems = request.taskCount))), prompt)
   val generated = result.getObject() ?: throw IllegalArgumentException("No structured response")
   AiRoutineDraft(generated.tasks, null) to result.response
  }
  val source = when (response.inferenceSource) {
   InferenceSource.ON_DEVICE -> GenerationSource.ON_DEVICE
   InferenceSource.IN_CLOUD -> GenerationSource.IN_CLOUD
   else -> throw IllegalStateException("Unknown inference source")
  }
  check(policy != GenerationPolicy.LOCAL_ONLY || source == GenerationSource.ON_DEVICE)
  val draft = RoutineValidation.draft(GeneratedRoutineDraft(value.tasks, value.reward), request)
  val actualModel = if (source == GenerationSource.ON_DEVICE) selected.onDeviceExtension?.getModelName() ?: "unreported" else BuildConfig.CLOUD_MODEL
  return GenerationOutput(draft, GenerationReceipt(source, actualModel, android.os.SystemClock.elapsedRealtime() - start))
 }
}

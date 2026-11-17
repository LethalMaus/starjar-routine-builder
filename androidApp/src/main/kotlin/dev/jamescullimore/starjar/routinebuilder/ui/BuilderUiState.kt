package dev.jamescullimore.starjar.routinebuilder.ui

import dev.jamescullimore.starjar.routinebuilder.core.GenerationPolicy
import dev.jamescullimore.starjar.routinebuilder.core.GenerationSource
import dev.jamescullimore.starjar.routinebuilder.core.ReviewedRoutine

enum class BuilderPage { HOME, CREATE, REVIEW, HELP }
enum class ModelReadiness { CHECKING, READY, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE, CONFIG_REQUIRED }
data class EditableItem(val name: String = "", val points: String = "5")
data class BuilderUiState(
    val page: BuilderPage = BuilderPage.HOME,
    val uiLanguage: String = "en",
    val goal: String = "",
    val outputLanguage: String = "en",
    val taskCount: Int = 3,
    val includeReward: Boolean = true,
    val constraints: Set<String> = emptySet(),
    val policy: GenerationPolicy = GenerationPolicy.LOCAL_ONLY,
    val readiness: ModelReadiness = ModelReadiness.CHECKING,
    val downloadProgress: Float? = null,
    val cloudRemaining: Int? = 10,
    val busy: Boolean = false,
    val saving: Boolean = false,
    val title: String = "",
    val tasks: List<EditableItem> = emptyList(),
    val reward: EditableItem? = null,
    val savedRoutines: List<ReviewedRoutine> = emptyList(),
    val source: GenerationSource? = null,
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val diagnostics: String = "",
)

sealed interface BuilderAction {
    data class Navigate(val page: BuilderPage) : BuilderAction
    data object NewRoutine : BuilderAction
    data object ManualRoutine : BuilderAction
    data class UsePreset(val id: String) : BuilderAction
    data class SetGoal(val value: String) : BuilderAction
    data class SetOutputLanguage(val value: String) : BuilderAction
    data class SetTaskCount(val value: Int) : BuilderAction
    data class SetIncludeReward(val value: Boolean) : BuilderAction
    data class ToggleConstraint(val id: String) : BuilderAction
    data class SetPolicy(val value: GenerationPolicy) : BuilderAction
    data object Generate : BuilderAction
    data object CancelGeneration : BuilderAction
    data class SetTitle(val value: String) : BuilderAction
    data class SetTaskName(val index: Int, val value: String) : BuilderAction
    data class SetTaskPoints(val index: Int, val value: String) : BuilderAction
    data class RemoveTask(val index: Int) : BuilderAction
    data object AddTask : BuilderAction
    data class SetRewardName(val value: String) : BuilderAction
    data class SetRewardPoints(val value: String) : BuilderAction
    data object RemoveReward : BuilderAction
    data object AddReward : BuilderAction
    data object Save : BuilderAction
    data object Export : BuilderAction
    data object Copy : BuilderAction
    data class OpenSaved(val id: String) : BuilderAction
    data class DeleteSaved(val id: String) : BuilderAction
    data object DismissMessage : BuilderAction
    data object RefreshReadiness : BuilderAction
    data object DownloadModel : BuilderAction
    data object CloudDiagnostic : BuilderAction
}

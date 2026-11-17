package dev.jamescullimore.starjar.routinebuilder.core

import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale

@Serializable
data class RoutineRequest(
    val goal: String,
    val language: String = "en",
    val taskCount: Int = 3,
    val includeReward: Boolean = true,
    val constraints: List<String> = emptyList(),
)

@Serializable
data class RoutineItem(val name: String, val pointAmount: Int)

/** Only reviewed content is persisted; never the parent's original request. */
@Serializable
data class ReviewedRoutine(
    val id: String,
    val title: String,
    val language: String,
    val tasks: List<RoutineItem>,
    val reward: RoutineItem? = null,
)

enum class GenerationPolicy { LOCAL_ONLY, CLOUD_ALLOWED }
enum class GenerationSource { ON_DEVICE, IN_CLOUD }
data class GenerationReceipt(val source: GenerationSource, val model: String, val durationMs: Long)
data class GeneratedRoutineDraft(val tasks: List<String>, val reward: String?)

enum class ValidationCode { REQUEST, LANGUAGE, CONSTRAINT, TASK_COUNT, NAME, DUPLICATE, REWARD, POINTS, ROUTINE, EXPORT_SIZE, NEEDS_INPUT }
open class ValidationException(val code: ValidationCode) : IllegalArgumentException(code.name)
class NeedsInput : ValidationException(ValidationCode.NEEDS_INPUT)

object RoutineValidation {
    val supportedConstraints = setOf("no_purchases", "no_screens", "short_tasks")
    private val languages = setOf("en", "de")
    const val MAX_REQUEST = 1_200
    const val MAX_GENERATED_NAME = 80
    const val MAX_NAME = 120
    const val MAX_POINTS = 1_000_000
    const val MAX_ITEMS = 100

    fun request(request: RoutineRequest) {
        check(request.goal.isNotBlank() && request.goal.length <= MAX_REQUEST, ValidationCode.REQUEST)
        check(request.language in languages, ValidationCode.LANGUAGE)
        check(request.taskCount in 1..3, ValidationCode.TASK_COUNT)
        check(request.constraints.size <= supportedConstraints.size &&
            request.constraints.distinct().size == request.constraints.size &&
            request.constraints.all { it in supportedConstraints }, ValidationCode.CONSTRAINT)
    }

    /** Normalise boundaries, then validate. Limits intentionally count UTF-16 code units like StarJar. */
    fun draft(draft: GeneratedRoutineDraft, request: RoutineRequest): GeneratedRoutineDraft {
        this.request(request)
        if (draft.tasks.isEmpty() && draft.reward == null) throw NeedsInput()
        check(draft.tasks.size == request.taskCount, ValidationCode.TASK_COUNT)
        val tasks = draft.tasks.map { it.trim().also { text -> name(text, MAX_GENERATED_NAME) } }
        // NFKC + root-locale case conversion catches width/case equivalents, including Straße/STRASSE.
        val comparison = tasks.map { Normalizer.normalize(it, Normalizer.Form.NFKC).uppercase(Locale.ROOT) }
        check(comparison.distinct().size == tasks.size, ValidationCode.DUPLICATE)
        val reward = draft.reward?.trim()
        if (request.includeReward) {
            check(reward != null, ValidationCode.REWARD)
            name(reward!!, MAX_GENERATED_NAME)
        } else check(reward == null, ValidationCode.REWARD)
        return GeneratedRoutineDraft(tasks, reward)
    }

    fun reviewed(routine: ReviewedRoutine) {
        check(routine.id.isNotBlank() && routine.id.length <= 128, ValidationCode.ROUTINE)
        name(routine.title, MAX_NAME)
        check(routine.language in languages, ValidationCode.LANGUAGE)
        check(routine.tasks.size + (if (routine.reward == null) 0 else 1) in 1..MAX_ITEMS, ValidationCode.TASK_COUNT)
        (routine.tasks + listOfNotNull(routine.reward)).forEach {
            name(it.name, MAX_NAME)
            check(it.pointAmount in 1..MAX_POINTS, ValidationCode.POINTS)
        }
    }

    /** Call before making a RoutineItem: floats, signs, overflow and empty input are rejected. */
    fun points(text: String): Int {
        check(text.isNotEmpty() && text.length <= 7 && text.all { it in '0'..'9' }, ValidationCode.POINTS)
        val amount = text.toIntOrNull()
        check(amount != null && amount in 1..MAX_POINTS, ValidationCode.POINTS)
        return amount!!
    }

    private fun name(value: String, maximum: Int) {
        check(value.isNotBlank() && value == value.trim() && value.length <= maximum, ValidationCode.NAME)
        // Reject malformed UTF-16 and embedded controls; printable Unicode remains unchanged.
        var index = 0
        while (index < value.length) {
            val char = value[index]
            check(!char.isISOControl(), ValidationCode.NAME)
            if (char.isHighSurrogate()) {
                check(index + 1 < value.length && value[index + 1].isLowSurrogate(), ValidationCode.NAME)
                index++
            } else check(!char.isLowSurrogate(), ValidationCode.NAME)
            index++
        }
    }

    private fun check(condition: Boolean, code: ValidationCode) {
        if (!condition) throw ValidationException(code)
    }
}

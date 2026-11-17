package dev.jamescullimore.starjar.routinebuilder.core

import org.junit.Assert.*
import org.junit.Test

class RoutineValidationTest {
    private val request = RoutineRequest("A school morning", taskCount = 2, includeReward = false)

    @Test fun requestRejectsEmptyOversizedUnknownLanguageAndConstraints() {
        rejects(ValidationCode.REQUEST) { RoutineValidation.request(request.copy(goal = "   ")) }
        rejects(ValidationCode.REQUEST) { RoutineValidation.request(request.copy(goal = "x".repeat(1201))) }
        rejects(ValidationCode.LANGUAGE) { RoutineValidation.request(request.copy(language = "fr")) }
        rejects(ValidationCode.TASK_COUNT) { RoutineValidation.request(request.copy(taskCount = 4)) }
        rejects(ValidationCode.CONSTRAINT) { RoutineValidation.request(request.copy(constraints = listOf("invented"))) }
        rejects(ValidationCode.CONSTRAINT) { RoutineValidation.request(request.copy(constraints = listOf("no_screens", "no_screens"))) }
        RoutineValidation.request(request.copy(goal = "x".repeat(1200), constraints = RoutineValidation.supportedConstraints.toList()))
    }

    @Test fun generatedNamesTrimAndRetainGermanUnicode() {
        val result = RoutineValidation.draft(GeneratedRoutineDraft(listOf(" Zähne putzen ", "Schuhe anziehen"), null), request.copy(language = "de"))
        assertEquals(listOf("Zähne putzen", "Schuhe anziehen"), result.tasks)
    }

    @Test fun duplicateDetectionIncludesUnicodeWidthAndCase() {
        for (names in listOf(listOf("Books", "books"), listOf("Books", "Ｂｏｏｋｓ"), listOf("Straße", "STRASSE"))) {
            rejects(ValidationCode.DUPLICATE) { RoutineValidation.draft(GeneratedRoutineDraft(names, null), request) }
        }
    }

    @Test fun clarificationIsNotAValidDraftAndEmptyRewardDoesNotCount() {
        assertThrows(NeedsInput::class.java) { RoutineValidation.draft(GeneratedRoutineDraft(emptyList(), null), request) }
        rejects(ValidationCode.TASK_COUNT) { RoutineValidation.draft(GeneratedRoutineDraft(emptyList(), "Play"), request) }
        rejects(ValidationCode.REWARD) { RoutineValidation.draft(GeneratedRoutineDraft(listOf("Books", "Shoes"), "Play"), request) }
        rejects(ValidationCode.REWARD) { RoutineValidation.draft(GeneratedRoutineDraft(listOf("Books", "Shoes"), null), request.copy(includeReward = true)) }
        rejects(ValidationCode.NAME) { RoutineValidation.draft(GeneratedRoutineDraft(listOf("Books", "Shoes"), "  "), request.copy(includeReward = true)) }
    }

    @Test fun generatedCountLengthAndMalformedUnicodeAreRejected() {
        rejects(ValidationCode.TASK_COUNT) { RoutineValidation.draft(GeneratedRoutineDraft(listOf("Books"), null), request) }
        for (bad in listOf(" ", "x".repeat(81), "\uD83D", "\uDE00", "Books\u0000")) {
            rejects(ValidationCode.NAME) { RoutineValidation.draft(GeneratedRoutineDraft(listOf(bad, "Shoes"), null), request) }
        }
    }

    @Test fun limitsUseUtf16AsTheActualStarJarImporterDoes() {
        val emoji = "\uD83D\uDE00"
        RoutineValidation.draft(GeneratedRoutineDraft(listOf(emoji.repeat(40), "Shoes"), null), request)
        rejects(ValidationCode.NAME) { RoutineValidation.draft(GeneratedRoutineDraft(listOf(emoji.repeat(41), "Shoes"), null), request) }
        RoutineValidation.reviewed(routine().copy(tasks = listOf(RoutineItem(emoji.repeat(60), 5))))
        rejects(ValidationCode.NAME) { RoutineValidation.reviewed(routine().copy(tasks = listOf(RoutineItem(emoji.repeat(61), 5)))) }
    }

    @Test fun editablePointsRejectFractionalBooleanNegativeAndOverflow() {
        for (bad in listOf("", "0", "-1", "1.5", "5.0", "true", "null", "1000001", "2147483648", " 5", "+5", "５")) {
            rejects(ValidationCode.POINTS) { RoutineValidation.points(bad) }
        }
        assertEquals(1, RoutineValidation.points("1"))
        assertEquals(1_000_000, RoutineValidation.points("1000000"))
    }

    @Test fun reviewedAllowsRewardOnlyButRejectsEmptyAndOversizedCollections() {
        RoutineValidation.reviewed(routine().copy(tasks = emptyList(), reward = RoutineItem("Choose a game", 30)))
        rejects(ValidationCode.TASK_COUNT) { RoutineValidation.reviewed(routine().copy(tasks = emptyList())) }
        RoutineValidation.reviewed(routine().copy(tasks = List(100) { RoutineItem("Task $it", 1) }))
        rejects(ValidationCode.TASK_COUNT) { RoutineValidation.reviewed(routine().copy(tasks = List(100) { RoutineItem("Task $it", 1) }, reward = RoutineItem("Game", 30))) }
        for (bad in listOf(0, -1, 1_000_001, Int.MAX_VALUE)) {
            rejects(ValidationCode.POINTS) { RoutineValidation.reviewed(routine().copy(tasks = listOf(RoutineItem("Books", bad)))) }
        }
    }

    private fun rejects(code: ValidationCode, block: () -> Unit) {
        assertEquals(code, assertThrows(ValidationException::class.java) { block() }.code)
    }
}

internal fun routine() = ReviewedRoutine("synthetic-id", "School morning", "en", listOf(RoutineItem("Brush teeth", 5)))

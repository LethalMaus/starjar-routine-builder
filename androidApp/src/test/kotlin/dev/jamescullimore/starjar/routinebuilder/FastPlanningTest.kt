package dev.jamescullimore.starjar.routinebuilder

import dev.jamescullimore.starjar.routinebuilder.ai.LocalParentPlanning
import org.junit.Assert.*
import org.junit.Test

class FastPlanningTest {
    @Test fun keepsUniqueActionsAndIgnoresMalformedLines() {
        val draft = LocalParentPlanning.parseFast("""
            Here are some ideas:
            TASK: Setz dich im Bett auf.
            - task: Setz dich im Bett auf
            REWARD: Such dir ein Brettspiel aus
            TASK: [action goes here]
            REWARD | one | two
        """.trimIndent())
        assertEquals("Setz dich im Bett auf", draft.tasks.single().name)
        assertEquals(5, draft.tasks.single().pointAmount)
        assertEquals("Such dir ein Brettspiel aus", draft.rewards.single().name)
        assertEquals(20, draft.rewards.single().pointAmount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutputWithoutAnyUsableIdeas() {
        LocalParentPlanning.parseFast("TASK: []\nREWARD:")
    }

    @Test fun fastPromptRetainsGermanFollowUpAndExcludesIconCatalogue() {
        val prompt = LocalParentPlanning.fastPrompt(
            """REQUEST_DATA:
{"goal":"Aufstehen","language":"de","feedback":"Ohne Musik","discarded":["Lied wählen"],"constraints":["no_screens"],"iconIds":["SomeIcon"]}"""
        )
        for (value in listOf("German", "Aufstehen", "Ohne Musik", "Lied wählen", "no_screens")) assertTrue(value in prompt)
        assertFalse("SomeIcon" in prompt)
    }
}

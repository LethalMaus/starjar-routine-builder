package dev.jamescullimore.starjar.routinebuilder

import dev.jamescullimore.starjar.routinebuilder.core.*
import org.junit.Test
import org.junit.Assert.*

class LegacyPlansTest {
    @Test fun savedPrototypePlanKeepsIdentityAndAmounts() {
        val old=ReviewedRoutine("old-id","Morning","de",listOf(RoutineItem("Kleidung auswählen",7)),RoutineItem("Spiel auswählen",23))
        val migrated=LegacyPlans.saved(old)
        assertEquals(old.id,migrated.id)
        assertEquals(listOf(7,23),migrated.items.map{it.pointAmount})
        assertTrue(migrated.items.all{it.state=="kept"})
        assertEquals("de",migrated.language)
    }
    @Test fun draftUsesReviewedFieldsAndExcludesOldPromptContext() {
        val raw="""{"id":"draft","title":"Morning","language":"en","tasks":[{"name":"Choose clothes","points":"7"}],"reward":null,"goal":"private context","constraints":[]}"""
        val migrated=LegacyPlans.draft(raw)
        assertEquals(7,migrated.items.single().pointAmount)
        assertFalse(migrated.toString().contains("private context"))
    }
}

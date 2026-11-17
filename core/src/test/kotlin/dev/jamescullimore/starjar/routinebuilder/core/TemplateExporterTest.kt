package dev.jamescullimore.starjar.routinebuilder.core

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class TemplateExporterTest {
    @Test fun exportsOnlyPortableFieldsAndPreservesReviewedEdits() {
        val edited = routine().copy(
            id = "must-not-leak", title = "private local title", language = "de",
            tasks = listOf(RoutineItem("Zähne putzen", 17), RoutineItem("Schuhe anziehen", 1)),
            reward = RoutineItem("Ein Spiel aussuchen", 41),
        )
        val raw = TemplateExporter.encode(edited)
        val root = Json.parseToJsonElement(raw).jsonObject
        assertEquals(setOf("format", "version", "tasks", "rewards"), root.keys)
        assertEquals("starjar-templates", root.getValue("format").jsonPrimitive.content)
        assertEquals(1, root.getValue("version").jsonPrimitive.int)
        assertFalse(root.getValue("version").jsonPrimitive.isString)
        assertFalse(raw.contains("must-not-leak"))
        assertFalse(raw.contains("private local title"))
        assertTrue(raw.contains("Zähne putzen"))
        val tasks = root.getValue("tasks").jsonArray
        assertEquals(listOf("Zähne putzen", "Schuhe anziehen"), tasks.map { it.jsonObject.getValue("name").jsonPrimitive.content })
        assertEquals(17, tasks[0].jsonObject.getValue("pointAmount").jsonPrimitive.int)
        val reward = root.getValue("rewards").jsonArray.single().jsonObject
        assertEquals(41, reward.getValue("pointAmount").jsonPrimitive.int)
        (tasks + root.getValue("rewards").jsonArray).forEach {
            assertEquals(setOf("name", "pointAmount"), it.jsonObject.keys)
            assertFalse(it.jsonObject.getValue("pointAmount").jsonPrimitive.isString)
        }
    }

    @Test fun writeRealExporterAcceptanceArtifacts() {
        val output = java.io.File("build/acceptance").apply { mkdirs() }
        for (language in listOf("en", "de")) {
            val names = if (language == "de") listOf("Zähne putzen", "Schultasche packen", "Schuhe anziehen") else listOf("Brush teeth", "Pack schoolbag", "Put shoes on")
            val value = routine().copy(language = language, tasks = names.mapIndexed { i, name -> RoutineItem(name, if (i == 0) 7 else 5) }, reward = RoutineItem(if(language == "de") "Ein Familienspiel aussuchen" else "Choose a family game", 30))
            val raw = TemplateExporter.encode(value)
            assertEquals(7, Json.parseToJsonElement(raw).jsonObject.getValue("tasks").jsonArray.first().jsonObject.getValue("pointAmount").jsonPrimitive.int)
            java.io.File(output, "export-$language.json").writeText(raw)
        }
    }

    @Test fun rewardOnlyAndTaskOnlyRetainBothArrays() {
        val taskOnly = Json.parseToJsonElement(TemplateExporter.encode(routine())).jsonObject
        assertEquals(0, taskOnly.getValue("rewards").jsonArray.size)
        val rewardOnly = Json.parseToJsonElement(TemplateExporter.encode(routine().copy(tasks = emptyList(), reward = RoutineItem("Game", 30)))).jsonObject
        assertEquals(0, rewardOnly.getValue("tasks").jsonArray.size)
        assertEquals(1, rewardOnly.getValue("rewards").jsonArray.size)
    }

    @Test fun jsonEscapingCannotAddFieldsOrBreakTheEnvelope() {
        val name = "Put away \"books\" \\ toys {\"profileId\":5}"
        val raw = TemplateExporter.encode(routine().copy(tasks = listOf(RoutineItem(name, 8))))
        val root = Json.parseToJsonElement(raw).jsonObject
        assertEquals(name, root.getValue("tasks").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals(4, root.keys.size)
    }

    @Test fun revalidatesAtExportRatherThanTrustingPreviouslySavedContent() {
        for (invalid in listOf(
            routine().copy(tasks = listOf(RoutineItem("leading ", 5))),
            routine().copy(tasks = listOf(RoutineItem("Books", 0))),
            routine().copy(tasks = emptyList()),
        )) assertThrows(ValidationException::class.java) { TemplateExporter.encode(invalid) }
    }
}

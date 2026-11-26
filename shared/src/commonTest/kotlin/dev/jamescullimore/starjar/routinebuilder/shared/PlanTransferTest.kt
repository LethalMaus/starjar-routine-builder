package dev.jamescullimore.starjar.routinebuilder.shared

import kotlin.test.*
import kotlinx.serialization.json.*

class PlanTransferTest {
    private var serial = 0

    private fun id() = "test-${++serial}"

    private val task =
        PlanItem(
            "task",
            state = "kept",
            name = "Pack the bag",
            icon = "School",
            badges = (1..20).map { Badge(it * 5, "Milestone $it", it) },
            streakBonus = Streak(5),
            reminder = Reminder(daysOfWeek = listOf(1, 3, 5), time = "07:30"),
            isUrgent = true,
            dueByDate = 1800000000000,
            urgencyBonus = 2,
        )
    private val plan
        get() =
            ParentPlan(
                "plan",
                "Our week",
                items =
                    listOf(
                        task,
                        PlanItem("reward", "reward", "suggested", "Choose a game", icon = "Games"),
                    ),
                discarded = listOf(DiscardedIdea("task", "Old idea")),
            )

    @Test
    fun nativeLanguageUsesGermanVariantsAndEnglishFallback() {
        for (tag in listOf("de", "de-DE", "de_AT", "DE-ch")) assertEquals("de", appLanguage(tag))
        for (tag in listOf("en-IE", "fr-FR", "ar", "", "ja-JP")) assertEquals("en", appLanguage(tag))
    }

    @Test
    fun editableRoundTripPreservesEveryChoice() {
        assertEquals(plan, PlanTransfer.read(PlanTransfer.editable(plan), ::id))
    }

    @Test
    fun selectedTemplatesKeepOptionsAndStripIdentity() {
        val raw = PlanTransfer.templates(plan)
        assertFalse(raw.contains("Choose a game"))
        assertFalse(raw.contains("originalName"))
        val incoming = PlanTransfer.read(raw, ::id)
        assertEquals(task.badges, incoming.items.single().badges)
        assertEquals(task.reminder, incoming.items.single().reminder)
        assertEquals(task.streakBonus, incoming.items.single().streakBonus)
        assertEquals(
            Json.parseToJsonElement(raw),
            Json.parseToJsonElement(PlanTransfer.templates(incoming)),
        )
    }

    @Test
    fun acceptsNumericStringsWrittenByWebEditors() {
        val raw = PlanTransfer.editable(plan).replace("\"pointAmount\":5", "\"pointAmount\":\"5\"")
        assertEquals(plan, PlanTransfer.read(raw, ::id))
    }

    @Test
    fun rejectsUnsafeOrUnsupportedIncomingContent() {
        for (raw in
            listOf(
                "{}",
                "x".repeat(200001),
                PlanTransfer.editable(plan).replace("\"version\":1", "\"version\":2"),
                PlanTransfer.editable(plan).replace("\"pointAmount\":5", "\"pointAmount\":-1"),
            )) {
            assertFails { PlanTransfer.read(raw, ::id) }
        }
        assertFails {
            PlanTransfer.editable(
                plan.copy(items = listOf(task.copy(badges = task.badges + Badge(101, "Too many"))))
            )
        }
    }

    @Test
    fun generatedUnknownIconsFallbackButFileImportsRemainStrict() {
        val response = """{"tasks":[{"name":"Shoes","pointAmount":3,"icon":"Invented"}],"rewards":[{"name":"Game","pointAmount":15,"icon":"Game"}]}"""
        val result = PlanEngine.merge(ParentPlan("fresh", "Plan"), response, FeatureChoices(), ::id)
        assertEquals(listOf("Check", "Games"), result.items.map { it.icon })
        val invalidFile = """{"format":"starjar-templates","version":1,"tasks":[{"name":"Shoes","pointAmount":3,"icon":"Invented"}],"rewards":[]}"""
        assertFails { PlanTransfer.read(invalidFile, ::id) }
        val valid = response.replace("Invented", "School").replace("\"icon\":\"Game\"", "\"icon\":\"Games\"")
        assertEquals("School", PlanEngine.merge(ParentPlan("valid", "Plan"), valid, FeatureChoices(), ::id).items.first().icon)
    }

    @Test
    fun newSuggestionsCannotOverwriteChoicesOrRestoreDiscardedIdeas() {
        val response =
            """{"tasks":[{"name":"Pack the bag","pointAmount":3},{"name":"Old idea","pointAmount":3},{"name":"Put shoes on","pointAmount":3}],"rewards":[]}"""
        val result = PlanEngine.merge(plan, response, FeatureChoices(), ::id)
        assertEquals(plan.items, result.items.take(2))
        assertEquals("Put shoes on", result.items.last().name)
        assertEquals("suggested", result.items.last().state)
        assertEquals(3, result.items.size)
    }

    @Test
    fun unselectedFeaturesAreRemovedAndRequestedBadgeCountIsEnforced() {
        val response = PlanTransfer.templates(plan)
        val result = PlanEngine.merge(ParentPlan("empty", "Plan"), response, FeatureChoices(), ::id)
        val generated = PlanEngine.merge(ParentPlan("empty", "Plan"), response, FeatureChoices(badgeCount = 20), ::id)
        assertEquals(task.badges.first().copy(completionsRequired = 1), generated.items.single().badges.first())
        assertEquals(task.badges.drop(1), generated.items.single().badges.drop(1))
        assertEquals(task.badges, PlanTransfer.read(response, ::id).items.single().badges)
        assertTrue(result.items.single().badges.isEmpty())
        assertNull(result.items.single().reminder)
        assertFalse(result.items.single().isUrgent)
        assertFails {
            PlanEngine.merge(
                ParentPlan("empty", "Plan"),
                response,
                FeatureChoices(badgeCount = 10),
                ::id,
            )
        }
    }

    @Test
    fun templatesTrimNamesAndRespectStarJarCharacterLimit() {
        val padded = plan.copy(items = listOf(task.copy(name = "  Pack the bag  ")))
        val incoming = PlanTransfer.read(PlanTransfer.templates(padded), ::id)
        assertEquals("Pack the bag", incoming.items.single().name)
        val large = plan.copy(items = (1..40).map {
            task.copy(id = "item-$it", name = "Task $it", description = "a".repeat(2000))
        })
        // Valid editable plans may still be too large for StarJar's stricter import limit.
        assertTrue(PlanTransfer.editable(large).length > 100_000)
        assertFails { PlanTransfer.templates(large) }
    }

    @Test
    fun normalizedDuplicatesCannotBeExported() {
        assertFails {
            PlanTransfer.templates(
                plan.copy(items = listOf(task, task.copy(id = "second", name = "ＰＡＣＫ ＴＨＥ ＢＡＧ")))
            )
        }
    }
}

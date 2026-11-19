package dev.jamescullimore.starjar.routinebuilder.shared

import kotlin.test.*

class PlanLibraryTest {
    private fun plan(id: String) =
        ParentPlan(
            id,
            "Plan $id",
            items =
                listOf(
                    PlanItem(
                        "task-$id",
                        state = "kept",
                        name = "Choose clothes",
                        badges = listOf(Badge(5, "Milestone", 2)),
                    )
                ),
        )

    @Test
    fun deletionRemovesOnlyTheChosenSavedCopy() {
        val original = PlanLibrary.save(PlanLibrary.save("[]", plan("one")), plan("two"))
        assertEquals(listOf(plan("two")), PlanLibrary.read(PlanLibrary.delete(original, "one")))
        assertEquals(listOf(plan("two"), plan("one")), PlanLibrary.read(original))
        assertFails { PlanLibrary.delete(original, "missing") }
    }

    @Test
    fun malformedOrDuplicateEntriesCannotBeSilentlyOverwritten() {
        val valid = PlanTransfer.editable(plan("one"))
        for (bad in listOf("broken", "[$valid,{}]", "[$valid,$valid]")) {
            assertFails { PlanLibrary.save(bad, plan("new")) }
            assertFails { PlanLibrary.delete(bad, "one") }
            assertFails { PlanLibrary.read(bad) }
        }
    }

    @Test
    fun updatingAFullLibraryPreservesOthersAndDoesNotAddDuplicates() {
        val full = (1..100).fold("[]") { doc, id -> PlanLibrary.save(doc, plan(id.toString())) }
        val revised = plan("50").copy(title = "Revised plan")
        val updated = PlanLibrary.read(PlanLibrary.save(full, revised))
        assertEquals(100, updated.size)
        assertEquals(revised, updated.first())
        assertEquals(99, updated.count { it.id != "50" })
        assertFails { PlanLibrary.save(full, plan("101")) }
    }
}

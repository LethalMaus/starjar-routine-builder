package dev.jamescullimore.starjar.routinebuilder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.jamescullimore.starjar.routinebuilder.shared.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlanPersistenceUiTest {
    @get:Rule val compose = createComposeRule()

    private class Store(var draft: String? = null, var library: String = "[]") : PlatformBridge {
        var writes = 0
        var serial = 0

        override fun newId() = "test-${++serial}"

        override fun loadDraft() = draft

        override fun saveDraft(document: String) {
            draft = document
            writes++
        }

        override fun loadLibrary() = library

        override fun saveLibrary(document: String) {
            library = document
        }

        override fun share(document: String, filename: String) {}

        override fun openDocument(completion: (String?) -> Unit) {}

        override fun localAvailability(completion: (String) -> Unit) {
            completion("unavailable")
        }

        override fun generate(
            prompt: String,
            cloudAllowed: Boolean,
            advanced: Boolean,
            completion: (String?, String?, String?) -> Unit,
        ) {}
    }

    @Test
    fun deletingASavedPlanRequiresConfirmationAndPreservesTheDraft() {
        val first = ParentPlan("one", "Morning plan")
        val second = ParentPlan("two", "Evening plan")
        val store =
            Store(
                PlanTransfer.editable(first),
                PlanLibrary.save(PlanLibrary.save("[]", first), second),
            )
        val original = store.library
        compose.setContent { ParentApp(store) }
        compose.onNodeWithText("Saved plans").performScrollTo().performClick()
        compose
            .onNodeWithContentDescription("Delete: Morning plan")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(original, store.library)
        compose.onNodeWithContentDescription("Delete: Morning plan").performClick()
        compose.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        compose.waitUntil { PlanLibrary.read(store.library).size == 1 }
        assertEquals("two", PlanLibrary.read(store.library).single().id)
        assertEquals(PlanTransfer.editable(first), store.draft)
    }

    @Test
    fun unreadableDraftIsNotOverwrittenByEditing() {
        val store = Store("broken draft")
        compose.setContent { ParentApp(store) }
        compose
            .onNodeWithText("The saved draft could not be read.", substring = true)
            .assertExists()
        compose.onNodeWithText("Write my own").performScrollTo().performClick()
        compose
            .onNode(hasSetTextAction() and hasText("Name"))
            .performScrollTo()
            .performTextReplacement("My new task")
        assertEquals("broken draft", store.draft)
        assertEquals(0, store.writes)
        compose.onNodeWithText("New ideas").performScrollTo().performClick()
        compose
            .onNodeWithText("This replaces the unreadable draft", substring = true)
            .assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("broken draft", store.draft)
    }

    @Test
    fun malformedLibraryIsNotShownAsEmptyOrReplacedOnSave() {
        val store =
            Store(PlanTransfer.editable(ParentPlan("one", "Morning plan")), "broken library")
        compose.setContent { ParentApp(store) }
        compose.onNodeWithText("Save ideas").performScrollTo().performClick()
        assertEquals("broken library", store.library)
        compose.onNodeWithText("Saved plans").performScrollTo().performClick()
        compose.onNodeWithText("Saved plans could not be read.", substring = true).assertExists()
        compose.onNodeWithText("No saved plans yet.").assertDoesNotExist()
        assertEquals("broken library", store.library)
    }
}

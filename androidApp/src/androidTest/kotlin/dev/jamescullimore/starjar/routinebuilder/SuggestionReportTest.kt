package dev.jamescullimore.starjar.routinebuilder

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.jamescullimore.starjar.routinebuilder.shared.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SuggestionReportTest {
    @get:Rule val compose = createComposeRule()
    private class Bridge : PlatformBridge {
        val calls = mutableListOf<List<String>>()
        var callback: ((Boolean) -> Unit)? = null
        override fun newId() = "12345678-1234-1234-1234-123456789012"
        override fun loadDraft(): String? = null
        override fun saveDraft(document: String) {}
        override fun loadLibrary() = "[]"
        override fun saveLibrary(document: String) {}
        override fun share(document: String, filename: String) {}
        override fun openDocument(completion: (String?) -> Unit) {}
        override fun localAvailability(completion: (String) -> Unit) = completion("unavailable")
        override fun generate(prompt: String, cloudAllowed: Boolean, advanced: Boolean, completion: (String?, String?, String?) -> Unit) {}
        override fun reportSuggestion(id: String, name: String, description: String, comment: String, completion: (Boolean) -> Unit) {
            calls.add(listOf(id,name,description,comment)); callback = completion
        }
    }
    @Test fun reportRequiresConfirmationAndRetryKeepsId() {
        val bridge = Bridge()
        compose.setContent { RoutineBuilderTheme { SuggestionReportDialog(PlanItem("item",name="Synthetic idea",description="Synthetic description"),"en",bridge,{}) } }
        assertTrue(bridge.calls.isEmpty())
        compose.onNode(hasSetTextAction() and hasText("Comment (optional)")).performScrollTo().performTextInput("Unsafe suggestion")
        compose.onNodeWithText("Send report").performClick()
        compose.waitUntil { bridge.calls.size == 1 }
        assertEquals(listOf(bridge.newId(),"Synthetic idea","Synthetic description","Unsafe suggestion"),bridge.calls.single())
        compose.onNodeWithText("Send report").assertIsNotEnabled()
        compose.runOnIdle { bridge.callback?.invoke(false) }
        compose.onNodeWithText("The report could not be confirmed.",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Send report").performClick()
        compose.waitUntil { bridge.calls.size == 2 }
        assertEquals(bridge.calls[0],bridge.calls[1])
        compose.runOnIdle { bridge.callback?.invoke(true) }
        compose.onNodeWithText("Report received.",substring=true).assertIsDisplayed()
    }
    @Test fun germanCancelSendsNothing() {
        val bridge = Bridge(); var dismissed=false
        compose.setContent { RoutineBuilderTheme { SuggestionReportDialog(PlanItem("item",name="Testvorschlag"),"de",bridge,{dismissed=true}) } }
        compose.onNodeWithText("Abbrechen").performClick()
        assertTrue(dismissed);assertTrue(bridge.calls.isEmpty())
    }
}

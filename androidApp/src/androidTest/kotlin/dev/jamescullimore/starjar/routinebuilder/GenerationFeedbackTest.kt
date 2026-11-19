package dev.jamescullimore.starjar.routinebuilder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.jamescullimore.starjar.routinebuilder.shared.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GenerationFeedbackTest {
    @get:Rule val compose = createComposeRule()
    private class Bridge : PlatformBridge {
        var calls = 0
        var completion: ((String?, String?, String?) -> Unit)? = null
        override fun newId() = "test"
        override fun loadDraft(): String? = null
        override fun saveDraft(document: String) {}
        override fun loadLibrary() = "[]"
        override fun saveLibrary(document: String) {}
        override fun share(document: String, filename: String) {}
        override fun openDocument(completion: (String?) -> Unit) {}
        override fun localAvailability(completion: (String) -> Unit) = completion("unavailable")
        override fun generate(prompt: String, cloudAllowed: Boolean, advanced: Boolean, completion: (String?, String?, String?) -> Unit) {
            calls++; this.completion = completion
        }
    }
    @Test fun submitDismissesKeyboardAndKeepsWarningProgressAndFailureByTheButton() {
        val bridge = Bridge()
        var hides = 0
        val keyboard = object : SoftwareKeyboardController {
            override fun show() {}
            override fun hide() { hides++ }
        }
        compose.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) { ParentApp(bridge) }
        }
        val field = compose.onNode(hasSetTextAction() and hasText("What would you like to make easier?"))
        field.performScrollTo().performTextInput("Getting dressed before school")
        compose.onNodeWithText("Find tasks and rewards").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(hides > 0)
        field.assertIsNotFocused()
        compose.onNodeWithText("On-device AI is not ready here.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Find tasks and rewards").assertIsDisplayed()
        assertEquals(0, bridge.calls)
        compose.onNodeWithText("Allow cloud generation for this session").performScrollTo().performClick()
        compose.onNodeWithText("Find tasks and rewards").performScrollTo().performClick()
        compose.waitUntil(5000) { bridge.calls == 1 }
        compose.onNodeWithText("Creating suggestions…").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.runOnIdle { bridge.completion?.invoke(null, null, "synthetic failure") }
        compose.waitForIdle()
        compose.onNodeWithText("Generation failed. Your existing ideas are unchanged.").assertIsDisplayed()
        compose.onNodeWithText("Find tasks and rewards").assertIsDisplayed()
        compose.onNodeWithText("Creating suggestions…").assertDoesNotExist()
    }
}

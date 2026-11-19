package dev.jamescullimore.starjar.routinebuilder

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ParentFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun manualTaskExportsThroughTheNativeShareSheet() {
        compose.onNodeWithText("I am 18 or older · Get started").assertDoesNotExist()
        compose.onNodeWithText("Deutsch").assertDoesNotExist()
        compose.onNodeWithText("English").assertDoesNotExist()
        if (
            compose
                .onAllNodesWithText("Choose what fits your family")
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            compose.onNodeWithText("New ideas").performScrollTo().performClick()
            compose.onNodeWithText("Start new").performClick()
        }
        compose.onNodeWithText("Write my own").performScrollTo().performClick()
        compose.onNodeWithText("Choose what fits your family").assertExists()
        compose
            .onNode(hasSetTextAction() and hasText("Name"))
            .performScrollTo()
            .performTextReplacement("Choose clothes")
        compose.onNodeWithText("Share kept items").performScrollTo().performClick()
        val file = File(compose.activity.cacheDir, "shared-plans/starjar-templates.json")
        compose.waitUntil(5000) { file.exists() }
        val doc = Json.parseToJsonElement(file.readText()).jsonObject
        assertEquals("starjar-templates", doc.getValue("format").jsonPrimitive.content)
        assertEquals(
            "Choose clothes",
            doc.getValue("tasks")
                .jsonArray
                .single()
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content,
        )
        assertFalse(file.readText().contains("originalName"))
    }

    @Test
    fun deviceLanguageSelectsGermanAndFallsBackToEnglish() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val manager = compose.activity.getSystemService(android.app.LocaleManager::class.java)
        val original = manager.applicationLocales
        try {
            for ((tag, text) in listOf("de-DE" to "Neue Ideen", "fr-FR" to "New ideas")) {
                compose.runOnUiThread {
                    manager.applicationLocales = android.os.LocaleList.forLanguageTags(tag)
                }
                compose.waitUntil(10000) {
                    // Locale changes recreate the Activity; the Compose tree is briefly absent.
                    runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
                        .getOrDefault(false)
                }
                compose.onNodeWithText("Deutsch").assertDoesNotExist()
                compose.onNodeWithText("English").assertDoesNotExist()
                compose.onNodeWithText("I am 18 or older · Get started").assertDoesNotExist()
            }
        } finally {
            compose.runOnUiThread { manager.applicationLocales = original }
        }
    }

    @Test
    fun labelledToggleAndTaskActionsExposeAccessibleStateAndTargets() {
        compose.onNodeWithText("New ideas").performScrollTo().performClick()
        if (compose.onAllNodesWithText("Start new").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Start new").performClick()
        compose.onNodeWithText("For parents aged 18+").assertDoesNotExist()
        val toggle = compose.onNodeWithText("No purchases")
        toggle
            .performScrollTo()
            .assertIsToggleable()
            .assertIsOff()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
            .assertIsOn()
        compose.onAllNodes(hasText("No purchases") and isToggleable()).assertCountEquals(1)
        compose
            .onNodeWithText("Find what works for your child.")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Write my own").performScrollTo().performClick()
        compose
            .onNode(hasSetTextAction() and hasText("Name"))
            .performScrollTo()
            .performTextReplacement("Choose clothes")
        compose
            .onNodeWithContentDescription("Keep: Choose clothes")
            .performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Discard: Choose clothes").assertHeightIsAtLeast(48.dp)
        val stars = compose.onNode(hasSetTextAction() and hasText("Stars"))
        stars.performScrollTo().performTextReplacement("0")
        stars.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithText("Enter a whole number between 1 and 1000000.").assertExists()
        stars.performTextReplacement("5")
        stars.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose.onNodeWithText("Choose icon").performScrollTo().performClick()
        compose
            .onNodeWithContentDescription("Account Circle")
            .assertIsToggleable()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText("Close").performClick()
    }
}

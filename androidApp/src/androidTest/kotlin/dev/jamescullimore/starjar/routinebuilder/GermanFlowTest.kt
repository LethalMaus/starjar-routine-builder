package dev.jamescullimore.starjar.routinebuilder
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jamescullimore.starjar.routinebuilder.core.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with the app locale set to de; also exercised with emulator networking disabled. */
@RunWith(AndroidJUnit4::class)
class GermanFlowTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun germanManualSaveReopenAndExport() {
  val allowance=CloudAllowance(File(compose.activity.noBackupFilesDir,"cloud-usage.json"))
  repeat(allowance.status().remaining) { allowance.reserve() }
  assertEquals(0,allowance.status().remaining)
  compose.waitForIdle()
  if(compose.onAllNodesWithTag("adultContinue").fetchSemanticsNodes().isNotEmpty())compose.onNodeWithTag("adultContinue").performScrollTo().performClick()
  compose.onNodeWithTag("newRoutine").performScrollTo().assertTextContains("Routine erstellen").performClick()
  compose.onNodeWithTag("manualCreate").performScrollTo().assertTextContains("Routine selbst schreiben").performClick()
  compose.onNodeWithTag("routineTitle").performScrollTo().performTextReplacement("Deutscher Abendtest")
  listOf("Spielsachen aufräumen","Schlafanzug anziehen","Ein Buch lesen").forEachIndexed { i,text->compose.onNodeWithTag("task-$i-name").performScrollTo().performTextReplacement(text) }
  compose.onNodeWithTag("reward-name").performScrollTo().performTextReplacement("Ein Brettspiel aussuchen")
  compose.onNodeWithTag("saveRoutine").performScrollTo().assertTextContains("Routine speichern").performClick()
  val repository=FileRoutineRepository(File(compose.activity.noBackupFilesDir,"routines"))
  compose.waitUntil(5000) { repository.load().any { it.title=="Deutscher Abendtest" } }
  val routine=repository.load().first { it.title=="Deutscher Abendtest" };assertEquals("de",routine.language)
  compose.activityRule.scenario.recreate();compose.waitForIdle();compose.onNodeWithTag("home").performClick()
  compose.onNodeWithTag("openRoutine-${routine.id}").performScrollTo().assertTextContains("Routine öffnen").performClick()
  compose.onNodeWithTag("task-0-name").performScrollTo().assertTextContains("Spielsachen aufräumen")
  compose.onNodeWithTag("exportRoutine").performScrollTo().assertTextContains("JSON-Datei speichern")
  val output=File(compose.activity.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
  File(output,"german-ui-export.json").writeText(TemplateExporter.encode(routine))
 }
}

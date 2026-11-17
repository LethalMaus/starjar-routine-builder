package dev.jamescullimore.starjar.routinebuilder
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dev.jamescullimore.starjar.routinebuilder.core.FileRoutineRepository
import dev.jamescullimore.starjar.routinebuilder.core.TemplateExporter
import java.io.File
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class RoutineFlowTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @Test fun manualEditsSurviveRecreationAndExportBothLanguages() {
  compose.waitForIdle()
  if(compose.onAllNodesWithTag("adultContinue").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("adultContinue").performScrollTo().performClick()
  compose.onNodeWithTag("manualHome").performScrollTo().performClick()
  compose.onNodeWithTag("routineTitle").performScrollTo().performTextReplacement("School morning")
  listOf("Brush teeth","Pack schoolbag","Put shoes on").forEachIndexed { i,name->
   compose.onNodeWithTag("task-$i-name").performScrollTo().performTextReplacement(name)
  }
  compose.onNodeWithTag("task-0-points").performScrollTo().performTextReplacement("7")
  compose.onNodeWithTag("reward-name").performScrollTo().performTextReplacement("Choose a family game")
  compose.onNodeWithTag("saveRoutine").performScrollTo().performClick()
  compose.waitUntil(8000) { FileRoutineRepository(File(compose.activity.noBackupFilesDir,"routines")).load().any { it.title=="School morning" } }
  compose.activityRule.scenario.recreate()
  compose.waitForIdle()
  compose.onNodeWithTag("home").performClick()
  compose.onNodeWithTag("newRoutine").assertExists()
  compose.onNodeWithText("Open routine").performScrollTo().performClick()
  compose.onNodeWithTag("task-0-name").performScrollTo().assertTextContains("Brush teeth")
  compose.onNodeWithTag("task-0-points").performScrollTo().assertTextContains("7")
  val stored=FileRoutineRepository(File(compose.activity.noBackupFilesDir,"routines")).load().first { it.title=="School morning" }
  assertEquals(7,stored.tasks.first().pointAmount)
  val output=File(compose.activity.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
  File(output,"export-en.json").writeText(TemplateExporter.encode(stored))
  val german=stored.copy(title="Schulmorgen",language="de",tasks=stored.tasks.mapIndexed { i,t->t.copy(name=listOf("Zähne putzen","Schultasche packen","Schuhe anziehen")[i]) },reward=stored.reward?.copy(name="Ein Familienspiel aussuchen"))
  File(output,"export-de.json").writeText(TemplateExporter.encode(german))
  assertFalse(TemplateExporter.encode(german).contains("School morning"))
 }
}

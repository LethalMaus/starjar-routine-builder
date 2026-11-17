package dev.jamescullimore.starjar.routinebuilder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jamescullimore.starjar.routinebuilder.core.ReviewedRoutine
import dev.jamescullimore.starjar.routinebuilder.core.RoutineItem

/** Hand-authored synthetic UI fixtures. These are not model inference evidence. */
fun previewState(page: BuilderPage, language: String = "en"): BuilderUiState {
    val de = language == "de"
    val title = if (de) "Unser Schulmorgen" else "Our school morning"
    val tasks = if (de) listOf("Zähne putzen", "Schulranzen an die Tür stellen", "Schuhe anziehen") else listOf("Brush your teeth", "Put your schoolbag by the door", "Put your shoes on")
    val reward = if (de) "Ein Spiel für den Familienabend aussuchen" else "Choose a family board game"
    return BuilderUiState(
        page = page,
        uiLanguage = language,
        outputLanguage = language,
        goal = if (de) "Eine einfache Schulroutine mit drei kurzen Aufgaben. Ohne Bildschirm und ohne Einkäufe." else "A simple school morning with three short tasks. No screens or purchases.",
        title = title,
        tasks = tasks.map { EditableItem(it, "5") },
        reward = EditableItem(reward, "30"),
        readiness = ModelReadiness.UNAVAILABLE,
        constraints = setOf("no_purchases", "no_screens", "short_tasks"),
        savedRoutines = listOf(ReviewedRoutine("synthetic-morning", title, language, tasks.map { RoutineItem(it, 5) }, RoutineItem(reward, 30))),
    )
}

@Preview(name = "Home EN", widthDp = 411, heightDp = 891, showBackground = true)
@Composable fun RoutineHomeEnPreview() = RoutineBuilderScreen(previewState(BuilderPage.HOME)) {}

@Preview(name = "Home DE", locale = "de", widthDp = 411, heightDp = 891, showBackground = true)
@Composable fun RoutineHomeDePreview() = RoutineBuilderScreen(previewState(BuilderPage.HOME, "de")) {}

@Preview(name = "Create EN", widthDp = 411, heightDp = 1600, showBackground = true)
@Composable fun RoutineCreateEnPreview() = RoutineBuilderScreen(previewState(BuilderPage.CREATE)) {}

@Preview(name = "Create DE", locale = "de", widthDp = 411, heightDp = 1600, showBackground = true)
@Composable fun RoutineCreateDePreview() = RoutineBuilderScreen(previewState(BuilderPage.CREATE, "de")) {}

@Preview(name = "Review EN", widthDp = 411, heightDp = 2300, showBackground = true)
@Composable fun RoutineReviewEnPreview() = RoutineBuilderScreen(previewState(BuilderPage.REVIEW)) {}

@Preview(name = "Review DE", locale = "de", widthDp = 411, heightDp = 2300, showBackground = true)
@Composable fun RoutineReviewDePreview() = RoutineBuilderScreen(previewState(BuilderPage.REVIEW, "de")) {}

@Preview(name = "Help EN", widthDp = 411, heightDp = 1800, showBackground = true)
@Composable fun RoutineHelpEnPreview() = RoutineBuilderScreen(previewState(BuilderPage.HELP)) {}

@Preview(name = "Help DE", locale = "de", widthDp = 411, heightDp = 1800, showBackground = true)
@Composable fun RoutineHelpDePreview() = RoutineBuilderScreen(previewState(BuilderPage.HELP, "de")) {}

@Preview(name = "Home small", widthDp = 320, heightDp = 568, showBackground = true)
@Composable fun RoutineSmallPreview() = RoutineBuilderScreen(previewState(BuilderPage.HOME)) {}

@Preview(name = "Create tablet", widthDp = 840, heightDp = 1180, showBackground = true)
@Composable fun RoutineTabletPreview() = RoutineBuilderScreen(previewState(BuilderPage.CREATE)) {}

@Preview(name = "Review large text", locale = "de", widthDp = 320, heightDp = 2000, fontScale = 2f, showBackground = true)
@Composable fun RoutineLargeTextPreview() = RoutineBuilderScreen(previewState(BuilderPage.REVIEW, "de")) {}

@Composable private fun AllowanceStates(de: Boolean) = RoutineBuilderTheme {
 Surface(Modifier.fillMaxSize()) {
  Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
   CloudAllowanceNotice(10, de)
   CloudAllowanceNotice(0, de)
   CloudAllowanceNotice(null, de)
  }
 }
}
@Preview @Composable fun UsageAllowanceEnPreview() = AllowanceStates(false)
@Preview(locale = "de") @Composable fun UsageAllowanceDePreview() = AllowanceStates(true)

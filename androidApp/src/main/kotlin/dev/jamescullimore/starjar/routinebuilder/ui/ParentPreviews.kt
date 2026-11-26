package dev.jamescullimore.starjar.routinebuilder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamescullimore.starjar.routinebuilder.shared.*

private class PreviewBridge(val hasPlan: Boolean, val language: String = "en") : PlatformBridge {
    override fun newId() = "preview"

    override fun loadDraft() =
        if (hasPlan)
            PlanTransfer.editable(
                ParentPlan(
                    "preview",
                    if (language == "de") "Unser Morgen" else "Our morning",
                    items =
                        listOf(
                            PlanItem(
                                "task",
                                state = "kept",
                                name = if (language == "de") "Kleidung auswählen" else "Choose your clothes",
                                description = if (language == "de") "Wähle eines von zwei gemeinsam bereitgelegten Outfits aus." else "Choose between two outfits laid out together.",
                                icon = "Checkroom",
                            ),
                            PlanItem(
                                "reward",
                                kind = "reward",
                                name = if (language == "de") "Ein Familienspiel aussuchen" else "Choose a family game",
                                icon = "Games",
                                pointAmount = 20,
                            ),
                        ),
                )
            )
        else null

    override fun saveDraft(document: String) {}

    override fun loadLibrary() = "[]"

    override fun saveLibrary(document: String) {}

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

@Preview @Composable fun ParentCreateEnPreview() = ParentApp(PreviewBridge(false))

@Preview
@Composable
fun ParentCreateDePreview() = ParentApp(PreviewBridge(false), initialLanguage = "de")

@Preview @Composable fun ParentChoicesEnPreview() = ParentApp(PreviewBridge(true))

@Preview
@Composable
fun ParentChoicesDePreview() = ParentApp(PreviewBridge(true, "de"), initialLanguage = "de")

@Preview @Composable fun ParentControlsEnPreview() = ParentControlsPreview("en")

@Preview @Composable fun ParentControlsDePreview() = ParentControlsPreview("de")

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ParentControlsPreview(language: String) {
    dev.jamescullimore.starjar.routinebuilder.shared.RoutineBuilderTheme {
        androidx.compose.material3.Surface(androidx.compose.ui.Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement =
                        androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                ) {
                    for (label in
                        if (language == "de")
                            listOf("Neue Ideen", "Gespeicherte Pläne", "Datei öffnen")
                        else listOf("New ideas", "Saved plans", "Open file")) {
                        ParentOutlinedButton(onClick = {}) {
                            androidx.compose.material3.Text(label)
                        }
                    }
                }
                Toggle(
                    if (language == "de") "Bestätigung durch Eltern erforderlich"
                    else "Parent approval required",
                    true,
                    {},
                )
                NumberField(if (language == "de") "Sterne" else "Stars", 5, {}, minimum = 1)
                NativeItemCard(
                    PlanItem(
                        "preview-card",
                        state = "kept",
                        name =
                            if (language == "de") "Kleidung auswählen" else "Choose your clothes",
                    ),
                    language,
                    true,
                    {},
                    {},
                )
            }
        }
    }
}

@Preview @Composable fun ParentLibraryEnPreview() = ParentLibraryPreview("en")

@Preview @Composable fun ParentLibraryDePreview() = ParentLibraryPreview("de")

@Composable
private fun ParentLibraryPreview(language: String) {
    RoutineBuilderTheme {
        androidx.compose.material3.Surface(androidx.compose.ui.Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Text(
                    if (language == "de") "Gespeicherte Pläne" else "Saved plans",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                )
                SavedPlanCard(
                    ParentPlan(
                        "morning",
                        if (language == "de") "Unser entspannter Start in den Tag"
                        else "Our calmer start to the morning",
                    ),
                    language,
                    {},
                    {},
                )
                SavedPlanCard(
                    ParentPlan(
                        "evening",
                        if (language == "de") "Gemeinsam den Abend ausklingen lassen"
                        else "Winding down together in the evening",
                    ),
                    language,
                    {},
                    {},
                )
            }
        }
    }
}

@Preview @Composable fun ParentAccessEnPreview() = ParentAccessPreview("en")

@Preview @Composable fun ParentAccessDePreview() = ParentAccessPreview("de")

@Composable
private fun ParentAccessPreview(language: String) {
    RoutineBuilderTheme {
        androidx.compose.material3.Surface(androidx.compose.ui.Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                CloudAccessNotice(true, language)
                Toggle(
                    if (language == "de") "Cloud-Vorschläge für diese Sitzung erlauben"
                    else "Allow cloud generation for this session",
                    false,
                    {},
                )
                CloudAccessNotice(false, language)
            }
        }
    }
}

@Preview @Composable fun ParentGenerationEnPreview() = GenerationPreview("en")
@Preview @Composable fun ParentGenerationDePreview() = GenerationPreview("de")
@Composable private fun GenerationPreview(language: String) {
    RoutineBuilderTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) {
                GenerationActions(false, if (language == "de") "Die lokale KI ist hier nicht bereit. Aktiviere oben ‘Cloud-Generierung für diese Sitzung erlauben’ oder wähle ‘Selbst schreiben’." else "On-device AI is not ready here. Enable ‘Allow cloud generation for this session’ above, or choose ‘Write my own’.", false, language, true, {}, {})
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.padding(12.dp))
                GenerationActions(true, "", false, language, true, {}, {})
            }
        }
    }
}

@Preview @Composable fun ReportEnPreview() = MaterialReportPreview("en")
@Preview @Composable fun ReportDePreview() = MaterialReportPreview("de")
@Composable private fun MaterialReportPreview(language: String) {
    RoutineBuilderTheme {
        SuggestionReportDialog(
            PlanItem("preview", name = if (language == "de") "Kleidung aussuchen" else "Choose your clothes", description = if (language == "de") "Wähle gemeinsam eines von zwei bereitgelegten Outfits aus." else "Choose between two outfits laid out together."),
            language, PreviewBridge(false), onDismiss = {},
        )
    }
}

@Preview @Composable fun ParentFeaturesEnPreview() = ParentFeaturesPreview("en")
@Preview @Composable fun ParentFeaturesDePreview() = ParentFeaturesPreview("de")
@Composable private fun ParentFeaturesPreview(language: String) {
    dev.jamescullimore.starjar.routinebuilder.shared.RoutineBuilderTheme {
        androidx.compose.material3.Surface {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(20.dp)) {
                dev.jamescullimore.starjar.routinebuilder.shared.FeatureEditor(
                    dev.jamescullimore.starjar.routinebuilder.shared.FeatureChoices(), language, true, initiallyExpanded = true, onChange = {}
                )
            }
        }
    }
}

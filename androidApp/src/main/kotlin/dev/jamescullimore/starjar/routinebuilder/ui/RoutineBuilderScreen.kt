package dev.jamescullimore.starjar.routinebuilder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jamescullimore.starjar.routinebuilder.BuildConfig
import dev.jamescullimore.starjar.routinebuilder.core.GenerationPolicy
import dev.jamescullimore.starjar.routinebuilder.core.GenerationSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineBuilderScreen(state: BuilderUiState, onAction: (BuilderAction) -> Unit) {
    val de = state.uiLanguage == "de"
    fun copy(en: String, german: String) = if (de) german else en
    var cloudDialog by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var replaceAction by remember { mutableStateOf<BuilderAction?>(null) }
    fun guarded(action: BuilderAction) {
        val replacesWithSuggestions = action == BuilderAction.Generate || action == BuilderAction.CloudDiagnostic
        val hasDraft = state.tasks.isNotEmpty() || state.reward != null
        if (state.hasUnsavedChanges || (replacesWithSuggestions && hasDraft)) replaceAction = action
        else onAction(action)
    }
    RoutineBuilderTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.page != BuilderPage.HOME) {
                        IconButton(onClick = { onAction(BuilderAction.Navigate(BuilderPage.HOME)) }, modifier = Modifier.size(48.dp).testTag("home")) {
                            Icon(Icons.Outlined.Home, contentDescription = copy("Home", "Start"))
                        }
                    } else {
                        StarryMark(Modifier.size(44.dp).padding(4.dp))
                    }
                    Text("StarJar\nRoutine Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onAction(BuilderAction.Navigate(BuilderPage.HELP)) }, modifier = Modifier.size(48.dp).testTag("help")) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = copy("Help & privacy", "Hilfe & Datenschutz"))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        state.message?.let { message ->
                            Surface(color = if (state.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(message, color = if (state.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.testTag("message"))
                                    BrandTextButton(onClick = { onAction(BuilderAction.DismissMessage) }) { Text(copy("Dismiss", "Schließen")) }
                                }
                            }
                        }
                        when (state.page) {
                            BuilderPage.HOME -> {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(26.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        StarryMark(Modifier.height(100.dp).align(Alignment.CenterHorizontally))
                                        Text(copy("A little structure.\nRoom for your family.", "Ein bisschen Struktur.\nPlatz für eure Ideen."), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                                        Text(copy("Make a routine that fits your day. Choose the tasks, edit the stars and keep what works for you.", "Stell eine Routine zusammen, die zu eurem Alltag passt. Wähle die Aufgaben und Sterne so, wie es für euch stimmt."))
                                        Text(copy("For parents and adult caregivers", "Für Eltern und erwachsene Bezugspersonen"), style = MaterialTheme.typography.labelLarge)
                                        BrandButton(onClick = { guarded(BuilderAction.NewRoutine) }, modifier = Modifier.fillMaxWidth().testTag("newRoutine")) { Text(copy("Create a routine", "Routine erstellen")) }
                                    }
                                }
                                ReadinessCard(state, onAction)
                                SectionTitle(copy("Your routines", "Deine Routinen"))
                                if (state.savedRoutines.isEmpty()) {
                                    Text(copy("Your saved routines will appear here. You can create one yourself even when AI is unavailable.", "Hier findest du deine gespeicherten Routinen. Du kannst auch ohne KI eine Routine erstellen."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedButton(onClick = { guarded(BuilderAction.ManualRoutine) }, modifier = Modifier.fillMaxWidth().testTag("manualHome")) { Text(copy("Create without AI", "Ohne KI erstellen")) }
                                } else {
                                    state.savedRoutines.forEach { routine ->
                                        ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(routine.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                                Text((if (routine.tasks.size == 1) copy("1 task", "1 Aufgabe") else copy("${routine.tasks.size} tasks", "${routine.tasks.size} Aufgaben")) + (if (routine.reward != null) copy(" · with a reward", " · mit Belohnung") else ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    BrandButton(onClick = { guarded(BuilderAction.OpenSaved(routine.id)) }, modifier = Modifier.testTag("openRoutine-${routine.id}")) { Text(copy("Open routine", "Routine öffnen")) }
                                                    BrandTextButton(onClick = { deleteId = routine.id }) { Text(copy("Delete", "Löschen")) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            BuilderPage.CREATE -> {
                                PageHeading(copy("What would help today?", "Was würde euch heute helfen?"), copy("Describe a routine. You’ll review every suggestion before saving it.", "Beschreibe eine Routine. Du prüfst und bearbeitest die Vorschläge, bevor du sie speicherst."))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("morning" to copy("School morning", "Schulmorgens"), "bedtime" to copy("Getting ready for bed", "Abendroutine"), "home" to copy("Helping at home", "Zu Hause helfen")).forEach { (id, label) ->
                                        SuggestionChip(onClick = { onAction(BuilderAction.UsePreset(id)) }, label = { Text(label) }, enabled = !state.busy)
                                    }
                                }
                                OutlinedTextField(
                                    value = state.goal, onValueChange = { onAction(BuilderAction.SetGoal(it.take(1200))) },
                                    label = { Text(copy("Tell us about the routine", "Beschreibe die Routine")) },
                                    placeholder = { Text(copy("A calmer school morning, with a few short tasks…", "Ein entspannterer Schulmorgen mit ein paar kurzen Aufgaben …")) },
                                    supportingText = { Text(copy("Keep names and personal details out. ", "Bitte keine Namen oder persönlichen Angaben. ") + "${state.goal.length}/1200") },
                                    minLines = 3, modifier = Modifier.fillMaxWidth().testTag("goal"), enabled = !state.busy,
                                )
                                SectionTitle(copy("Suggestion language", "Sprache der Vorschläge"))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("en" to "English", "de" to "Deutsch").forEach { (id, label) ->
                                        FilterChip(selected = state.outputLanguage == id, onClick = { onAction(BuilderAction.SetOutputLanguage(id)) }, label = { Text(label) }, enabled = !state.busy)
                                    }
                                }
                                SectionTitle(copy("How many tasks?", "Wie viele Aufgaben?"))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    (1..3).forEach { count ->
                                        FilterChip(selected = state.taskCount == count, onClick = { onAction(BuilderAction.SetTaskCount(count)) }, label = { Text("$count") }, enabled = !state.busy, modifier = Modifier.testTag("taskCount-$count"))
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text(copy("Include a reward", "Belohnung vorschlagen"), fontWeight = FontWeight.SemiBold); Text(copy("You can edit it or leave it out later.", "Du kannst sie später ändern oder weglassen."), style = MaterialTheme.typography.bodySmall) }
                                    Switch(checked = state.includeReward, onCheckedChange = { onAction(BuilderAction.SetIncludeReward(it)) }, enabled = !state.busy, modifier = Modifier.testTag("includeReward").semantics { contentDescription = copy("Include a reward", "Belohnung vorschlagen") })
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("no_purchases" to copy("No purchases", "Ohne Einkäufe"), "no_screens" to copy("No screens", "Ohne Bildschirm"), "short_tasks" to copy("Short tasks", "Kurze Aufgaben")).forEach { (id, label) ->
                                        FilterChip(selected = id in state.constraints, onClick = { onAction(BuilderAction.ToggleConstraint(id)) }, label = { Text(label) }, enabled = !state.busy)
                                    }
                                }
                                PolicyCard(state, onAction, { cloudDialog = true })
                                CloudAllowanceNotice(state.cloudRemaining, de)
                                if (state.busy) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                    Text(copy("Preparing suggestions…", "Vorschläge werden erstellt …"))
                                    OutlinedButton(onClick = { onAction(BuilderAction.CancelGeneration) }, modifier = Modifier.fillMaxWidth().testTag("cancelGeneration")) { Text(copy("Cancel generation", "Erstellung abbrechen")) }
                                } else {
                                    BrandButton(onClick = { guarded(BuilderAction.Generate) }, enabled = state.goal.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("generate")) { Text(copy("Create suggestions", "Vorschläge erstellen")) }
                                    OutlinedButton(onClick = { guarded(BuilderAction.ManualRoutine) }, modifier = Modifier.fillMaxWidth().testTag("manualCreate")) { Text(copy("Write the routine myself", "Routine selbst schreiben")) }
                                }
                            }
                            BuilderPage.REVIEW -> {
                                PageHeading(copy("Make it your own", "So passt es zu euch"), copy("Edit the wording and choose the stars. Nothing is awarded automatically.", "Passe die Texte an und wähle die Sterne. Es werden keine Sterne automatisch vergeben."))
                                state.source?.let { source ->
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                                        Text(if (source == GenerationSource.ON_DEVICE) copy("Created on this device", "Auf diesem Gerät erstellt") else copy("Created using cloud AI", "Mit Cloud-KI erstellt"), Modifier.padding(12.dp).testTag("generationSource"), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                OutlinedTextField(state.title, { onAction(BuilderAction.SetTitle(it.take(120))) }, label = { Text(copy("Routine title", "Name der Routine")) }, modifier = Modifier.fillMaxWidth().testTag("routineTitle"), singleLine = false)
                                state.tasks.forEachIndexed { index, item ->
                                    EditableCard(copy("Task ${index + 1}", "Aufgabe ${index + 1}"), item, de, "task-$index", { onAction(BuilderAction.SetTaskName(index, it)) }, { onAction(BuilderAction.SetTaskPoints(index, it)) }, { onAction(BuilderAction.RemoveTask(index)) })
                                }
                                if (state.tasks.size < 3) OutlinedButton(onClick = { onAction(BuilderAction.AddTask) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Add a task", "Aufgabe hinzufügen")) }
                                state.reward?.let { item ->
                                    EditableCard(copy("Optional reward", "Freiwillige Belohnung"), item, de, "reward", { onAction(BuilderAction.SetRewardName(it)) }, { onAction(BuilderAction.SetRewardPoints(it)) }, { onAction(BuilderAction.RemoveReward) }, reward = true)
                                } ?: OutlinedButton(onClick = { onAction(BuilderAction.AddReward) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Add a reward", "Belohnung hinzufügen")) }
                                BrandButton(onClick = { onAction(BuilderAction.Save) }, enabled = !state.saving && !state.busy, modifier = Modifier.fillMaxWidth().testTag("saveRoutine")) { Text(if (state.saving) copy("Saving…", "Wird gespeichert …") else copy("Save routine", "Routine speichern")) }
                                Text(copy("Saved on this device. You can reopen your routines without AI or an internet connection.", "Auf diesem Gerät gespeichert. Du kannst deine Routinen auch ohne KI und Internet wieder öffnen."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider()
                                SectionTitle(copy("Use it in StarJar", "In StarJar verwenden"))
                                Text(copy("Export the names and stars as a StarJar template file. Your routine title and AI details stay out of the export.", "Exportiere die Aufgaben, Belohnung und Sterne als StarJar-Vorlage. Der Name der Routine und KI-Angaben werden nicht exportiert."), style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = { onAction(BuilderAction.Export) }, enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("exportRoutine")) { Text(copy("Save JSON file", "JSON-Datei speichern")) }
                                BrandTextButton(onClick = { onAction(BuilderAction.Copy) }, enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("copyRoutine")) { Text(copy("Copy template", "Vorlage kopieren")) }
                                BrandTextButton(onClick = { onAction(BuilderAction.Navigate(BuilderPage.CREATE)) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Back to suggestions", "Zurück zu den Vorschlägen")) }
                            }
                            BuilderPage.HELP -> {
                                PageHeading(copy("Your routine, your choice", "Deine Routine, deine Entscheidung"), copy("A tool for adults to prepare routines. Review suggestions before using them with your family.", "Ein Werkzeug für Erwachsene, um Routinen vorzubereiten. Prüfe Vorschläge, bevor ihr sie im Alltag nutzt."))
                                ReadinessCard(state, onAction)
                                PolicyCard(state, onAction, { cloudDialog = true })
                                CloudAllowanceNotice(state.cloudRemaining, de)
                                SectionTitle(copy("What is saved?", "Was wird gespeichert?"))
                                Text(copy("Your edited routines are stored on this device. Raw requests are not saved with them. You can delete a routine from the home screen.", "Deine bearbeiteten Routinen bleiben auf diesem Gerät. Die ursprünglichen Anfragen werden nicht mitgespeichert. Auf der Startseite kannst du Routinen löschen."))
                                Text(copy("Free to use: 10 cloud-enabled attempts per installation in 24 hours. Confirmed on-device results do not count. Failed or cancelled cloud attempts may already have reached the service and do count. Manual editing, saving and exporting have no usage allowance. Shared service limits may also apply.", "Kostenlos nutzbar: 10 Versuche mit erlaubtem Cloud-Zugriff pro Installation innerhalb von 24 Stunden. Ergebnisse vom Gerät zählen nicht mit. Fehlgeschlagene oder abgebrochene Cloud-Versuche zählen mit, da sie den Dienst bereits erreicht haben können. Routinen selbst zu bearbeiten, zu speichern und zu exportieren bleibt ohne Nutzungslimit. Zusätzlich können gemeinsame Dienstlimits gelten."))
                                SectionTitle(copy("About on-device AI", "Über KI auf dem Gerät"))
                                Text(copy("A compatible device and a downloaded model are needed for local suggestions. Model downloads and service diagnostics may use a network connection. German on-device quality still needs separate evaluation.", "Für lokale Vorschläge brauchst du ein geeignetes Gerät und ein heruntergeladenes Modell. Downloads und Dienstdiagnosen können eine Internetverbindung nutzen. Die Qualität deutscher Vorschläge auf dem Gerät muss noch gesondert geprüft werden."))
                                Text(copy("Cloud access also uses Google’s app verification, which processes app and device signals to help prevent abuse.", "Für den Cloud-Zugriff prüft Google außerdem die App. Dabei werden App- und Gerätesignale verarbeitet, um Missbrauch zu verhindern."))
                                Text(copy("If you turn off cloud access during generation, we cancel the request where possible and ignore late results. Anything already sent to the cloud cannot be recalled.", "Wenn du den Cloud-Zugriff während der Erstellung ausschaltest, brechen wir die Anfrage nach Möglichkeit ab und verwerfen verspätete Ergebnisse. Bereits übermittelte Daten lassen sich nicht zurückrufen."))
                                OutlinedButton(onClick = { guarded(BuilderAction.ManualRoutine) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Create without AI", "Ohne KI erstellen")) }
                                if (BuildConfig.DEBUG) {
                                    HorizontalDivider()
                                    SectionTitle(copy("Developer diagnostics", "Entwicklerdiagnose"))
                                    Text(state.diagnostics.ifBlank { copy("No inference evidence recorded in this session.", "In dieser Sitzung wurde noch keine KI-Ausführung dokumentiert.") }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("diagnostics"))
                                    OutlinedButton(onClick = { guarded(BuilderAction.CloudDiagnostic) }, enabled = state.policy == GenerationPolicy.CLOUD_ALLOWED && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(copy("Run explicit cloud test", "Cloud-Test ausdrücklich starten")) }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        if (cloudDialog) AlertDialog(
            onDismissRequest = { cloudDialog = false },
            title = { Text(copy("Allow cloud suggestions?", "Cloud-Vorschläge erlauben?")) },
            text = { Text(copy("When needed, your current request, selected language and constraints will be sent to Google’s cloud. Saved routines are not included. You can turn this off at any time.", "Bei Bedarf werden deine aktuelle Anfrage, die gewählte Sprache und deine Vorgaben an die Google-Cloud gesendet. Gespeicherte Routinen werden nicht mitgesendet. Du kannst das jederzeit ausschalten.")) },
            confirmButton = { BrandTextButton(onClick = { cloudDialog = false; onAction(BuilderAction.SetPolicy(GenerationPolicy.CLOUD_ALLOWED)) }) { Text(copy("Allow cloud", "Cloud erlauben")) } },
            dismissButton = { BrandTextButton(onClick = { cloudDialog = false }) { Text(copy("Keep on device", "Auf dem Gerät bleiben")) } },
        )
        deleteId?.let { id -> AlertDialog(
            onDismissRequest = { deleteId = null }, title = { Text(copy("Delete this routine?", "Diese Routine löschen?")) },
            text = { Text(copy("This removes the saved routine from this device.", "Die gespeicherte Routine wird von diesem Gerät entfernt.")) },
            confirmButton = { BrandTextButton(onClick = { deleteId = null; onAction(BuilderAction.DeleteSaved(id)) }) { Text(copy("Delete routine", "Routine löschen")) } },
            dismissButton = { BrandTextButton(onClick = { deleteId = null }) { Text(copy("Keep routine", "Routine behalten")) } },
        ) }
        replaceAction?.let { action -> AlertDialog(
            onDismissRequest = { replaceAction = null }, title = { Text(copy("Replace your current draft?", "Aktuellen Entwurf ersetzen?")) },
            text = { Text(if (action == BuilderAction.Generate || action == BuilderAction.CloudDiagnostic) copy("New suggestions replace this draft only if generation succeeds. Save any edits you want to keep first.", "Neue Vorschläge ersetzen diesen Entwurf nur, wenn die Erstellung klappt. Speichere vorher alle Änderungen, die du behalten möchtest.") else if (action is BuilderAction.OpenSaved) copy("Save any changes you want to keep before opening another routine.", "Speichere Änderungen, die du behalten möchtest, bevor du eine andere Routine öffnest.") else copy("Save any changes you want to keep before starting a new routine.", "Speichere Änderungen, die du behalten möchtest, bevor du eine neue Routine beginnst.")) },
            confirmButton = { BrandTextButton(onClick = { replaceAction = null; onAction(action) }) { Text(copy("Continue", "Fortfahren")) } },
            dismissButton = { BrandTextButton(onClick = { replaceAction = null }) { Text(copy("Keep editing", "Weiter bearbeiten")) } },
        ) }
    }
}

@Composable
private fun PageHeading(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })

@Composable
private fun ReadinessCard(state: BuilderUiState, onAction: (BuilderAction) -> Unit) {
    val de = state.uiLanguage == "de"
    fun copy(en: String, german: String) = if (de) german else en
    val label = when (state.readiness) {
        ModelReadiness.CHECKING -> copy("Checking on-device AI…", "KI auf dem Gerät wird geprüft …")
        ModelReadiness.READY -> copy("On-device AI is ready", "KI auf dem Gerät ist bereit")
        ModelReadiness.DOWNLOADABLE -> copy("Download needed for local AI", "Download für lokale KI erforderlich")
        ModelReadiness.DOWNLOADING -> copy("Downloading the local model…", "Das lokale Modell wird heruntergeladen …")
        ModelReadiness.UNAVAILABLE -> copy("On-device suggestions unavailable", "Lokale Vorschläge nicht verfügbar")
        ModelReadiness.CONFIG_REQUIRED -> copy("AI setup is not complete yet", "Die KI-Einrichtung ist noch nicht abgeschlossen")
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("readiness"))
            if (state.readiness != ModelReadiness.READY) Text(copy("You can still write, save and export a routine yourself.", "Du kannst trotzdem selbst eine Routine schreiben, speichern und exportieren."), style = MaterialTheme.typography.bodySmall)
            when (state.readiness) {
                ModelReadiness.DOWNLOADABLE -> BrandButton(onClick = { onAction(BuilderAction.DownloadModel) }) { Text(copy("Download model", "Modell herunterladen")) }
                ModelReadiness.DOWNLOADING -> state.downloadProgress?.let { progress -> LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                ModelReadiness.CHECKING -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> BrandTextButton(onClick = { onAction(BuilderAction.RefreshReadiness) }) { Text(copy("Check availability", "Verfügbarkeit prüfen")) }
            }
        }
    }
}

@Composable
private fun PolicyCard(state: BuilderUiState, onAction: (BuilderAction) -> Unit, requestCloud: () -> Unit) {
    val de = state.uiLanguage == "de"
    fun copy(en: String, german: String) = if (de) german else en
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(copy("Where suggestions are made", "Wo die Vorschläge entstehen"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.policy == GenerationPolicy.LOCAL_ONLY, onClick = { onAction(BuilderAction.SetPolicy(GenerationPolicy.LOCAL_ONLY)) }, modifier = Modifier.testTag("localOnly").semantics { contentDescription = copy("On this device only", "Nur auf diesem Gerät") })
                Text(copy("On this device only", "Nur auf diesem Gerät"), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.policy == GenerationPolicy.CLOUD_ALLOWED, onClick = requestCloud, modifier = Modifier.testTag("allowCloud").semantics { contentDescription = copy("Allow cloud when needed", "Cloud bei Bedarf erlauben") })
                Text(copy("Allow cloud when needed", "Cloud bei Bedarf erlauben"), modifier = Modifier.weight(1f))
            }
            Text(copy("Only your current request is used for suggestions. Your saved routines stay out of AI requests.", "Für Vorschläge wird nur deine aktuelle Anfrage verwendet. Gespeicherte Routinen werden nicht an die KI übermittelt."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditableCard(title: String, item: EditableItem, de: Boolean, tag: String, setName: (String) -> Unit, setPoints: (String) -> Unit, remove: () -> Unit, reward: Boolean = false) {
    fun copy(en: String, german: String) = if (de) german else en
    Surface(color = if (reward) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(item.name, { setName(it.take(120)) }, label = { Text(copy("Name", "Name")) }, modifier = Modifier.fillMaxWidth().testTag("$tag-name"))
            OutlinedTextField(item.points, { setPoints(it.take(12)) }, label = { Text(if (reward) copy("Star cost", "Benötigte Sterne") else copy("Stars", "Sterne")) }, supportingText = { Text(copy("Choose a whole number from 1 to 1,000,000.", "Wähle eine ganze Zahl von 1 bis 1.000.000.")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("$tag-points"))
            BrandTextButton(onClick = remove, modifier = Modifier.align(Alignment.End)) { Text(if (reward) copy("Remove reward", "Belohnung entfernen") else copy("Remove task", "Aufgabe entfernen")) }
        }
    }
}

@Composable
internal fun CloudAllowanceNotice(remaining: Int?, de: Boolean) {
 fun copy(en: String, german: String) = if (de) german else en
 Text(if (remaining != null) copy("Cloud attempts available: ${remaining}/10. Each attempt renews after 24 hours.", "Verfügbare Cloud-Versuche: ${remaining}/10. Jeder Versuch wird nach 24 Stunden wieder freigegeben.") else copy("Cloud allowance unavailable. On-device and manual routines still work.", "Das Cloud-Kontingent ist gerade nicht verfügbar. Vorschläge auf dem Gerät und eigene Routinen sind weiterhin möglich."), modifier = Modifier.testTag("cloudAllowance"))
}

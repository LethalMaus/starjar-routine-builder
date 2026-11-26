package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jamescullimore.starjar.routinebuilder.shared.resources.Res
import dev.jamescullimore.starjar.routinebuilder.shared.resources.starry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentApp(
    bridge: PlatformBridge,
    incomingDocument: String? = null,
    consumeDocument: () -> Unit = {},
    initialLanguage: String = "en",
    judgingAccess: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val generationAnchor = remember { BringIntoViewRequester() }
    var generationMessage by remember { mutableStateOf("") }
    val language = appLanguage(initialLanguage)
    val initialDraft = remember {
        runCatching {
            bridge
                .loadDraft()
                ?.takeIf { it.isNotBlank() }
                ?.let { PlanTransfer.read(it, bridge::newId) }
        }
    }
    var draftProtected by remember { mutableStateOf(initialDraft.isFailure) }
    var plan by remember { mutableStateOf(initialDraft.getOrNull()) }
    val scroll = rememberScrollState()
    var goal by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("unspecified") }
    var minutes by remember { mutableStateOf("") }
    var features by remember { mutableStateOf(FeatureChoices()) }
    var constraints by remember { mutableStateOf(emptyList<String>()) }
    var cloud by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf("") }
    var localStatus by remember { mutableStateOf("checking") }
    var busy by remember { mutableStateOf(false) }
    var ticket by remember { mutableIntStateOf(0) }
    var confirmFresh by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(false) }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var deletePlan by remember { mutableStateOf<ParentPlan?>(null) }
    var reportItem by remember { mutableStateOf<PlanItem?>(null) }
    var incoming by remember { mutableStateOf<ParentPlan?>(null) }
    LaunchedEffect(plan?.id, library) { scroll.scrollTo(0) }
    fun t(en: String, de: String) = if (language == "de") de else en
    LaunchedEffect(bridge) {
        bridge.localAvailability { result -> scope.launch { localStatus = result } }
    }
    LaunchedEffect(incomingDocument) {
        if (incomingDocument != null) {
            runCatching { PlanTransfer.read(incomingDocument, bridge::newId, language, t("Imported ideas", "Importierte Ideen")) }
                .onSuccess { incoming = it }
                .onFailure { status = t("Invalid plan file.", "Ungültige Plandatei.") }
            consumeDocument()
        }
    }
    fun persist(p: ParentPlan) {
        plan = p
        if (draftProtected) return
        runCatching { bridge.saveDraft(PlanTransfer.editable(p)) }
            .onFailure {
                status =
                    t(
                        "Check the edited values before saving.",
                        "Prüfe die geänderten Angaben vor dem Speichern.",
                    )
            }
    }
    fun fresh() {
        runCatching { bridge.saveDraft("") }
            .onSuccess {
                draftProtected = false
                plan = null
                goal = ""
                feedback = ""
                status = ""
                receipt = ""
                library = false
            }
            .onFailure {
                status =
                    t(
                        "Could not start a new draft. Your existing data is unchanged.",
                        "Ein neuer Entwurf konnte nicht angelegt werden. Deine bisherigen Daten bleiben erhalten.",
                    )
            }
    }
    fun update(item: PlanItem) {
        plan?.let { p ->
            persist(p.copy(items = p.items.map { if (it.id == item.id) item else it }))
        }
    }
    LaunchedEffect(generationMessage, busy) {
        if (generationMessage.isNotBlank() || busy) generationAnchor.bringIntoView()
    }
    fun generate(more: Boolean) {
        if (busy) return
        keyboard?.hide()
        focus.clearFocus()
        generationMessage = ""
        status = ""
        val current = plan ?: ParentPlan(bridge.newId(), t("My plan", "Unser Plan"), language)
        val request = runCatching {
            PlanEngine.request(
                current,
                goal.ifBlank { feedback },
                if (more) feedback else "",
                age,
                minutes,
                features,
                constraints,
            )
        }
            .getOrElse {
                generationMessage =
                    t(
                        "Check your description and options.",
                        "Prüfe deine Beschreibung und Einstellungen.",
                    )
                return
            }
        val advanced =
            features.badgeCount > 0 || features.streaks || features.reminders || features.urgency
        if (advanced && !cloud) {
            generationMessage =
                t(
                    "Allow cloud generation for these extra task settings, or add them manually.",
                    "Erlaube die Cloud für diese zusätzlichen Angaben oder ergänze sie selbst.",
                )
            return
        }
        if (!cloud && localStatus != "available") {
            generationMessage = t(
                "On-device AI is not ready here. Enable ‘Allow cloud generation for this session’ above, or choose ‘Write my own’.",
                "Die lokale KI ist hier nicht bereit. Aktiviere oben ‘Cloud-Generierung für diese Sitzung erlauben’ oder wähle ‘Selbst schreiben’.",
            )
            return
        }
        busy = true
        val active = ++ticket
        scope.launch {
            bridge.generate(
                PlanEngine.generationPrompt(request, advanced),
                cloud,
                advanced,
            ) { text, source, error ->
                scope.launch {
                    if (active != ticket) return@launch
                    busy = false
                    if (
                        error != null || text == null || source !in listOf("ON_DEVICE", "IN_CLOUD")
                    ) {
                        generationMessage =
                            t(
                                "Generation failed. Your existing ideas are unchanged.",
                                "Die Erstellung ist fehlgeschlagen. Deine bisherigen Ideen bleiben erhalten.",
                            )
                    } else
                        runCatching { PlanEngine.merge(current, text, features, bridge::newId) }
                            .onSuccess {
                                persist(it)
                                receipt =
                                    if (source == "ON_DEVICE")
                                        t("Generated on this device", "Auf diesem Gerät erstellt")
                                    else t("Generated in the cloud", "In der Cloud erstellt")
                                feedback = ""
                                generationMessage = ""
                                status =
                                    t(
                                        "Review the new ideas. Existing choices are unchanged.",
                                        "Prüfe die neuen Ideen. Deine bisherige Auswahl bleibt erhalten.",
                                    )
                            }
                            .onFailure {
                                generationMessage =
                                    t(
                                        "The response contained invalid details. Your edits are unchanged.",
                                        "Die Antwort enthält ungültige Angaben. Deine Änderungen bleiben erhalten.",
                                    )
                            }
                }
            }
        }
    }
    RoutineBuilderTheme {
        CompositionLocalProvider(LocalPlanLanguage provides language) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    // safeDrawing already includes the keyboard inset; apply it only once.
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(scroll)
                        .padding(20.dp)
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(Res.drawable.starry), null, Modifier.size(48.dp))
                        Text(
                            "StarJar\nRoutine Builder",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ParentOutlinedButton(
                            onClick = {
                                if (plan != null || draftProtected) confirmFresh = true else fresh()
                            },
                            enabled = !busy,
                        ) {
                            Text(t("New ideas", "Neue Ideen"))
                        }
                        ParentOutlinedButton(onClick = { library = !library }, enabled = !busy) {
                            Text(t("Saved plans", "Gespeicherte Pläne"))
                        }
                        ParentOutlinedButton(
                            onClick = {
                                bridge.openDocument { raw ->
                                    scope.launch {
                                        if (raw != null)
                                            runCatching {
                                                PlanTransfer.read(
                                                    raw,
                                                    bridge::newId,
                                                    language,
                                                    t("Imported ideas", "Importierte Ideen"),
                                                )
                                            }
                                                .onSuccess { incoming = it }
                                                .onFailure {
                                                    status =
                                                        t(
                                                            "Invalid plan file.",
                                                            "Ungültige Plandatei.",
                                                        )
                                                }
                                    }
                                }
                            },
                            enabled = !busy,
                        ) {
                            Text(t("Open file", "Datei öffnen"))
                        }
                    }
                    if (draftProtected)
                        Text(
                            t(
                                "The saved draft could not be read. Automatic saving is paused to protect it. You can edit and export ideas here, or choose New ideas to replace the unreadable draft.",
                                "Der gespeicherte Entwurf kann nicht gelesen werden. Damit er erhalten bleibt, ist das automatische Speichern pausiert. Du kannst hier Ideen bearbeiten und exportieren oder mit Neue Ideen den unlesbaren Entwurf ersetzen.",
                            ),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                        )
                    if (status.isNotBlank())
                        Text(
                            status,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    if (library) {
                        Text(
                            t("Saved plans", "Gespeicherte Pläne"),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        val saved =
                            remember(libraryRevision) {
                                runCatching { PlanLibrary.read(bridge.loadLibrary()) }
                            }
                        if (saved.isFailure) {
                            Text(
                                t(
                                    "Saved plans could not be read. They have not been changed. You can still edit and export your current plan.",
                                    "Gespeicherte Pläne können nicht gelesen werden. Sie wurden nicht verändert. Deinen aktuellen Plan kannst du weiterhin bearbeiten und exportieren.",
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                            ParentOutlinedButton(onClick = { libraryRevision++ }) {
                                Text(t("Try again", "Erneut versuchen"))
                            }
                        } else {
                            if (saved.getOrThrow().isEmpty())
                                Text(t("No saved plans yet.", "Noch keine gespeicherten Pläne."))
                            saved.getOrThrow().forEach { savedPlan ->
                                SavedPlanCard(
                                    savedPlan,
                                    language,
                                    onOpen = { incoming = savedPlan },
                                    onDelete = { deletePlan = savedPlan },
                                )
                            }
                        }
                        return@Column
                    }
                    val p = plan
                    if (p == null) {
                        Text(
                            t(
                                "Find what works for your child.",
                                "Finde heraus, was deinem Kind hilft.",
                            ),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            t(
                                "Describe the difficult part. Keep, edit or discard the ideas that follow.",
                                "Beschreibe, was schwerfällt. Behalte, ändere oder verwirf die vorgeschlagenen Ideen.",
                            )
                        )
                        ParentTextField(
                            goal,
                            { goal = it },
                            Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    t(
                                        "What would you like to make easier?",
                                        "Was möchtest du leichter machen?",
                                    )
                                )
                            },
                            minLines = 3,
                            enabled = !busy,
                        )
                        Text(
                            t(
                                "Leave out names, schools and other personal details.",
                                "Lass Namen, Schulen und andere persönliche Angaben weg.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Choice(
                            t("Approximate age", "Altersgruppe"),
                            age,
                            listOf("unspecified", "preschool", "primary", "teen"),
                            { age = it },
                            !busy,
                        )
                        ParentTextField(
                            minutes,
                            { minutes = it.filter(Char::isDigit) },
                            Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    t(
                                        "Minutes available (optional)",
                                        "Verfügbare Minuten (optional)",
                                    )
                                )
                            },
                            enabled = !busy,
                        )
                        Toggle(
                            t("No purchases", "Ohne Einkäufe"),
                            "no_purchases" in constraints,
                            { v ->
                                constraints =
                                    if (v) constraints + "no_purchases"
                                    else constraints - "no_purchases"
                            },
                            !busy,
                        )
                        Text(
                            t(
                                "Suggest rewards using things you already have, without buying anything.",
                                "Belohnungen mit Dingen vorschlagen, die ihr schon habt – ohne etwas zu kaufen.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Toggle(
                            t("No screens", "Ohne Bildschirm"),
                            "no_screens" in constraints,
                            { v ->
                                constraints =
                                    if (v) constraints + "no_screens"
                                    else constraints - "no_screens"
                            },
                            !busy,
                        )
                        Text(
                            t(
                                "No TV, phones, tablets or video games as rewards.",
                                "Keine Belohnungen mit Fernseher, Handy, Tablet oder Videospielen.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            t("Choose what fits your family", "Wähle, was zu euch passt"),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        if (receipt.isNotBlank()) Text(receipt)
                        ParentTextField(
                            p.title,
                            { persist(p.copy(title = it.take(120))) },
                            Modifier.fillMaxWidth(),
                            label = { Text(t("Plan title", "Name des Plans")) },
                            enabled = !busy,
                        )
                        for (kind in listOf("task", "reward")) {
                            Text(
                                if (kind == "task") t("Task ideas", "Aufgabenideen")
                                else t("Reward ideas", "Belohnungsideen"),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            p.items
                                .filter { it.kind == kind }
                                .forEach { item ->
                                    key(item.id) {
                                        NativeItemCard(
                                            item,
                                            language,
                                            !busy,
                                            ::update,
                                            { persist(PlanEngine.discard(p, item.id)) },
                                            report = if (bridge.supportsReporting()) ({ reportItem = item }) else null,
                                        )
                                    }
                                }
                            ParentOutlinedButton(
                                onClick = {
                                    if (p.items.size < 40)
                                        persist(
                                            p.copy(
                                                items =
                                                    p.items +
                                                        PlanItem(
                                                            bridge.newId(),
                                                            kind,
                                                            "kept",
                                                            icon =
                                                                if (kind == "task") "Check"
                                                                else "Redeem",
                                                            pointAmount =
                                                                if (kind == "task") 5 else 20,
                                                        )
                                            )
                                        )
                                },
                                enabled = !busy,
                            ) {
                                Text(
                                    if (kind == "task")
                                        t("Add my own task", "Eigene Aufgabe hinzufügen")
                                    else t("Add my own reward", "Eigene Belohnung hinzufügen")
                                )
                            }
                        }
                        Text(
                            t(
                                "${p.items.count{it.state=="kept"}} ${if (p.items.count{it.state=="kept"} == 1) "item" else "items"} kept",
                                "${p.items.count{it.state=="kept"}} ${if (p.items.count{it.state=="kept"} == 1) "Eintrag" else "Einträge"} ausgewählt",
                            )
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ParentButton(
                                onClick = {
                                    runCatching {
                                        bridge.saveLibrary(
                                            PlanLibrary.save(bridge.loadLibrary(), p)
                                        )
                                        libraryRevision++
                                    }
                                        .onSuccess {
                                            status =
                                                t(
                                                    "Saved on this device.",
                                                    "Auf diesem Gerät gespeichert.",
                                                )
                                        }
                                        .onFailure {
                                            status =
                                                t(
                                                    "Could not save the plan. Check its details and saved plans; existing saved plans have not been replaced.",
                                                    "Der Plan konnte nicht gespeichert werden. Prüfe seine Angaben und die gespeicherten Pläne; bestehende Pläne wurden nicht ersetzt.",
                                                )
                                        }
                                },
                                enabled = !busy,
                            ) {
                                Text(t("Save ideas", "Ideen speichern"))
                            }
                            ParentOutlinedButton(
                                onClick = {
                                    runCatching {
                                        bridge.share(
                                            PlanTransfer.templates(p),
                                            "starjar-templates.json",
                                        )
                                    }
                                        .onFailure {
                                            status =
                                                t(
                                                    "Keep valid items before exporting.",
                                                    "Wähle vor dem Export gültige Einträge aus.",
                                                )
                                        }
                                },
                                enabled = !busy,
                            ) {
                                Text(t("Share kept items", "Auswahl teilen"))
                            }
                            ParentOutlinedButton(
                                onClick = {
                                    runCatching {
                                        bridge.share(
                                            PlanTransfer.editable(p),
                                            "starjar-ideas-plan.json",
                                        )
                                    }
                                        .onFailure {
                                            status =
                                                t("Check your edits.", "Prüfe deine Änderungen.")
                                        }
                                },
                                enabled = !busy,
                            ) {
                                Text(t("Share editable plan", "Bearbeitbaren Plan teilen"))
                            }
                        }
                        ParentTextField(
                            feedback,
                            { feedback = it.take(600) },
                            Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    t(
                                        "More context or alternatives",
                                        "Mehr Informationen oder Alternativen",
                                    )
                                )
                            },
                            minLines = 2,
                            enabled = !busy,
                        )
                    }
                    Text(
                        when (localStatus) {
                            "available" ->
                                t(
                                    "On-device generation is available.",
                                    "Vorschläge können auf diesem Gerät erstellt werden.",
                                )
                            "checking" ->
                                t(
                                    "Checking on-device availability…",
                                    "Verfügbarkeit auf diesem Gerät wird geprüft…",
                                )
                            else ->
                                t(
                                    "On-device generation is unavailable here. You can allow cloud access or write your own ideas.",
                                    "Auf diesem Gerät sind lokale KI-Vorschläge nicht verfügbar. Du kannst die Cloud erlauben oder eigene Ideen schreiben.",
                                )
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    CloudAccessNotice(judgingAccess, language)
                    FeatureEditor(features, language, !busy) { features = it }
                    Toggle(
                        t(
                            "Allow cloud generation for this session",
                            "Cloud-Generierung für diese Sitzung erlauben",
                        ),
                        cloud,
                        { cloud = it; generationMessage = "" },
                        !busy,
                    )
                    Text(
                        t(
                            "Extra AI task settings currently require cloud access. Nothing is added to StarJar automatically.",
                            "Zusätzliche KI-Aufgabenangaben benötigen derzeit Cloud-Zugriff. In StarJar wird nichts automatisch erstellt.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    GenerationActions(
                        busy = busy,
                        message = generationMessage,
                        more = p != null,
                        language = language,
                        enabled = if (p == null) goal.isNotBlank() else feedback.isNotBlank(),
                        modifier = Modifier.bringIntoViewRequester(generationAnchor),
                        onGenerate = { generate(p != null) },
                        onCancel = { ticket++; busy = false; generationMessage = "" },
                    )
                    if (p == null)
                        ParentOutlinedButton(
                            onClick = {
                                persist(
                                    ParentPlan(
                                        bridge.newId(),
                                        t("My plan", "Unser Plan"),
                                        language,
                                        listOf(PlanItem(bridge.newId(), state = "kept")),
                                    )
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(t("Write my own", "Selbst schreiben"))
                        }
                }
            }
        }
        reportItem?.let { item ->
            SuggestionReportDialog(item, language, bridge, onDismiss = { reportItem = null })
        }
        if (confirmFresh)
            AlertDialog(
                onDismissRequest = { confirmFresh = false },
                title = { Text(t("Start a new plan?", "Neuen Plan beginnen?")) },
                text = {
                    Text(
                        t(
                            if (draftProtected)
                                "This replaces the unreadable draft on this device. Saved plans are unaffected. Export any current edits first."
                            else
                                "Save your current ideas first if you want to keep them. This replaces the draft on this device.",
                            if (draftProtected)
                                "Der unlesbare Entwurf auf diesem Gerät wird ersetzt. Gespeicherte Pläne bleiben erhalten. Exportiere vorher deine aktuellen Änderungen."
                            else
                                "Speichere deine Ideen zuerst, wenn du sie behalten möchtest. Der Entwurf auf diesem Gerät wird ersetzt.",
                        )
                    )
                },
                confirmButton = {
                    ParentTextButton(
                        onClick = {
                            fresh()
                            confirmFresh = false
                        }
                    ) {
                        Text(t("Start new", "Neu beginnen"))
                    }
                },
                dismissButton = {
                    ParentTextButton(onClick = { confirmFresh = false }) {
                        Text(t("Cancel", "Abbrechen"))
                    }
                },
            )
        deletePlan?.let { target ->
            AlertDialog(
                onDismissRequest = { deletePlan = null },
                title = { Text(t("Delete saved plan?", "Gespeicherten Plan löschen?")) },
                text = {
                    Text(
                        target.title +
                            "\n\n" +
                            t(
                                "This removes only this saved copy. Your current draft and other plans are unchanged.",
                                "Nur diese gespeicherte Kopie wird gelöscht. Dein aktueller Entwurf und andere Pläne bleiben erhalten.",
                            )
                    )
                },
                confirmButton = {
                    ParentTextButton(
                        onClick = {
                            runCatching {
                                bridge.saveLibrary(
                                    PlanLibrary.delete(bridge.loadLibrary(), target.id)
                                )
                            }
                                .onSuccess {
                                    libraryRevision++
                                    status =
                                        t("Saved plan deleted.", "Gespeicherter Plan gelöscht.")
                                }
                                .onFailure {
                                    status =
                                        t(
                                            "Could not delete the plan. Saved plans are unchanged.",
                                            "Der Plan konnte nicht gelöscht werden. Gespeicherte Pläne bleiben unverändert.",
                                        )
                                }
                            deletePlan = null
                        }
                    ) {
                        Text(t("Delete", "Löschen"))
                    }
                },
                dismissButton = {
                    ParentTextButton(onClick = { deletePlan = null }) {
                        Text(t("Cancel", "Abbrechen"))
                    }
                },
            )
        }
        incoming?.let { p ->
            AlertDialog(
                onDismissRequest = { incoming = null },
                title = { Text(t("Open incoming plan?", "Eingehenden Plan öffnen?")) },
                text = {
                    Column {
                        Text(p.title)
                        Text(p.items.take(8).joinToString("\n") { it.name })
                        Text(
                            t(
                                "This replaces the current draft. Nothing is sent to AI or created in StarJar.",
                                "Der aktuelle Entwurf wird ersetzt. Es wird nichts an die KI gesendet oder in StarJar erstellt.",
                            )
                        )
                    }
                },
                confirmButton = {
                    ParentTextButton(
                        onClick = {
                            persist(p)
                            incoming = null
                            library = false
                            goal = ""
                            feedback = ""
                            receipt = ""
                        }
                    ) {
                        Text(t("Open", "Öffnen"))
                    }
                },
                dismissButton = {
                    ParentTextButton(onClick = { incoming = null }) {
                        Text(t("Cancel", "Abbrechen"))
                    }
                },
            )
        }
    }
}

@Composable
fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = value,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(value, null, enabled = enabled)
        Text(label, Modifier.weight(1f))
    }
}

@Composable
fun Choice(
    label: String,
    value: String,
    values: List<String>,
    onChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    val language = LocalPlanLanguage.current
    val expandedLabel = if (language == "de") "Ausgeklappt" else "Expanded"
    val collapsedLabel = if (language == "de") "Eingeklappt" else "Collapsed"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            ParentOutlinedButton(
                onClick = { open = true },
                enabled = enabled,
                modifier =
                    Modifier.semantics {
                        contentDescription = "$label: ${optionLabel(value, language)}"
                        stateDescription = if (open) expandedLabel else collapsedLabel
                    },
            ) {
                Text(optionLabel(value, LocalPlanLanguage.current))
            }
            DropdownMenu(open, { open = false }) {
                values.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(v, LocalPlanLanguage.current)) },
                        onClick = {
                            onChange(v)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
    minimum: Int = 0,
    maximum: Int = 1000000,
) {
    var draft by remember { mutableStateOf(value.toString()) }
    var lastEmitted by remember { mutableIntStateOf(value) }
    LaunchedEffect(value) {
        if (value != lastEmitted) {
            draft = value.toString()
            lastEmitted = value
        }
    }
    val invalid = draft.toIntOrNull()?.let { it !in minimum..maximum } ?: true
    val language = LocalPlanLanguage.current
    ParentTextField(
        draft,
        {
            draft = it
            lastEmitted = it.toIntOrNull() ?: 0
            onChange(lastEmitted)
        },
        Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = invalid,
        supportingText =
            if (invalid)
                ({
                    Text(
                        if (language == "de")
                            "Gib eine ganze Zahl zwischen $minimum und $maximum ein."
                        else "Enter a whole number between $minimum and $maximum."
                    )
                })
            else null,
    )
}

@Composable
fun FeatureEditor(
    value: FeatureChoices,
    language: String,
    enabled: Boolean,
    initiallyExpanded: Boolean = false,
    onChange: (FeatureChoices) -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    fun t(en: String, de: String) = if (language == "de") de else en
    ParentOutlinedButton(
        onClick = { expanded = !expanded },
        enabled = enabled,
        modifier =
            Modifier.semantics {
                stateDescription =
                    if (expanded) t("Expanded", "Ausgeklappt") else t("Collapsed", "Eingeklappt")
            },
    ) {
        Text(t("StarJar options", "StarJar-Einstellungen"))
    }
    if (expanded) {
        NumberField(
            t("Badges per task (0–20)", "Abzeichen pro Aufgabe (0–20)"),
            value.badgeCount,
            { onChange(value.copy(badgeCount = it)) },
            enabled,
            maximum = 20,
        )
        Text(t("What else should AI consider in its suggestions?", "Was soll die KI bei ihren Vorschlägen zusätzlich berücksichtigen?"))
        Toggle(
            t("Suggest frequency limits", "Vorschlagen, wie oft etwas pro Tag oder Woche möglich sein soll"),
            value.frequency,
            { onChange(value.copy(frequency = it)) },
            enabled,
        )
        Toggle(
            t("Suggest streak bonuses", "Bonus für mehrere Erledigungen in Folge vorschlagen"),
            value.streaks,
            { onChange(value.copy(streaks = it)) },
            enabled,
        )
        Toggle(
            t("Suggest reminders", "Erinnerungen vorschlagen"),
            value.reminders,
            { onChange(value.copy(reminders = it)) },
            enabled,
        )
        Toggle(
            t("Consider urgency", "Bei den Vorschlägen berücksichtigen, was dringend ist oder bis wann es erledigt sein muss"),
            value.urgency,
            { onChange(value.copy(urgency = it)) },
            enabled,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedPlanCard(
    savedPlan: ParentPlan,
    language: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    fun t(en: String, de: String) = if (language == "de") de else en
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(savedPlan.title, Modifier.semantics { heading() })
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ParentButton(
                    onClick = onOpen,
                    modifier =
                        Modifier.semantics {
                            contentDescription = t("Open", "Öffnen") + ": " + savedPlan.title
                        },
                ) {
                    Text(
                        t("Open", "Öffnen"),
                        Modifier.clearAndSetSemantics {},
                    )
                }
                ParentTextButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.semantics {
                            contentDescription = t("Delete", "Löschen") + ": " + savedPlan.title
                        },
                ) {
                    Text(
                        t("Delete", "Löschen"),
                        Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

@Composable
fun CloudAccessNotice(judgingAccess: Boolean, language: String) {
    val text =
        if (judgingAccess) {
            if (language == "de")
                "Diese Testversion hat kein Cloud-Limit pro Installation. Der Cloud-Zugriff erfordert weiterhin deine Zustimmung und hängt von der Verfügbarkeit des Dienstes ab."
            else
                "This evaluation build has no per-installation cloud limit. Cloud access still requires your consent and depends on service availability."
        } else {
            if (language == "de")
                "Der kostenlose Cloud-Zugriff erlaubt bis zu 10 Versuche pro Installation innerhalb von 24 Stunden. Eigene Ideen bearbeiten und teilen kannst du unbegrenzt."
            else
                "Free cloud access allows up to 10 attempts per installation in 24 hours. Manual editing and sharing are unlimited."
        }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SuggestionReportDialog(item: PlanItem, language: String, bridge: PlatformBridge, onDismiss: () -> Unit) {
    fun t(en: String, de: String) = if (language == "de") de else en
    val id = remember { bridge.newId() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var comment by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(t("Report suggestion", "Vorschlag melden")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sent) {
                    Text(t("Report received. Thank you for helping us improve the suggestions.", "Meldung erhalten. Danke, dass du uns hilfst, die Vorschläge zu verbessern."), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    Text(t("Only the suggestion below and your optional comment will be sent to James Cullimore. Your original request and other ideas are not included. Reports are stored privately and automatically removed after 30 days (during the next hourly cleanup).", "Nur der folgende Vorschlag und dein optionaler Kommentar werden an James Cullimore gesendet. Deine ursprüngliche Anfrage und andere Ideen werden nicht mitgeschickt. Meldungen werden geschützt gespeichert und nach 30 Tagen bei der nächsten stündlichen Bereinigung automatisch gelöscht."))
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    if (item.description.isNotBlank()) Text(item.description)
                    ParentTextField(comment, { comment = it.take(600) }, Modifier.fillMaxWidth(), enabled = !sending, label = { Text(t("Comment (optional)", "Kommentar (optional)")) })
                    Text(t("Please leave out names and other personal details. Sending requires an internet connection.", "Bitte nenne keine Namen oder anderen persönlichen Angaben. Zum Senden ist eine Internetverbindung nötig."))
                    if (failed) Text(t("The report could not be confirmed. Check your connection and try again; retrying will not create a duplicate.", "Der Eingang konnte nicht bestätigt werden. Prüfe deine Verbindung und versuche es erneut. Dabei wird keine doppelte Meldung angelegt."), Modifier.semantics { liveRegion = LiveRegionMode.Assertive }, color = MaterialTheme.colorScheme.error)
                    if (sending) CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = t("Sending report", "Meldung wird gesendet") })
                }
            }
        },
        confirmButton = {
            ParentTextButton(enabled = !sending, onClick = {
                if (sent) onDismiss() else {
                    keyboard?.hide()
                    sending = true
                    failed = false
                    bridge.reportSuggestion(id, item.name, item.description, comment) { success -> scope.launch { sending = false; sent = success; failed = !success } }
                }
            }) { Text(if (sent) t("Done", "Fertig") else t("Send report", "Meldung senden")) }
        },
        dismissButton = { if (!sent) ParentTextButton(onClick = onDismiss, enabled = !sending) { Text(t("Cancel", "Abbrechen")) } },
    )
}

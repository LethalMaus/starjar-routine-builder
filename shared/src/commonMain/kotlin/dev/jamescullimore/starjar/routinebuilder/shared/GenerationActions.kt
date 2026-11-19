package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Feedback stays with the action so it remains visible after submitting a long form. */
@Composable
fun GenerationActions(
    busy: Boolean,
    message: String,
    more: Boolean,
    language: String,
    enabled: Boolean,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun t(en: String, de: String) = if (language == "de") de else en
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ParentButton(onClick = onGenerate, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth()) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp).clearAndSetSemantics {}, strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                if (busy) t("Creating suggestions…", "Vorschläge werden erstellt…")
                else if (more) t("Suggest more ideas", "Weitere Ideen vorschlagen")
                else t("Find tasks and rewards", "Aufgaben und Belohnungen finden"),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = if (busy) MaterialTheme.colorScheme.onSurface else LocalContentColor.current,
            )
        }
        if (message.isNotBlank()) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = MaterialTheme.shapes.medium) {
                Text(message, Modifier.fillMaxWidth().padding(16.dp).semantics {
                    liveRegion = LiveRegionMode.Assertive
                    error(message)
                }, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (busy) ParentOutlinedButton(onClick = onCancel) { Text(t("Cancel", "Abbrechen")) }
    }
}

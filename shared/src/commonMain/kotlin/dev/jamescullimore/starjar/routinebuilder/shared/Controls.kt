package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Yellow remains the filled action colour; text actions retain readable contrast on light surfaces.
 */
@Composable
fun ParentTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick,
        modifier.heightIn(min = 48.dp),
        enabled,
        colors =
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        content = content,
    )
}

@Composable
fun ParentOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick,
        modifier.heightIn(min = 48.dp),
        enabled,
        colors =
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        content = content,
    )
}

internal val LocalPlanLanguage = androidx.compose.runtime.staticCompositionLocalOf { "en" }

internal fun optionLabel(value: String, language: String): String {
    val pair =
        when (value) {
            "unspecified" -> "Not specified" to "Keine Angabe"
            "preschool" -> "Preschool" to "Kindergartenalter"
            "primary" -> "Primary school" to "Grundschulalter"
            "teen" -> "Teenager" to "Jugendalter"
            "NONE" -> "No limit" to "Keine Grenze"
            "DAILY" -> "Per day" to "Pro Tag"
            "WEEKLY" -> "Per week" to "Pro Woche"
            "MONTHLY" -> "Per month" to "Pro Monat"
            "TOTAL" -> "In total" to "Insgesamt"
            "TIME" -> "Completions" to "Erledigungen"
            "DAY" -> "Days" to "Tage"
            "WEEK" -> "Weeks" to "Wochen"
            "MONTH" -> "Months" to "Monate"
            "NEVER" -> "Never" to "Nie"
            "ON_DATE" -> "On a date" to "An einem Datum"
            "AFTER_OCCURRENCES" ->
                "After a number of reminders" to "Nach einer Anzahl von Erinnerungen"
            "GREEN" -> "Green" to "Grün"
            "BLUE" -> "Blue" to "Blau"
            "GRAY" -> "Grey" to "Grau"
            "WHITE" -> "White" to "Weiß"
            "PURPLE" -> "Purple" to "Lila"
            "YELLOW" -> "Yellow" to "Gelb"
            "RED" -> "Red" to "Rot"
            else -> value to value
        }
    return if (language == "de") pair.second else pair.first
}

/** A visible field boundary and cursor in both branded themes. */
@Composable
fun ParentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value,
        onValueChange,
        modifier,
        enabled = enabled,
        label = label,
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        keyboardOptions = keyboardOptions.copy(
            imeAction = if (keyboardOptions.imeAction == ImeAction.Default || keyboardOptions.imeAction == ImeAction.Unspecified) ImeAction.Done else keyboardOptions.imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        supportingText = supportingText,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
fun ParentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, content = content)
}

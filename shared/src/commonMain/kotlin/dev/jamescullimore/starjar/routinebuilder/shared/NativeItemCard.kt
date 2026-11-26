package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.datetime.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativeItemCard(
    item: PlanItem,
    language: String,
    enabled: Boolean,
    update: (PlanItem) -> Unit,
    discard: () -> Unit,
    report: (() -> Unit)? = null,
) {
    fun t(en: String, de: String) = if (language == "de") de else en
    val ideaName = item.name.ifBlank { t("Your idea", "Deine Idee") }
    var editing by remember { mutableStateOf(item.name.isBlank()) }
    var icons by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(IconUtils.getIconByName(item.icon), null)
                Text(
                    ideaName,
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${item.pointAmount} ★",
                    Modifier.semantics {
                        contentDescription =
                            t("${item.pointAmount} stars", "${item.pointAmount} Sterne")
                    },
                )
            }
            if (!editing && item.description.isNotBlank()) Text(item.description)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterChip(
                    selected = item.state == "kept",
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            contentDescription = t("Keep", "Behalten") + ": " + ideaName
                        },
                    onClick = {
                        update(item.copy(state = if (item.state == "kept") "suggested" else "kept"))
                    },
                    enabled = enabled,
                    label = { Text(t("Keep", "Behalten"), Modifier.clearAndSetSemantics {}) },
                )
                ParentTextButton(
                    onClick = { editing = !editing },
                    enabled = enabled,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                (if (editing) t("Close editor", "Bearbeitung schließen")
                                else t("Edit", "Bearbeiten")) + ": " + ideaName
                        },
                ) {
                    Text(
                        if (editing) t("Close editor", "Bearbeitung schließen")
                        else t("Edit", "Bearbeiten"),
                        Modifier.clearAndSetSemantics {},
                    )
                }
                ParentTextButton(
                    onClick = discard,
                    enabled = enabled,
                    modifier =
                        Modifier.semantics {
                            contentDescription = t("Discard", "Verwerfen") + ": " + ideaName
                        },
                ) {
                    Text(t("Discard", "Verwerfen"), Modifier.clearAndSetSemantics {})
                }
            }
            if (report != null) {
                ParentTextButton(onClick = report, enabled = enabled, modifier = Modifier.semantics { contentDescription = t("Report suggestion", "Vorschlag melden") + ": " + ideaName }) {
                    Text(t("Report suggestion", "Vorschlag melden"), Modifier.clearAndSetSemantics {})
                }
            }
            if (editing) {
                ParentTextField(
                    item.name,
                    { update(item.copy(name = it.take(120))) },
                    Modifier.fillMaxWidth(),
                    label = { Text(t("Name", "Name")) },
                    enabled = enabled,
                )
                ParentTextField(
                    item.description,
                    { update(item.copy(description = it.take(2000))) },
                    Modifier.fillMaxWidth(),
                    label = { Text(t("Description", "Beschreibung")) },
                    enabled = enabled,
                )
                NumberField(
                    t("Stars", "Sterne"),
                    item.pointAmount,
                    { update(item.copy(pointAmount = it)) },
                    enabled,
                    minimum = 1,
                )
                ParentOutlinedButton(onClick = { icons = true }, enabled = enabled) {
                    Icon(IconUtils.getIconByName(item.icon), null)
                    Spacer(Modifier.width(8.dp))
                    Text(t("Choose icon", "Symbol wählen"))
                }
                Choice(
                    t("Colour", "Farbe"),
                    item.color,
                    PlanTransfer.colors,
                    { update(item.copy(color = it)) },
                    enabled,
                )
                Choice(
                    t("Frequency limit", "Häufigkeitsgrenze"),
                    item.limitType,
                    PlanTransfer.frequencies,
                    { update(item.copy(limitType = it)) },
                    enabled,
                )
                if (item.limitType != "NONE")
                    NumberField(
                        t("Maximum completions", "Maximale Anzahl"),
                        item.limitValue,
                        { update(item.copy(limitValue = it)) },
                        enabled,
                    )
                Toggle(
                    t("Parent approval required", "Bestätigung durch Eltern erforderlich"),
                    item.requireApproval,
                    { update(item.copy(requireApproval = it)) },
                    enabled,
                )
                if (item.kind == "task") {
                    Text(
                        t("Badges (${item.badges.size}/20)", "Abzeichen (${item.badges.size}/20)"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    item.badges.forEachIndexed { index, badge ->
                        fun change(value: Badge) =
                            update(
                                item.copy(
                                    badges =
                                        item.badges.mapIndexed { i, old ->
                                            if (i == index) value else old
                                        }
                                )
                            )
                        ParentTextField(
                            badge.title,
                            { change(badge.copy(title = it.take(120))) },
                            Modifier.fillMaxWidth(),
                            label = { Text(t("Badge title", "Name des Abzeichens")) },
                            enabled = enabled,
                        )
                        NumberField(
                            t("After completions", "Nach dieser Anzahl"),
                            badge.completionsRequired,
                            { change(badge.copy(completionsRequired = it)) },
                            enabled,
                        )
                        NumberField(
                            t("Bonus stars", "Bonussterne"),
                            badge.bonusPoints ?: 0,
                            { change(badge.copy(bonusPoints = it)) },
                            enabled,
                        )
                        ParentTextButton(
                            onClick = {
                                update(
                                    item.copy(
                                        badges = item.badges.filterIndexed { i, _ -> i != index }
                                    )
                                )
                            },
                            enabled = enabled,
                        ) {
                            Text(t("Remove badge", "Abzeichen entfernen"))
                        }
                    }
                    ParentOutlinedButton(
                        onClick = {
                            update(
                                item.copy(
                                    badges =
                                        item.badges +
                                            Badge(
                                                (item.badges.lastOrNull()?.completionsRequired
                                                    ?: 0) + 5,
                                                t("New milestone", "Neuer Meilenstein"),
                                            )
                                )
                            )
                        },
                        enabled = enabled && item.badges.size < 20,
                    ) {
                        Text(t("Add badge", "Abzeichen hinzufügen"))
                    }
                    Toggle(
                        t("Streak bonus", "Serienbonus"),
                        item.streakBonus != null,
                        { update(item.copy(streakBonus = if (it) Streak(5) else null)) },
                        enabled,
                    )
                    item.streakBonus?.let { streak ->
                        Text(t(
                            "A confirmed completion earns the bonus when it finishes a qualifying streak. For example: once a day for 5 days, or 3 completions per day for 5 consecutive days. Missed periods break the streak; completed streaks can earn the bonus again. These settings do not schedule reminders.",
                            "Der Bonus wird vergeben, wenn eine bestätigte Erledigung die Serie abschließt. Zum Beispiel: einmal täglich für 5 Tage oder 3 Erledigungen pro Tag an 5 aufeinanderfolgenden Tagen. Ausgelassene Zeiträume unterbrechen die Serie. Eine neue vollständige Serie kann erneut einen Bonus bringen. Erinnerungen werden separat eingestellt."
                        ))
                        NumberField(
                            t("Bonus stars", "Bonussterne"),
                            streak.bonusPoints,
                            { update(item.copy(streakBonus = streak.copy(bonusPoints = it))) },
                            enabled,
                        )
                        NumberField(
                            t("Required frequency", "Häufigkeit für die Serie"),
                            streak.everyValue,
                            { update(item.copy(streakBonus = streak.copy(everyValue = it))) },
                            enabled,
                        )
                        Choice(
                            t("Frequency unit", "Zeiteinheit oder Erledigungen"),
                            streak.everyUnit,
                            PlanTransfer.units,
                            { update(item.copy(streakBonus = streak.copy(everyUnit = it))) },
                            enabled,
                        )
                        NumberField(
                            t("Streak target", "Ziel der Serie"),
                            streak.forValue,
                            { update(item.copy(streakBonus = streak.copy(forValue = it))) },
                            enabled,
                        )
                        Choice(
                            t("Streak target unit", "Einheit für das Ziel der Serie"),
                            streak.forUnit,
                            PlanTransfer.units,
                            { update(item.copy(streakBonus = streak.copy(forUnit = it))) },
                            enabled,
                        )
                    }
                    Toggle(
                        t("Reminder", "Erinnerung"),
                        item.reminder != null,
                        { update(item.copy(reminder = if (it) Reminder() else null)) },
                        enabled,
                    )
                    item.reminder?.let { reminder ->
                        NumberField(
                            t("Repeat every", "Wiederholen alle"),
                            reminder.frequencyValue,
                            { update(item.copy(reminder = reminder.copy(frequencyValue = it))) },
                            enabled,
                        )
                        Choice(
                            t("Repeat unit", "Wiederholungseinheit"),
                            reminder.frequencyUnit,
                            PlanTransfer.units.drop(1),
                            { update(item.copy(reminder = reminder.copy(frequencyUnit = it))) },
                            enabled,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            (1..7).forEach { day ->
                                FilterChip(
                                    selected = day in reminder.daysOfWeek,
                                    modifier =
                                        Modifier.heightIn(min = 48.dp).semantics {
                                            contentDescription =
                                                (if (language == "de")
                                                    listOf(
                                                        "Montag",
                                                        "Dienstag",
                                                        "Mittwoch",
                                                        "Donnerstag",
                                                        "Freitag",
                                                        "Samstag",
                                                        "Sonntag",
                                                    )
                                                else
                                                    listOf(
                                                        "Monday",
                                                        "Tuesday",
                                                        "Wednesday",
                                                        "Thursday",
                                                        "Friday",
                                                        "Saturday",
                                                        "Sunday",
                                                    ))[day - 1]
                                        },
                                    onClick = {
                                        update(
                                            item.copy(
                                                reminder =
                                                    reminder.copy(
                                                        daysOfWeek =
                                                            if (day in reminder.daysOfWeek)
                                                                reminder.daysOfWeek - day
                                                            else
                                                                (reminder.daysOfWeek + day).sorted()
                                                    )
                                            )
                                        )
                                    },
                                    enabled = enabled,
                                    label = {
                                        Text(
                                            (if (language == "de")
                                                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                                            else
                                                listOf(
                                                    "Mon",
                                                    "Tue",
                                                    "Wed",
                                                    "Thu",
                                                    "Fri",
                                                    "Sat",
                                                    "Sun",
                                                ))[day - 1]
                                        )
                                    },
                                )
                            }
                        }
                        ParentTextField(
                            reminder.time.orEmpty(),
                            {
                                update(
                                    item.copy(reminder = reminder.copy(time = it.ifBlank { null }))
                                )
                            },
                            Modifier.fillMaxWidth(),
                            label = {
                                Text(t("Time (HH:mm, optional)", "Uhrzeit (HH:mm, optional)"))
                            },
                            enabled = enabled,
                        )
                        DateField(t("Start date", "Startdatum"), reminder.startDate, enabled) {
                            update(item.copy(reminder = reminder.copy(startDate = it)))
                        }
                        Choice(
                            t("Ends", "Endet"),
                            reminder.endType,
                            listOf("NEVER", "ON_DATE", "AFTER_OCCURRENCES"),
                            { update(item.copy(reminder = reminder.copy(endType = it))) },
                            enabled,
                        )
                        if (reminder.endType == "ON_DATE")
                            DateField(t("End date", "Enddatum"), reminder.endDate, enabled) {
                                update(item.copy(reminder = reminder.copy(endDate = it)))
                            }
                        if (reminder.endType == "AFTER_OCCURRENCES")
                            NumberField(
                                t("Occurrences", "Anzahl"),
                                reminder.endOccurrences ?: 1,
                                {
                                    update(item.copy(reminder = reminder.copy(endOccurrences = it)))
                                },
                                enabled,
                            )
                    }
                    Toggle(
                        t("Urgent", "Dringend"),
                        item.isUrgent,
                        { update(item.copy(isUrgent = it)) },
                        enabled,
                    )
                    if (item.isUrgent) {
                        DateField(t("Due date", "Fälligkeitsdatum"), item.dueByDate, enabled) {
                            update(item.copy(dueByDate = it))
                        }
                        NumberField(
                            t("Urgency bonus stars", "Dringlichkeitsbonus"),
                            item.urgencyBonus ?: 0,
                            { update(item.copy(urgencyBonus = it)) },
                            enabled,
                        )
                    }
                }
            }
        }
    }
    if (icons)
        AlertDialog(
            onDismissRequest = { icons = false },
            title = { Text(t("Choose an icon", "Symbol wählen")) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(IconCatalog.ids.toList()) { id ->
                        IconToggleButton(
                            checked = item.icon == id,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            onCheckedChange = {
                                update(item.copy(icon = id))
                                icons = false
                            },
                        ) {
                            Icon(
                                IconUtils.getIconByName(id),
                                iconLabel(id, language),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                ParentTextButton(onClick = { icons = false }) { Text(t("Close", "Schließen")) }
            },
        )
}

@Composable
private fun DateField(label: String, value: Long?, enabled: Boolean, update: (Long?) -> Unit) {
    fun display(millis: Long?) =
        millis
            ?.let {
                Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()
            }
            .orEmpty()
    var draft by remember(value) { mutableStateOf(display(value)) }
    val parsed = runCatching {
        LocalDate.parse(draft).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
        .getOrNull()
    ParentTextField(
        draft,
        {
            draft = it
            if (it.isBlank()) update(null)
            else
                runCatching {
                        LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    }
                    .onSuccess(update)
        },
        Modifier.fillMaxWidth(),
        label = { Text("$label (YYYY-MM-DD)") },
        enabled = enabled,
        isError = draft.isNotBlank() && parsed == null,
    )
}

package dev.jamescullimore.starjar.routinebuilder.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Badge(val completionsRequired: Int, val title: String, val bonusPoints: Int? = null)

@Serializable
data class Streak(
    val bonusPoints: Int,
    val everyValue: Int = 1,
    val everyUnit: String = "DAY",
    val forValue: Int = 5,
    val forUnit: String = "DAY",
)

@Serializable
data class Reminder(
    val frequencyValue: Int = 1,
    val frequencyUnit: String = "DAY",
    val daysOfWeek: List<Int> = emptyList(),
    val time: String? = null,
    val startDate: Long? = null,
    val endType: String = "NEVER",
    val endDate: Long? = null,
    val endOccurrences: Int? = null,
)

@Serializable
data class PlanItem(
    val id: String,
    val kind: String = "task",
    val state: String = "suggested",
    val name: String = "",
    val originalName: String = "",
    val description: String = "",
    val pointAmount: Int = 5,
    val icon: String = "Check",
    val color: String = "BLUE",
    val limitType: String = "NONE",
    val limitValue: Int = 1,
    val requireApproval: Boolean = true,
    val badges: List<Badge> = emptyList(),
    val streakBonus: Streak? = null,
    val reminder: Reminder? = null,
    val isUrgent: Boolean = false,
    val dueByDate: Long? = null,
    val urgencyBonus: Int? = null,
)

@Serializable
data class DiscardedIdea(val kind: String, val name: String, val originalName: String = "")

@Serializable
data class ParentPlan(
    val id: String,
    val title: String,
    val language: String = "en",
    val items: List<PlanItem> = emptyList(),
    val discarded: List<DiscardedIdea> = emptyList(),
)

@Serializable
data class FeatureChoices(
    val badgeCount: Int = 0,
    val frequency: Boolean = true,
    val streaks: Boolean = false,
    val reminders: Boolean = false,
    val urgency: Boolean = false,
)

@Serializable
data class ParentRequest(
    val goal: String,
    val language: String = "en",
    val ageBand: String = "unspecified",
    val availableMinutes: Int? = null,
    val constraints: List<String> = emptyList(),
    val feedback: String = "",
    val features: FeatureChoices = FeatureChoices(),
    val existing: List<ContextItem> = emptyList(),
    val discarded: List<DiscardedIdea> = emptyList(),
)

@Serializable
data class ContextItem(val kind: String, val name: String, val pointAmount: Int, val state: String)

/** Identical file envelope to the web companion; no goal or private conversation is stored. */
object PlanTransfer {
    val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }
    private val baseFields =
        setOf(
            "name",
            "description",
            "pointAmount",
            "icon",
            "color",
            "limitType",
            "limitValue",
            "requireApproval",
        )
    private val taskFields =
        baseFields +
            setOf("badges", "streakBonus", "reminder", "isUrgent", "dueByDate", "urgencyBonus")

    fun validate(plan: ParentPlan, complete: Boolean = false) {
        require(
            plan.id.isNotBlank() &&
                plan.id.length <= 128 &&
                plan.title.length <= 120 &&
                plan.language in listOf("en", "de")
        )
        require(
            plan.items.size <= 40 &&
                plan.items.map { it.id }.distinct().size == plan.items.size &&
                plan.discarded.size <= 80
        )
        plan.items.forEach { item ->
            require(
                item.id.isNotBlank() &&
                    item.id.length <= 128 &&
                    item.kind in listOf("task", "reward") &&
                    item.state in listOf("suggested", "kept")
            )
            text(item.name, 120, !complete)
            text(item.description, 2000, true)
            text(item.originalName, 120, true)
            require(
                item.pointAmount in 1..1000000 &&
                    item.icon in IconCatalog.ids &&
                    item.color in colors &&
                    item.limitType in frequencies &&
                    item.limitValue in 1..10000
            )
            require(item.badges.size <= 20)
            var previous = 0
            item.badges.forEach { b ->
                require(b.completionsRequired in 1..1000000 && b.completionsRequired > previous)
                previous = b.completionsRequired
                text(b.title, 120)
                require(b.bonusPoints == null || b.bonusPoints in 0..1000000)
            }
            item.streakBonus?.let { b ->
                require(
                    b.bonusPoints in 1..1000000 &&
                        b.everyValue in 1..10000 &&
                        b.forValue in 1..10000 &&
                        b.everyUnit in units &&
                        b.forUnit in units
                )
            }
            date(item.dueByDate)
            require(item.urgencyBonus == null || item.urgencyBonus in 0..1000000)
            item.reminder?.let { r ->
                require(
                    r.frequencyValue in 1..10000 &&
                        r.frequencyUnit in units.drop(1) &&
                        r.daysOfWeek.size <= 7 &&
                        r.daysOfWeek.distinct().size == r.daysOfWeek.size &&
                        r.daysOfWeek.all { it in 1..7 }
                )
                require(r.time == null || Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(r.time))
                date(r.startDate)
                date(r.endDate)
                require(r.endType in listOf("NEVER", "ON_DATE", "AFTER_OCCURRENCES"))
                require(r.endOccurrences == null || r.endOccurrences in 1..10000)
                require(r.endType != "ON_DATE" || r.endDate != null)
                require(r.endType != "AFTER_OCCURRENCES" || r.endOccurrences != null)
                require(r.startDate == null || r.endDate == null || r.endDate >= r.startDate)
            }
        }
        plan.discarded.forEach {
            require(it.kind in listOf("task", "reward"))
            text(it.name, 120, true)
            text(it.originalName, 120, true)
        }
    }

    fun editable(plan: ParentPlan): String {
        validate(plan)
        return bounded(
            buildJsonObject {
                put("format", "starjar-ideas-plan")
                put("version", 1)
                put("plan", json.encodeToJsonElement(plan))
            }
                .toString()
        )
    }

    fun templates(plan: ParentPlan): String {
        val selected = plan.items.filter { it.state == "kept" }.map { it.copy(name = it.name.trim()) }
        require(selected.isNotEmpty())
        validate(plan.copy(items = selected), true)
        fun items(kind: String) =
            JsonArray(
                selected
                    .filter { it.kind == kind }
                    .also { require(it.map { normalizedName(it.name) }.distinct().size == it.size) }
                    .map { item ->
                        JsonObject(
                            json.encodeToJsonElement(item).jsonObject.filterKeys {
                                it in if (kind == "task") taskFields else baseFields
                            }
                        )
                    }
            )
        return bounded(
            buildJsonObject {
                put("format", "starjar-templates")
                put("version", 1)
                put("tasks", items("task"))
                put("rewards", items("reward"))
            }
                .toString()
        ).also { require(it.length <= 100_000) { "StarJar templates exceed the import size limit" } }
    }

    fun read(
        text: String,
        newId: () -> String,
        language: String = "en",
        title: String = "Imported ideas",
    ): ParentPlan {
        bounded(text)
        val obj = json.parseToJsonElement(text).jsonObject
        require(obj["version"]?.jsonPrimitive?.int == 1)
        val plan =
            when (obj["format"]?.jsonPrimitive?.content) {
                "starjar-ideas-plan" ->
                    json.decodeFromJsonElement<ParentPlan>(
                        normalisePlan(obj.getValue("plan").jsonObject)
                    )
                "starjar-templates" -> {
                    val items =
                        listOf("task", "reward").flatMap { kind ->
                            obj.getValue(if (kind == "task") "tasks" else "rewards")
                                .jsonArray
                                .map { value ->
                                    val fields =
                                        value.jsonObject
                                            .filterKeys {
                                                it in if (kind == "task") taskFields else baseFields
                                            }
                                            .toMutableMap()
                                    fields.entries.removeAll { it.value is JsonNull }
                                    fields["id"] = JsonPrimitive(newId())
                                    fields["kind"] = JsonPrimitive(kind)
                                    fields["state"] = JsonPrimitive("kept")
                                    fields["originalName"] = fields.getValue("name")
                                    json.decodeFromJsonElement<PlanItem>(JsonObject(fields))
                                }
                        }
                    require(items.isNotEmpty())
                    ParentPlan(newId(), title, language, items)
                }
                else -> error("Unsupported plan format")
            }
        validate(plan)
        return plan
    }

    private fun normalisePlan(plan: JsonObject) =
        JsonObject(
            plan.toMutableMap().also { p ->
                p["items"] =
                    JsonArray(
                        plan.getValue("items").jsonArray.map { v ->
                            JsonObject(
                                v.jsonObject.toMutableMap().also { x ->
                                    for (k in listOf("pointAmount", "limitValue")) {
                                        val n = x[k]?.jsonPrimitive?.content?.toIntOrNull()
                                        require(n != null)
                                        x[k] = JsonPrimitive(n)
                                    }
                                }
                            )
                        }
                    )
            }
        )

    private fun bounded(text: String): String {
        require(text.encodeToByteArray().size <= 200000)
        return text
    }

    private fun date(value: Long?) {
        require(value == null || value in 0..253402300799999L)
    }

    private fun text(value: String, max: Int, empty: Boolean = false) {
        require(
            value.length <= max && (empty || value.isNotBlank()) && value.none { it.isISOControl() }
        )
    }

    val colors = listOf("GREEN", "BLUE", "GRAY", "WHITE", "PURPLE", "YELLOW", "RED")
    val frequencies = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "TOTAL")
    val units = listOf("TIME", "DAY", "WEEK", "MONTH")
}

expect fun normalizedName(value: String): String

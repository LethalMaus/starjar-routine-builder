package dev.jamescullimore.starjar.routinebuilder.shared

import kotlinx.serialization.json.*

object PlanEngine {
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    suspend fun generationPrompt(request: ParentRequest, advanced: Boolean): String {
        val instructions =
            dev.jamescullimore.starjar.routinebuilder.shared.resources.Res.readBytes(
                    if (advanced) "files/parent-instructions.txt"
                    else "files/parent-local-instructions.txt"
                )
                .decodeToString()
        val data = PlanTransfer.json.encodeToJsonElement(request).jsonObject.toMutableMap()
        data["iconIds"] = JsonArray(IconCatalog.ids.map { JsonPrimitive(it) })
        return instructions + "\nREQUEST_DATA:\n" + JsonObject(data)
    }

    fun request(
        plan: ParentPlan,
        goal: String,
        feedback: String,
        age: String,
        minutes: String,
        features: FeatureChoices,
        constraints: List<String>,
    ): ParentRequest {
        require(
            goal.isNotBlank() &&
                goal.length <= 1200 &&
                feedback.length <= 600 &&
                features.badgeCount in 0..20
        )
        require(age in listOf("unspecified", "preschool", "primary", "teen"))
        require(
            constraints.size <= 2 &&
                constraints.distinct().size == constraints.size &&
                constraints.all { it in listOf("no_purchases", "no_screens") }
        )
        val time = if (minutes.isBlank()) null else minutes.toInt().also { require(it in 1..180) }
        return ParentRequest(
            goal,
            plan.language,
            age,
            time,
            constraints,
            feedback,
            features,
            plan.items
                .filter { it.name.isNotBlank() }
                .map { ContextItem(it.kind, it.name, it.pointAmount, it.state) },
            plan.discarded,
        )
    }

    // Model suggestions may invent decorative identifiers. File imports stay strict.
    private fun generatedIcons(items: JsonArray, fallback: String): JsonArray =
        JsonArray(items.map { element ->
            val item = element.jsonObject
            val icon = (item["icon"] as? JsonPrimitive)?.contentOrNull
            if (icon in IconCatalog.ids) item
            else JsonObject(item + ("icon" to JsonPrimitive(fallback)))
        })

    @Throws(IllegalArgumentException::class)
    fun merge(
        plan: ParentPlan,
        response: String,
        features: FeatureChoices,
        newId: () -> String,
    ): ParentPlan {
        val raw = PlanTransfer.json.parseToJsonElement(response).jsonObject
        val tasks = raw.getValue("tasks").jsonArray
        val rewards = raw.getValue("rewards").jsonArray
        require(tasks.size <= 6 && rewards.size <= 3)
        val document = buildJsonObject {
            put("format", "starjar-templates")
            put("version", 1)
            put("tasks", generatedIcons(tasks, "Check"))
            put("rewards", generatedIcons(rewards, "Games"))
        }
        if (tasks.isEmpty() && rewards.isEmpty()) return plan
        val incoming = PlanTransfer.read(document.toString(), newId, plan.language)
        val blocked =
            (plan.items.flatMap {
                    listOf(
                        it.kind + ":" + normalizedName(it.name),
                        it.kind + ":" + normalizedName(it.originalName),
                    )
                } +
                    plan.discarded.flatMap {
                        listOf(
                            it.kind + ":" + normalizedName(it.name),
                            it.kind + ":" + normalizedName(it.originalName),
                        )
                    })
                .toMutableSet()
        val additions =
            incoming.items
                .map { item ->
                    val limited =
                        if (features.frequency) item
                        else item.copy(limitType = "NONE", limitValue = 1)
                    if (item.kind == "reward")
                        limited.copy(
                            state = "suggested",
                            requireApproval = true,
                            badges = emptyList(),
                            streakBonus = null,
                            reminder = null,
                            isUrgent = false,
                            dueByDate = null,
                            urgencyBonus = null,
                        )
                    else {
                        if (features.badgeCount > 0)
                            require(
                                item.badges.size ==
                                    (if (limited.limitType == "TOTAL")
                                        minOf(features.badgeCount, limited.limitValue)
                                    else features.badgeCount)
                            )
                        limited.copy(
                            state = "suggested",
                            requireApproval = true,
                            badges = if (features.badgeCount > 0) {
                                listOf(item.badges.first().copy(completionsRequired = 1)) + item.badges.drop(1)
                            } else emptyList(),
                            streakBonus = if (features.streaks) item.streakBonus else null,
                            reminder = if (features.reminders) item.reminder else null,
                            isUrgent = features.urgency && item.isUrgent,
                            dueByDate = if (features.urgency) item.dueByDate else null,
                            urgencyBonus = if (features.urgency) item.urgencyBonus else null,
                        )
                    }
                }
                .filter { blocked.add(it.kind + ":" + normalizedName(it.name)) }
                .take(40 - plan.items.size)
        return plan.copy(items = plan.items + additions).also { PlanTransfer.validate(it) }
    }

    fun discard(plan: ParentPlan, id: String): ParentPlan {
        require(plan.discarded.size < 80)
        val item = plan.items.first { it.id == id }
        return plan.copy(
            items = plan.items.filterNot { it.id == id },
            discarded = plan.discarded + DiscardedIdea(item.kind, item.name, item.originalName),
        )
    }
}

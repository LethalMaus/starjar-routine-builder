package dev.jamescullimore.starjar.routinebuilder

import dev.jamescullimore.starjar.routinebuilder.core.ReviewedRoutine
import dev.jamescullimore.starjar.routinebuilder.shared.*
import kotlinx.serialization.json.*

/** Copies the earlier prototype's data into the shared format; callers retain the old files. */
internal object LegacyPlans {
    fun saved(routine: ReviewedRoutine): ParentPlan =
        ParentPlan(
                routine.id,
                routine.title,
                routine.language,
                routine.tasks.mapIndexed { index, task ->
                    PlanItem(
                        "task-$index",
                        state = "kept",
                        name = task.name,
                        originalName = task.name,
                        pointAmount = task.pointAmount,
                    )
                } +
                    listOfNotNull(
                        routine.reward?.let {
                            PlanItem(
                                "reward",
                                kind = "reward",
                                state = "kept",
                                name = it.name,
                                originalName = it.name,
                                pointAmount = it.pointAmount,
                                icon = "Redeem",
                            )
                        }
                    ),
            )
            .also { PlanTransfer.validate(it) }

    fun draft(raw: String): ParentPlan {
        require(raw.encodeToByteArray().size <= 200000)
        val objectValue = Json.parseToJsonElement(raw).jsonObject
        fun item(value: JsonElement, id: String, kind: String): PlanItem {
            val obj = value.jsonObject
            return PlanItem(
                id,
                kind,
                "kept",
                obj.getValue("name").jsonPrimitive.content,
                pointAmount = obj.getValue("points").jsonPrimitive.content.toInt(),
                icon = if (kind == "task") "Check" else "Redeem",
            )
        }
        return ParentPlan(
                objectValue.getValue("id").jsonPrimitive.content,
                objectValue.getValue("title").jsonPrimitive.content,
                objectValue.getValue("language").jsonPrimitive.content,
                objectValue.getValue("tasks").jsonArray.mapIndexed { i, x ->
                    item(x, "task-$i", "task")
                } +
                    listOfNotNull(
                        objectValue["reward"]
                            ?.takeUnless { it is JsonNull }
                            ?.let { item(it, "reward", "reward") }
                    ),
            )
            .also { PlanTransfer.validate(it) }
    }
}

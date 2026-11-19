package dev.jamescullimore.starjar.routinebuilder.shared

import kotlinx.serialization.json.*

/**
 * Validate the entire library before a mutation so unreadable entries are never silently dropped.
 */
object PlanLibrary {
    fun read(document: String): List<ParentPlan> {
        require(document.length <= 20_000_000)
        val entries = PlanTransfer.json.parseToJsonElement(document).jsonArray
        require(entries.size <= 100)
        val plans = entries.map { entry ->
            require(entry.jsonObject["format"]?.jsonPrimitive?.content == "starjar-ideas-plan")
            PlanTransfer.read(entry.toString(), { error("Stored plans must already have an ID") })
        }
        require(plans.map { it.id }.distinct().size == plans.size)
        return plans
    }

    fun save(document: String, plan: ParentPlan): String {
        val previous = read(document)
        val next = listOf(plan) + previous.filter { it.id != plan.id }
        require(next.size <= 100)
        return encode(next)
    }

    fun delete(document: String, id: String): String {
        val previous = read(document)
        require(previous.any { it.id == id })
        return encode(previous.filter { it.id != id })
    }

    private fun encode(plans: List<ParentPlan>) =
        JsonArray(plans.map { PlanTransfer.json.parseToJsonElement(PlanTransfer.editable(it)) })
            .toString()
}

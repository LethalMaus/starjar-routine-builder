package dev.jamescullimore.starjar.routinebuilder.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TemplateExporter {
    private val json = Json { encodeDefaults = true; prettyPrint = true }
    private const val MAX_EXPORT = 100_000

    /** Fresh allowlisted envelope: local IDs/title/language/provenance cannot leak into exports. */
    fun encode(routine: ReviewedRoutine): String {
        RoutineValidation.reviewed(routine)
        val envelope = ExportEnvelope(
            tasks = routine.tasks.map { ExportItem(it.name, it.pointAmount) },
            rewards = listOfNotNull(routine.reward).map { ExportItem(it.name, it.pointAmount) },
        )
        return json.encodeToString(envelope).also {
            if (it.length > MAX_EXPORT) throw ValidationException(ValidationCode.EXPORT_SIZE)
        }
    }

    @Serializable
    private data class ExportItem(val name: String, val pointAmount: Int)
    @Serializable
    private data class ExportEnvelope(
        val format: String = "starjar-templates",
        val version: Int = 1,
        val tasks: List<ExportItem>,
        val rewards: List<ExportItem>,
    )
}

package dev.jamescullimore.starjar.routinebuilder.core

import kotlinx.serialization.json.Json

/** The model has no Kotlin default values: all request options must be explicit on the wire. */
object RoutineRequestEncoding {
 private val json = Json { encodeDefaults = true }
 fun encode(request: RoutineRequest): String {
  RoutineValidation.request(request)
  return json.encodeToString(request)
 }
}

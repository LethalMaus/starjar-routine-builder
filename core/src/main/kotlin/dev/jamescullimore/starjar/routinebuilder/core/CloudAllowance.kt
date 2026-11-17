package dev.jamescullimore.starjar.routinebuilder.core

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CloudAllowanceExceeded : IllegalStateException("Cloud allowance exhausted")
class CloudAllowanceUnavailable(cause: Exception) : IllegalStateException("Cloud allowance storage unavailable", cause)
data class CloudAllowanceStatus(val remaining: Int, val resetsAt: Long?)
@Serializable private data class CloudAttempt(val id: String, val time: Long)
@Serializable private data class CloudUsage(val lastObserved: Long = 0, val attempts: List<CloudAttempt> = emptyList())

/** Per-installation courtesy allowance, not a tamper-proof server quota or money cap. */
class CloudAllowance(private val file: File, private val clock: () -> Long = System::currentTimeMillis) {
 companion object { const val LIMIT = 10; const val WINDOW_MS = 24 * 60 * 60 * 1000L; private val lock = Any() }
 private fun read(): CloudUsage = try {
  val old = if (file.exists()) Json.decodeFromString<CloudUsage>(file.readText()) else CloudUsage()
  require(old.attempts.size <= LIMIT && old.attempts.all { it.time >= 0 } && old.lastObserved >= 0)
  val now = maxOf(clock(), old.lastObserved)
  old.copy(lastObserved = now, attempts = old.attempts.filter { now - it.time < WINDOW_MS })
 } catch(e: Exception) { throw CloudAllowanceUnavailable(e) }
 private fun write(value: CloudUsage) {
  val temporary = File(file.parentFile, file.name + ".tmp")
  try {
   file.parentFile?.mkdirs()
   temporary.outputStream().use { it.write(Json.encodeToString(value).toByteArray()); it.fd.sync() }
   Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  } catch(e: Exception) { throw CloudAllowanceUnavailable(e) } finally { temporary.delete() }
 }
 fun status(): CloudAllowanceStatus = synchronized(lock) {
  val value = read()
  CloudAllowanceStatus(LIMIT - value.attempts.size, value.attempts.minOfOrNull { it.time }?.plus(WINDOW_MS))
 }
 fun reserve(): String = synchronized(lock) {
  val value = read()
  if (value.attempts.size >= LIMIT) throw CloudAllowanceExceeded()
  val id = UUID.randomUUID().toString()
  write(value.copy(attempts = value.attempts + CloudAttempt(id, value.lastObserved)))
  id
 }
 fun refundLocalResult(id: String) = synchronized(lock) {
  val value = read(); write(value.copy(attempts = value.attempts.filterNot { it.id == id }))
 }
}

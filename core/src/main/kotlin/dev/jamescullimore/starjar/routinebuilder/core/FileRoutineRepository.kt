package dev.jamescullimore.starjar.routinebuilder.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Synchronous app-private storage. Invoke on an IO dispatcher, not the UI thread. */
class FileRoutineRepository internal constructor(
    private val directory: File,
    private val writer: SnapshotWriter,
) {
    constructor(directory: File) : this(directory, AtomicSnapshotWriter())

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    private val snapshot get() = File(directory, "routines.json")

    @Synchronized
    fun load(): List<ReviewedRoutine> = readSnapshot()

    @Synchronized
    fun save(routine: ReviewedRoutine) {
        RoutineValidation.reviewed(routine)
        val previous = readSnapshot()
        val position = previous.indexOfFirst { it.id == routine.id }
        val next = previous.toMutableList()
        if (position == -1) next.add(0, routine) else next[position] = routine
        commit(next)
    }

    @Synchronized
    fun delete(id: String) {
        val previous = readSnapshot()
        val next = previous.filterNot { it.id == id }
        if (next.size != previous.size) commit(next)
    }

    private fun readSnapshot(): List<ReviewedRoutine> {
        if (!snapshot.exists()) return emptyList()
        try {
            if (snapshot.length() > MAX_BYTES) throw IOException("Routine storage exceeds its size limit")
            val stored = json.decodeFromString<Snapshot>(snapshot.readText(Charsets.UTF_8))
            if (stored.version != 1) throw IOException("Unsupported routine storage version")
            validateCollection(stored.routines)
            return stored.routines
        } catch (error: IOException) {
            throw error
        } catch (_: Exception) {
            // Never report success/empty state and overwrite an unreadable collection.
            // Do not include raw stored content in exceptions or diagnostics.
            throw IOException("Routine storage could not be read")
        }
    }

    private fun commit(routines: List<ReviewedRoutine>) {
        validateCollection(routines)
        val bytes = json.encodeToString(Snapshot(routines = routines)).toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BYTES) throw IOException("Routine storage exceeds its size limit")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Routine storage could not be created")
        writer.write(snapshot.toPath(), bytes)
    }

    private fun validateCollection(routines: List<ReviewedRoutine>) {
        if (routines.size > MAX_ROUTINES || routines.map { it.id }.distinct().size != routines.size)
            throw IOException("Invalid routine collection")
        routines.forEach { RoutineValidation.reviewed(it) }
    }

    @Serializable
    private data class Snapshot(val version: Int = 1, val routines: List<ReviewedRoutine>)

    companion object {
        const val MAX_ROUTINES = 250
        private const val MAX_BYTES = 4 * 1024 * 1024
    }
}

internal fun interface SnapshotWriter {
    fun write(target: Path, bytes: ByteArray)
}

/** Failure before the atomic replacement keeps the previous snapshot intact. */
internal class AtomicSnapshotWriter(private val beforeReplace: () -> Unit = {}) : SnapshotWriter {
    override fun write(target: Path, bytes: ByteArray) {
        val temporary = Files.createTempFile(target.parent, "routine-", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use {
                it.write(bytes)
                it.fd.sync()
            }
            beforeReplace()
            // No non-atomic fallback: if the filesystem cannot support this, saving visibly fails.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

package dev.jamescullimore.starjar.routinebuilder.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class FileRoutineRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun savesEditsReopensFromNewRepositoryAndDeletesDeliberately() {
        val directory = temporary.newFolder()
        val repository = FileRoutineRepository(directory)
        assertEquals(emptyList<ReviewedRoutine>(), repository.load())
        val first = routine()
        val second = first.copy(id = "second", title = "Abend", language = "de", tasks = listOf(RoutineItem("Zähne putzen", 7)), reward = RoutineItem("Spiel aussuchen", 31))
        repository.save(first)
        repository.save(second)
        val edited = first.copy(tasks = listOf(RoutineItem("Put away books", 22)))
        repository.save(edited)
        assertEquals(listOf(second, edited), FileRoutineRepository(directory).load())
        repository.delete(second.id)
        assertEquals(listOf(edited), FileRoutineRepository(directory).load())
        repository.delete("not-present")
        assertEquals(listOf(edited), repository.load())
        val raw = File(directory, "routines.json").readText()
        assertFalse(raw.contains("goal"))
        assertFalse(raw.contains("constraints"))
    }

    @Test fun failedAtomicReplacementKeepsPreviousContentAndRemovesTemporaryFile() {
        val directory = temporary.newFolder()
        FileRoutineRepository(directory).save(routine())
        val before = File(directory, "routines.json").readBytes()
        val broken = FileRoutineRepository(directory, AtomicSnapshotWriter { throw IOException("simulated disk failure") })
        assertThrows(IOException::class.java) { broken.save(routine().copy(title = "Changed")) }
        assertArrayEquals(before, File(directory, "routines.json").readBytes())
        assertEquals(listOf(routine()), FileRoutineRepository(directory).load())
        assertEquals(listOf("routines.json"), directory.list()!!.toList())
    }

    @Test fun invalidSaveDoesNotChangeExistingSnapshot() {
        val directory = temporary.newFolder()
        val repository = FileRoutineRepository(directory)
        repository.save(routine())
        val before = File(directory, "routines.json").readBytes()
        assertThrows(ValidationException::class.java) { repository.save(routine().copy(tasks = emptyList())) }
        assertArrayEquals(before, File(directory, "routines.json").readBytes())
    }

    @Test fun corruptedOrUnknownVersionStorageNeverBecomesAnEmptySuccessfulLoad() {
        val directory = temporary.newFolder()
        val file = File(directory, "routines.json")
        val repository = FileRoutineRepository(directory)
        for (raw in listOf("{", "{\"version\":2,\"routines\":[]}", "{\"version\":1,\"routines\":[],\"private\":true}")) {
            file.writeText(raw)
            assertThrows(IOException::class.java) { repository.load() }
            assertThrows(IOException::class.java) { repository.save(routine()) }
            assertEquals(raw, file.readText())
        }
    }

    @Test fun duplicateStoredIdsAndInvalidPointsAreNotSilentlyAccepted() {
        val directory = temporary.newFolder()
        val repository = FileRoutineRepository(directory)
        repository.save(routine())
        val file = File(directory, "routines.json")
        val valid = file.readText()
        file.writeText(valid.replace("\"pointAmount\":5", "\"pointAmount\":0"))
        assertThrows(IOException::class.java) { repository.load() }
        val item = valid.substringAfter("\"routines\":[").substringBeforeLast("]}")
        file.writeText("{\"version\":1,\"routines\":[$item,$item]}")
        assertThrows(IOException::class.java) { repository.load() }
    }
}

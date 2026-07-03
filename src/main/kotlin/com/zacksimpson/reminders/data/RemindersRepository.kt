package com.zacksimpson.reminders.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.time.LocalDate

/** Snapshot of everything the app persists. */
data class AppData(
    val lists: List<ReminderList>,
    val tasks: List<Task>,
    val settings: Settings,
)

/** Thrown when a stored value exists but can't be parsed. The bad data is left on disk
 *  untouched — callers should surface an error, not reset. */
class DataCorruptionException(key: String, cause: Throwable) :
    Exception("Stored data for '$key' is unreadable; it was preserved, not overwritten.", cause)

/**
 * Single source of truth for persisted data, backed by the SDK's shared DataStore.
 *
 * All state lives in DataStore — no in-memory cache. Every mutation is one atomic
 * edit(), so writes can't tear or race across screens, and multi-key changes commit
 * together. Reads treat an absent key as a default; a corrupt key throws and aborts the
 * edit before any write, so bad data is never overwritten. Instances are stateless.
 */
class RemindersRepository(private val dataStore: DataStore<Preferences>) {

    private val listSerializer = ListSerializer(ReminderList.serializer())
    private val taskSerializer = ListSerializer(Task.serializer())

    // ── Reads ────────────────────────────────────────────────────────────────

    /** Live snapshot; re-emits on every change. Throws [DataCorruptionException] to
     *  collectors if a stored value is unparseable. */
    val appData: Flow<AppData> = dataStore.data.map { it.toAppData() }

    private fun Preferences.toAppData() = AppData(
        lists = readLists(),
        tasks = readTasks(),
        settings = readSettings(),
    )

    private fun Preferences.readLists(): List<ReminderList> =
        decode(this[LISTS_KEY], LISTS_KEY, listSerializer) ?: listOf(SEED_INBOX)

    private fun Preferences.readTasks(): List<Task> =
        decode(this[TASKS_KEY], TASKS_KEY, taskSerializer) ?: emptyList()

    private fun Preferences.readSettings(): Settings =
        decode(this[SETTINGS_KEY], SETTINGS_KEY, Settings.serializer()) ?: Settings()

    /** Absent key → null (caller applies a default); present but unparseable → throw. */
    private fun <T> decode(raw: String?, key: Preferences.Key<String>, serializer: KSerializer<T>): T? {
        if (raw == null) return null
        return try {
            appJson.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            throw DataCorruptionException(key.name, e)
        }
    }

    // ── Writes (encoders) ──────────────────────────────────────────────────────

    private fun MutablePreferences.writeLists(v: List<ReminderList>) {
        this[LISTS_KEY] = appJson.encodeToString(listSerializer, v)
    }

    private fun MutablePreferences.writeTasks(v: List<Task>) {
        this[TASKS_KEY] = appJson.encodeToString(taskSerializer, v)
    }

    private fun MutablePreferences.writeSettings(v: Settings) {
        this[SETTINGS_KEY] = appJson.encodeToString(Settings.serializer(), v)
    }

    // ── List operations ────────────────────────────────────────────────────────

    suspend fun addList(title: String) {
        dataStore.edit { p ->
            val lists = p.readLists()
            p.writeLists(lists + ReminderList(generateId(), title, now(), lists.size))
        }
    }

    suspend fun renameList(id: String, title: String) {
        dataStore.edit { p ->
            p.writeLists(p.readLists().map { if (it.id == id) it.copy(title = title) else it })
        }
    }

    /** Deletes the list and reassigns its tasks to the default list — in one atomic edit. */
    suspend fun deleteList(id: String) {
        dataStore.edit { p ->
            val defaultId = p.readSettings().defaultListId
            p.writeLists(p.readLists().filter { it.id != id })
            p.writeTasks(p.readTasks().map { if (it.listId == id) it.copy(listId = defaultId) else it })
        }
    }

    suspend fun moveListUp(id: String) = reorderList(id, -1)
    suspend fun moveListDown(id: String) = reorderList(id, +1)

    private suspend fun reorderList(id: String, direction: Int) {
        dataStore.edit { p ->
            val sorted = p.readLists().sortedBy { it.order }
            val idx = sorted.indexOfFirst { it.id == id }
            val target = idx + direction
            if (idx < 0 || target < 0 || target >= sorted.size) return@edit
            // Swap the two neighbours' order values.
            p.writeLists(
                sorted.mapIndexed { i, l ->
                    when (i) {
                        idx -> l.copy(order = target)
                        target -> l.copy(order = idx)
                        else -> l
                    }
                },
            )
        }
    }

    // ── Task operations ──────────────────────────────────────────────────────────

    suspend fun addTask(
        title: String,
        listId: String,
        date: String? = null,
        time: String? = null,
        recurrence: Recurrence? = null,
        subtasks: List<Subtask> = emptyList(),
    ): Task {
        lateinit var created: Task
        dataStore.edit { p ->
            val tasks = p.readTasks()
            val order = RemindersLogic.computeOrder(tasks, listId, p.readSettings().addPosition)
            created = Task(
                id = generateId(),
                title = title,
                listId = listId,
                date = date,
                time = time,
                recurrence = recurrence,
                subtasks = subtasks,
                completed = false,
                completedAt = null,
                createdAt = now(),
                order = order,
            )
            p.writeTasks(tasks + created)
        }
        return created
    }

    /** Apply [transform] to the task with [id]. `id` and `createdAt` are always preserved. */
    suspend fun updateTask(id: String, transform: (Task) -> Task) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id == id) transform(t).copy(id = t.id, createdAt = t.createdAt) else t
                },
            )
        }
    }

    suspend fun deleteTask(id: String) {
        dataStore.edit { p -> p.writeTasks(p.readTasks().filter { it.id != id }) }
    }

    suspend fun clearCompletedTasks(listId: String) {
        dataStore.edit { p ->
            p.writeTasks(p.readTasks().filterNot { it.listId == listId && it.completed })
        }
    }

    /** Toggle completion. Completing a dated recurring task also spawns its next occurrence. */
    suspend fun toggleTask(id: String) {
        dataStore.edit { p ->
            val toggled = p.readTasks().map { t ->
                when {
                    t.id != id -> t
                    t.completed -> t.copy(completed = false, completedAt = null)
                    else -> t.copy(completed = true, completedAt = now())
                }
            }
            val updated = toggled.firstOrNull { it.id == id }
            var finalTasks = toggled
            if (updated != null && updated.completed) {
                RemindersLogic.spawnNextOccurrence(updated, LocalDate.now(), ::generateId, ::now)
                    ?.let { finalTasks = toggled + it }
            }
            p.writeTasks(finalTasks)
        }
    }

    // ── Subtask operations ───────────────────────────────────────────────────────

    suspend fun addSubtask(taskId: String, title: String) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id != taskId) t
                    else t.copy(subtasks = t.subtasks + Subtask(generateId(), title, false, now()))
                },
            )
        }
    }

    suspend fun toggleSubtask(taskId: String, subtaskId: String) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id != taskId) t
                    else t.copy(
                        subtasks = t.subtasks.map { s ->
                            if (s.id == subtaskId) s.copy(completed = !s.completed) else s
                        },
                    )
                },
            )
        }
    }

    suspend fun deleteSubtask(taskId: String, subtaskId: String) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id != taskId) t else t.copy(subtasks = t.subtasks.filter { it.id != subtaskId })
                },
            )
        }
    }

    /** Swap the order values of two tasks (used by reorder mode). No-op if either is gone. */
    suspend fun swapTaskOrder(idA: String, idB: String) {
        dataStore.edit { p ->
            val tasks = p.readTasks()
            val a = tasks.firstOrNull { it.id == idA } ?: return@edit
            val b = tasks.firstOrNull { it.id == idB } ?: return@edit
            p.writeTasks(
                tasks.map { t ->
                    when (t.id) {
                        idA -> t.copy(order = b.order)
                        idB -> t.copy(order = a.order)
                        else -> t
                    }
                },
            )
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────────

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        dataStore.edit { p -> p.writeSettings(transform(p.readSettings())) }
    }

    // ── Backup restore (additive merge by id; never deletes) ─────────────────────

    /** Merge backup lists/tasks in, keeping any existing rows and appending only new ids. */
    suspend fun restoreBackup(lists: List<ReminderList>, tasks: List<Task>) {
        dataStore.edit { p ->
            val curLists = p.readLists()
            val curTasks = p.readTasks()
            val listIds = curLists.mapTo(HashSet()) { it.id }
            val taskIds = curTasks.mapTo(HashSet()) { it.id }
            p.writeLists(curLists + lists.filter { it.id !in listIds })
            p.writeTasks(curTasks + tasks.filter { it.id !in taskIds })
        }
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        val LISTS_KEY = stringPreferencesKey("reminders:lists")
        val TASKS_KEY = stringPreferencesKey("reminders:tasks")
        val SETTINGS_KEY = stringPreferencesKey("reminders:settings")
    }
}

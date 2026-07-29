package com.zacksimpson.reminders.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

/** Stored value exists but can't be parsed; left on disk untouched, not reset. */
class DataCorruptionException(key: String, cause: Throwable) :
    Exception("Stored data for '$key' is unreadable; it was preserved, not overwritten.", cause)

/**
 * Single source of truth for persisted data, backed by the SDK's shared DataStore.
 * No in-memory cache — every mutation is one atomic edit(). A corrupt key throws
 * before any write, so bad data is never overwritten.
 */
class RemindersRepository(private val dataStore: DataStore<Preferences>) {

    private val listSerializer = ListSerializer(ReminderList.serializer())
    private val taskSerializer = ListSerializer(Task.serializer())

    // ── Reads ────────────────────────────────────────────────────────────────

    /** Live snapshot, excludes soft-deleted rows. Throws [DataCorruptionException] if a
     *  stored value is unparseable. */
    val appData: Flow<AppData> = dataStore.data.map { it.toAppData() }

    private fun Preferences.toAppData() = AppData(
        lists = readLists().filterNot { it.deleted },
        tasks = readTasks().filterNot { it.deleted },
        settings = readSettings(),
    )

    /** Full read including tombstones, for [SyncEngine] to propagate deletions. */
    suspend fun snapshotForSync(): AppData =
        dataStore.data.first().let { p -> AppData(p.readLists(), p.readTasks(), p.readSettings()) }

    /** Replaces lists/tasks/settings wholesale with [SyncEngine]'s reconciled result. */
    suspend fun applySyncedState(lists: List<ReminderList>, tasks: List<Task>, settings: Settings) {
        dataStore.edit { p ->
            p.writeLists(lists)
            p.writeTasks(tasks)
            p.writeSettings(settings)
        }
    }

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
            p.writeLists(
                p.readLists().map { if (it.id == id) it.copy(title = title, updatedAt = now()) else it },
            )
        }
    }

    /** Soft-deletes the list and reassigns its tasks to the default list. */
    suspend fun deleteList(id: String) {
        dataStore.edit { p ->
            val defaultId = p.readSettings().defaultListId
            p.writeLists(p.readLists().map { if (it.id == id) it.copy(deleted = true, updatedAt = now()) else it })
            p.writeTasks(
                p.readTasks().map { if (it.listId == id) it.copy(listId = defaultId, updatedAt = now()) else it },
            )
        }
    }

    suspend fun moveListUp(id: String) = reorderList(id, -1)
    suspend fun moveListDown(id: String) = reorderList(id, +1)

    /** Persists the reordered active-list array as one field on Settings — a single
     *  write no matter how many lists exist, matching reminders-web's `reorderLists()`
     *  (LIST_TASK_ORDER_MIGRATION.md). Individual list docs' own `order` field is left
     *  untouched; it only remains as the fallback sort key for ids not yet in the array. */
    private suspend fun reorderList(id: String, direction: Int) {
        dataStore.edit { p ->
            val settings = p.readSettings()
            val active = p.readLists().filterNot { it.deleted }
            val sorted = RemindersLogic.applyOrder(active, ReminderList::id, settings.listOrder) { it.order }
            val idx = sorted.indexOfFirst { it.id == id }
            val target = idx + direction
            if (idx < 0 || target < 0 || target >= sorted.size) return@edit
            val reordered = sorted.toMutableList().apply { add(target, removeAt(idx)) }
            p.writeSettings(settings.copy(listOrder = reordered.map { it.id }, updatedAt = now()))
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
            val order = RemindersLogic.computeOrder(tasks.filterNot { it.deleted }, listId, p.readSettings().addPosition)
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

    /** `id` and `createdAt` are preserved; `updatedAt` always advances to now. */
    suspend fun updateTask(id: String, transform: (Task) -> Task) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id == id) transform(t).copy(id = t.id, createdAt = t.createdAt, updatedAt = now()) else t
                },
            )
        }
    }

    /** Soft-delete, not a real removal — see [SyncableDocument]. */
    suspend fun deleteTask(id: String) {
        dataStore.edit { p ->
            p.writeTasks(p.readTasks().map { if (it.id == id) it.copy(deleted = true, updatedAt = now()) else it })
        }
    }

    suspend fun moveTaskUp(id: String, listId: String) = reorderTask(id, listId, -1)
    suspend fun moveTaskDown(id: String, listId: String) = reorderTask(id, listId, +1)

    /** Persists the reordered active-task array as one field on [listId]'s own
     *  ReminderList doc — a single write no matter how many tasks are in the list,
     *  matching reminders-web's `reorderTasks()` (LIST_TASK_ORDER_MIGRATION.md). Task
     *  docs' own `order` field is left untouched; it only remains as the fallback sort
     *  key for ids not yet in the array. */
    private suspend fun reorderTask(id: String, listId: String, direction: Int) {
        dataStore.edit { p ->
            val lists = p.readLists()
            val list = lists.firstOrNull { it.id == listId } ?: return@edit
            val active = p.readTasks().filter { it.listId == listId && !it.completed && !it.deleted }
            val sorted = RemindersLogic.applyOrder(active, Task::id, list.taskOrder) { it.order }
            val idx = sorted.indexOfFirst { it.id == id }
            val target = idx + direction
            if (idx < 0 || target < 0 || target >= sorted.size) return@edit
            val reordered = sorted.toMutableList().apply { add(target, removeAt(idx)) }
            p.writeLists(
                lists.map { l ->
                    if (l.id == listId) l.copy(taskOrder = reordered.map { it.id }, updatedAt = now()) else l
                },
            )
        }
    }

    /** Soft-deletes every completed task in the list — same as [deleteTask]. */
    suspend fun clearCompletedTasks(listId: String) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map {
                    if (it.listId == listId && it.completed) it.copy(deleted = true, updatedAt = now()) else it
                },
            )
        }
    }

    /** Toggle completion. Completing a dated recurring task also spawns its next occurrence. */
    suspend fun toggleTask(id: String) {
        dataStore.edit { p ->
            val toggled = p.readTasks().map { t ->
                when {
                    t.id != id -> t
                    t.completed -> t.copy(completed = false, completedAt = null, updatedAt = now())
                    else -> t.copy(completed = true, completedAt = now(), updatedAt = now())
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
                    else t.copy(
                        subtasks = t.subtasks + Subtask(generateId(), title, false, now()),
                        updatedAt = now(),
                    )
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
                        updatedAt = now(),
                    )
                },
            )
        }
    }

    suspend fun deleteSubtask(taskId: String, subtaskId: String) {
        dataStore.edit { p ->
            p.writeTasks(
                p.readTasks().map { t ->
                    if (t.id != taskId) t
                    else t.copy(subtasks = t.subtasks.filter { it.id != subtaskId }, updatedAt = now())
                },
            )
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────────

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        dataStore.edit { p -> p.writeSettings(transform(p.readSettings()).copy(updatedAt = now())) }
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

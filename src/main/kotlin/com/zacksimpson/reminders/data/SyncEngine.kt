package com.zacksimpson.reminders.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonObject

class SyncException(message: String) : Exception(message)

/**
 * Orchestrates one full sync pass (SYNC_PLAN.md §3 step 4): pull remote, reconcile with
 * local via [SyncLogic]'s merge, push whatever local won, persist the result back to
 * [RemindersRepository]. The impure, network/storage-touching counterpart to
 * [SyncLogic]'s pure (and unit-tested) functions.
 */
class SyncEngine(
    private val remindersRepo: RemindersRepository,
    private val authRepo: AuthRepository,
    private val firestore: FirestoreClient,
) {
    suspend fun sync() {
        val uid = (authRepo.state.first() as? AuthState.SignedIn)?.uid
            ?: throw SyncException("Not signed in")

        val local = remindersRepo.snapshotForSync()
        val remoteLists = fetchCollection(uid, "lists", ReminderList.serializer())
        val remoteTasks = fetchCollection(uid, "tasks", Task.serializer())
        val remoteSettings = firestore.getDocument(uid, "settings", "singleton")
            ?.let { firestoreModelJson.decodeFromJsonElement(Settings.serializer(), it.fields) }

        val listsResult = SyncLogic.mergeCollection(local.lists, remoteLists)
        val tasksResult = SyncLogic.mergeCollection(local.tasks, remoteTasks)
        val settingsResult = SyncLogic.mergeSettings(local.settings, remoteSettings)

        remindersRepo.applySyncedState(listsResult.merged, tasksResult.merged, settingsResult.merged)

        listsResult.toPush.forEach { pushDocument(uid, "lists", it.id, ReminderList.serializer(), it) }
        tasksResult.toPush.forEach { pushDocument(uid, "tasks", it.id, Task.serializer(), it) }
        if (settingsResult.needsPush) {
            pushDocument(uid, "settings", "singleton", Settings.serializer(), settingsResult.merged)
        }

        authRepo.recordSyncSuccess()
    }

    private suspend fun <T> fetchCollection(uid: String, collection: String, serializer: KSerializer<T>): List<T> =
        firestore.listDocuments(uid, collection)
            .map { firestoreModelJson.decodeFromJsonElement(serializer, it.fields) }

    private suspend fun <T> pushDocument(
        uid: String,
        collection: String,
        docId: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        val fields = firestoreModelJson.encodeToJsonElement(serializer, value).jsonObject
        firestore.setDocument(uid, collection, docId, fields)
    }
}

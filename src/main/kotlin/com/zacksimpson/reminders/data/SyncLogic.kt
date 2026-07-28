package com.zacksimpson.reminders.data

/** Result of reconciling one local vs. one remote copy of an id-keyed collection
 *  (Task, ReminderList): [merged] is what local storage should hold afterward — a
 *  union of both sides, newer [SyncableDocument.updatedAt] wins per id; [toPush] is
 *  exactly the subset that needs writing to Firestore, because the local copy is
 *  newer than (or doesn't exist in) the server's. */
data class CollectionMergeResult<T>(val merged: List<T>, val toPush: List<T>)

/** Result of reconciling the singleton Settings document. */
data class SettingsMergeResult(val merged: Settings, val needsPush: Boolean)

/**
 * Pure last-write-wins merge for phone<->desktop sync (SYNC_PLAN.md §3 step 4).
 * Whole-document conflict resolution, not per-field: comparing `updatedAt` per id is
 * enough because every local mutation — including a delete, which sets
 * `deleted = true` and bumps `updatedAt` rather than removing the row (see
 * RemindersRepository's soft-delete methods) — bumps it. No network or storage calls
 * here; [SyncEngine] is the thin, impure layer that fetches real data and applies the
 * result this produces.
 */
object SyncLogic {
    fun <T : SyncableDocument> mergeCollection(local: List<T>, remote: List<T>): CollectionMergeResult<T> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }

        val merged = mutableListOf<T>()
        val toPush = mutableListOf<T>()

        for (docId in localById.keys + remoteById.keys) {
            val l = localById[docId]
            val r = remoteById[docId]
            when {
                r == null -> requireNotNull(l).let { merged += it; toPush += it }
                l == null -> merged += r
                l.updatedAt > r.updatedAt -> { merged += l; toPush += l }
                r.updatedAt > l.updatedAt -> merged += r
                else -> merged += l // Equal timestamps: already in sync: keep either, push neither.
            }
        }
        return CollectionMergeResult(merged, toPush)
    }

    /** [remote] is null when the user has never synced settings before — local always
     *  wins that case (and needs pushing) since there's nothing to compare against. */
    fun mergeSettings(local: Settings, remote: Settings?): SettingsMergeResult = when {
        remote == null -> SettingsMergeResult(local, needsPush = true)
        local.updatedAt > remote.updatedAt -> SettingsMergeResult(local, needsPush = true)
        remote.updatedAt > local.updatedAt -> SettingsMergeResult(remote, needsPush = false)
        else -> SettingsMergeResult(local, needsPush = false)
    }
}

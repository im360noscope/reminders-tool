package com.zacksimpson.reminders.data

import android.util.Log
import com.thelightphone.sdk.LightJob
import com.thelightphone.sdk.LightJobHandler
import com.thelightphone.sdk.LightJobResult
import kotlinx.coroutines.flow.first

private const val TAG = "SyncJob"

/** WorkManager key shared by the periodic schedule and the immediate one-shot poke —
 *  see [com.zacksimpson.reminders.MainScreen] for where both get enqueued. */
const val SYNC_JOB_KEY = "sync-reminders"

/** Uniqueness slot for the immediate one-shot, distinct from [SYNC_JOB_KEY]'s own
 *  periodic slot so triggering one doesn't cancel the other. Shared by
 *  [com.zacksimpson.reminders.MainScreen]'s on-open poke and Account's "Sync now" row. */
const val SYNC_NOW_TAG = "$SYNC_JOB_KEY-now"

/**
 * Background sync (SYNC_PLAN.md §3 step 5). No-op success when signed out — this job
 * is scheduled unconditionally since there's no signed-out-aware way to cancel it.
 *
 * light-sdk exposes no network-connectivity check to tool code, so this just attempts
 * a sync and lets any failure fall through to [LightJobResult.Retry], which
 * WorkManager backs off and retries automatically.
 */
@LightJob(SYNC_JOB_KEY)
val syncRemindersJob: LightJobHandler = { lightContext, _ ->
    val authRepo = AuthRepository(lightContext.dataStore)
    try {
        if (authRepo.state.first() !is AuthState.SignedIn) {
            LightJobResult.Success()
        } else {
            val firestore = FirestoreClient(authRepo)
            try {
                SyncEngine(RemindersRepository(lightContext.dataStore), authRepo, firestore).sync()
                LightJobResult.Success()
            } finally {
                firestore.close()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Sync failed, will retry", e)
        LightJobResult.Retry
    } finally {
        authRepo.close()
    }
}

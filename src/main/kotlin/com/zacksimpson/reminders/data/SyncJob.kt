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

/**
 * Background sync (SYNC_PLAN.md §3 step 5). A no-op success when signed out — this job
 * is scheduled unconditionally (there's no signed-out-aware way to cancel/reschedule
 * from [com.thelightphone.sdk.LightWork]'s API), so most runs before the user ever signs
 * in are expected to hit this path and return immediately.
 *
 * light-sdk exposes no network-connectivity check to tool code (`SealedLightContext` has
 * no [android.content.Context] tool code can reach — see SYNC_PLAN.md §2.2), so instead
 * of checking connectivity before attempting a sync, this just attempts one and lets a
 * failure (network or otherwise) fall through to [LightJobResult.Retry], which
 * WorkManager backs off and retries automatically — same practical effect without a
 * connectivity API that doesn't exist here.
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

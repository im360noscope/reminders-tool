package com.zacksimpson.reminders

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

/**
 * App-level entry point. Runs once when the tool launches.
 *
 * Notifications are intentionally deferred for v2.0.0 — the SDK currently has no
 * exact-time local alarm capability (see README §Notifications), so push handling
 * is left as a no-op default until a scheduling story is chosen.
 */
@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
        // No server-backed features yet. This tool is fully offline.
    }
}

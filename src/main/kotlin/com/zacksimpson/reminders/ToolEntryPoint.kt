package com.zacksimpson.reminders

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

/** App-level entry point; runs once on launch. Notifications are deferred (see README),
 *  so push handling stays the no-op default for now. */
@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
        // No server-backed features yet; the tool is fully offline.
    }
}

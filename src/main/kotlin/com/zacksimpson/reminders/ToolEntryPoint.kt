package com.zacksimpson.reminders

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

/** app-level entry point; runs once on launch. notifications are deferred (see README),
 *  so push handling stays the no-op default for now. */
@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(serverData: StateFlow<LightServerData?>) {
    }
}

package com.zacksimpson.reminders

import com.zacksimpson.reminders.data.AppData
import com.zacksimpson.reminders.data.RemindersRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** UI state for the app's data: loading, ready, or unreadable (corruption). */
sealed interface DataState {
    data object Loading : DataState
    data class Ready(val data: AppData) : DataState
    data class Corrupt(val message: String) : DataState
}

/** Collect the repository's live data as a [DataState], turning a corruption error into
 *  [DataState.Corrupt] rather than crashing the collector. */
fun RemindersRepository.dataStateIn(scope: CoroutineScope): StateFlow<DataState> =
    appData
        .map<AppData, DataState> { DataState.Ready(it) }
        .catch { e -> emit(DataState.Corrupt(e.message ?: "Data is unreadable")) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), DataState.Loading)

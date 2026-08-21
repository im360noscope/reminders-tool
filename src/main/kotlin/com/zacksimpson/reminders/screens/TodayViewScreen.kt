package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer
import com.zacksimpson.reminders.ui.ToggleSwitch
import kotlinx.coroutines.launch

class TodayViewViewModel(private val repo: RemindersRepository) : LightViewModel<Unit>() {
    val state = repo.dataStateIn(viewModelScope)

    fun setShowOverdue(value: Boolean) {
        viewModelScope.launch { repo.updateSettings { it.copy(showOverdue = value) } }
    }
}

/** Today View settings, just the Show Overdue toggle. */
class TodayViewScreen(
    sealedActivity: SealedLightActivity,
) : LightScreen<Unit, TodayViewViewModel>(sealedActivity) {

    override val viewModelClass: Class<TodayViewViewModel>
        get() = TodayViewViewModel::class.java

    override fun createViewModel() =
        TodayViewViewModel(RemindersRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            val state by viewModel.state.collectAsState()
            val showOverdue = (state as? DataState.Ready)?.data?.settings?.showOverdue ?: true

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text("Today View"),
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                ToggleSwitch(
                    label = "Show Overdue",
                    description = "indicated with *",
                    value = showOverdue,
                    onValueChange = { viewModel.setShowOverdue(it) },
                )
            }
            }
        }
    }
}

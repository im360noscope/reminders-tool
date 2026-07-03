package com.zacksimpson.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.zacksimpson.reminders.ui.RemindersTheme
import kotlinx.coroutines.flow.MutableStateFlow

enum class Tab { LISTS, TODAY, ADD, SETTINGS }

class MainViewModel : LightViewModel<Unit>() {
    val selectedTab = MutableStateFlow(Tab.LISTS)
    fun select(tab: Tab) {
        selectedTab.value = tab
    }
}

/** Boot screen: the four-tab host. Each tab's body is a placeholder until its real
 *  screen is built. */
@InitialScreen
class MainScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, MainViewModel>(sealedActivity) {

    override val viewModelClass: Class<MainViewModel>
        get() = MainViewModel::class.java

    override fun createViewModel() = MainViewModel()

    @Composable
    override fun Content() {
        RemindersTheme {
            val tab by viewModel.selectedTab.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.LISTS -> PlaceholderTab("Lists")
                        Tab.TODAY -> PlaceholderTab("Today")
                        Tab.ADD -> PlaceholderTab("Add")
                        Tab.SETTINGS -> PlaceholderTab("Settings")
                    }
                }
                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(LightIcons.LIST, onClick = { viewModel.select(Tab.LISTS) }),
                        // No calendar/"today" icon in the SDK set yet — ALARM is a placeholder.
                        LightBarButton.LightIcon(LightIcons.ALARM, onClick = { viewModel.select(Tab.TODAY) }),
                        LightBarButton.LightIcon(LightIcons.ADD, onClick = { viewModel.select(Tab.ADD) }),
                        LightBarButton.LightIcon(LightIcons.SETTINGS, onClick = { viewModel.select(Tab.SETTINGS) }),
                    ),
                )
            }
        }
    }
}

/** Placeholder tab body — replaced by the real screen in a later phase. */
@Composable
private fun PlaceholderTab(name: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        LightText(text = name, variant = LightTextVariant.Title)
    }
}

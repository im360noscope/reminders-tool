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
import androidx.lifecycle.viewModelScope
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
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.screens.ListDetailScreen
import com.zacksimpson.reminders.screens.ListsTab
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class Tab { LISTS, TODAY, ADD, SETTINGS }

class MainViewModel(private val repo: RemindersRepository) : LightViewModel<Unit>() {
    val selectedTab = MutableStateFlow(Tab.LISTS)
    val state = repo.dataStateIn(viewModelScope)

    fun select(tab: Tab) {
        selectedTab.value = tab
    }

    fun addList(title: String) {
        viewModelScope.launch { repo.addList(title) }
    }
}

/** Boot screen: the four-tab host. Lists is wired to real data; the others are
 *  placeholders until their screens are built. */
@InitialScreen
class MainScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, MainViewModel>(sealedActivity) {

    override val viewModelClass: Class<MainViewModel>
        get() = MainViewModel::class.java

    override fun createViewModel() = MainViewModel(RemindersRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        RemindersTheme {
            val tab by viewModel.selectedTab.collectAsState()
            val dataState by viewModel.state.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.LISTS -> ListsTab(
                            state = dataState,
                            onAddList = {
                                navigateTo(
                                    screenFactory = { TextEditorScreen(it, TextEditorRequest("New list")) },
                                    resultCallback = { name ->
                                        name.trim().takeIf(String::isNotEmpty)?.let(viewModel::addList)
                                    },
                                )
                            },
                            onOpenList = { list ->
                                navigateTo(screenFactory = { ListDetailScreen(it, list.id, list.title) })
                            },
                        )

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

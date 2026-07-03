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
import com.zacksimpson.reminders.data.AddPosition
import com.zacksimpson.reminders.data.AfterAddBehavior
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.data.Settings
import com.zacksimpson.reminders.screens.ADD_POSITION_OPTIONS
import com.zacksimpson.reminders.screens.AFTER_ADD_OPTIONS
import com.zacksimpson.reminders.screens.ListDetailScreen
import com.zacksimpson.reminders.screens.ListsTab
import com.zacksimpson.reminders.screens.OptionPickerScreen
import com.zacksimpson.reminders.screens.PickerOption
import com.zacksimpson.reminders.screens.SettingsTab
import com.zacksimpson.reminders.screens.addPositionKey
import com.zacksimpson.reminders.screens.afterAddKey
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

    fun setDefaultList(id: String) = update { it.copy(defaultListId = id) }

    fun setAfterAdd(key: String) = update {
        it.copy(afterAddBehavior = if (key == "go-to-list") AfterAddBehavior.GO_TO_LIST else AfterAddBehavior.TOAST)
    }

    fun setAddPosition(key: String) = update {
        it.copy(addPosition = if (key == "top") AddPosition.TOP else AddPosition.BOTTOM)
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { repo.updateSettings(transform) }
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
                        Tab.SETTINGS -> SettingsTab(
                            state = dataState,
                            onOpenDefaultList = {
                                (viewModel.state.value as? DataState.Ready)?.data?.let { d ->
                                    navigateTo(
                                        screenFactory = { activity ->
                                            OptionPickerScreen(
                                                activity,
                                                "Default List",
                                                d.lists.sortedBy { it.order }.map { PickerOption(it.id, it.title) },
                                                d.settings.defaultListId,
                                            )
                                        },
                                        resultCallback = { key -> viewModel.setDefaultList(key) },
                                    )
                                }
                            },
                            onOpenAfterQuickAdd = {
                                (viewModel.state.value as? DataState.Ready)?.data?.let { d ->
                                    navigateTo(
                                        screenFactory = { activity ->
                                            OptionPickerScreen(
                                                activity,
                                                "After Quick Add",
                                                AFTER_ADD_OPTIONS,
                                                afterAddKey(d.settings.afterAddBehavior),
                                            )
                                        },
                                        resultCallback = { key -> viewModel.setAfterAdd(key) },
                                    )
                                }
                            },
                            onOpenAddPosition = {
                                (viewModel.state.value as? DataState.Ready)?.data?.let { d ->
                                    navigateTo(
                                        screenFactory = { activity ->
                                            OptionPickerScreen(
                                                activity,
                                                "Add New Tasks",
                                                ADD_POSITION_OPTIONS,
                                                addPositionKey(d.settings.addPosition),
                                            )
                                        },
                                        resultCallback = { key -> viewModel.setAddPosition(key) },
                                    )
                                }
                            },
                        )
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

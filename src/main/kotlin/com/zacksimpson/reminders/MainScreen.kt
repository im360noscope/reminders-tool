package com.zacksimpson.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.LightWork
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightThemeTokens
import com.zacksimpson.reminders.data.AddPosition
import com.zacksimpson.reminders.data.AfterAddBehavior
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.data.SYNC_JOB_KEY
import com.zacksimpson.reminders.data.SYNC_NOW_TAG
import com.zacksimpson.reminders.data.Settings
import com.zacksimpson.reminders.screens.ADD_POSITION_OPTIONS
import com.zacksimpson.reminders.screens.AFTER_ADD_OPTIONS
import com.zacksimpson.reminders.screens.AccountScreen
import com.zacksimpson.reminders.screens.AddTaskScreen
import com.zacksimpson.reminders.screens.ListAction
import com.zacksimpson.reminders.screens.ListActionsScreen
import com.zacksimpson.reminders.screens.ListDetailScreen
import com.zacksimpson.reminders.screens.ListsTab
import com.zacksimpson.reminders.screens.OptionPickerScreen
import com.zacksimpson.reminders.screens.PickerOption
import com.zacksimpson.reminders.screens.SettingsTab
import com.zacksimpson.reminders.screens.TaskAction
import com.zacksimpson.reminders.screens.TaskActionsScreen
import com.zacksimpson.reminders.screens.TaskDetailScreen
import com.zacksimpson.reminders.screens.TodayTab
import com.zacksimpson.reminders.screens.TodayViewScreen
import com.zacksimpson.reminders.screens.addPositionKey
import com.zacksimpson.reminders.screens.afterAddKey
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

enum class Tab { LISTS, TODAY, SETTINGS }

class MainViewModel(private val repo: RemindersRepository) : LightViewModel<Unit>() {
    val selectedTab = MutableStateFlow(Tab.LISTS)
    val state = repo.dataStateIn(viewModelScope)

    /** Forces the Today tab's date/overdue math to re-run: once a minute while visible,
     *  and immediately whenever this screen is (re)shown (e.g. app resumed from
     *  background overnight) — RN used a 60s interval plus an AppState listener for the
     *  same two reasons. onScreenShow is the SDK-sanctioned hook for the latter;
     *  androidx.compose.ui.platform.LocalLifecycleOwner is a blocked import, so raw
     *  platform lifecycle observation isn't an option here. */
    val refreshTick = MutableStateFlow(0)

    // Lives here rather than as Composable remember state inside TodayTab: MainScreen's
    // Content() (and everything inside it, including TodayTab) gets recomposed fresh
    // whenever a pushed screen (e.g. TaskActionsScreen) pops back to it, which silently
    // discards remember-based state — the ViewModel survives that round trip.
    val todayShowCompleted = MutableStateFlow(false)

    // Same reasoning as todayShowCompleted: ListActionsScreen pops back to this same
    // screen instance, so reorder-mode state can't live in ListsTab's own remember.
    val listsReordering = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshTick.value++
            }
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        refreshTick.value++
    }

    fun select(tab: Tab) {
        selectedTab.value = tab
    }

    fun addList(title: String) {
        viewModelScope.launch { repo.addList(title) }
    }

    fun toggleTask(id: String) {
        viewModelScope.launch { repo.toggleTask(id) }
    }

    fun toggleTodayShowCompleted() {
        todayShowCompleted.value = !todayShowCompleted.value
    }

    fun startListsReordering() {
        listsReordering.value = true
    }

    fun stopListsReordering() {
        listsReordering.value = false
    }

    fun moveListUp(id: String) {
        viewModelScope.launch { repo.moveListUp(id) }
    }

    fun moveListDown(id: String) {
        viewModelScope.launch { repo.moveListDown(id) }
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

    // Scheduling lives here, on the Screen, not in MainViewModel's own onScreenShow: a
    // SealedLightContext is only reachable via LightScreen's protected lightContext —
    // LightViewModel.onScreenShow(screen) can't reach it (protected isn't visible across
    // that class boundary), so SYNC_PLAN.md §3 step 5's "trigger from onScreenShow" has
    // to mean this hook instead. enqueuePeriodic/enqueue are both safe to call every
    // time this screen appears — REPLACE/UPDATE policies make them idempotent, not
    // duplicate schedules.
    override fun willShow() {
        super.willShow()
        LightWork.enqueuePeriodic(lightContext, SYNC_JOB_KEY, repeatInterval = 15.minutes)
        LightWork.enqueue(lightContext, SYNC_JOB_KEY, tag = SYNC_NOW_TAG)
    }

    @Composable
    override fun Content() {
        RemindersTheme {
            val tab by viewModel.selectedTab.collectAsState()
            val dataState by viewModel.state.collectAsState()
            val refreshTick by viewModel.refreshTick.collectAsState()
            val todayShowCompleted by viewModel.todayShowCompleted.collectAsState()
            val listsReordering by viewModel.listsReordering.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.LISTS -> ListsTab(
                            state = dataState,
                            isReordering = listsReordering,
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
                            onLongPressList = { list ->
                                navigateTo(
                                    screenFactory = { ListActionsScreen(it, list.id) },
                                    resultCallback = { result ->
                                        when (result) {
                                            ListAction.START_REORDER -> viewModel.startListsReordering()
                                        }
                                    },
                                )
                            },
                            onStopReordering = { viewModel.stopListsReordering() },
                            onMoveListUp = { viewModel.moveListUp(it) },
                            onMoveListDown = { viewModel.moveListDown(it) },
                        )

                        Tab.TODAY -> TodayTab(
                            state = dataState,
                            showOverdue = (dataState as? DataState.Ready)?.data?.settings?.showOverdue ?: true,
                            refreshTick = refreshTick,
                            showCompleted = todayShowCompleted,
                            onToggleShowCompleted = { viewModel.toggleTodayShowCompleted() },
                            onAddTask = {
                                val defaultListId =
                                    (viewModel.state.value as? DataState.Ready)?.data?.settings?.defaultListId ?: "inbox"
                                navigateTo(
                                    screenFactory = { AddTaskScreen(it, defaultListId, LocalDate.now().toString()) },
                                )
                            },
                            onOpenTask = { task ->
                                navigateTo(screenFactory = { TaskDetailScreen(it, task.id) })
                            },
                            onLongPressTask = { task ->
                                navigateTo(
                                    screenFactory = { TaskActionsScreen(it, task.id) },
                                    // TaskActionsScreen already shows the "deleted" toast
                                    // itself before returning.
                                    resultCallback = { result ->
                                        when (result) {
                                            TaskAction.DELETED -> Unit
                                            // Reordering only makes sense within a single list, so
                                            // Today hands off to that task's own List Detail screen
                                            // already in reorder mode. This is a push, not RN's
                                            // router.replace — the SDK has no replace primitive —
                                            // but that's the right shape here anyway: List Detail
                                            // doesn't already exist to hand off to (unlike within
                                            // List Detail's own reorder flow, where TaskActionsScreen
                                            // just pops back to the existing instance instead of
                                            // creating a new one), and a single back-press from it
                                            // correctly lands back on Today underneath.
                                            TaskAction.START_REORDER -> {
                                                val listTitle = (viewModel.state.value as? DataState.Ready)
                                                    ?.data?.lists?.firstOrNull { it.id == task.listId }?.title ?: "List"
                                                navigateTo(
                                                    screenFactory = {
                                                        ListDetailScreen(it, task.listId, listTitle, startInReorderMode = true)
                                                    },
                                                )
                                            }
                                        }
                                    },
                                )
                            },
                            onToggle = { viewModel.toggleTask(it) },
                        )
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
                            onOpenTodayView = {
                                navigateTo(screenFactory = { TodayViewScreen(it) })
                            },
                            onOpenAccount = {
                                navigateTo(screenFactory = { AccountScreen(it) })
                            },
                        )
                    }
                }
                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(LightIcons.LIST, onClick = { viewModel.select(Tab.LISTS) }),
                        // No calendar/"today" icon in the SDK set yet — ALARM is a placeholder.
                        LightBarButton.LightIcon(LightIcons.ALARM, onClick = { viewModel.select(Tab.TODAY) }),
                        // Add is an action, not a tab — it opens the New Task screen.
                        LightBarButton.LightIcon(LightIcons.ADD, onClick = {
                            val defaultListId =
                                (viewModel.state.value as? DataState.Ready)?.data?.settings?.defaultListId ?: "inbox"
                            navigateTo(screenFactory = { AddTaskScreen(it, defaultListId) })
                        }),
                        LightBarButton.LightIcon(LightIcons.SETTINGS, onClick = { viewModel.select(Tab.SETTINGS) }),
                    ),
                )
            }
        }
    }
}

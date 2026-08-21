package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.R
import com.zacksimpson.reminders.data.AppData
import com.zacksimpson.reminders.data.RemindersLogic
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.data.Task
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer
import com.zacksimpson.reminders.ui.TaskRowView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ListDetailViewModel(
    private val repo: RemindersRepository,
    val listId: String,
    startInReorderMode: Boolean = false,
    initialData: AppData? = null,
) : LightViewModel<Unit>() {
    val state = repo.dataStateIn(viewModelScope, initialData?.let { DataState.Ready(it) } ?: DataState.Loading)
    // Live here rather than as Composable remember state: the SDK recomposes this
    // screen's Content() fresh whenever a pushed screen pops back to it, which
    // discards remember-based state — the ViewModel survives that round trip.
    val isReordering = MutableStateFlow(startInReorderMode)
    val showCompleted = MutableStateFlow(false)

    fun toggleTask(id: String) {
        viewModelScope.launch { repo.toggleTask(id) }
    }

    fun moveTaskUp(id: String) {
        viewModelScope.launch { repo.moveTaskUp(id, listId) }
    }

    fun moveTaskDown(id: String) {
        viewModelScope.launch { repo.moveTaskDown(id, listId) }
    }

    fun startReordering() {
        isReordering.value = true
    }

    fun stopReordering() {
        isReordering.value = false
    }

    fun toggleShowCompleted() {
        showCompleted.value = !showCompleted.value
    }
}

/**
 * A single list's tasks: active (sorted by order) then a collapsible completed section
 * (sorted newest-completed-first), matching the RN list-detail screen. Tapping a task row's
 * checkbox/overdue-asterisk toggles completion; tapping its content opens TaskDetailScreen
 * for full editing. [startInReorderMode] lets Today's task-actions flow land here already
 * reordering, since reordering only makes sense within a single list.
 */
class ListDetailScreen(
    sealedActivity: SealedLightActivity,
    private val listId: String,
    private val listTitle: String,
    private val startInReorderMode: Boolean = false,
    private val initialData: AppData? = null,
) : LightScreen<Unit, ListDetailViewModel>(sealedActivity) {

    override val viewModelClass: Class<ListDetailViewModel>
        get() = ListDetailViewModel::class.java

    override fun createViewModel() =
        ListDetailViewModel(RemindersRepository(lightContext.dataStore), listId, startInReorderMode, initialData)

    @Composable
    override fun Content() {
        RemindersTheme {
            val state by viewModel.state.collectAsState()
            val showCompleted by viewModel.showCompleted.collectAsState()
            val isReordering by viewModel.isReordering.collectAsState()

            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text(listTitle),
                    rightButton = if (isReordering) {
                        LightBarButton.Text("DONE", onClick = { viewModel.stopReordering() })
                    } else {
                        // sizeUnits reduced — ic_plus's artwork fills its box edge-to-edge,
                        // unlike LightIcons' own icons, so the default size reads too big.
                        LightBarButton.Icon(
                            painterResource(R.drawable.ic_plus),
                            onClick = {
                                val d = (viewModel.state.value as? DataState.Ready)?.data
                                navigateTo(
                                    screenFactory = {
                                        AddTaskScreen(it, listId, initialData = d, honorAfterAddBehavior = false, isModal = false)
                                    },
                                )
                            },
                            sizeUnits = 1.2f,
                        )
                    },
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                when (val s = state) {
                    is DataState.Loading -> Unit

                    is DataState.Corrupt -> Message(s.message)

                    is DataState.Ready -> {
                        val listTasks = s.data.tasks.filter { it.listId == listId }
                        val taskOrder = s.data.lists.firstOrNull { it.id == listId }?.taskOrder
                        val active = RemindersLogic.applyOrder(
                            listTasks.filterNot { it.completed },
                            Task::id,
                            taskOrder,
                        ) { it.order }
                        val completed = listTasks.filter { it.completed }
                            .sortedByDescending { it.completedAt ?: 0L }

                        if (listTasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LightText(text = "no tasks", variant = LightTextVariant.Copy)
                            }
                        } else {
                            LightScrollView(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                active.forEachIndexed { index, task ->
                                    TaskRowView(
                                        task = task,
                                        listTitle = listTitle,
                                        onToggle = { viewModel.toggleTask(task.id) },
                                        onPress = {
                                            navigateTo(screenFactory = { TaskDetailScreen(it, task.id, initialData = s.data) })
                                        },
                                        onLongPress = {
                                            navigateTo(
                                                screenFactory = { TaskActionsScreen(it, task.id, s.data) },
                                                resultCallback = { result ->
                                                    when (result) {
                                                        TaskAction.DELETED -> Unit
                                                        TaskAction.START_REORDER -> viewModel.startReordering()
                                                    }
                                                },
                                            )
                                        },
                                        isReordering = isReordering,
                                        isFirst = index == 0,
                                        isLast = index == active.lastIndex,
                                        onMoveUp = { viewModel.moveTaskUp(task.id) },
                                        onMoveDown = { viewModel.moveTaskDown(task.id) },
                                    )
                                }

                                if (completed.isNotEmpty()) {
                                    LightText(
                                        text = "Completed (${completed.size})",
                                        variant = LightTextVariant.Paragraph,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .lightClickable { viewModel.toggleShowCompleted() }
                                            .alpha(0.5f)
                                            .padding(
                                                start = 1.5f.gridUnitsAsDp(),
                                                top = 1f.gridUnitsAsDp(),
                                                bottom = 1f.gridUnitsAsDp(),
                                            ),
                                    )
                                    if (showCompleted) {
                                        completed.forEach { task ->
                                            TaskRowView(
                                                task = task,
                                                listTitle = listTitle,
                                                onToggle = { viewModel.toggleTask(task.id) },
                                                onPress = {
                                            navigateTo(screenFactory = { TaskDetailScreen(it, task.id, initialData = s.data) })
                                        },
                                                onLongPress = {
                                                    navigateTo(
                                                        screenFactory = { TaskActionsScreen(it, task.id, s.data) },
                                                        resultCallback = { result ->
                                                            when (result) {
                                                                TaskAction.DELETED -> Unit
                                                                TaskAction.START_REORDER -> viewModel.startReordering()
                                                            }
                                                        },
                                                    )
                                                },
                                                dimmed = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    @Composable
    private fun Message(text: String) {
        LightText(
            text = text,
            variant = LightTextVariant.Copy,
            modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
        )
    }
}

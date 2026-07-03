package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.TaskRowView
import kotlinx.coroutines.launch

class ListDetailViewModel(
    private val repo: RemindersRepository,
    val listId: String,
) : LightViewModel<Unit>() {
    val state = repo.dataStateIn(viewModelScope)

    fun toggleTask(id: String) {
        viewModelScope.launch { repo.toggleTask(id) }
    }
}

/**
 * A single list's tasks: active (sorted by order) then a collapsible completed section
 * (sorted newest-completed-first), matching the RN list-detail screen. Tapping a task row
 * toggles completion via the checkbox/overdue-asterisk; opening the task for full editing
 * isn't built yet (Task Detail is its own, larger screen — see project notes) so the row's
 * content area is a no-op tap for now. Reorder mode is deferred alongside list actions.
 */
class ListDetailScreen(
    sealedActivity: SealedLightActivity,
    private val listId: String,
    private val listTitle: String,
) : LightScreen<Unit, ListDetailViewModel>(sealedActivity) {

    override val viewModelClass: Class<ListDetailViewModel>
        get() = ListDetailViewModel::class.java

    override fun createViewModel() =
        ListDetailViewModel(RemindersRepository(lightContext.dataStore), listId)

    @Composable
    override fun Content() {
        RemindersTheme {
            val state by viewModel.state.collectAsState()
            var showCompleted by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text(listTitle),
                    rightButton = LightBarButton.LightIcon(
                        LightIcons.ADD,
                        onClick = { navigateTo(screenFactory = { AddTaskScreen(it, listId) }) },
                    ),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                when (val s = state) {
                    is DataState.Loading -> Unit

                    is DataState.Corrupt -> Message(s.message)

                    is DataState.Ready -> {
                        val listTasks = s.data.tasks.filter { it.listId == listId }
                        val active = listTasks.filterNot { it.completed }.sortedBy { it.order }
                        val completed = listTasks.filter { it.completed }
                            .sortedByDescending { it.completedAt ?: 0L }

                        if (listTasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LightText(text = "no tasks", variant = LightTextVariant.Copy)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                active.forEach { task ->
                                    TaskRowView(
                                        task = task,
                                        listTitle = listTitle,
                                        onToggle = { viewModel.toggleTask(task.id) },
                                        onPress = {},
                                    )
                                }

                                if (completed.isNotEmpty()) {
                                    LightText(
                                        text = "Completed (${completed.size})",
                                        variant = LightTextVariant.Paragraph,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCompleted = !showCompleted }
                                            .alpha(0.5f)
                                            .padding(
                                                horizontal = 1.5f.gridUnitsAsDp(),
                                                vertical = 1f.gridUnitsAsDp(),
                                            ),
                                    )
                                    if (showCompleted) {
                                        completed.forEach { task ->
                                            TaskRowView(
                                                task = task,
                                                listTitle = listTitle,
                                                onToggle = { viewModel.toggleTask(task.id) },
                                                onPress = {},
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

    @Composable
    private fun Message(text: String) {
        LightText(
            text = text,
            variant = LightTextVariant.Copy,
            modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
        )
    }
}

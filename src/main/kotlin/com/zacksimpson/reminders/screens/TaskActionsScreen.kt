package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.ConfirmScreen
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer
import kotlinx.coroutines.launch

/** Signals what happened back to whichever screen opened this one. */
enum class TaskAction { DELETED, START_REORDER }

class TaskActionsViewModel(
    private val repo: RemindersRepository,
    private val taskId: String,
) : LightViewModel<TaskAction>() {
    val state = repo.dataStateIn(viewModelScope)

    fun toggle() {
        viewModelScope.launch { repo.toggleTask(taskId) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            onDeleted()
        }
    }
}

/**
 * Long-press action sheet for a task — Mark Complete/Incomplete, Edit Details, Reorder
 * Tasks, Delete — ported from RN's task-actions/[id].tsx.
 */
class TaskActionsScreen(
    sealedActivity: SealedLightActivity,
    private val taskId: String,
) : LightScreen<TaskAction, TaskActionsViewModel>(sealedActivity) {

    override val viewModelClass: Class<TaskActionsViewModel>
        get() = TaskActionsViewModel::class.java

    override fun createViewModel() =
        TaskActionsViewModel(RemindersRepository(lightContext.dataStore), taskId)

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            val state by viewModel.state.collectAsState()
            val task = (state as? DataState.Ready)?.data?.tasks?.firstOrNull { it.id == taskId }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text("Edit Task"),
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                ActionRow(
                    text = if (task?.completed == true) "Mark as Incomplete" else "Mark as Completed",
                    onClick = {
                        viewModel.toggle()
                        goBack(null)
                    },
                )
                ActionRow(
                    text = "Edit Details",
                    onClick = { navigateTo(screenFactory = { TaskDetailScreen(it, taskId) }) },
                )
                ActionRow(
                    text = "Reorder Tasks",
                    onClick = { goBack(TaskAction.START_REORDER) },
                )
                ActionRow(
                    text = "Delete Task",
                    onClick = {
                        val message = if (task != null) {
                            "Are you sure you want to delete \"${task.title}\"?"
                        } else {
                            "Are you sure you want to delete this task?"
                        }
                        navigateTo(
                            screenFactory = { ConfirmScreen(it, message, "Delete") },
                            resultCallback = { confirmed ->
                                if (confirmed == true) viewModel.delete { goBack(TaskAction.DELETED) }
                            },
                        )
                    },
                )
            }
            }
        }
    }

    @Composable
    private fun ActionRow(text: String, onClick: () -> Unit) {
        LightText(
            text = text,
            variant = LightTextVariant.Heading,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
        )
    }
}

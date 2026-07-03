package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
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
import com.zacksimpson.reminders.data.Subtask
import com.zacksimpson.reminders.data.generateId
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.DeleteIcon
import com.zacksimpson.reminders.ui.PlusCircleIcon
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.TapField
import com.zacksimpson.reminders.ui.TaskCheckboxIcon
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import com.zacksimpson.reminders.ui.TitleField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddTaskViewModel(
    private val repo: RemindersRepository,
    defaultListId: String,
) : LightViewModel<Unit>() {
    val title = MutableStateFlow("")
    val selectedListId = MutableStateFlow(defaultListId)
    val subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val state = repo.dataStateIn(viewModelScope)

    fun setTitle(value: String) {
        title.value = value
    }

    fun setListId(id: String) {
        selectedListId.value = id
    }

    fun addSubtask(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        subtasks.value = subtasks.value + Subtask(generateId(), t, completed = false, createdAt = System.currentTimeMillis())
    }

    fun renameSubtask(id: String, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        subtasks.value = subtasks.value.map { if (it.id == id) it.copy(title = t) else it }
    }

    fun toggleSubtask(id: String) {
        subtasks.value = subtasks.value.map { if (it.id == id) it.copy(completed = !it.completed) else it }
    }

    fun removeSubtask(id: String) {
        subtasks.value = subtasks.value.filter { it.id != id }
    }

    fun save(onSaved: () -> Unit) {
        val t = title.value.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            repo.addTask(title = t, listId = selectedListId.value, subtasks = subtasks.value)
            onSaved()
        }
    }
}

/**
 * New-task form. Title, List, and Subtasks are functional. Date/Time/Recurring are shown
 * unconditionally with "None" and a no-op tap until their pickers are built — RN only shows
 * Time/Recurring once a date is set; restore that gating when Date is real.
 */
class AddTaskScreen(
    sealedActivity: SealedLightActivity,
    private val defaultListId: String,
) : LightScreen<Unit, AddTaskViewModel>(sealedActivity) {

    override val viewModelClass: Class<AddTaskViewModel>
        get() = AddTaskViewModel::class.java

    override fun createViewModel() =
        AddTaskViewModel(RemindersRepository(lightContext.dataStore), defaultListId)

    @Composable
    override fun Content() {
        RemindersTheme {
            val title by viewModel.title.collectAsState()
            val listId by viewModel.selectedListId.collectAsState()
            val subtasks by viewModel.subtasks.collectAsState()
            val state by viewModel.state.collectAsState()
            val lists = (state as? DataState.Ready)?.data?.lists.orEmpty()
            val selectedListTitle = lists.firstOrNull { it.id == listId }?.title ?: "Inbox"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text("New Task"),
                    rightButton = if (title.isNotBlank()) {
                        LightBarButton.LightIcon(LightIcons.ACCEPT, onClick = { viewModel.save { goBack(null) } })
                    } else {
                        null
                    },
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TitleField(
                        value = title,
                        placeholder = "Task name",
                        onClick = {
                            navigateTo(
                                screenFactory = { TextEditorScreen(it, TextEditorRequest("Task name", title)) },
                                resultCallback = { viewModel.setTitle(it) },
                            )
                        },
                    )

                    TapField(
                        label = "List",
                        value = selectedListTitle,
                        onClick = {
                            navigateTo(
                                screenFactory = { activity ->
                                    OptionPickerScreen(
                                        activity,
                                        "List",
                                        lists.sortedBy { it.order }.map { PickerOption(it.id, it.title) },
                                        listId,
                                    )
                                },
                                resultCallback = { key -> viewModel.setListId(key) },
                            )
                        },
                    )

                    // Date/Time/Recurring: layout only for now — pickers not built yet.
                    TapField(label = "Date", value = "None", onClick = {})
                    TapField(label = "Time", value = "None", onClick = {})
                    TapField(label = "Recurring", value = "None", onClick = {})

                    SubtasksSection(
                        subtasks = subtasks,
                        onAdd = {
                            navigateTo(
                                screenFactory = { TextEditorScreen(it, TextEditorRequest("Subtask")) },
                                resultCallback = { text -> viewModel.addSubtask(text) },
                            )
                        },
                        onRename = { subtask ->
                            navigateTo(
                                screenFactory = { TextEditorScreen(it, TextEditorRequest("Subtask", subtask.title)) },
                                resultCallback = { text -> viewModel.renameSubtask(subtask.id, text) },
                            )
                        },
                        onToggle = { viewModel.toggleSubtask(it) },
                        onDelete = { viewModel.removeSubtask(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtasksSection(
    subtasks: List<Subtask>,
    onAdd: () -> Unit,
    onRename: (Subtask) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column {
        LightText(
            text = "Subtasks",
            variant = LightTextVariant.Superfine,
            modifier = Modifier.padding(
                start = 1.5f.gridUnitsAsDp(),
                top = 1.5f.gridUnitsAsDp(),
                bottom = 0.5f.gridUnitsAsDp(),
            ),
        )
        subtasks.forEach { subtask ->
            SubtaskRow(subtask, onRename = { onRename(subtask) }, onToggle = { onToggle(subtask.id) }, onDelete = { onDelete(subtask.id) })
        }
        Row(
            modifier = Modifier
                .clickable(onClick = onAdd)
                .padding(start = 1f.gridUnitsAsDp(), top = 0.6f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlusCircleIcon(size = 20.dp, modifier = Modifier.padding(horizontal = 0.9f.gridUnitsAsDp()))
            LightText(text = "Add subtask…", variant = LightTextVariant.Paragraph)
        }
    }
}

@Composable
private fun SubtaskRow(subtask: Subtask, onRename: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.5f.gridUnitsAsDp(), end = 1.5f.gridUnitsAsDp()),
        verticalAlignment = Alignment.Top,
    ) {
        TaskCheckboxIcon(
            checked = subtask.completed,
            size = 20.dp,
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 0.9f.gridUnitsAsDp(), vertical = 0.6f.gridUnitsAsDp()),
        )
        LightText(
            text = subtask.title,
            variant = LightTextVariant.Paragraph,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onRename)
                .padding(vertical = 0.65f.gridUnitsAsDp())
                .alpha(if (subtask.completed) 0.4f else 1f),
        )
        DeleteIcon(
            size = 14.dp,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(start = 0.5f.gridUnitsAsDp(), top = 0.75f.gridUnitsAsDp(), bottom = 0.6f.gridUnitsAsDp()),
        )
    }
}

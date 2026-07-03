package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
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
import com.zacksimpson.reminders.data.Recurrence
import com.zacksimpson.reminders.data.Task
import com.zacksimpson.reminders.data.formatDisplayDate
import com.zacksimpson.reminders.data.formatRecurrence
import com.zacksimpson.reminders.data.formatTime
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.ConfirmScreen
import com.zacksimpson.reminders.ui.DeleteIcon
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SubtasksSection
import com.zacksimpson.reminders.ui.TapField
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import com.zacksimpson.reminders.ui.TitleField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val repo: RemindersRepository,
    private val taskId: String,
) : LightViewModel<Unit>() {
    val title = MutableStateFlow("")
    val selectedListId = MutableStateFlow("")
    val date = MutableStateFlow<String?>(null)
    val time = MutableStateFlow<String?>(null)
    val recurrence = MutableStateFlow<Recurrence?>(null)
    val seeded = MutableStateFlow(false)
    val notFound = MutableStateFlow(false)
    val state = repo.dataStateIn(viewModelScope)

    init {
        // Draft fields are seeded ONCE from the task's state at open time, then evolve
        // independently until Save — mirrors the RN screen's useState(task?.x) seeding.
        // Subtasks are NOT drafted here; they read/write live (see below), matching RN's
        // addSubtask/toggleSubtask/deleteSubtask calls, which apply immediately.
        viewModelScope.launch {
            val first = state.first { it !is DataState.Loading }
            val task = (first as? DataState.Ready)?.data?.tasks?.firstOrNull { it.id == taskId }
            if (task == null) {
                notFound.value = true
            } else {
                title.value = task.title
                selectedListId.value = task.listId
                date.value = task.date
                time.value = task.time
                recurrence.value = task.recurrence
            }
            seeded.value = true
        }
    }

    fun setTitle(value: String) {
        title.value = value
    }

    fun setListId(id: String) {
        selectedListId.value = id
    }

    fun clearDate() {
        date.value = null
        time.value = null
        recurrence.value = null
    }

    fun clearTime() {
        time.value = null
    }

    fun clearRecurrence() {
        recurrence.value = null
    }

    fun save(onSaved: () -> Unit) {
        val t = title.value.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            repo.updateTask(taskId) {
                it.copy(
                    title = t,
                    listId = selectedListId.value,
                    date = date.value,
                    time = time.value,
                    recurrence = recurrence.value,
                )
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            onDeleted()
        }
    }

    // Subtask mutations are immediate/live — the task already exists, so there's no
    // "draft" to hold them in (unlike Add Task, which is creating a brand-new task).
    fun addSubtask(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { repo.addSubtask(taskId, t) }
    }

    fun renameSubtask(id: String, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            repo.updateTask(taskId) { task ->
                task.copy(subtasks = task.subtasks.map { if (it.id == id) it.copy(title = t) else it })
            }
        }
    }

    fun toggleSubtask(id: String) {
        viewModelScope.launch { repo.toggleSubtask(taskId, id) }
    }

    fun removeSubtask(id: String) {
        viewModelScope.launch { repo.deleteSubtask(taskId, id) }
    }
}

/**
 * Full task edit screen — the RN app's biggest single screen, ported field-for-field:
 * title, list, date/time/recurring, subtasks, delete.
 *
 * Save is an explicit checkmark tap rather than RN's auto-save-on-swipe-back: the SDK's
 * system back gesture calls the activity's goBack() directly and never consults the
 * screen's ViewModel, so there's no reliable hook to save on back (confirmed by reading
 * LightActivity/LightScreen source) — matches the explicit-Save pattern already used on
 * Add Task.
 */
class TaskDetailScreen(
    sealedActivity: SealedLightActivity,
    private val taskId: String,
) : LightScreen<Unit, TaskDetailViewModel>(sealedActivity) {

    override val viewModelClass: Class<TaskDetailViewModel>
        get() = TaskDetailViewModel::class.java

    override fun createViewModel() =
        TaskDetailViewModel(RemindersRepository(lightContext.dataStore), taskId)

    @Composable
    override fun Content() {
        RemindersTheme {
            val seeded by viewModel.seeded.collectAsState()
            val notFound by viewModel.notFound.collectAsState()
            val title by viewModel.title.collectAsState()
            val listId by viewModel.selectedListId.collectAsState()
            val date by viewModel.date.collectAsState()
            val time by viewModel.time.collectAsState()
            val recurrence by viewModel.recurrence.collectAsState()
            val state by viewModel.state.collectAsState()

            val ready = state as? DataState.Ready
            val liveTask = ready?.data?.tasks?.firstOrNull { it.id == taskId }
            val lists = ready?.data?.lists.orEmpty()
            val selectedListTitle = lists.firstOrNull { it.id == listId }?.title ?: "Inbox"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text("Edit"),
                    rightButton = LightBarButton.LightIcon(
                        LightIcons.ACCEPT,
                        onClick = { viewModel.save { goBack(null) } },
                    ),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                when {
                    !seeded -> Unit

                    notFound -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LightText(text = "task not found", variant = LightTextVariant.Paragraph)
                    }

                    else -> Column(
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

                        // Date/Time/Recurring: display + clear are wired for forward-compat,
                        // but there's no picker yet, so tapping the value is a no-op — same
                        // placeholder state as Add Task until those pickers exist.
                        ClearableField(label = "Date", value = date?.let(::formatDisplayDate), onClear = { viewModel.clearDate() })
                        ClearableField(label = "Time", value = time?.let { formatTime(it) }, onClear = { viewModel.clearTime() })
                        ClearableField(
                            label = "Recurring",
                            value = recurrence?.let(::formatRecurrence),
                            onClear = { viewModel.clearRecurrence() },
                        )

                        SubtasksSection(
                            subtasks = liveTask?.subtasks.orEmpty(),
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

                        DeleteRow(task = liveTask, onDeleted = { viewModel.delete { goBack(null) } })
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearableField(label: String, value: String?, onClear: () -> Unit) {
        if (value == null) {
            TapField(label = label, value = "None", onClick = {})
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    LightText(text = label, variant = LightTextVariant.Superfine)
                    LightText(
                        text = value,
                        variant = LightTextVariant.Copy,
                        modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
                    )
                }
                DeleteIcon(
                    size = 14.dp,
                    modifier = Modifier
                        .clickable(onClick = onClear)
                        .padding(top = 0.9f.gridUnitsAsDp(), start = 1f.gridUnitsAsDp()),
                )
            }
        }
    }

    @Composable
    private fun DeleteRow(task: Task?, onDeleted: () -> Unit) {
        LightText(
            text = "DELETE",
            variant = LightTextVariant.Button,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (task == null) return@clickable
                    val message = if (task.recurrence != null) {
                        "This is a recurring task. Delete all occurrences?"
                    } else {
                        "Are you sure you want to delete \"${task.title}\"?"
                    }
                    navigateTo(
                        screenFactory = { ConfirmScreen(it, message, "Delete") },
                        resultCallback = { confirmed -> if (confirmed == true) onDeleted() },
                    )
                }
                .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1.8f.gridUnitsAsDp()),
            align = TextAlign.Center,
        )
    }
}

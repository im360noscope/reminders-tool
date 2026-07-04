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
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.Recurrence
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.data.Subtask
import com.zacksimpson.reminders.data.formatDisplayDate
import com.zacksimpson.reminders.data.formatRecurrence
import com.zacksimpson.reminders.data.formatTime
import com.zacksimpson.reminders.data.generateId
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.ClearableField
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SubtasksSection
import com.zacksimpson.reminders.ui.TapField
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import com.zacksimpson.reminders.ui.TitleField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddTaskViewModel(
    private val repo: RemindersRepository,
    defaultListId: String,
    defaultDate: String?,
) : LightViewModel<Unit>() {
    val title = MutableStateFlow("")
    val selectedListId = MutableStateFlow(defaultListId)
    val date = MutableStateFlow(defaultDate)
    val time = MutableStateFlow<String?>(null)
    val recurrence = MutableStateFlow<Recurrence?>(null)
    val subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val state = repo.dataStateIn(viewModelScope)

    fun setTitle(value: String) {
        title.value = value
    }

    fun setListId(id: String) {
        selectedListId.value = id
    }

    fun setDate(value: String) {
        date.value = value
    }

    fun clearDate() {
        date.value = null
        time.value = null
        recurrence.value = null
    }

    fun setTime(value: String) {
        time.value = value
    }

    fun clearTime() {
        time.value = null
    }

    fun setRecurrence(value: Recurrence) {
        recurrence.value = value
    }

    fun clearRecurrence() {
        recurrence.value = null
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
            repo.addTask(
                title = t,
                listId = selectedListId.value,
                date = date.value,
                time = time.value,
                recurrence = recurrence.value,
                subtasks = subtasks.value,
            )
            onSaved()
        }
    }
}

/**
 * New-task form. Title, List, Date, and Subtasks are functional. Time/Recurring are still
 * placeholders (no picker yet) and, matching RN, only appear once a Date is set.
 */
class AddTaskScreen(
    sealedActivity: SealedLightActivity,
    private val defaultListId: String,
    private val defaultDate: String? = null,
) : LightScreen<Unit, AddTaskViewModel>(sealedActivity) {

    override val viewModelClass: Class<AddTaskViewModel>
        get() = AddTaskViewModel::class.java

    override fun createViewModel() =
        AddTaskViewModel(RemindersRepository(lightContext.dataStore), defaultListId, defaultDate)

    @Composable
    override fun Content() {
        RemindersTheme {
            val title by viewModel.title.collectAsState()
            val listId by viewModel.selectedListId.collectAsState()
            val date by viewModel.date.collectAsState()
            val time by viewModel.time.collectAsState()
            val recurrence by viewModel.recurrence.collectAsState()
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
                        // ACCEPT's own artwork fills its box edge-to-edge (unlike BACK's,
                        // which has generous built-in padding), so it reads much bigger at
                        // the same default sizeUnits — sized down to match BACK's weight.
                        LightBarButton.LightIcon(LightIcons.ACCEPT, onClick = { viewModel.save { goBack(null) } }, sizeUnits = 1.5f)
                    } else {
                        null
                    },
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier.fillMaxSize(),
                    scrollBarPosition = LightScrollBarPosition.Inside,
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

                    ClearableField(
                        label = "Date",
                        value = date?.let(::formatDisplayDate),
                        onClick = {
                            navigateTo(
                                screenFactory = { DatePickerScreen(it, date) },
                                resultCallback = { result -> result?.let { viewModel.setDate(it) } },
                            )
                        },
                        onClear = { viewModel.clearDate() },
                    )
                    // Time/Recurring only shown once a date is set, matching RN. Still
                    // placeholders: no Time/Recurrence picker yet.
                    if (date != null) {
                        ClearableField(
                            label = "Time",
                            value = time?.let { formatTime(it) },
                            onClick = {
                                navigateTo(
                                    screenFactory = { TimePickerScreen(it, time) },
                                    resultCallback = { result -> result?.let { viewModel.setTime(it) } },
                                )
                            },
                            onClear = { viewModel.clearTime() },
                        )
                        ClearableField(
                            label = "Recurring",
                            value = recurrence?.let(::formatRecurrence),
                            onClick = {
                                navigateTo(
                                    screenFactory = { RecurrencePickerScreen(it, recurrence) },
                                    resultCallback = { result -> result?.let { viewModel.setRecurrence(it) } },
                                )
                            },
                            onClear = { viewModel.clearRecurrence() },
                        )
                    }

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

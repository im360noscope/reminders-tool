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
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.RemindersRepository
import com.zacksimpson.reminders.dataStateIn
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddTaskViewModel(
    private val repo: RemindersRepository,
    defaultListId: String,
) : LightViewModel<Unit>() {
    val title = MutableStateFlow("")
    val selectedListId = MutableStateFlow(defaultListId)
    val state = repo.dataStateIn(viewModelScope)

    fun setTitle(value: String) {
        title.value = value
    }

    fun setListId(id: String) {
        selectedListId.value = id
    }

    fun save(onSaved: () -> Unit) {
        val t = title.value.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            repo.addTask(title = t, listId = selectedListId.value)
            onSaved()
        }
    }
}

/** New-task form. Title + list for now; date/time/recurrence/subtasks land next. */
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

                LightTextField(
                    label = "Title",
                    value = title,
                    placeholder = "Task name",
                    onClick = {
                        navigateTo(
                            screenFactory = { TextEditorScreen(it, TextEditorRequest("Title", title)) },
                            resultCallback = { viewModel.setTitle(it) },
                        )
                    },
                    modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                )

                LightTextField(
                    label = "List",
                    value = selectedListTitle,
                    placeholder = "Inbox",
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
                    modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
                )
            }
        }
    }
}

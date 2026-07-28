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
import com.zacksimpson.reminders.ui.TextEditorRequest
import com.zacksimpson.reminders.ui.TextEditorScreen
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Signals what happened back to whichever screen opened this one. */
enum class ListAction { START_REORDER }

class ListActionsViewModel(
    private val repo: RemindersRepository,
    private val listId: String,
) : LightViewModel<ListAction>() {
    val state = repo.dataStateIn(viewModelScope)

    fun rename(title: String, onRenamed: () -> Unit) {
        val t = title.trim()
        if (t.isEmpty()) {
            onRenamed()
            return
        }
        viewModelScope.launch {
            // Same NonCancellable reasoning as clearCompleted()/delete() below.
            withContext(NonCancellable) {
                repo.renameList(listId, t)
            }
            onRenamed()
        }
    }

    fun clearCompleted(onCleared: () -> Unit) {
        viewModelScope.launch {
            // NonCancellable so the write finishes even if this screen is destroyed
            // mid-write (viewModelScope gets cancelled on destroy).
            withContext(NonCancellable) {
                repo.clearCompletedTasks(listId)
            }
            onCleared()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            // Same NonCancellable reasoning as clearCompleted() above.
            withContext(NonCancellable) {
                repo.deleteList(listId)
            }
            onDeleted()
        }
    }
}

/**
 * Long-press action sheet for a list — Rename, Reorder Lists, Clear Completed, Delete —
 * ported from RN's list-actions/[id]/index.tsx.
 */
class ListActionsScreen(
    sealedActivity: SealedLightActivity,
    private val listId: String,
) : LightScreen<ListAction, ListActionsViewModel>(sealedActivity) {

    override val viewModelClass: Class<ListActionsViewModel>
        get() = ListActionsViewModel::class.java

    override fun createViewModel() =
        ListActionsViewModel(RemindersRepository(lightContext.dataStore), listId)

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            val state by viewModel.state.collectAsState()
            val list = (state as? DataState.Ready)?.data?.lists?.firstOrNull { it.id == listId }
            val listTitle = list?.title ?: "List"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text(listTitle),
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                ActionRow(
                    text = "Rename",
                    onClick = {
                        navigateTo(
                            screenFactory = { TextEditorScreen(it, TextEditorRequest("Rename", listTitle)) },
                            resultCallback = { text -> viewModel.rename(text) { goBack(null) } },
                        )
                    },
                )
                ActionRow(
                    text = "Reorder Lists",
                    onClick = { goBack(ListAction.START_REORDER) },
                )
                ActionRow(
                    text = "Clear Completed",
                    onClick = {
                        navigateTo(
                            screenFactory = {
                                ConfirmScreen(
                                    it,
                                    "Are you sure you want to delete all completed tasks in \"$listTitle\"?",
                                    "Clear",
                                )
                            },
                            resultCallback = { confirmed ->
                                if (confirmed == true) {
                                    viewModel.clearCompleted { goBack(null) }
                                }
                            },
                        )
                    },
                )
                ActionRow(
                    text = "Delete",
                    onClick = {
                        navigateTo(
                            screenFactory = {
                                ConfirmScreen(
                                    it,
                                    "Are you sure you want to delete \"$listTitle\"? Tasks will be moved to your default list.",
                                    "Delete",
                                )
                            },
                            resultCallback = { confirmed ->
                                if (confirmed == true) {
                                    viewModel.delete { goBack(null) }
                                }
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

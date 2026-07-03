package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

class ListDetailViewModel(
    repo: RemindersRepository,
    val listId: String,
) : LightViewModel<Unit>() {
    val state = repo.dataStateIn(viewModelScope)
}

/**
 * A single list's tasks, read-only for now (add/toggle land in later increments).
 * Tasks are sorted by order; completed ones are dimmed to 0.4.
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

                when (val s = state) {
                    is DataState.Loading -> Unit

                    is DataState.Corrupt -> Message(s.message)

                    is DataState.Ready -> {
                        val tasks = s.data.tasks.filter { it.listId == listId }.sortedBy { it.order }
                        if (tasks.isEmpty()) {
                            Message("No tasks yet")
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                tasks.forEach { task ->
                                    LightText(
                                        text = task.title,
                                        variant = LightTextVariant.Copy,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 1f.gridUnitsAsDp(),
                                                vertical = 0.5f.gridUnitsAsDp(),
                                            )
                                            .alpha(if (task.completed) 0.4f else 1f),
                                    )
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
            modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
        )
    }
}

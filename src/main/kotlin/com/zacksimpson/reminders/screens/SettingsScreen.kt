package com.zacksimpson.reminders.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.data.AddPosition
import com.zacksimpson.reminders.data.AfterAddBehavior

val AFTER_ADD_OPTIONS = listOf(
    PickerOption("toast", "Add Next"),
    PickerOption("go-to-list", "Go to List"),
)
val ADD_POSITION_OPTIONS = listOf(
    PickerOption("top", "Top of List"),
    PickerOption("bottom", "Bottom of List"),
)

fun afterAddKey(v: AfterAddBehavior) = if (v == AfterAddBehavior.GO_TO_LIST) "go-to-list" else "toast"
fun addPositionKey(v: AddPosition) = if (v == AddPosition.TOP) "top" else "bottom"

private fun afterAddLabel(v: AfterAddBehavior) = AFTER_ADD_OPTIONS.first { it.key == afterAddKey(v) }.label
private fun addPositionLabel(v: AddPosition) = ADD_POSITION_OPTIONS.first { it.key == addPositionKey(v) }.label

/**
 * Settings tab: each row shows a small label over its current value and opens a picker.
 * Notifications / Today View / Backup rows arrive with those features.
 */
@Composable
fun SettingsTab(
    state: DataState,
    onOpenDefaultList: () -> Unit,
    onOpenAfterQuickAdd: () -> Unit,
    onOpenAddPosition: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = null,
            center = LightTopBarCenter.Text("Settings"),
            rightButton = null,
            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
        )

        when (state) {
            is DataState.Loading -> Unit

            is DataState.Corrupt -> LightText(
                text = state.message,
                variant = LightTextVariant.Copy,
                modifier = Modifier.padding(horizontal = 1.5f.gridUnitsAsDp()),
            )

            is DataState.Ready -> {
                val s = state.data.settings
                val defaultTitle = state.data.lists.firstOrNull { it.id == s.defaultListId }?.title ?: "Inbox"
                LightScrollView(
                    modifier = Modifier.fillMaxSize(),
                    scrollBarPosition = LightScrollBarPosition.Inside,
                ) {
                    SettingsRow("Default List", defaultTitle, onOpenDefaultList)
                    SettingsRow("After Quick Add", afterAddLabel(s.afterAddBehavior), onOpenAfterQuickAdd)
                    SettingsRow("Add New Tasks", addPositionLabel(s.addPosition), onOpenAddPosition)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Paragraph)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}

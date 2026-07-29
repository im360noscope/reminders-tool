package com.zacksimpson.reminders.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.DataState
import com.zacksimpson.reminders.R
import com.zacksimpson.reminders.data.ReminderList
import com.zacksimpson.reminders.data.RemindersLogic
import com.zacksimpson.reminders.ui.ReorderArrows

/**
 * Lists tab: the header plus every list, sorted by order. Tap a list to open it, long-press
 * for Rename/Reorder/Clear Completed/Delete, `+` to add one. In reorder mode, tap/long-press
 * are disabled and up/down arrows appear instead — matches RN's isReordering behavior.
 */
@Composable
fun ListsTab(
    state: DataState,
    isReordering: Boolean,
    onAddList: () -> Unit,
    onOpenList: (ReminderList) -> Unit,
    onLongPressList: (ReminderList) -> Unit,
    onStopReordering: () -> Unit,
    onMoveListUp: (String) -> Unit,
    onMoveListDown: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = null,
            center = LightTopBarCenter.Text("Lists"),
            rightButton = if (isReordering) {
                LightBarButton.Text("DONE", onClick = onStopReordering)
            } else {
                // ic_plus's artwork fills its box edge-to-edge, unlike LightIcons' own
                // icons — sizeUnits reduced to match BACK's visual weight.
                LightBarButton.Icon(painterResource(R.drawable.ic_plus), onClick = onAddList, sizeUnits = 1.2f)
            },
            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
        )

        when (state) {
            is DataState.Loading -> Unit

            is DataState.Corrupt -> LightText(
                text = state.message,
                variant = LightTextVariant.Copy,
                modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
            )

            is DataState.Ready -> LightScrollView(
                modifier = Modifier.fillMaxSize(),
                scrollBarPosition = LightScrollBarPosition.Inside,
            ) {
                val sorted = RemindersLogic.applyOrder(
                    state.data.lists,
                    ReminderList::id,
                    state.data.settings.listOrder,
                ) { it.order }
                sorted.forEachIndexed { index, list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Scroll-indicator clearance (LightScrollBarPosition.Inside
                            // overlays rather than reserving its own column) — otherwise
                            // ReorderArrows sits under the indicator in reorder mode.
                            .padding(end = 2f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = list.title,
                            variant = LightTextVariant.Heading,
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = { if (!isReordering) onOpenList(list) },
                                    onLongClick = if (isReordering) null else { { onLongPressList(list) } },
                                )
                                .padding(
                                    horizontal = 1.5f.gridUnitsAsDp(),
                                    // Matches ActionRow/OptionPickerScreen/TapField's
                                    // vertical inset — same RN paddingVertical/fontSize
                                    // family (n(11-14) + n(30)) as this row.
                                    vertical = 0.75f.gridUnitsAsDp(),
                                ),
                        )
                        if (isReordering) {
                            ReorderArrows(
                                isFirst = index == 0,
                                isLast = index == sorted.lastIndex,
                                onMoveUp = { onMoveListUp(list.id) },
                                onMoveDown = { onMoveListDown(list.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

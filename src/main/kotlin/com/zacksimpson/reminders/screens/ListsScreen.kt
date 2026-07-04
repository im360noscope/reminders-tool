package com.zacksimpson.reminders.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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

/**
 * Lists tab: the header plus every list, sorted by order. Tap a list to open it, `+` to
 * add one. (List detail and long-press actions are wired in later increments.)
 */
@Composable
fun ListsTab(
    state: DataState,
    onAddList: () -> Unit,
    onOpenList: (ReminderList) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LightTopBar(
            leftButton = null,
            center = LightTopBarCenter.Text("Lists"),
            // ic_plus's artwork fills its box edge-to-edge (unlike LightIcons' own icons,
            // which have built-in padding) — sizeUnits is reduced to match BACK's visual weight.
            rightButton = LightBarButton.Icon(painterResource(R.drawable.ic_plus), onClick = onAddList, sizeUnits = 1.2f),
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
                state.data.lists.sortedBy { it.order }.forEach { list ->
                    LightText(
                        text = list.title,
                        variant = LightTextVariant.Heading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenList(list) }
                            .padding(
                                horizontal = 1.5f.gridUnitsAsDp(),
                                vertical = 0.5f.gridUnitsAsDp(),
                            ),
                    )
                }
            }
        }
    }
}

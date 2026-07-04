package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer

data class PickerOption(val key: String, val label: String)

/**
 * Single-select list. Tapping an option returns its key as the screen result; the current
 * selection is underlined. Returns nothing if backed out.
 */
class OptionPickerScreen(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val options: List<PickerOption>,
    private val selectedKey: String,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(null) }),
                    center = LightTopBarCenter.Text(title),
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )
                LightScrollView(
                    modifier = Modifier.fillMaxSize(),
                    scrollBarPosition = LightScrollBarPosition.Inside,
                ) {
                    options.forEach { option ->
                        LightText(
                            text = option.label,
                            variant = LightTextVariant.Heading,
                            underline = option.key == selectedKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goBack(option.key) }
                                .padding(
                                    horizontal = 1.5f.gridUnitsAsDp(),
                                    vertical = 0.75f.gridUnitsAsDp(),
                                ),
                        )
                    }
                }
            }
            }
        }
    }
}

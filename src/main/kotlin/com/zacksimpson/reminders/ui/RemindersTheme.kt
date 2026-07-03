package com.zacksimpson.reminders.ui

import androidx.compose.runtime.Composable
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors

/**
 * App theme wrapper. Single-mode white-on-black (LightThemeColors.Dark); no invert.
 * Wrap every screen's Content() in this.
 */
@Composable
fun RemindersTheme(content: @Composable () -> Unit) {
    LightTheme(colors = LightThemeColors.Dark, content = content)
}

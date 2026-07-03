package com.zacksimpson.reminders.ui

import androidx.compose.runtime.Composable
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors

/**
 * App theme wrapper — the single source of truth for the palette.
 *
 * Single-mode **white-on-black** (`LightThemeColors.Dark`: black background, white
 * content, #BBBBBB secondary). Invert-colors is intentionally out of scope for the
 * rewrite, so there is exactly one palette and no [com.thelightphone.sdk.ui.LightThemeController]
 * toggle wiring anywhere.
 *
 * Wrap every screen's `Content()` in this. Inside it, read colors via
 * `LightThemeTokens.colors` and text renders in Akkurat automatically.
 */
@Composable
fun RemindersTheme(content: @Composable () -> Unit) {
    LightTheme(colors = LightThemeColors.Dark, content = content)
}

package com.zacksimpson.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import kotlinx.coroutines.delay

/**
 * full-screen, self-dismissing message. the SDK has nothing toast-like to build on, so
 * this is pushed as a screen and pops itself.
 */
class ToastScreen(
    sealedActivity: SealedLightActivity,
    private val message: String,
    private val durationMs: Long = 1000L,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            LaunchedEffect(Unit) {
                delay(durationMs)
                goBack(Unit)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
                contentAlignment = Alignment.Center,
            ) {
                LightText(text = message, variant = LightTextVariant.Subtitle)
            }
        }
    }
}

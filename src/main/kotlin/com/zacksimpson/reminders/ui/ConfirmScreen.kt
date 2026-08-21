package com.zacksimpson.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

/**
 * generic confirmation screen, a message plus a single confirm action, pinned to the
 * bottom. returns true if confirmed, false/null if backed out via
 * `navigateTo(...) { result -> }`.
 */
class ConfirmScreen(
    sealedActivity: SealedLightActivity,
    private val message: String,
    private val confirmText: String = "Confirm",
) : SimpleLightScreen<Boolean>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(onSwipeBack = { goBack(false) }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = { goBack(false) }),
                    center = null,
                    rightButton = null,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 2.5f.gridUnitsAsDp(), vertical = 4f.gridUnitsAsDp()),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LightText(
                        text = message,
                        // Paragraph read too small here; Copy matches better.
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.8f.gridUnitsAsDp()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LightText(
                        text = confirmText.uppercase(),
                        variant = LightTextVariant.Button,
                        modifier = Modifier.lightClickable { goBack(true) },
                    )
                }
            }
            }
        }
    }
}

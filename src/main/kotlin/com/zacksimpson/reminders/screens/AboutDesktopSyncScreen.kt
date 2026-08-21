package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.ui.RemindersTheme
import com.zacksimpson.reminders.ui.SwipeBackContainer

/**
 * one-off explainer, not back-navigable: no back chevron, swipe-back disabled, a single
 * bottom-pinned "UNDERSTOOD" dismisses it. opened from the Account screen.
 */
class AboutDesktopSyncScreen(
    sealedActivity: SealedLightActivity,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            SwipeBackContainer(enabled = false, onSwipeBack = { goBack(Unit) }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("About Desktop Sync"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1.5f.gridUnitsAsDp()),
                    ) {
                        InfoParagraph(
                            "The Reminders tool can automatically sync with the web " +
                                "version so you can manage your tasks from anywhere.",
                        )
                        LightText(
                            text = "To get started, create an account at",
                            variant = LightTextVariant.Paragraph,
                        )
                        LightText(
                            text = "reminders-tool.web.app",
                            variant = LightTextVariant.Paragraph,
                            underline = true,
                            modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                        )
                        InfoParagraph("Then sign in to that same account here under Settings → Account.")
                        InfoParagraph(
                            "Automatic syncing happens whenever you open the app.",
                            bottomPadding = 0f,
                        )
                    }
                    LightBottomBar(
                        items = listOf(LightBarButton.Text(text = "UNDERSTOOD", onClick = { goBack(Unit) })),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoParagraph(text: String, bottomPadding: Float = 1f) {
    LightText(
        text = text,
        variant = LightTextVariant.Paragraph,
        modifier = Modifier.padding(bottom = bottomPadding.gridUnitsAsDp()),
    )
}

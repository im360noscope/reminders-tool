package com.zacksimpson.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.zacksimpson.reminders.ui.RemindersTheme

class HomeScreenViewModel : LightViewModel<Unit>()

/** Boot screen. Placeholder until the tab structure lands. */
@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel(): HomeScreenViewModel = HomeScreenViewModel()

    @Composable
    override fun Content() {
        RemindersTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(32.dp),
            ) {
                LightText(
                    text = "Reminders",
                    variant = LightTextVariant.Title,
                )
                LightText(
                    text = "Native rewrite — scaffold",
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

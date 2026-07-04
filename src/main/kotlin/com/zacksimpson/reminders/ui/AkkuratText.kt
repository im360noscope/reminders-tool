package com.zacksimpson.reminders.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp

/**
 * For bespoke text the fixed [com.thelightphone.sdk.ui.LightTextVariant] table doesn't
 * cover — a literal design-pixel size (scaled via the SDK's own [designVerticalPxToSp],
 * same convention every LightText size uses) plus an explicit [FontWeight], still on the
 * real Akkurat family (pulled from the shared typography, since [LightTextVariant] doesn't
 * expose a weight override). Used for the time-picker's big digit display (Light weight,
 * mirroring the RN app's PublicSans-Thin) and its numpad digits.
 */
@Composable
fun AkkuratText(
    text: String,
    fontSizeDesignPx: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
) {
    val fontFamily = LightThemeTokens.typography.copy.fontFamily
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = LightThemeTokens.colors.content,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSizeDesignPx.designVerticalPxToSp(),
            textAlign = align ?: TextAlign.Unspecified,
        ),
    )
}

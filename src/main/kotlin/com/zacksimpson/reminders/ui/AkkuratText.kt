package com.zacksimpson.reminders.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp

/**
 * for bespoke text the fixed [com.thelightphone.sdk.ui.LightTextVariant] table doesn't
 * cover, a literal design-pixel size (scaled via the SDK's own [designVerticalPxToSp],
 * same convention every LightText size uses) plus an explicit [FontWeight], still on the
 * real Akkurat family (pulled from the shared typography, since [LightTextVariant] doesn't
 * expose a weight override). used for the time-picker's big digit display (Light weight)
 * and its numpad digits.
 */
@Composable
fun AkkuratText(
    text: String,
    fontSizeDesignPx: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val fontFamily = LightThemeTokens.typography.copy.fontFamily
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            color = LightThemeTokens.colors.content,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSizeDesignPx.designVerticalPxToSp(),
            textAlign = align ?: TextAlign.Unspecified,
        ),
    )
}

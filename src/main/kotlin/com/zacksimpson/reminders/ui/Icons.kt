package com.zacksimpson.reminders.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.thelightphone.sdk.ui.LightThemeTokens
import com.zacksimpson.reminders.R

/** Custom icons ported from the RN app's raw SVG path data — theme content color is
 *  hardcoded white into the vector resources since the app is single-mode. */

@Composable
fun PlusCircleIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_plus_circle),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

@Composable
fun DeleteIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_delete),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** Ring when unchecked; ring + filled center when checked (matches TaskCheckbox.tsx). */
@Composable
fun TaskCheckboxIcon(checked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val color = LightThemeTokens.colors.content
    Box(modifier = modifier.size(size)) {
        if (checked) {
            Canvas(modifier = Modifier.size(size)) {
                drawCircle(color = color, radius = this.size.minDimension * (35.375f / 84f))
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_checkbox_ring),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}

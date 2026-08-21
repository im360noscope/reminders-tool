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

/** custom vector icons, theme content color is hardcoded white into the vector
 *  resources since the app is single-mode. */

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

/** circular-outline ✕, distinct from [DeleteIcon], used for "clear this field" buttons
 *  rather than subtask-row deletion. */
@Composable
fun ClearIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_clear),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

@Composable
fun OverdueAsteriskIcon(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_overdue_asterisk),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

// radius of the filled center as a fraction of the icon's size, small enough to leave
// ic_checkbox_ring's outline visible around it.
private const val CHECKBOX_FILL_RADIUS_FRACTION = 0.421f

/** ring when unchecked; ring + filled center when checked. */
@Composable
fun TaskCheckboxIcon(checked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val color = LightThemeTokens.colors.content
    Box(modifier = modifier.size(size)) {
        if (checked) {
            Canvas(modifier = Modifier.size(size)) {
                drawCircle(color = color, radius = this.size.minDimension * CHECKBOX_FILL_RADIUS_FRACTION)
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_checkbox_ring),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}

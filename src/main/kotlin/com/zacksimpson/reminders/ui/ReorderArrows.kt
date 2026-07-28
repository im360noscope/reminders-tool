package com.zacksimpson.reminders.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.gridUnitsAsDp

/** Up/down reorder arrows shared by task rows and list rows. */
@Composable
fun ReorderArrows(isFirst: Boolean, isLast: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    // Tighter than the icons' own spacing suggests — the enlarged clickable padding
    // below already adds a gap between the two tap zones.
    Row(
        modifier = Modifier.padding(top = 0.75f.gridUnitsAsDp(), end = 0.5f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.spacedBy(0.2f.gridUnitsAsDp()),
    ) {
        LightIcon(
            icon = LightIcons.UP,
            size = 1.6f,
            // UP/DOWN are the BACK chevron rotated ±90°, off-center in its own box — UP's
            // ink sits high, DOWN's sits low. Applied before .clickable() since it's a
            // visual offset, not part of the tap area.
            modifier = Modifier
                .padding(top = 0.58f.gridUnitsAsDp())
                .alpha(if (isFirst) 0.3f else 1f)
                .clickable(enabled = !isFirst, onClick = onMoveUp)
                // Enlarges the tap target beyond the glyph, matching the checkbox/asterisk
                // treatment.
                .padding(horizontal = 0.4f.gridUnitsAsDp(), vertical = 0.35f.gridUnitsAsDp()),
        )
        LightIcon(
            icon = LightIcons.DOWN,
            size = 1.6f,
            modifier = Modifier
                .alpha(if (isLast) 0.3f else 1f)
                .clickable(enabled = !isLast, onClick = onMoveDown)
                .padding(horizontal = 0.4f.gridUnitsAsDp(), vertical = 0.35f.gridUnitsAsDp()),
        )
    }
}

package com.zacksimpson.reminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToDp
import com.thelightphone.sdk.ui.gridUnitsAsDp

/**
 * Big tap-to-edit title input: the value (or placeholder) at Heading size with a full
 * underline beneath and no label. Mirrors the RN title field. The right inset leaves room
 * for the scroll indicator, which overlays on top of content (LightScrollBarPosition.Inside)
 * rather than reserving its own column.
 */
@Composable
fun TitleField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = 1.5f.gridUnitsAsDp(),
                top = 0.75f.gridUnitsAsDp(),
                bottom = 0.75f.gridUnitsAsDp(),
            ),
    ) {
        LightText(
            text = value.ifBlank { placeholder },
            variant = LightTextVariant.Heading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 2.5f.gridUnitsAsDp()),
        )
        Spacer(modifier = Modifier.height(0.4f.gridUnitsAsDp()))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 2.5f.gridUnitsAsDp())
                .height(3f.designVerticalPxToDp())
                .background(LightThemeTokens.colors.content),
        )
    }
}

/**
 * Label-over-value field row (List / Date / Time / Recurring). Small label above a larger
 * value, no underline — mirrors the RN field style.
 */
@Composable
fun TapField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Detail)
        LightText(
            text = value,
            variant = LightTextVariant.Copy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
        )
    }
}

/**
 * Field row with a value that can be cleared (Date / Time / Recurring once set) — shows
 * "None" with a no-op tap when [value] is null, otherwise the value plus a [ClearIcon]
 * button. Shared by Add Task and Task Detail.
 */
@Composable
fun ClearableField(label: String, value: String?, onClick: () -> Unit, onClear: () -> Unit) {
    if (value == null) {
        TapField(label = label, value = "None", onClick = onClick)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                // end bumped to match TaskRowView/TitleField's scroll-indicator clearance
                // (LightScrollBarPosition.Inside overlays rather than reserving its own
                // column) — otherwise ClearIcon sits right under the indicator.
                start = 1.5f.gridUnitsAsDp(),
                top = 0.75f.gridUnitsAsDp(),
                end = 2f.gridUnitsAsDp(),
                bottom = 0.75f.gridUnitsAsDp(),
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            LightText(text = label, variant = LightTextVariant.Detail)
            LightText(
                text = value,
                variant = LightTextVariant.Copy,
                modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
            )
        }
        ClearIcon(
            size = 24.dp,
            modifier = Modifier
                .clickable(onClick = onClear)
                .padding(top = 0.6f.gridUnitsAsDp(), start = 1f.gridUnitsAsDp()),
        )
    }
}

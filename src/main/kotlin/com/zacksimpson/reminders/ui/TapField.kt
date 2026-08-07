package com.zacksimpson.reminders.ui

import androidx.compose.foundation.background
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
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToDp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

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
            .lightClickable(onClick = onClick)
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
    // Values like a picked list/date are short and benefit from truncating rather than
    // wrapping; longer status text (e.g. "Last synced <date>, <time>") needs to wrap
    // instead, or it silently loses the end of the string behind an ellipsis.
    singleLine: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Detail)
        LightText(
            text = value,
            variant = LightTextVariant.Heading,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
        )
    }
}

/**
 * Field row with a value that can be cleared (Date / Time / Recurring once set) — shows
 * "None" with a no-op tap when [value] is null, otherwise the value plus a DELETE icon
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
            .lightClickable(onClick = onClick)
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
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
            )
        }
        LightIcon(
            icon = LightIcons.DELETE,
            // Top offset centers against the value line below the label, not the
            // label itself — the label's own line sits above this icon's top edge.
            modifier = Modifier
                .lightClickable(onClick = onClear)
                .padding(top = 1.72f.gridUnitsAsDp(), start = 1f.gridUnitsAsDp()),
        )
    }
}

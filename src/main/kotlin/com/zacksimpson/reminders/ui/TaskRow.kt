package com.zacksimpson.reminders.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.data.Task
import com.zacksimpson.reminders.data.formatDate
import com.zacksimpson.reminders.data.formatRecurrence
import com.zacksimpson.reminders.data.formatTime
import com.zacksimpson.reminders.data.isOverdue

private fun buildMeta(task: Task, listTitle: String): String {
    val subtaskCount = task.subtasks.size
    val subtaskLabel = if (subtaskCount > 0) {
        "$subtaskCount " + if (subtaskCount == 1) "Subtask" else "Subtasks"
    } else {
        null
    }
    return listOfNotNull(
        listTitle,
        task.date?.let(::formatDate),
        task.time?.let { formatTime(it) },
        subtaskLabel,
    ).joinToString(" · ")
}

/**
 * Shared task row: checkbox (or overdue asterisk in place of it), title, meta line
 * (list · date · time · subtask count), and recurrence line. Used by list detail and the
 * Today tab. Long-pressing the title/meta column opens TaskActionsScreen — matches RN's
 * TaskRow, which attached onLongPress to the same pressable as onPress. In reorder mode
 * the checkbox/asterisk is hidden and up/down arrows appear instead, and tap/long-press
 * are disabled — matches RN's isReordering behavior exactly.
 */
@Composable
fun TaskRowView(
    task: Task,
    listTitle: String,
    onToggle: () -> Unit,
    onPress: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    dimmed: Boolean = false,
    isReordering: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val overdue = isOverdue(task.date, task.time) && !task.completed
    val meta = buildMeta(task, listTitle)
    val recurrenceLabel = task.recurrence?.let(::formatRecurrence)

    // The scroll indicator overlays on top of content (LightScrollBarPosition.Inside)
    // rather than reserving its own column, so this row leaves its own room to avoid
    // overlap — matching RN's rows, which needed extra paddingRight for the same reason.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f)
            .padding(end = 2f.gridUnitsAsDp()),
        verticalAlignment = Alignment.Top,
    ) {
        if (isReordering) {
            // No checkbox/asterisk while reordering — matches RN, which omits leftControl
            // entirely in this mode.
        } else if (overdue) {
            OverdueAsteriskIcon(
                // Matches TaskCheckboxIcon's size exactly, since they occupy the same
                // row slot as alternates of each other.
                size = 17.dp,
                // Same top-inset alignment treatment as TaskCheckboxIcon below, so it
                // lines up with the first line of the title instead of sitting high.
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(
                        start = 0.9f.gridUnitsAsDp(),
                        end = 0.9f.gridUnitsAsDp(),
                        top = 0.95f.gridUnitsAsDp(),
                        bottom = 0.1f.gridUnitsAsDp(),
                    ),
            )
        } else {
            // Same artwork RN used, but at this size next to Akkurat text it read too big —
            // shrunk as a first pass; top padding matches the text column's own top inset
            // as a starting alignment reference (not yet true first-line-center alignment).
            TaskCheckboxIcon(
                checked = task.completed,
                size = 17.dp,
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(
                        start = 0.9f.gridUnitsAsDp(),
                        end = 0.9f.gridUnitsAsDp(),
                        top = 0.95f.gridUnitsAsDp(),
                        bottom = 0.1f.gridUnitsAsDp(),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // Disabled while reordering — matches RN, which guards onPress/onLongPress
                // with `if (isReordering) return;` at the call site.
                .combinedClickable(
                    onClick = { if (!isReordering) onPress() },
                    onLongClick = if (isReordering) null else onLongPress,
                )
                .padding(
                    // Replaces the left margin the checkbox/asterisk normally provides,
                    // since it's hidden in this mode — matches RN's taskContentReordering
                    // style (paddingLeft: n(22)), without which the row loses its left
                    // margin entirely while reordering.
                    start = if (isReordering) 1.5f.gridUnitsAsDp() else 0.dp,
                    top = 0.7f.gridUnitsAsDp(),
                    bottom = 0.7f.gridUnitsAsDp(),
                ),
        ) {
            // LightTextVariant.Copy bakes in lineHeight = fontSize * 1.50, which reads
            // much looser across wraps than RN's title (no explicit line-height at all).
            // AkkuratText at the same 30 design-px size lets the font's own natural line
            // height apply instead.
            AkkuratText(text = task.title, fontSizeDesignPx = 30f)
            if (meta.isNotEmpty()) {
                LightText(
                    text = meta,
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(top = 0.15f.gridUnitsAsDp()),
                )
            }
            recurrenceLabel?.let {
                LightText(
                    text = it,
                    variant = LightTextVariant.Detail,
                    modifier = Modifier.padding(top = 0.15f.gridUnitsAsDp()),
                )
            }
        }

        if (isReordering) {
            ReorderArrows(isFirst = isFirst, isLast = isLast, onMoveUp = onMoveUp, onMoveDown = onMoveDown)
        }
    }
}

@Composable
private fun ReorderArrows(isFirst: Boolean, isLast: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    Row(
        modifier = Modifier.padding(top = 0.75f.gridUnitsAsDp(), end = 0.5f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.spacedBy(0.6f.gridUnitsAsDp()),
    ) {
        LightIcon(
            icon = LightIcons.UP,
            size = 1.6f,
            // LightIcons.UP/DOWN are both the BACK chevron's path rotated ±90°, and that
            // path isn't vertically centered in its own box — after rotation, UP's ink
            // sits high in its box and DOWN's sits low, so at the same nominal position
            // they read visibly offset from each other. The previous top-padding
            // correction (0.22f) was half of what the geometry actually calls for at
            // this icon size — bumped to the full calculated offset.
            modifier = Modifier
                .padding(top = 0.58f.gridUnitsAsDp())
                .alpha(if (isFirst) 0.3f else 1f)
                .clickable(enabled = !isFirst, onClick = onMoveUp),
        )
        LightIcon(
            icon = LightIcons.DOWN,
            size = 1.6f,
            modifier = Modifier
                .alpha(if (isLast) 0.3f else 1f)
                .clickable(enabled = !isLast, onClick = onMoveDown),
        )
    }
}

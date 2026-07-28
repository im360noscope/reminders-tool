package com.zacksimpson.reminders.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
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
 * Shared task row: checkbox (or overdue asterisk), title, meta line (list · date · time
 * · subtask count), and recurrence line. Used by list detail and the Today tab. In
 * reorder mode the checkbox/asterisk is hidden, up/down arrows appear, and tap/long-press
 * are disabled — matches RN's isReordering behavior.
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

    // Leaves its own room on the right for the overlaid scroll indicator
    // (LightScrollBarPosition.Inside doesn't reserve its own column).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f)
            .padding(end = 2f.gridUnitsAsDp()),
        verticalAlignment = Alignment.Top,
    ) {
        if (isReordering) {
            // No checkbox/asterisk while reordering — matches RN.
        } else if (overdue) {
            OverdueAsteriskIcon(
                size = 17.dp, // matches TaskCheckboxIcon's size — same row slot
                // Top-biased to line up with the title's first line; bottom kept
                // generous to preserve tap-target size.
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(
                        start = 0.9f.gridUnitsAsDp(),
                        end = 0.9f.gridUnitsAsDp(),
                        top = 1f.gridUnitsAsDp(),
                        bottom = 0.3f.gridUnitsAsDp(),
                    ),
            )
        } else {
            // RN's artwork read too big next to Akkurat text at full size — shrunk here.
            TaskCheckboxIcon(
                checked = task.completed,
                size = 17.dp,
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(
                        start = 0.9f.gridUnitsAsDp(),
                        end = 0.9f.gridUnitsAsDp(),
                        top = 1f.gridUnitsAsDp(),
                        bottom = 0.3f.gridUnitsAsDp(),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // Disabled while reordering — matches RN.
                .combinedClickable(
                    onClick = { if (!isReordering) onPress() },
                    onLongClick = if (isReordering) null else onLongPress,
                )
                .padding(
                    // Replaces the left margin the checkbox/asterisk normally provides,
                    // since it's hidden while reordering.
                    start = if (isReordering) 1.5f.gridUnitsAsDp() else 0.dp,
                    top = 0.7f.gridUnitsAsDp(),
                    bottom = 0.7f.gridUnitsAsDp(),
                ),
        ) {
            // LightTextVariant.Copy's lineHeight = fontSize * 1.50 reads too loose across
            // wraps — AkkuratText at the same size uses the font's natural line height.
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

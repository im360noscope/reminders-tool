package com.zacksimpson.reminders.ui

import androidx.compose.foundation.clickable
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
 * Shared task row: checkbox (or overdue asterisk in place of it), title, meta line
 * (list · date · time · subtask count), and recurrence line. Used by list detail and,
 * later, the Today tab. Reorder-mode arrows aren't built yet (no entry point to trigger
 * reorder mode without the task-actions screen) — deferred alongside it.
 */
@Composable
fun TaskRowView(
    task: Task,
    listTitle: String,
    onToggle: () -> Unit,
    onPress: () -> Unit,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val overdue = isOverdue(task.date, task.time) && !task.completed
    val meta = buildMeta(task, listTitle)
    val recurrenceLabel = task.recurrence?.let(::formatRecurrence)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.4f else 1f)
            .padding(end = 2f.gridUnitsAsDp()),
        verticalAlignment = Alignment.Top,
    ) {
        if (overdue) {
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
                .clickable(onClick = onPress)
                .padding(vertical = 0.7f.gridUnitsAsDp()),
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
    }
}

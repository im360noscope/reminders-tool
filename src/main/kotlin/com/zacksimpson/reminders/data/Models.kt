package com.zacksimpson.reminders.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ─── Enums ──────────────────────────────────────────────────────────────────
// @SerialName values are the JSON strings — stable and human-readable.

@Serializable
enum class RecurrenceUnit {
    @SerialName("day") DAY,
    @SerialName("week") WEEK,
    @SerialName("month") MONTH,
    @SerialName("year") YEAR,
}

@Serializable
enum class AfterAddBehavior {
    @SerialName("toast") TOAST,
    @SerialName("go-to-list") GO_TO_LIST,
}

@Serializable
enum class AddPosition {
    @SerialName("top") TOP,
    @SerialName("bottom") BOTTOM,
}

// ─── Models ─────────────────────────────────────────────────────────────────
// Every optional field has a default so partial/older JSON still decodes.

@Serializable
data class Recurrence(
    val interval: Int,        // 1–30, enforced by the picker UI, not the type
    val unit: RecurrenceUnit,
)

@Serializable
data class Subtask(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class Task(
    val id: String,
    val title: String,
    val listId: String,
    val date: String? = null,          // "YYYY-MM-DD"
    val time: String? = null,          // "HH:MM" 24h
    val recurrence: Recurrence? = null,
    val subtasks: List<Subtask> = emptyList(),
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val order: Int,
)

@Serializable
data class ReminderList(
    val id: String,
    val title: String,
    val createdAt: Long,
    val order: Int,
)

@Serializable
data class Settings(
    val defaultListId: String = "inbox",
    val afterAddBehavior: AfterAddBehavior = AfterAddBehavior.TOAST,
    val addPosition: AddPosition = AddPosition.BOTTOM,
    val showOverdue: Boolean = true,
)

// ─── Defaults ───────────────────────────────────────────────────────────────

/** Default list, shown until the user first mutates lists. `createdAt` is a placeholder
 *  until a real value is persisted. */
val SEED_INBOX = ReminderList(id = "inbox", title = "Inbox", createdAt = 0L, order = 0)

// ─── Helpers ────────────────────────────────────────────────────────────────

fun generateId(): String = UUID.randomUUID().toString()

/** "Every 1 day" / "Every 3 weeks" — matches the RN formatRecurrence output. */
fun formatRecurrence(r: Recurrence): String {
    val unit = r.unit.name.lowercase()
    val label = if (r.interval == 1) unit else "${unit}s"
    return "Every ${r.interval} $label"
}

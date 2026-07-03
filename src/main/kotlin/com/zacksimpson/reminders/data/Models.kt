package com.zacksimpson.reminders.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ─── Enums ──────────────────────────────────────────────────────────────────
// @SerialName values match the exact strings the RN app used, so JSON stays stable
// and human-readable (and future backup files interchange cleanly).

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
// Direct translation of contexts/RemindersContext.tsx. Every optional field has a
// default so partial/older JSON always decodes (schema-evolution safety).

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

/**
 * Shown in-memory when no lists have ever been persisted (mirrors the RN DEFAULT_LIST
 * seed). `createdAt` is a stable placeholder — a real timestamp is only ever written
 * once the user actually mutates the list collection.
 */
val SEED_INBOX = ReminderList(id = "inbox", title = "Inbox", createdAt = 0L, order = 0)

// ─── Helpers ────────────────────────────────────────────────────────────────

/** Collision-resistant id. Nothing in the app parses id structure, so a UUID is fine. */
fun generateId(): String = UUID.randomUUID().toString()

/** "Every 1 day" / "Every 3 weeks" — matches the RN formatRecurrence output. */
fun formatRecurrence(r: Recurrence): String {
    val unit = r.unit.name.lowercase()
    val label = if (r.interval == 1) unit else "${unit}s"
    return "Every ${r.interval} $label"
}

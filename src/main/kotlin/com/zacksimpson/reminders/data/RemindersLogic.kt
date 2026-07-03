package com.zacksimpson.reminders.data

import java.time.LocalDate

/**
 * Pure domain logic — no storage, no Android, no clock (callers pass date/id/clock in).
 * Split out so ordering and recurrence can be unit-tested in isolation.
 */
internal object RemindersLogic {

    /**
     * Order value for a new task:
     *  - TOP    → (min of 0 and the list's existing orders) − 1
     *  - BOTTOM → (max of −1 and the list's existing orders) + 1
     */
    fun computeOrder(tasks: List<Task>, listId: String, position: AddPosition): Int {
        val orders = tasks.filter { it.listId == listId }.map { it.order }
        return if (position == AddPosition.TOP) {
            orders.fold(0) { acc, o -> minOf(acc, o) } - 1
        } else {
            orders.fold(-1) { acc, o -> maxOf(acc, o) } + 1
        }
    }

    /**
     * Advance a date by one recurrence interval. `plusMonths`/`plusYears` clamp
     * end-of-month (Jan 31 + 1 month → Feb 28) rather than overflowing.
     */
    fun addInterval(date: LocalDate, r: Recurrence): LocalDate = when (r.unit) {
        RecurrenceUnit.DAY -> date.plusDays(r.interval.toLong())
        RecurrenceUnit.WEEK -> date.plusWeeks(r.interval.toLong())
        RecurrenceUnit.MONTH -> date.plusMonths(r.interval.toLong())
        RecurrenceUnit.YEAR -> date.plusYears(r.interval.toLong())
    }

    /**
     * Next occurrence date as ISO "YYYY-MM-DD". Advances at least one interval past
     * [dateStr], then keeps going until the result is >= [today].
     */
    fun nextOccurrenceDate(dateStr: String, recurrence: Recurrence, today: LocalDate): String {
        var next = addInterval(LocalDate.parse(dateStr), recurrence)
        while (next.isBefore(today)) next = addInterval(next, recurrence)
        return next.toString()
    }

    /**
     * Follow-up task spawned when a dated recurring task is completed, or null if it isn't
     * one. Carries over title/list/time/recurrence and subtasks (reset to incomplete).
     */
    fun spawnNextOccurrence(
        task: Task,
        today: LocalDate,
        newId: () -> String,
        now: () -> Long,
    ): Task? {
        val date = task.date ?: return null
        val recurrence = task.recurrence ?: return null
        return Task(
            id = newId(),
            title = task.title,
            listId = task.listId,
            date = nextOccurrenceDate(date, recurrence, today),
            time = task.time,
            recurrence = recurrence,
            subtasks = task.subtasks.map { it.copy(completed = false) },
            completed = false,
            completedAt = null,
            createdAt = now(),
            order = task.order,
        )
    }
}

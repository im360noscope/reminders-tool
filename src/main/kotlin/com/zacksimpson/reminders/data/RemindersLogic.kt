package com.zacksimpson.reminders.data

import java.time.LocalDate

/**
 * Pure, side-effect-free domain logic — no storage, no Android, no ambient clock
 * (callers pass the date / id-generator / clock in). Split out of [RemindersRepository]
 * so the fiddly bits (task ordering, recurrence advancement) can be unit-tested in
 * isolation. See `src/test/.../RemindersLogicTest.kt`.
 */
internal object RemindersLogic {

    /**
     * Order value for a newly added task, ported exactly from the RN reducer:
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
     * Advance a date by one recurrence interval.
     *
     * Note: `plusMonths`/`plusYears` clamp end-of-month (e.g. Jan 31 + 1 month → Feb 28)
     * rather than overflowing into the next month like JS `Date.setMonth` did. This is the
     * intentional, saner behavior for a recurring reminder.
     */
    fun addInterval(date: LocalDate, r: Recurrence): LocalDate = when (r.unit) {
        RecurrenceUnit.DAY -> date.plusDays(r.interval.toLong())
        RecurrenceUnit.WEEK -> date.plusWeeks(r.interval.toLong())
        RecurrenceUnit.MONTH -> date.plusMonths(r.interval.toLong())
        RecurrenceUnit.YEAR -> date.plusYears(r.interval.toLong())
    }

    /**
     * The next occurrence date, as ISO "YYYY-MM-DD". Mirrors the RN do-while: advances at
     * least one interval past [dateStr], then keeps advancing until the result is >= [today]
     * (so completing a long-overdue recurring task skips forward to the next future slot).
     */
    fun nextOccurrenceDate(dateStr: String, recurrence: Recurrence, today: LocalDate): String {
        var next = addInterval(LocalDate.parse(dateStr), recurrence)
        while (next.isBefore(today)) next = addInterval(next, recurrence)
        return next.toString()
    }

    /**
     * The follow-up task spawned when a recurring, dated task is completed — or null if the
     * task isn't a dated recurring task. Carries over title/list/time/recurrence and all
     * subtasks (reset to incomplete), with a fresh id and the next occurrence date.
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

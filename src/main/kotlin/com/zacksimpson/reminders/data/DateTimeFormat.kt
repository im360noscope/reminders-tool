package com.zacksimpson.reminders.data

import java.time.LocalDate
import java.time.LocalDateTime

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "YYYY-MM-DD" -> "Jan 5". Hardcoded English abbreviations (not locale-dependent) to
 *  match the RN app's display exactly. */
fun formatDate(dateStr: String): String {
    val (_, mo, d) = dateStr.split("-").map(String::toInt)
    return "${MONTHS[mo - 1]} $d"
}

/** "YYYY-MM-DD" -> "Jan 5, 2024" — used in field rows (vs. the shorter [formatDate] used
 *  in task-row meta lines). */
fun formatDisplayDate(dateStr: String): String {
    val (y, mo, d) = dateStr.split("-").map(String::toInt)
    return "${MONTHS[mo - 1]} $d, $y"
}

/** "HH:MM" 24h -> "h:mm AM/PM" (or unchanged when use24Hour is true).
 *  TODO: use24Hour should read the device's clock format, but the SDK has no sanctioned
 *  API for it yet (android.text.format.DateFormat.is24HourFormat needs a Context, and
 *  android.content.Context is a blocked import) — defaults to 12-hour like the RN
 *  fallback, same as RN's `getCalendars()[0]?.uses24hourClock ?? false`. */
fun formatTime(time24: String, use24Hour: Boolean = false): String {
    if (use24Hour) return time24
    val (hStr, mStr) = time24.split(":")
    val h = hStr.toInt()
    val ampm = if (h >= 12) "PM" else "AM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:$mStr $ampm"
}

/** True if the task's date/time is in the past. */
fun isOverdue(date: String?, time: String?): Boolean {
    if (date == null) return false
    val today = LocalDate.now()
    if (time != null) {
        val (y, mo, d) = date.split("-").map(String::toInt)
        val (h, m) = time.split(":").map(String::toInt)
        return LocalDateTime.of(y, mo, d, h, m).isBefore(LocalDateTime.now())
    }
    return LocalDate.parse(date).isBefore(today)
}

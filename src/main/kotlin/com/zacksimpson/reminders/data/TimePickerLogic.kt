package com.zacksimpson.reminders.data

/**
 * Pure digit-entry validation for the time-picker numpad, ported literally from the RN
 * app's `TimePicker.tsx` — this logic was fiddly to get right the first time, so the
 * branch structure below mirrors the original exactly rather than being re-derived.
 *
 * Display slots (right-to-left fill):
 * 1 digit  "6"    ->  "  : 6"
 * 2 digits "63"   ->  "  :63"   <- 6 is future hour, 3 is future minutes-tens
 * 3 digits "630"  ->  "6:30"
 * 4 digits "1230" ->  "12:30"
 *
 * Validation rules per position:
 * Position 1 (d[0]): could be minutes-ones (0-9) or future hour — always allow
 * Position 2 (d[1]): usually minutes-tens (0-5), except in 24h mode where d[0]="1"
 *                    allows d[1]=6-9 (hours 16-19) on the way to a 4-digit time
 * Position 3 (d[2]): check full 3-digit time h=d[0], m=d[1]d[2]; in 24h mode
 *                    also allow as 4-digit prefix where h=d[0]d[1], minTens=d[2]
 * Position 4 (d[3]): check full 4-digit time: h=d[0]d[1], m=d[2]d[3]
 */
object TimePickerLogic {

    private fun isValid2Digits(proposed: String, use24Hour: Boolean): Boolean {
        val b = proposed[1].digitToInt()
        if (proposed[0] == '0') {
            // 24h: 00-09 all valid; 12h: 01-09
            return b >= (if (use24Hour) 0 else 1) && b <= 9
        }
        // In 24h mode, "1" followed by 6-9 is valid (hours 16-19)
        if (use24Hour && proposed[0] == '1') {
            return b <= 9
        }
        // Otherwise second digit is minutes-tens (0-5)
        return b <= 5
    }

    private fun isValid3Digits(proposed: String, use24Hour: Boolean): Boolean {
        val firstDigit = proposed[0].digitToInt()
        // Leading zero case: "0yz" -> on way to "0yzw" = 0y:zw
        if (firstDigit == 0) {
            val hourOnes = proposed[1].digitToInt()
            val minTens = proposed[2].digitToInt()
            val minHourOnes = if (use24Hour) 0 else 1
            return hourOnes >= minHourOnes && hourOnes <= 9 && minTens in 0..5
        }
        // In 24h mode, "1x" where x is 6-9 means we're building a 4-digit time
        // (e.g. "160" -> "1600" = 16:00). Validate as HH:M_ prefix.
        if (use24Hour) {
            val h = proposed.substring(0, 2).toInt()
            val minTens = proposed[2].digitToInt()
            if (h in 10..23 && minTens in 0..5) {
                return true
            }
        }
        // Normal H:MM case: first digit is hour (1-9), last two are minutes
        val m = proposed.substring(1).toInt()
        return firstDigit in 1..9 && m in 0..59
    }

    private fun isValid4Digits(proposed: String, use24Hour: Boolean): Boolean {
        val h = proposed.substring(0, 2).toInt()
        val m = proposed.substring(2).toInt()
        val maxH = if (use24Hour) 23 else 12
        val minH = if (use24Hour) 0 else 1
        return h in minH..maxH && m in 0..59
    }

    /** Whether appending [next] to [current] produces a digit string that's still a valid
     *  prefix toward some eventual 3- or 4-digit time. */
    fun isValidNextDigit(current: String, next: Char, use24Hour: Boolean): Boolean {
        val proposed = current + next
        return when (proposed.length) {
            1 -> true
            2 -> isValid2Digits(proposed, use24Hour)
            3 -> isValid3Digits(proposed, use24Hour)
            4 -> isValid4Digits(proposed, use24Hour)
            else -> false
        }
    }

    /** Raw typed digits -> the "H:MM"/"HH:MM"/partial display string, space-padded so the
     *  colon stays in a stable position as digits are typed. */
    fun buildDisplay(digits: String): String = when (digits.length) {
        0 -> "  :  "
        1 -> "  : ${digits[0]}"
        2 -> "  :$digits"
        3 -> "${digits[0]}:${digits.substring(1)}"
        4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
        else -> "  :  "
    }
}

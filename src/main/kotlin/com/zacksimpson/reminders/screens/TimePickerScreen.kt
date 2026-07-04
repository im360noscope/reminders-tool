package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.data.TimePickerLogic
import com.zacksimpson.reminders.data.digitsToTime
import com.zacksimpson.reminders.data.timeToDisplayParts
import com.zacksimpson.reminders.ui.AkkuratText
import com.zacksimpson.reminders.ui.RemindersTheme

/**
 * Numpad time entry, ported from RN's TimePicker.tsx — see [TimePickerLogic] for the
 * digit-validation state machine this screen drives. Tapping SAVE closes with the
 * confirmed "HH:MM" (24h) result; the dismiss (✕) — only available with zero digits
 * typed, matching RN — closes with null (no change). The digit string is re-seeded from
 * [initialValue] every time this screen opens (a fresh instance each push, unlike RN's
 * persistent-modal state that could retain stale digits across opens).
 */
class TimePickerScreen(
    sealedActivity: SealedLightActivity,
    private val initialValue: String?,
    private val use24Hour: Boolean = false,
) : SimpleLightScreen<String?>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            val seed = remember { initialValue?.let { timeToDisplayParts(it, use24Hour) } }
            var digits by remember { mutableStateOf(seed?.first ?: "") }
            var ampm by remember { mutableStateOf(seed?.second ?: "AM") }

            val hasDigits = digits.isNotEmpty()
            val canConfirm = digits.length == 3 || digits.length == 4

            fun tapDigit(d: Char) {
                if (digits.length >= 4) return
                if (TimePickerLogic.isValidNextDigit(digits, d, use24Hour)) digits += d
            }

            fun backspace() {
                digits = digits.dropLast(1)
            }

            fun confirm() {
                goBack(digitsToTime(digits, ampm, use24Hour))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                // No LightTopBar here (unlike DatePickerScreen's own reference use) — an
                // empty one still reserves its full 3 grid-unit height, which combined
                // with the padding below pushed the whole AM/PM+digits block noticeably
                // lower than the reference layout.
                // AM/PM pinned to the row's edges; time display absolutely centered over
                // the same Box so it never shifts the AM/PM buttons as digits are typed.
                // contentAlignment = Center applies to both children by default (neither
                // has its own .align()), so AM/PM sits centered against the tall time
                // text instead of defaulting to top-start.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 0.75f.gridUnitsAsDp(),
                            bottom = 0.5f.gridUnitsAsDp(),
                            start = 1.5f.gridUnitsAsDp(),
                            end = 1.5f.gridUnitsAsDp(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AmPmSlot("AM", ampm, use24Hour) { ampm = "AM" }
                        AmPmSlot("PM", ampm, use24Hour) { ampm = "PM" }
                    }
                    AkkuratText(
                        text = TimePickerLogic.buildDisplay(digits),
                        // The earlier "too tall" complaint was actually the missing
                        // contentAlignment centering above, not oversized text — 100 read
                        // smaller than the reference proportionally. Back up near Title.
                        fontSizeDesignPx = 115f,
                        fontWeight = FontWeight.Light,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Numpad. Each digit column is a weight(1f) box, so the 3 columns always
                // split the entire available row width evenly — more side padding means
                // a narrower row, which pulls the (centered) digit columns closer
                // together. Less padding widens the row and spreads them apart, the
                // opposite of what "tighten up" needs.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            start = 3f.gridUnitsAsDp(),
                            end = 3f.gridUnitsAsDp(),
                            bottom = 1f.gridUnitsAsDp(),
                        ),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("123", "456", "789").forEach { row ->
                        NumRow {
                            row.forEach { d -> NumBtn(d.toString()) { tapDigit(d) } }
                        }
                    }
                    NumRow {
                        // Matches NumBtn's own vertical padding so this row's height lines
                        // up with the numeral rows above it — without it, the icon-only
                        // cells were shorter than the text cells and threw off the
                        // Column's SpaceEvenly distribution across all four rows.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 0.2f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                canConfirm -> LightText(
                                    text = "SAVE",
                                    variant = LightTextVariant.Paragraph,
                                    modifier = Modifier.clickable { confirm() },
                                )
                                hasDigits -> Unit // blank — matches RN: no button while mid-entry
                                else -> LightIcon(
                                    icon = LightIcons.CLOSE,
                                    // Sized up to match the 48px numpad digits/backspace
                                    // chevron — CLOSE's own artwork fills its box more
                                    // evenly (~65% both dimensions) than BACK's asymmetric
                                    // chevron, so it lands a touch under BACK's 2.4f for
                                    // the same apparent weight.
                                    size = 2.2f,
                                    modifier = Modifier.clickable { goBack(null) },
                                )
                            }
                        }
                        NumBtn("0") { tapDigit('0') }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 0.2f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasDigits) {
                                LightIcon(
                                    icon = LightIcons.BACK,
                                    size = 2.4f,
                                    modifier = Modifier.clickable { backspace() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    // verticalAlignment = CenterVertically matters most on the bottom row, where the
    // SAVE/dismiss cell (small Paragraph text) and the "0" cell (much taller AkkuratText
    // digit) have different intrinsic heights — without it, Row's default Top alignment
    // pins the shorter SAVE cell above where "0" and the backspace chevron sit.
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NumBtn(digit: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 0.2f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        // RN's n(35) numText, per the same calibration used for the time display, lands
        // roughly between the Heading (38) and Subtitle (52) design-px sizes — a bare 35
        // rendered noticeably smaller than the reference.
        AkkuratText(text = digit, fontSizeDesignPx = 48f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AmPmSlot(
    value: String,
    selected: String,
    use24Hour: Boolean,
    onClick: () -> Unit,
) {
    if (use24Hour) {
        Box(modifier = Modifier.width(60.dp).height(34.dp))
        return
    }
    Box(
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LightText(text = value, variant = LightTextVariant.Copy)
            Box(
                modifier = Modifier
                    .padding(top = 0.15f.gridUnitsAsDp())
                    .width(32.dp)
                    .height(3.dp)
                    .background(if (selected == value) LightThemeTokens.colors.content else Color.Transparent),
            )
        }
    }
}

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
import com.thelightphone.sdk.ui.LightTopBar
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
                LightTopBar(leftButton = null, center = null, rightButton = null)

                // AM/PM pinned to the row's edges; time display absolutely centered over
                // the same Box so it never shifts the AM/PM buttons as digits are typed.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 2f.gridUnitsAsDp(),
                            bottom = 0.5f.gridUnitsAsDp(),
                            start = 1.5f.gridUnitsAsDp(),
                            end = 1.5f.gridUnitsAsDp(),
                        ),
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
                        fontSizeDesignPx = 90f,
                        fontWeight = FontWeight.Light,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                    )
                }

                // Numpad
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            start = 3.5f.gridUnitsAsDp(),
                            end = 3.5f.gridUnitsAsDp(),
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
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            when {
                                canConfirm -> LightText(
                                    text = "SAVE",
                                    variant = LightTextVariant.Paragraph,
                                    modifier = Modifier.clickable { confirm() },
                                )
                                hasDigits -> Unit // blank — matches RN: no button while mid-entry
                                else -> LightIcon(
                                    icon = LightIcons.CLOSE,
                                    size = 1.6f,
                                    modifier = Modifier.clickable { goBack(null) },
                                )
                            }
                        }
                        NumBtn("0") { tapDigit('0') }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
    Row(modifier = Modifier.fillMaxWidth(), content = content)
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
        AkkuratText(text = digit, fontSizeDesignPx = 35f)
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

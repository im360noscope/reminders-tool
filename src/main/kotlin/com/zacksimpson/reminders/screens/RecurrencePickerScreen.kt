package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.zacksimpson.reminders.data.Recurrence
import com.zacksimpson.reminders.data.RecurrenceUnit
import com.zacksimpson.reminders.data.RemindersLogic
import com.zacksimpson.reminders.ui.AkkuratText
import com.zacksimpson.reminders.ui.RemindersTheme

/**
 * "Recurs every: N unit(s)" stepper, ported from RN's RecurrencePicker.tsx. Interval wraps
 * 1<->30 (no clamping-and-stopping); unit cycles day->week->month->year->day. Re-seeds from
 * [initialValue] every time this screen opens — same as RN's `useEffect` resync on visible,
 * naturally matching since this is a fresh screen instance each push.
 */
class RecurrencePickerScreen(
    sealedActivity: SealedLightActivity,
    private val initialValue: Recurrence?,
) : SimpleLightScreen<Recurrence?>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            var interval by remember { mutableIntStateOf(initialValue?.interval ?: 1) }
            var unit by remember { mutableStateOf(initialValue?.unit ?: RecurrenceUnit.DAY) }

            fun decrement() {
                interval = RemindersLogic.decrementInterval(interval)
            }

            fun increment() {
                interval = RemindersLogic.incrementInterval(interval)
            }

            fun cycleUnit() {
                unit = RemindersLogic.nextRecurrenceUnit(unit)
            }

            val unitLabel = unit.name.lowercase().let { if (interval == 1) it else "${it}s" }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LightText(
                        text = "Recurs every:",
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.8f.gridUnitsAsDp(), bottom = 1.3f.gridUnitsAsDp()),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.1f.gridUnitsAsDp()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightIcon(
                            icon = LightIcons.DOWN,
                            size = 2.6f,
                            modifier = Modifier
                                .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp())
                                .clickable { decrement() },
                        )
                        Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.Center) {
                            AkkuratText(
                                text = interval.toString(),
                                // Matches TimePickerScreen's time display size.
                                fontSizeDesignPx = 115f,
                                fontWeight = FontWeight.Light,
                                align = TextAlign.Center,
                            )
                        }
                        LightIcon(
                            icon = LightIcons.UP,
                            size = 2.6f,
                            modifier = Modifier
                                .padding(horizontal = 2f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp())
                                .clickable { increment() },
                        )
                    }

                    LightText(
                        text = unitLabel,
                        variant = LightTextVariant.Heading,
                        underline = true,
                        modifier = Modifier
                            .clickable { cycleUnit() }
                            .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 0.5f.gridUnitsAsDp()),
                    )
                }

                // Footer: dismiss centered, SAVE on the right — matches RN's two flex:1
                // side-slots with the dismiss X between them.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(vertical = 0.9f.gridUnitsAsDp()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f))
                    LightIcon(
                        icon = LightIcons.CLOSE,
                        // Matches TimePickerScreen's dismiss X.
                        size = 2.2f,
                        modifier = Modifier
                            .padding(0.5f.gridUnitsAsDp())
                            .clickable { goBack(null) },
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        LightText(
                            text = "SAVE",
                            variant = LightTextVariant.Button,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 1.5f.gridUnitsAsDp(), top = 0.5f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp())
                                .clickable { goBack(Recurrence(interval, unit)) },
                        )
                    }
                }
            }
        }
    }
}

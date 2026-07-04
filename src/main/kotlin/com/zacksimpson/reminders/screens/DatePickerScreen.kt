package com.zacksimpson.reminders.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.reminders.ui.RemindersTheme
import java.time.LocalDate
import java.time.YearMonth

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val DAY_HEADERS = listOf("S", "M", "T", "W", "T", "F", "S")

/**
 * Calendar month picker. Tapping a day both selects AND closes the screen in one tap
 * (matches RN — there's no separate confirm step). The dismiss (✕) closes without
 * changing the value. Always opens on the current month, regardless of any already-set
 * date — matches RN's `useMonthNavigation`, which seeds from `new Date()`, not from the
 * task's existing date.
 */
class DatePickerScreen(
    sealedActivity: SealedLightActivity,
    private val initialValue: String?,
) : SimpleLightScreen<String?>(sealedActivity) {

    @Composable
    override fun Content() {
        RemindersTheme {
            val today = remember { LocalDate.now() }
            var viewYear by remember { mutableIntStateOf(today.year) }
            var viewMonth by remember { mutableIntStateOf(today.monthValue) } // 1-12

            fun prevMonth() {
                if (viewMonth == 1) {
                    viewMonth = 12
                    viewYear -= 1
                } else {
                    viewMonth -= 1
                }
            }

            fun nextMonth() {
                if (viewMonth == 12) {
                    viewMonth = 1
                    viewYear += 1
                } else {
                    viewMonth += 1
                }
            }

            val firstDayOfWeek = LocalDate.of(viewYear, viewMonth, 1).dayOfWeek.value % 7 // Sun=0..Sat=6
            val daysInMonth = YearMonth.of(viewYear, viewMonth).lengthOfMonth()
            val cells = buildList {
                repeat(firstDayOfWeek) { add(null) }
                for (d in 1..daysInMonth) add(d)
            }
            val rows = cells.chunked(7).map { row -> (row + List(7) { null }).take(7) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp()),
            ) {
                // Month/year header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 0.25f.gridUnitsAsDp(),
                            end = 0.25f.gridUnitsAsDp(),
                            top = 0.65f.gridUnitsAsDp(),
                            bottom = 1f.gridUnitsAsDp(),
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LightIcon(
                        icon = LightIcons.BACK,
                        size = 2.9f,
                        modifier = Modifier.clickable { prevMonth() },
                    )
                    LightText(text = "${MONTH_NAMES[viewMonth - 1]} $viewYear", variant = LightTextVariant.Paragraph)
                    LightIcon(
                        icon = LightIcons.ARROW_RIGHT,
                        size = 2.9f,
                        modifier = Modifier.clickable { nextMonth() },
                    )
                }

                // Day-of-week headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    DAY_HEADERS.forEach { d ->
                        Box(
                            modifier = Modifier.weight(1f).padding(vertical = 0.5f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(text = d, variant = LightTextVariant.Paragraph)
                        }
                    }
                }

                // Calendar grid
                Column {
                    rows.forEach { row ->
                        // height(IntrinsicSize.Min) + fillMaxHeight on each cell replicates
                        // CSS flexbox's default align-items:stretch, which RN relied on to
                        // keep blank cells the same height as day cells in the same row —
                        // Compose doesn't stretch row children to the tallest sibling by default.
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            row.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 0.55f.gridUnitsAsDp())
                                        .let {
                                            if (day != null) {
                                                it.clickable {
                                                    val dateStr = "%04d-%02d-%02d".format(viewYear, viewMonth, day)
                                                    goBack(dateStr)
                                                }
                                            } else {
                                                it
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (day != null) {
                                        val dateStr = "%04d-%02d-%02d".format(viewYear, viewMonth, day)
                                        val isSelected = dateStr == initialValue
                                        val showUnderline = isSelected || (initialValue == null && dateStr == today.toString())
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            LightText(text = day.toString(), variant = LightTextVariant.Paragraph)
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 0.2f.gridUnitsAsDp())
                                                    .width(14.dp)
                                                    .height(2.dp)
                                                    .background(
                                                        if (showUnderline) LightThemeTokens.colors.content else androidx.compose.ui.graphics.Color.Transparent,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Dismiss, anchored to the true bottom of the screen (matches RN's
                // position:absolute footer) via the SDK's own LightBottomBar — same
                // component + same LightIcons.CLOSE icon the SDK's own LightFullscreenModal
                // uses for its dismiss button, rather than a manually placed/sized icon.
                LightBottomBar(
                    items = listOf(LightBarButton.LightIcon(LightIcons.CLOSE, onClick = { goBack(null) })),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

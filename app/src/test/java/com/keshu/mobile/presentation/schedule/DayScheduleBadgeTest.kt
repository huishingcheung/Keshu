package com.keshu.mobile.presentation.schedule

import com.keshu.mobile.data.local.DaySchedule
import com.keshu.mobile.data.local.HolidayCalendar
import com.keshu.mobile.data.local.HolidayEntry
import com.keshu.mobile.data.local.HolidayKind
import com.keshu.mobile.data.local.holidayBlocks
import com.keshu.mobile.presentation.components.chineseName
import com.keshu.mobile.widget.dateSuffix
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The timetable and the widget both label a make-up day, and both have to keep an unverified
 * convention default visibly distinct from a weekday the user confirmed.
 */
class DayScheduleBadgeTest {
    @Test
    fun anOrdinaryDayHasNoBadge() {
        assertNull(DaySchedule.Normal(DayOfWeek.WEDNESDAY).headerBadge())
        assertNull(DaySchedule.Normal(DayOfWeek.WEDNESDAY).dateSuffix(null))
    }

    @Test
    fun aHolidayIsMarkedAsResting() {
        assertEquals("休", DaySchedule.Holiday.headerBadge())
        assertEquals("中秋节", DaySchedule.Holiday.dateSuffix(holiday("中秋节")))
        assertEquals("放假", DaySchedule.Holiday.dateSuffix(null))
    }

    @Test
    fun anUnconfirmedMakeupDayAsksForTheWeekday() {
        assertEquals("补?", DaySchedule.MakeupUnconfirmed.headerBadge())
        assertEquals("调休上课，补课星期待确认", DaySchedule.MakeupUnconfirmed.dateSuffix(null))
    }

    @Test
    fun anAssumedMakeupDayIsMarkedAsAGuess() {
        val assumed = DaySchedule.Makeup(DayOfWeek.TUESDAY, assumed = true)

        assertEquals("补二*", assumed.headerBadge())
        assertEquals("补周二（推测）", assumed.dateSuffix(null))
    }

    @Test
    fun aConfirmedMakeupDayDropsTheGuessMarker() {
        val confirmed = DaySchedule.Makeup(DayOfWeek.WEDNESDAY, assumed = false)

        assertEquals("补三", confirmed.headerBadge())
        assertEquals("补周三", confirmed.dateSuffix(null))
    }

    @Test
    fun chineseWeekdayNamesLineUpWithTheDayOfWeekIndex() {
        assertEquals("周一", DayOfWeek.MONDAY.chineseName())
        assertEquals("周三", DayOfWeek.WEDNESDAY.chineseName())
        assertEquals("周日", DayOfWeek.SUNDAY.chineseName())
    }

    @Test
    fun holidayBlocksCollapseConsecutiveDatesByLabel() {
        val blocks = HolidayCalendar.entries().holidayBlocks()

        assertEquals(2, blocks.size)
        assertEquals("中秋节", blocks[0].label)
        assertEquals(LocalDate.of(2026, 9, 25), blocks[0].start)
        assertEquals(LocalDate.of(2026, 9, 27), blocks[0].end)
        assertEquals("国庆节", blocks[1].label)
        assertEquals(LocalDate.of(2026, 10, 1), blocks[1].start)
        assertEquals(LocalDate.of(2026, 10, 7), blocks[1].end)
        // Make-up days are listed as editable rows, not as holiday blocks.
        assertTrue(blocks.none { it.start == LocalDate.of(2026, 9, 20) })
    }

    private fun holiday(label: String) = HolidayEntry(
        date = LocalDate.of(2026, 9, 25),
        kind = HolidayKind.HOLIDAY,
        label = label,
    )
}

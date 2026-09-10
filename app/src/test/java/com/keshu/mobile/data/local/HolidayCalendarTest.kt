package com.keshu.mobile.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arrangement dates come from the State Council notice, which never states the weekday a make-up
 * day replaces. The app therefore ships a convention default that stays flagged as assumed until the
 * user confirms a weekday of their own.
 */
class HolidayCalendarTest {
    private val entries = HolidayCalendar.entries().byDate()

    @Test
    fun anOrdinaryDateRunsItsOwnWeekday() {
        val date = LocalDate.of(2026, 9, 9)

        val schedule = resolveDaySchedule(date, entries)

        assertEquals(DaySchedule.Normal(DayOfWeek.WEDNESDAY), schedule)
        assertEquals(DayOfWeek.WEDNESDAY, schedule.timetableWeekday())
    }

    @Test
    fun aStatutoryHolidayHasNoClasses() {
        val schedule = resolveDaySchedule(LocalDate.of(2026, 9, 25), entries)

        assertEquals(DaySchedule.Holiday, schedule)
        assertNull(schedule.timetableWeekday())
    }

    @Test
    fun aMakeupDayStartsFromTheConventionDefault() {
        val schedule = resolveDaySchedule(LocalDate.of(2026, 9, 20), entries)

        assertEquals(DaySchedule.Makeup(DayOfWeek.TUESDAY, assumed = true), schedule)
        // The default still teaches classes, so the timetable is usable before anyone confirms it.
        assertEquals(DayOfWeek.TUESDAY, schedule.timetableWeekday())
    }

    @Test
    fun aUserChoiceOverridesTheDefaultAndStopsBeingAssumed() {
        val confirmed = HolidayCalendar.entries(
            confirmedMakeupWeekdays = mapOf(LocalDate.of(2026, 9, 20) to DayOfWeek.MONDAY),
        ).byDate()

        val schedule = resolveDaySchedule(LocalDate.of(2026, 9, 20), confirmed)

        assertEquals(DaySchedule.Makeup(DayOfWeek.MONDAY, assumed = false), schedule)
        assertEquals(DayOfWeek.MONDAY, schedule.timetableWeekday())
    }

    @Test
    fun changingOneMakeupDayLeavesTheOtherOnItsDefault() {
        val confirmed = HolidayCalendar.entries(
            confirmedMakeupWeekdays = mapOf(LocalDate.of(2026, 9, 20) to DayOfWeek.MONDAY),
        ).byDate()

        assertEquals(
            DaySchedule.Makeup(DayOfWeek.WEDNESDAY, assumed = true),
            resolveDaySchedule(LocalDate.of(2026, 10, 10), confirmed),
        )
    }

    @Test
    fun droppingTheUserChoiceFallsBackToTheDefault() {
        val reverted = HolidayCalendar.entries(confirmedMakeupWeekdays = emptyMap()).byDate()

        assertEquals(
            DaySchedule.Makeup(DayOfWeek.TUESDAY, assumed = true),
            resolveDaySchedule(LocalDate.of(2026, 9, 20), reverted),
        )
    }

    @Test
    fun aMakeupDayWithoutAnyWeekdayIsReportedAsUnconfirmed() {
        val entries = listOf(
            HolidayEntry(LocalDate.of(2027, 1, 4), HolidayKind.MAKEUP, "调休上课"),
        ).byDate()

        val schedule = resolveDaySchedule(LocalDate.of(2027, 1, 4), entries)

        assertEquals(DaySchedule.MakeupUnconfirmed, schedule)
        assertNull(schedule.timetableWeekday())
    }

    @Test
    fun theFallSemesterTableCoversBothHolidaysAndBothMakeupDays() {
        val holidays = HolidayCalendar.fall2026.filter { it.kind == HolidayKind.HOLIDAY }
        val makeup = HolidayCalendar.fall2026.filter { it.kind == HolidayKind.MAKEUP }

        // 中秋节 9/25-9/27 plus 国庆节 10/1-10/7.
        assertEquals(10, holidays.size)
        assertEquals(
            (1..7).map { LocalDate.of(2026, 10, it) },
            holidays.map { it.date }.filter { it.monthValue == 10 },
        )
        assertEquals(
            listOf(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 10)),
            makeup.map { it.date }.sorted(),
        )
    }

    @Test
    fun midAutumnCarriesNoMakeupDay() {
        val midAutumn = HolidayCalendar.fall2026
            .filter { it.date.monthValue == 9 && it.date.dayOfMonth in 25..27 }

        assertEquals(3, midAutumn.size)
        assertTrue(midAutumn.all { it.kind == HolidayKind.HOLIDAY })
    }

    @Test
    fun everyBuiltInMakeupDayIsFlaggedAsAssumed() {
        val makeup = HolidayCalendar.fall2026.filter { it.kind == HolidayKind.MAKEUP }

        assertEquals(2, makeup.size)
        assertTrue(makeup.all { it.makeupIsAssumed })
        assertTrue(makeup.all { it.makeupWeekday != null })
        assertFalse(HolidayCalendar.fall2026.any { it.kind == HolidayKind.HOLIDAY && it.makeupIsAssumed })
    }

    @Test
    fun confirmedMakeupWeekdaysRoundTripThroughStorage() {
        val values = mapOf(
            LocalDate.of(2026, 10, 10) to DayOfWeek.WEDNESDAY,
            LocalDate.of(2026, 9, 20) to DayOfWeek.MONDAY,
        )

        val decoded = decodeMakeupWeekdays(encodeMakeupWeekdays(values))

        assertEquals(values, decoded)
        assertTrue(encodeMakeupWeekdays(values).startsWith("2026-09-20"))
    }

    @Test
    fun malformedStoredMakeupWeekdaysAreIgnored() {
        assertTrue(decodeMakeupWeekdays(null).isEmpty())
        assertTrue(decodeMakeupWeekdays("").isEmpty())
        assertTrue(decodeMakeupWeekdays("nonsense").isEmpty())
        assertTrue(decodeMakeupWeekdays("2026-09-20,9").isEmpty())
        assertTrue(decodeMakeupWeekdays("2026-09-20,0").isEmpty())
        assertEquals(
            mapOf(LocalDate.of(2026, 9, 20) to DayOfWeek.MONDAY),
            decodeMakeupWeekdays("not-a-date,3|2026-09-20,1"),
        )
    }
}

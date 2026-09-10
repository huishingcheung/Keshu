package com.keshu.mobile.data.local

import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate

/** A date that departs from the ordinary weekly timetable. */
enum class HolidayKind {
    /** Statutory holiday: no classes take place. */
    HOLIDAY,

    /** Make-up teaching day: classes run instead of resting on a weekend. */
    MAKEUP,
}

/**
 * One deviation from the ordinary timetable.
 *
 * [makeupWeekday] is only meaningful for [HolidayKind.MAKEUP]. It records which weekday's timetable
 * runs on [date].
 *
 * [makeupIsAssumed] distinguishes a value this app defaulted to from one the user confirmed. The
 * State Council notice fixes the arrangement dates but never the weekday a make-up day replaces;
 * that mapping comes from each school's own notice, and schools assign the same date differently.
 */
data class HolidayEntry(
    val date: LocalDate,
    val kind: HolidayKind,
    val label: String,
    val makeupWeekday: DayOfWeek? = null,
    val makeupIsAssumed: Boolean = false,
)

/** What actually happens on a given date. */
sealed interface DaySchedule {
    /** An ordinary day running the timetable of [weekday]. */
    data class Normal(val weekday: DayOfWeek) : DaySchedule

    /** A statutory holiday with no classes. */
    data object Holiday : DaySchedule

    /** A make-up teaching day whose substituted weekday is still unknown. */
    data object MakeupUnconfirmed : DaySchedule

    /**
     * A make-up teaching day running the timetable of [weekday].
     *
     * [assumed] is true while the weekday is only this app's convention default, so the timetable
     * can label it as unverified and invite the user to correct it.
     */
    data class Makeup(val weekday: DayOfWeek, val assumed: Boolean) : DaySchedule
}

object HolidayCalendar {
    /**
     * Source of the arrangement dates: 《国务院办公厅关于2026年部分节假日安排的通知》,
     * 国办发明电〔2025〕7号. The notice fixes the holiday and make-up dates but says nothing about
     * the weekday a make-up day replaces, so the two make-up weekdays below are convention defaults
     * marked [HolidayEntry.makeupIsAssumed] rather than a school arrangement.
     *
     * They make up the last working days the holiday displaced, which leaves 10月6日 (Tuesday) and
     * 10月7日 (Wednesday). Schools do not agree on this mapping, and a stored choice replaces the
     * default.
     *
     * The table covers the 2026 fall semester, whose first teaching day is 2026-09-07, and is data
     * rather than logic so a later arrangement can replace it.
     */
    const val ARRANGEMENT_NOTICE = "国办发明电〔2025〕7号"

    val fall2026: List<HolidayEntry> = buildList {
        // 中秋节: three days off, no make-up day.
        add(HolidayEntry(LocalDate.of(2026, 9, 25), HolidayKind.HOLIDAY, "中秋节"))
        add(HolidayEntry(LocalDate.of(2026, 9, 26), HolidayKind.HOLIDAY, "中秋节"))
        add(HolidayEntry(LocalDate.of(2026, 9, 27), HolidayKind.HOLIDAY, "中秋节"))
        // 国庆节: seven days off with 9月20日 and 10月10日 as make-up teaching days.
        add(
            HolidayEntry(
                date = LocalDate.of(2026, 9, 20),
                kind = HolidayKind.MAKEUP,
                label = "调休上课",
                makeupWeekday = DayOfWeek.TUESDAY,
                makeupIsAssumed = true,
            ),
        )
        add(HolidayEntry(LocalDate.of(2026, 10, 1), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 2), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 3), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 4), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 5), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 6), HolidayKind.HOLIDAY, "国庆节"))
        add(HolidayEntry(LocalDate.of(2026, 10, 7), HolidayKind.HOLIDAY, "国庆节"))
        add(
            HolidayEntry(
                date = LocalDate.of(2026, 10, 10),
                kind = HolidayKind.MAKEUP,
                label = "调休上课",
                makeupWeekday = DayOfWeek.WEDNESDAY,
                makeupIsAssumed = true,
            ),
        )
    }

    /**
     * The built-in arrangement with the user's confirmed make-up weekdays applied. A user choice
     * always wins over the convention default and is no longer flagged as assumed.
     */
    fun entries(confirmedMakeupWeekdays: Map<LocalDate, DayOfWeek> = emptyMap()): List<HolidayEntry> {
        return fall2026.map { entry ->
            val confirmed = confirmedMakeupWeekdays[entry.date]
            if (entry.kind == HolidayKind.MAKEUP && confirmed != null) {
                entry.copy(makeupWeekday = confirmed, makeupIsAssumed = false)
            } else {
                entry
            }
        }
    }
}

internal fun Collection<HolidayEntry>.byDate(): Map<LocalDate, HolidayEntry> = associateBy { it.date }

/** A run of holiday dates sharing one label, so the settings list stays short. */
data class HolidayBlock(val label: String, val start: LocalDate, val end: LocalDate)

/** Collapses holiday dates into one block per label, in date order. */
fun List<HolidayEntry>.holidayBlocks(): List<HolidayBlock> {
    return filter { it.kind == HolidayKind.HOLIDAY }
        .groupBy { it.label }
        .map { (label, entries) ->
            HolidayBlock(label, entries.minOf { it.date }, entries.maxOf { it.date })
        }
        .sortedBy { it.start }
}

/** Resolves what actually happens on [date] given the active [entries]. */
internal fun resolveDaySchedule(
    date: LocalDate,
    entries: Map<LocalDate, HolidayEntry>,
): DaySchedule {
    return when (val entry = entries[date]) {
        null -> DaySchedule.Normal(date.dayOfWeek)
        else -> when (entry.kind) {
            HolidayKind.HOLIDAY -> DaySchedule.Holiday
            HolidayKind.MAKEUP -> entry.makeupWeekday
                ?.let { DaySchedule.Makeup(it, assumed = entry.makeupIsAssumed) }
                ?: DaySchedule.MakeupUnconfirmed
        }
    }
}

/**
 * The weekday whose sessions are taught on a date, or null when no classes are scheduled.
 *
 * A convention default still counts, so the timetable is usable before the user confirms anything.
 * A make-up day with no weekday at all reports null: whether it is a teaching day is known, but
 * which classes it runs is not.
 */
internal fun DaySchedule.timetableWeekday(): DayOfWeek? = when (this) {
    is DaySchedule.Normal -> weekday
    is DaySchedule.Makeup -> weekday
    is DaySchedule.Holiday -> null
    is DaySchedule.MakeupUnconfirmed -> null
}

class HolidayPreferences(private val preferences: SharedPreferences) {
    /** The make-up weekdays the user has set, keyed by date. */
    fun loadConfirmedMakeupWeekdays(): Map<LocalDate, DayOfWeek> {
        return decodeMakeupWeekdays(preferences.getString(KEY_MAKEUP_WEEKDAYS, null))
    }

    fun confirmMakeupWeekday(date: LocalDate, weekday: DayOfWeek) {
        val updated = loadConfirmedMakeupWeekdays() + (date to weekday)
        preferences.edit().putString(KEY_MAKEUP_WEEKDAYS, encodeMakeupWeekdays(updated)).apply()
    }

    /** Drops the user's choice so the date falls back to its built-in default. */
    fun clearMakeupWeekday(date: LocalDate) {
        val updated = loadConfirmedMakeupWeekdays() - date
        preferences.edit().putString(KEY_MAKEUP_WEEKDAYS, encodeMakeupWeekdays(updated)).apply()
    }

    companion object {
        private const val KEY_MAKEUP_WEEKDAYS = "holiday_makeup_weekdays"
    }
}

internal fun encodeMakeupWeekdays(values: Map<LocalDate, DayOfWeek>): String {
    return values.entries
        .sortedBy { it.key }
        .joinToString("|") { "${it.key},${it.value.value}" }
}

internal fun decodeMakeupWeekdays(value: String?): Map<LocalDate, DayOfWeek> {
    if (value.isNullOrBlank()) return emptyMap()
    return value.split('|').mapNotNull { entry ->
        val parts = entry.split(',', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val date = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: return@mapNotNull null
        val weekday = parts[1].trim().toIntOrNull()
            ?.takeIf { it in 1..7 }
            ?.let { DayOfWeek.of(it) }
            ?: return@mapNotNull null
        date to weekday
    }.toMap()
}

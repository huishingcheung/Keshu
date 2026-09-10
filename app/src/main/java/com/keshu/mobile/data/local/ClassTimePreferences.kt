package com.keshu.mobile.data.local

import android.content.SharedPreferences

enum class ClassTimeProfile(val id: String, val displayName: String) {
    HUAQIAO_PANYU("huaqiao_panyu", "华文 / 番禺"),
    MAIN_ZHUHAI("main_zhuhai", "校本部 / 珠海"),
    SHENZHEN("shenzhen", "深圳"),
    CUSTOM("custom", "自定义"),
    ;

    companion object {
        fun fromId(id: String?): ClassTimeProfile = entries.firstOrNull { it.id == id } ?: HUAQIAO_PANYU
    }
}

data class ClassPeriodTime(val start: String, val end: String)

data class ClassTimeSettings(
    val profile: ClassTimeProfile,
    val customPeriods: List<ClassPeriodTime> = ClassTimePresets.huaqiaoPanyu,
) {
    val periods: List<ClassPeriodTime>
        get() = when (profile) {
            ClassTimeProfile.HUAQIAO_PANYU -> ClassTimePresets.huaqiaoPanyu
            ClassTimeProfile.MAIN_ZHUHAI -> ClassTimePresets.mainZhuhai
            ClassTimeProfile.SHENZHEN -> ClassTimePresets.shenzhen
            ClassTimeProfile.CUSTOM -> customPeriods
        }

    fun period(section: Int): ClassPeriodTime? = periods.getOrNull(section - 1)
}

object ClassTimePresets {
    val huaqiaoPanyu = periods(
        "08:30-09:15", "09:25-10:10", "10:30-11:15", "11:25-12:10",
        "12:20-13:05", "14:00-14:45", "14:55-15:40", "15:50-16:35",
        "16:45-17:30", "18:30-19:15", "19:25-20:10", "20:20-21:05",
    )
    val mainZhuhai = periods(
        "08:00-08:45", "08:55-09:40", "10:00-10:45", "10:55-11:40",
        "12:40-13:25", "13:35-14:20", "14:30-15:15", "15:25-16:10",
        "16:20-17:05", "17:15-18:00", "19:00-19:45", "19:55-20:40",
        "20:50-21:35",
    )
    val shenzhen = periods(
        "08:00-08:45", "08:55-09:40", "10:00-10:45", "10:55-11:40",
        "13:30-14:15", "14:25-15:10", "15:20-16:05", "16:15-17:00",
        "18:00-18:45", "18:55-19:40", "19:50-20:35", "20:45-21:30",
    )

    private fun periods(vararg values: String): List<ClassPeriodTime> {
        return values.map { value ->
            val (start, end) = value.split('-', limit = 2)
            ClassPeriodTime(start, end)
        }
    }
}

class ClassTimePreferences(private val preferences: SharedPreferences) {
    fun load(): ClassTimeSettings {
        return ClassTimeSettings(
            profile = ClassTimeProfile.fromId(preferences.getString(KEY_PROFILE, null)),
            customPeriods = decodePeriods(preferences.getString(KEY_CUSTOM_PERIODS, null))
                ?: ClassTimePresets.huaqiaoPanyu,
        )
    }

    fun select(profile: ClassTimeProfile) {
        preferences.edit().putString(KEY_PROFILE, profile.id).apply()
    }

    fun saveCustom(periods: List<ClassPeriodTime>) {
        require(periods.isNotEmpty() && periods.all(::isValidClassPeriod))
        preferences.edit()
            .putString(KEY_CUSTOM_PERIODS, encodePeriods(periods))
            .putString(KEY_PROFILE, ClassTimeProfile.CUSTOM.id)
            .apply()
    }

    companion object {
        private const val KEY_PROFILE = "class_time_profile"
        private const val KEY_CUSTOM_PERIODS = "class_time_custom_periods"
    }
}

/**
 * Resolves a section time for timeline decisions, clamping sections past the end of the profile
 * onto its last period.
 *
 * A course can reference a section that the selected campus profile does not define, for example
 * a 13th period while a 12-period profile is selected. Returning null there would leave the
 * session without any end time, so it would never leave the "next class" slot.
 */
internal fun ClassTimeSettings.timelinePeriod(section: Int): ClassPeriodTime? {
    val available = periods
    if (available.isEmpty()) return null
    return available.getOrElse(section - 1) { available.last() }
}

internal fun isValidClassPeriod(period: ClassPeriodTime): Boolean {
    val start = period.start.toMinutesOfDay() ?: return false
    val end = period.end.toMinutesOfDay() ?: return false
    return end > start
}

internal fun encodePeriods(periods: List<ClassPeriodTime>): String {
    return periods.joinToString("|") { "${it.start},${it.end}" }
}

internal fun decodePeriods(value: String?): List<ClassPeriodTime>? {
    if (value.isNullOrBlank()) return null
    val periods = value.split('|').mapNotNull { entry ->
        val values = entry.split(',', limit = 2)
        values.takeIf { it.size == 2 }?.let { ClassPeriodTime(it[0], it[1]) }
    }
    return periods.takeIf { it.isNotEmpty() && it.all(::isValidClassPeriod) }
}

internal fun String.toMinutesOfDay(): Int? {
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(this) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

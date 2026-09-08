package com.keshu.mobile.data.local

import android.content.Context

data class ExamAvailability(
    val visibilityRange: String? = null,
) {
    fun detailMessage(): String {
        return visibilityRange
            ?.takeIf { it.isNotBlank() }
            ?.let { "学校开放的可查看时间范围：$it" }
            ?: "学校当前未提供可查看的考试安排。"
    }

    fun userMessage(): String {
        val range = visibilityRange
            ?.takeIf { it.isNotBlank() }
            ?.let { "（可查看时间：$it）" }
            .orEmpty()
        return "考试安排当前不可查看$range"
    }
}

interface ExamAvailabilityStore {
    fun markUnavailable(visibilityRange: String?)
    fun clearUnavailable()
}

class ExamAvailabilityPreferences(context: Context) : ExamAvailabilityStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getUnavailable(): ExamAvailability? {
        if (!preferences.getBoolean(KEY_UNAVAILABLE, false)) return null
        return ExamAvailability(preferences.getString(KEY_VISIBILITY_RANGE, null))
    }

    override fun markUnavailable(visibilityRange: String?) {
        preferences.edit()
            .putBoolean(KEY_UNAVAILABLE, true)
            .putString(KEY_VISIBILITY_RANGE, visibilityRange)
            .apply()
    }

    override fun clearUnavailable() {
        preferences.edit()
            .remove(KEY_UNAVAILABLE)
            .remove(KEY_VISIBILITY_RANGE)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "academic_settings"
        const val KEY_UNAVAILABLE = "exam_schedule_unavailable"
        const val KEY_VISIBILITY_RANGE = "exam_schedule_visibility_range"
    }
}

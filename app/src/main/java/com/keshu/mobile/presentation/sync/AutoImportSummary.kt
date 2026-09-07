package com.keshu.mobile.presentation.sync

import com.keshu.mobile.data.local.ExamAvailability

internal sealed interface ExamImportOutcome {
    data class Imported(val count: Int) : ExamImportOutcome
    data class Unavailable(val visibilityRange: String? = null) : ExamImportOutcome
    data class Failed(val reason: String? = null) : ExamImportOutcome
}

internal data class AutoImportSummary(
    val groups: Int,
    val courses: Int,
    val schedule: Int,
    val exams: ExamImportOutcome,
) {
    fun toUserMessage(): String {
        val imported = "一键同步完成：课群$groups 课程$courses 课表$schedule"
        return when (val outcome = exams) {
            is ExamImportOutcome.Imported -> "$imported 考试${outcome.count}"
            is ExamImportOutcome.Unavailable ->
                "$imported；${ExamAvailability(outcome.visibilityRange).userMessage()}"
            is ExamImportOutcome.Failed -> "$imported；考试安排同步失败，请稍后重试"
        }
    }
}

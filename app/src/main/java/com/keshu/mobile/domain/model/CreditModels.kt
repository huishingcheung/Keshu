package com.keshu.mobile.domain.model

import com.keshu.mobile.data.local.entity.CourseEntity

data class GraduationProgress(
    val requiredCredits: Double,
    val earnedCredits: Double,
    val largeGroupCount: Int,
    val metLargeGroupCount: Int,
    val met: Boolean,
)

data class LargeGroupProgress(
    val id: String,
    val name: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val smallGroupCount: Int,
    val metSmallGroupCount: Int,
    val met: Boolean,
    val childGroups: List<SmallGroupProgress>,
) {
    val smallGroups: List<SmallGroupProgress> = childGroups
    val allGroups: List<SmallGroupProgress> = childGroups.flatMap { it.flatten() }
}

data class SmallGroupProgress(
    val id: String,
    val largeGroupId: String,
    val parentSmallGroupId: String?,
    val depth: Int,
    val name: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val met: Boolean,
    val courses: List<CourseEntity>,
    val childGroups: List<SmallGroupProgress>,
) {
    val gapCredits: Double = (requiredCredits - earnedCredits).coerceAtLeast(0.0)
    fun flatten(): List<SmallGroupProgress> = listOf(this) + childGroups.flatMap { it.flatten() }
}

data class CreditTree(
    val graduation: GraduationProgress,
    val largeGroups: List<LargeGroupProgress>,
) {
    val unmetSmallGroups: List<SmallGroupProgress> =
        largeGroups.flatMap { it.allGroups }.filterNot { it.met }
}

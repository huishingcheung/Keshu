package com.keshu.mobile.presentation.credit

import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.SmallGroupProgress

internal data class ProjectedCreditMetrics(
    val credits: Double,
    val gapCredits: Double,
    val met: Boolean,
)

internal data class ProjectedLargeGroupMetrics(
    val credits: Double,
    val metSmallGroupCount: Int,
)

internal data class CreditTreeProjection(
    val largeGroups: Map<String, ProjectedLargeGroupMetrics>,
    val smallGroups: Map<String, ProjectedCreditMetrics>,
)

internal fun buildCreditTreeProjection(tree: CreditTree): CreditTreeProjection {
    val smallGroups = tree.largeGroups
        .flatMap { it.allGroups }
        .associate { group ->
            val credits = group.earnedCredits + group.takingCreditsIncludingChildren()
            group.id to ProjectedCreditMetrics(
                credits = credits,
                gapCredits = (group.requiredCredits - credits).coerceAtLeast(0.0),
                met = credits >= group.requiredCredits,
            )
        }
    val largeGroups = tree.largeGroups.associate { group ->
        val credits = group.earnedCredits + group.allGroups
            .flatMap { it.courses }
            .takingCredits()
        group.id to ProjectedLargeGroupMetrics(
            credits = credits,
            metSmallGroupCount = group.childGroups.count { child -> smallGroups[child.id]?.met == true },
        )
    }
    return CreditTreeProjection(largeGroups = largeGroups, smallGroups = smallGroups)
}

internal fun creditProgress(credits: Double, requiredCredits: Double): Float {
    return if (requiredCredits <= 0.0) 0f else (credits / requiredCredits).toFloat().coerceIn(0f, 1f)
}

private fun SmallGroupProgress.takingCreditsIncludingChildren(): Double {
    return flatten().flatMap { it.courses }.takingCredits()
}

private fun List<CourseEntity>.takingCredits(): Double {
    return asSequence()
        .filter { it.status == CourseStatus.TAKING }
        .distinctBy { it.code.ifBlank { it.name } }
        .sumOf { it.credit }
}

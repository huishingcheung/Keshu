package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.local.dao.CourseDao
import com.keshu.mobile.data.local.dao.GroupDao
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.data.local.entity.LargeGroupEntity
import com.keshu.mobile.data.local.entity.SmallGroupEntity
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.GraduationProgress
import com.keshu.mobile.domain.model.LargeGroupProgress
import com.keshu.mobile.domain.model.SmallGroupProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BuildCreditTreeUseCase(
    private val groupDao: GroupDao,
    private val courseDao: CourseDao,
) {
    operator fun invoke(requiredGraduationCredits: Double = 160.0): Flow<CreditTree> {
        return combine(
            groupDao.observeLargeGroups(),
            groupDao.observeSmallGroups(),
            courseDao.observeAll(),
            groupDao.observeAcademicProgress(),
        ) { largeGroups, smallGroups, courses, academicProgress ->
            val coursesByGroup = courses.groupBy { it.smallGroupId }
            val groupsByParent = smallGroups.groupBy { it.parentSmallGroupId }
            val topGroupsByLarge = smallGroups
                .filter { it.parentSmallGroupId == null }
                .groupBy { it.largeGroupId }

            val largeProgress = largeGroups.map { large ->
                buildLargeGroupProgress(
                    large = large,
                    topGroups = topGroupsByLarge[large.id].orEmpty(),
                    groupsByParent = groupsByParent,
                    coursesByGroup = coursesByGroup,
                )
            }
            val requiredCredits = academicProgress?.requiredCredits ?: requiredGraduationCredits
            val earnedCredits = academicProgress?.earnedCredits ?: 0.0
            CreditTree(
                graduation = GraduationProgress(
                    requiredCredits = requiredCredits,
                    earnedCredits = earnedCredits,
                    largeGroupCount = largeProgress.size,
                    metLargeGroupCount = largeProgress.count { it.met },
                    met = earnedCredits >= requiredCredits,
                ),
                largeGroups = largeProgress,
            )
        }
    }

    private fun buildLargeGroupProgress(
        large: LargeGroupEntity,
        topGroups: List<SmallGroupEntity>,
        groupsByParent: Map<String?, List<SmallGroupEntity>>,
        coursesByGroup: Map<String, List<CourseEntity>>,
    ): LargeGroupProgress {
        val children = topGroups
            .sortedWith(compareBy<SmallGroupEntity> { it.sortOrder }.thenBy { it.name })
            .map { buildSmallGroupProgress(it, groupsByParent, coursesByGroup) }
        val earnedCredits = large.earnedCredits
        return LargeGroupProgress(
            id = large.id,
            name = large.name,
            requiredCredits = large.requiredCredits,
            earnedCredits = earnedCredits,
            smallGroupCount = children.size,
            metSmallGroupCount = children.count { it.met },
            met = earnedCredits >= large.requiredCredits,
            childGroups = children,
        )
    }

    private fun buildSmallGroupProgress(
        group: SmallGroupEntity,
        groupsByParent: Map<String?, List<SmallGroupEntity>>,
        coursesByGroup: Map<String, List<CourseEntity>>,
    ): SmallGroupProgress {
        val children = groupsByParent[group.id]
            .orEmpty()
            .sortedWith(compareBy<SmallGroupEntity> { it.sortOrder }.thenBy { it.name })
            .map { buildSmallGroupProgress(it, groupsByParent, coursesByGroup) }
        val directCourses = coursesByGroup[group.id].orEmpty()
        val earnedCredits = group.earnedCredits
        return SmallGroupProgress(
            id = group.id,
            largeGroupId = group.largeGroupId,
            parentSmallGroupId = group.parentSmallGroupId,
            depth = group.depth,
            name = group.name,
            requiredCredits = group.requiredCredits,
            earnedCredits = earnedCredits,
            met = earnedCredits >= group.requiredCredits,
            courses = directCourses,
            childGroups = children,
        )
    }

}

package com.keshu.mobile.presentation.credit

import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.GraduationProgress
import com.keshu.mobile.domain.model.LargeGroupProgress
import com.keshu.mobile.domain.model.SmallGroupProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditProjectionTest {
    @Test
    fun takingCoursesRollUpThroughTheVisibleCreditTreeWithoutDuplicates() {
        val takingCourse = course("taking-1", "C001", 3.0, CourseStatus.TAKING, "child")
        val duplicateTakingCourse = course("taking-2", "C001", 3.0, CourseStatus.TAKING, "child")
        val child = smallGroup(
            id = "child",
            parentId = "parent",
            required = 5.0,
            earned = 2.0,
            courses = listOf(takingCourse, duplicateTakingCourse),
        )
        val parent = smallGroup(
            id = "parent",
            required = 7.0,
            earned = 4.0,
            children = listOf(child),
        )
        val tree = CreditTree(
            graduation = GraduationProgress(160.0, 80.0, 1, 0, false),
            largeGroups = listOf(
                LargeGroupProgress(
                    id = "large",
                    name = "大类",
                    requiredCredits = 10.0,
                    earnedCredits = 8.0,
                    smallGroupCount = 1,
                    metSmallGroupCount = 0,
                    met = false,
                    childGroups = listOf(parent),
                ),
            ),
        )

        val projection = buildCreditTreeProjection(tree)

        assertEquals(5.0, projection.smallGroups.getValue("child").credits, 0.0)
        assertTrue(projection.smallGroups.getValue("child").met)
        assertEquals(7.0, projection.smallGroups.getValue("parent").credits, 0.0)
        assertTrue(projection.smallGroups.getValue("parent").met)
        assertEquals(11.0, projection.largeGroups.getValue("large").credits, 0.0)
        assertEquals(1, projection.largeGroups.getValue("large").metSmallGroupCount)
    }

    @Test
    fun progressIsBoundedForPredictionDisplay() {
        assertEquals(0f, creditProgress(4.0, 0.0))
        assertEquals(1f, creditProgress(12.0, 10.0))
        assertEquals(0.5f, creditProgress(5.0, 10.0))
    }

    private fun smallGroup(
        id: String,
        parentId: String? = null,
        required: Double,
        earned: Double,
        courses: List<CourseEntity> = emptyList(),
        children: List<SmallGroupProgress> = emptyList(),
    ) = SmallGroupProgress(
        id = id,
        largeGroupId = "large",
        parentSmallGroupId = parentId,
        depth = if (parentId == null) 1 else 2,
        name = id,
        requiredCredits = required,
        earnedCredits = earned,
        met = earned >= required,
        courses = courses,
        childGroups = children,
    )

    private fun course(
        id: String,
        code: String,
        credit: Double,
        status: CourseStatus,
        groupId: String,
    ) = CourseEntity(
        id = id,
        code = code,
        name = id,
        credit = credit,
        smallGroupId = groupId,
        status = status,
    )
}

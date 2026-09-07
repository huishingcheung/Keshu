package com.keshu.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CourseStatus { NOT_TAKEN, TAKING, PASSED, FAILED }

@Entity(tableName = "large_groups")
data class LargeGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val requiredCredits: Double,
    val sortOrder: Int = 0,
    val earnedCredits: Double = 0.0,
)

@Entity(
    tableName = "small_groups",
    foreignKeys = [
        ForeignKey(
            entity = LargeGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["largeGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("largeGroupId")],
)
data class SmallGroupEntity(
    @PrimaryKey val id: String,
    val largeGroupId: String,
    val parentSmallGroupId: String? = null,
    val name: String,
    val requiredCredits: Double,
    val sortOrder: Int = 0,
    val depth: Int = 1,
    val earnedCredits: Double = 0.0,
)

@Entity(tableName = "academic_progress")
data class AcademicProgressEntity(
    @PrimaryKey val id: String = "graduation",
    val requiredCredits: Double,
    val earnedCredits: Double,
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SmallGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["smallGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("smallGroupId"), Index("code"), Index("status")],
)
data class CourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val credit: Double,
    val smallGroupId: String,
    val teacher: String? = null,
    val term: String? = null,
    val status: CourseStatus = CourseStatus.NOT_TAKEN,
    val score: Double? = null,
    val tags: String = "",
)

@Entity(tableName = "transcript_courses", indices = [Index("code"), Index("status")])
data class TranscriptCourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val credit: Double,
    val status: CourseStatus,
)

@Entity(tableName = "course_feedback_stats", indices = [Index("courseCode")])
data class CourseFeedbackStatsEntity(
    @PrimaryKey val id: String,
    val courseCode: String,
    val courseName: String,
    val teacher: String? = null,
    val avgScore: Double? = null,
    val passRate: Double? = null,
    val easiness: Int = 3,
    val workload: Int = 3,
    val recommendCount: Int = 0,
    val feedbackCount: Int = 0,
    val source: String = "user_feedback",
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val courseName: String,
    val startsAtMillis: Long,
    val location: String,
    val seatNo: String? = null,
)

@Entity(tableName = "class_sessions", indices = [Index("term"), Index("dayOfWeek"), Index("courseCode")])
data class ClassSessionEntity(
    @PrimaryKey val id: String,
    val term: String,
    val courseCode: String,
    val courseName: String,
    val teacher: String,
    val credit: Double?,
    val weeksText: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val location: String,
    val className: String,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueAtMillis: Long,
    val sourceText: String,
    val done: Boolean = false,
    val remindersEnabled: Boolean = true,
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val tag: String,
    val weight: Int = 1,
)

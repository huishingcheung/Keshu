package edu.jnu.smartedu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import edu.jnu.smartedu.data.local.dao.CourseDao
import edu.jnu.smartedu.data.local.dao.CourseFeedbackStatsDao
import edu.jnu.smartedu.data.local.dao.ClassScheduleDao
import edu.jnu.smartedu.data.local.dao.ExamDao
import edu.jnu.smartedu.data.local.dao.GroupDao
import edu.jnu.smartedu.data.local.dao.TaskDao
import edu.jnu.smartedu.data.local.dao.TranscriptCourseDao
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.AcademicProgressEntity
import edu.jnu.smartedu.data.local.entity.CourseFeedbackStatsEntity
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import edu.jnu.smartedu.data.local.entity.ExamEntity
import edu.jnu.smartedu.data.local.entity.LargeGroupEntity
import edu.jnu.smartedu.data.local.entity.SmallGroupEntity
import edu.jnu.smartedu.data.local.entity.TaskEntity
import edu.jnu.smartedu.data.local.entity.TranscriptCourseEntity
import edu.jnu.smartedu.data.local.entity.UserPreferenceEntity

@Database(
    entities = [
        CourseEntity::class,
        AcademicProgressEntity::class,
        CourseFeedbackStatsEntity::class,
        ClassSessionEntity::class,
        SmallGroupEntity::class,
        LargeGroupEntity::class,
        ExamEntity::class,
        TaskEntity::class,
        TranscriptCourseEntity::class,
        UserPreferenceEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class JnuDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun courseFeedbackStatsDao(): CourseFeedbackStatsDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun groupDao(): GroupDao
    abstract fun examDao(): ExamDao
    abstract fun taskDao(): TaskDao
    abstract fun transcriptCourseDao(): TranscriptCourseDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcript_courses` (
                        `id` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `credit` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_courses_code` ON `transcript_courses` (`code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_courses_status` ON `transcript_courses` (`status`)")
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `transcript_courses` (`id`, `code`, `name`, `credit`, `status`)
                    SELECT `id`, `code`, `name`, `credit`, `status`
                    FROM `courses`
                    WHERE `score` IS NOT NULL OR `smallGroupId` = 'small-unclassified-transcript'
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `large_groups` ADD COLUMN `earnedCredits` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `small_groups` ADD COLUMN `earnedCredits` REAL NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_progress` (
                        `id` TEXT NOT NULL,
                        `requiredCredits` REAL NOT NULL,
                        `earnedCredits` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

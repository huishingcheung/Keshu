package edu.jnu.smartedu.data

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import edu.jnu.smartedu.background.ExamAlarmScheduler
import edu.jnu.smartedu.data.local.JnuDatabase
import edu.jnu.smartedu.data.network.DeepSeekApi
import edu.jnu.smartedu.data.network.DeepSeekClient
import edu.jnu.smartedu.data.parser.AcademicHtmlParser
import edu.jnu.smartedu.data.repository.AcademicRepository
import edu.jnu.smartedu.data.repository.AdvisorRepository
import edu.jnu.smartedu.domain.usecase.BuildCreditTreeUseCase
import edu.jnu.smartedu.domain.usecase.AddTaskUseCase
import edu.jnu.smartedu.domain.usecase.GetCourseAdviceUseCase
import edu.jnu.smartedu.domain.usecase.ImportCurrentPageUseCase
import edu.jnu.smartedu.domain.usecase.ObserveLocalCourseFeedbackUseCase
import edu.jnu.smartedu.domain.usecase.ObserveCreditSummaryUseCase
import edu.jnu.smartedu.domain.usecase.ObserveScheduleUseCase
import edu.jnu.smartedu.domain.usecase.ScheduleExamAlarmsUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val database: JnuDatabase = Room.databaseBuilder(
        context,
        JnuDatabase::class.java,
        "jnu_smart_edu.db",
    ).addMigrations(JnuDatabase.MIGRATION_5_6, JnuDatabase.MIGRATION_6_7).fallbackToDestructiveMigration().build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val deepSeekApi = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DeepSeekApi::class.java)

    private val repository = AcademicRepository(
        database.courseDao(),
        database.classScheduleDao(),
        database.groupDao(),
        database.examDao(),
        database.taskDao(),
        database.transcriptCourseDao(),
        AcademicHtmlParser(),
    )
    private val deepSeekClient = DeepSeekClient(deepSeekApi)
    private val advisorRepository = AdvisorRepository(
        database.courseDao(),
        database.groupDao(),
        database.courseFeedbackStatsDao(),
        deepSeekClient,
    )

    val buildCreditTreeUseCase = BuildCreditTreeUseCase(database.groupDao(), database.courseDao())
    val observeScheduleUseCase = ObserveScheduleUseCase(database.examDao(), database.taskDao(), database.classScheduleDao())
    val observeCreditSummaryUseCase = ObserveCreditSummaryUseCase(database.courseDao(), database.classScheduleDao())
    val classScheduleDao = database.classScheduleDao()
    val observeLocalCourseFeedbackUseCase = ObserveLocalCourseFeedbackUseCase(database.courseDao(), database.courseFeedbackStatsDao())
    val importCurrentPageUseCase = ImportCurrentPageUseCase(repository)
    val getCourseAdviceUseCase = GetCourseAdviceUseCase(advisorRepository)
    val addTaskUseCase = AddTaskUseCase(database.taskDao(), ExamAlarmScheduler(context), context.applicationContext)
    val scheduleExamAlarmsUseCase = ScheduleExamAlarmsUseCase(
        database.examDao(),
        ExamAlarmScheduler(context),
    )
}

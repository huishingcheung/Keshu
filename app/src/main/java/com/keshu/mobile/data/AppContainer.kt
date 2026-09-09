package com.keshu.mobile.data

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.keshu.mobile.background.ExamAlarmScheduler
import com.keshu.mobile.data.local.KeshuDatabase
import com.keshu.mobile.data.local.ExamAvailabilityPreferences
import com.keshu.mobile.data.network.DeepSeekApi
import com.keshu.mobile.data.network.DeepSeekClient
import com.keshu.mobile.data.portal.JnuDataSourceFactory
import com.keshu.mobile.data.repository.AcademicRepository
import com.keshu.mobile.data.repository.AdvisorRepository
import com.keshu.mobile.data.source.AcademicDataSourceRegistry
import com.keshu.mobile.data.update.AppUpdateChecker
import com.keshu.mobile.domain.usecase.BuildCreditTreeUseCase
import com.keshu.mobile.domain.usecase.AddTaskUseCase
import com.keshu.mobile.domain.usecase.GetCourseAdviceUseCase
import com.keshu.mobile.domain.usecase.ImportAcademicDataUseCase
import com.keshu.mobile.domain.usecase.ObserveLocalCourseFeedbackUseCase
import com.keshu.mobile.domain.usecase.ObserveCreditSummaryUseCase
import com.keshu.mobile.domain.usecase.ObserveScheduleUseCase
import com.keshu.mobile.domain.usecase.ScheduleExamAlarmsUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val examAvailabilityPreferences = ExamAvailabilityPreferences(context.applicationContext)
    val database: KeshuDatabase = Room.databaseBuilder(
        context,
        KeshuDatabase::class.java,
        "keshu.db",
    ).addMigrations(
        KeshuDatabase.MIGRATION_5_6,
        KeshuDatabase.MIGRATION_6_7,
        KeshuDatabase.MIGRATION_7_8,
    ).fallbackToDestructiveMigration().build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val updateHttp = okHttp.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
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
    val academicDataSourceRegistry = AcademicDataSourceRegistry(
        factories = listOf(JnuDataSourceFactory()),
        defaultSourceId = "jnu-undergraduate",
    )
    val importAcademicDataUseCase = ImportAcademicDataUseCase(repository)
    val getCourseAdviceUseCase = GetCourseAdviceUseCase(advisorRepository)
    val addTaskUseCase = AddTaskUseCase(database.taskDao(), ExamAlarmScheduler(context), context.applicationContext)
    val appUpdateChecker = AppUpdateChecker(
        client = updateHttp,
        moshi = moshi,
        preferences = context.applicationContext.getSharedPreferences("app_update", Context.MODE_PRIVATE),
    )
    val scheduleExamAlarmsUseCase = ScheduleExamAlarmsUseCase(
        database.examDao(),
        ExamAlarmScheduler(context),
    )
}

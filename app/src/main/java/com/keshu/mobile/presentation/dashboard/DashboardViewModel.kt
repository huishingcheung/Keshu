package com.keshu.mobile.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keshu.mobile.data.local.dao.ClassScheduleDao
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.TaskEntity
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.CourseAdvice
import com.keshu.mobile.domain.usecase.BuildCreditTreeUseCase
import com.keshu.mobile.domain.usecase.AddTaskUseCase
import com.keshu.mobile.domain.usecase.CreditSummary
import com.keshu.mobile.domain.usecase.GetCourseAdviceUseCase
import com.keshu.mobile.domain.usecase.ObserveLocalCourseFeedbackUseCase
import com.keshu.mobile.domain.usecase.ObserveCreditSummaryUseCase
import com.keshu.mobile.domain.usecase.ObserveScheduleUseCase
import com.keshu.mobile.domain.usecase.ScheduleSnapshot
import com.keshu.mobile.domain.usecase.ScheduleExamAlarmsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class DashboardUiState(
    val loading: Boolean = true,
    val creditTree: CreditTree? = null,
    val creditSummary: CreditSummary = CreditSummary(),
    val schedule: ScheduleSnapshot = ScheduleSnapshot(emptyList(), emptyList(), emptyList()),
    val advice: CourseAdvice? = null,
    val advising: Boolean = false,
    val error: String? = null,
)

class DashboardViewModel(
    buildCreditTree: BuildCreditTreeUseCase,
    observeCreditSummary: ObserveCreditSummaryUseCase,
    observeSchedule: ObserveScheduleUseCase,
    val observeLocalFeedback: ObserveLocalCourseFeedbackUseCase,
    private val getCourseAdvice: GetCourseAdviceUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val scheduleExamAlarms: ScheduleExamAlarmsUseCase,
    private val classScheduleDao: ClassScheduleDao,
) : ViewModel() {
    private val transientState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = combine(
        buildCreditTree().map { DashboardUiState(loading = false, creditTree = it) },
        observeCreditSummary(),
        observeSchedule(),
        transientState,
    ) { base, creditSummary, schedule, transient ->
        base.copy(
            creditSummary = creditSummary,
            schedule = schedule,
            advice = transient.advice,
            advising = transient.advising,
            error = transient.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun scheduleAlarms() {
        viewModelScope.launch { scheduleExamAlarms() }
    }

    fun addTask(title: String, dueAtMillis: Long, remindersEnabled: Boolean) {
        viewModelScope.launch {
            runCatching { addTaskUseCase(title, dueAtMillis, remindersEnabled) }
                .onFailure { transientState.value = transientState.value.copy(error = it.message) }
        }
    }

    fun updateTask(task: TaskEntity, title: String, dueAtMillis: Long, remindersEnabled: Boolean) {
        viewModelScope.launch {
            runCatching { addTaskUseCase.update(task, title, dueAtMillis, remindersEnabled) }
                .onFailure { transientState.value = transientState.value.copy(error = it.message) }
        }
    }

    fun setTaskDone(task: TaskEntity, done: Boolean) {
        viewModelScope.launch {
            runCatching { addTaskUseCase.setDone(task, done) }
                .onFailure { transientState.value = transientState.value.copy(error = it.message) }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            runCatching { addTaskUseCase.delete(task) }
                .onFailure { transientState.value = transientState.value.copy(error = it.message) }
        }
    }

    fun requestAdvice(apiKey: String) {
        viewModelScope.launch {
            transientState.value = transientState.value.copy(advising = true, advice = null, error = null)
            val result = runCatching { getCourseAdvice(apiKey) }
            transientState.value = result.fold(
                onSuccess = { transientState.value.copy(advising = false, advice = it) },
                onFailure = { transientState.value.copy(advising = false, error = it.toAdviceErrorMessage()) },
            )
        }
    }

    fun updateClassSession(session: ClassSessionEntity) {
        viewModelScope.launch {
            classScheduleDao.updateSession(
                id = session.id,
                term = session.term,
                courseName = session.courseName,
                teacher = session.teacher,
                weeksText = session.weeksText,
                dayOfWeek = session.dayOfWeek.coerceIn(1, 7),
                startSection = session.startSection.coerceAtLeast(1),
                endSection = session.endSection.coerceAtLeast(session.startSection),
                location = session.location,
                className = session.className,
            )
        }
    }
}

private fun Throwable.toAdviceErrorMessage(): String {
    return when (this) {
        is HttpException -> when (code()) {
            401, 403 -> "DeepSeek API Key 无效或没有访问权限"
            402 -> "DeepSeek 账户余额不足，请充值后重试"
            429 -> "DeepSeek 请求过于频繁，请稍后再试"
            in 500..599 -> "DeepSeek 服务暂时不可用，请稍后再试"
            else -> "DeepSeek 请求失败（HTTP ${code()}）"
        }
        is IOException -> "网络连接失败，请检查网络后重试"
        is IllegalArgumentException -> message ?: "输入信息不完整"
        else -> message ?: "生成建议失败，请稍后再试"
    }
}

class DashboardViewModelFactory(
    private val buildCreditTree: BuildCreditTreeUseCase,
    private val observeCreditSummary: ObserveCreditSummaryUseCase,
    private val observeSchedule: ObserveScheduleUseCase,
    private val observeLocalFeedback: ObserveLocalCourseFeedbackUseCase,
    private val getCourseAdvice: GetCourseAdviceUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val scheduleExamAlarms: ScheduleExamAlarmsUseCase,
    private val classScheduleDao: ClassScheduleDao,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(
            buildCreditTree,
            observeCreditSummary,
            observeSchedule,
            observeLocalFeedback,
            getCourseAdvice,
            addTaskUseCase,
            scheduleExamAlarms,
            classScheduleDao,
        ) as T
    }
}

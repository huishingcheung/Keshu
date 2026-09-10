package com.keshu.mobile.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.ClassPeriodTime
import com.keshu.mobile.data.local.ClassTimeProfile
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.HolidayEntry
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.domain.DEFAULT_TEACHING_WEEKS
import com.keshu.mobile.domain.explicitWeekNumbers
import com.keshu.mobile.domain.weekNumbers
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.components.*
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
internal fun SchedulePage(
    state: DashboardUiState,
    semesterStartDate: String,
    classTimeSettings: ClassTimeSettings,
    holidayEntries: Map<LocalDate, HolidayEntry>,
    today: LocalDate,
    showSetupNotice: Boolean,
    onSemesterStartDateChange: (String) -> Unit,
    onClassTimeProfileChange: (ClassTimeProfile) -> Unit,
    onCustomClassTimesSave: (List<ClassPeriodTime>) -> Unit,
    onMakeupWeekdaySelected: (LocalDate, DayOfWeek) -> Unit,
    onMakeupWeekdayCleared: (LocalDate) -> Unit,
    onSetupNoticeDismiss: () -> Unit,
    onSessionSave: (ClassSessionEntity) -> Unit,
) {
    var selectedTerm by remember { mutableStateOf<String?>(null) }
    var selectedWeek by remember { mutableStateOf<Int?>(null) }
    var selectedSession by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showCustomTimeEditor by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentWeek = remember(semesterStartDate, today) { weekFromSemesterStart(semesterStartDate, today) }
    val terms = remember(state.schedule.classSessions) {
        state.schedule.classSessions.map { it.term }.distinct().sortedDescending()
    }
    val activeTerm = selectedTerm?.takeIf { it in terms } ?: terms.firstOrNull()
    val termSessions = remember(state.schedule.classSessions, activeTerm) {
        state.schedule.classSessions
            .filter { activeTerm == null || it.term == activeTerm }
            .sortedWith(compareBy<ClassSessionEntity> { it.dayOfWeek }.thenBy { it.startSection })
    }
    val weekIndex = remember(termSessions, currentWeek) { buildScheduleWeekIndex(termSessions, currentWeek) }
    val weekOptions = weekIndex.weeks
    val activeWeek = selectedWeek?.takeIf { it in weekOptions } ?: currentWeek?.takeIf { it in weekOptions } ?: weekOptions.firstOrNull()
    val pagerState = rememberPagerState(
        initialPage = weekOptions.indexOf(activeWeek).coerceAtLeast(0),
        pageCount = { weekOptions.size },
    )
    val visibleWeek = weekOptions.getOrNull(pagerState.currentPage) ?: activeWeek

    LaunchedEffect(pagerState.settledPage, weekOptions) {
        weekOptions.getOrNull(pagerState.settledPage)?.let { settledWeek ->
            if (selectedWeek != settledWeek) selectedWeek = settledWeek
        }
    }
    LaunchedEffect(activeWeek, weekOptions) {
        val targetPage = weekOptions.indexOf(activeWeek)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 360.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Text("课表设置", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "管理开学日期和学期课表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        SemesterStartCard(
                            value = semesterStartDate,
                            currentWeek = currentWeek,
                            onValueChange = onSemesterStartDateChange,
                        )
                    }
                    item {
                        ClassTimeSettingsCard(
                            settings = classTimeSettings,
                            onProfileSelected = onClassTimeProfileChange,
                            onEditCustom = { showCustomTimeEditor = true },
                        )
                    }
                    item {
                        HolidaySettingsCard(
                            entries = holidayEntries.values.sortedBy { it.date },
                            onMakeupWeekdaySelected = onMakeupWeekdaySelected,
                            onMakeupWeekdayCleared = onMakeupWeekdayCleared,
                        )
                    }
                    if (terms.isNotEmpty()) {
                        item { Text("学期课表", style = MaterialTheme.typography.titleMedium) }
                        items(terms, key = { it }) { term ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { selectedTerm = term },
                                shape = RoundedCornerShape(14.dp),
                                color = if (term == activeTerm) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (term == activeTerm) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                ),
                            ) {
                                Text(
                                    term,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (term == activeTerm) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScheduleTopHeader(
                activeWeek = visibleWeek,
                activeTerm = activeTerm,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSelectWeek = { showWeekPicker = true },
            )
            if (termSessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无课程，完成课表同步后会显示在这里",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    key = { page -> weekOptions[page] },
                ) { page ->
                    val week = weekOptions[page]
                    ScheduleWeekGrid(
                        modifier = Modifier.fillMaxSize(),
                        sessions = weekIndex.sessionsByWeek[week].orEmpty(),
                        selectedWeek = week,
                        semesterStartDate = semesterStartDate,
                        classTimeSettings = classTimeSettings,
                        holidayEntries = holidayEntries,
                        today = today,
                        onSessionClick = { selectedSession = it },
                    )
                }
            }
        }
    }
    if (showWeekPicker) {
        ScheduleWeekPickerDialog(
            weeks = weekOptions,
            selectedWeek = activeWeek,
            onSelected = {
                selectedWeek = it
                showWeekPicker = false
            },
            onDismiss = { showWeekPicker = false },
        )
    }
    if (showSetupNotice) {
        AlertDialog(
            onDismissRequest = onSetupNoticeDismiss,
            confirmButton = {
                Button(onClick = onSetupNoticeDismiss) { Text("知道了") }
            },
            title = { Text("课表使用提示") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("使用前请先点击左上角菜单（☰），修改开学日期和课表时间。")
                    Text(
                        "放假和调休日期取自国务院节假日安排。补课具体上星期几的课由各校自行通知，" +
                            "日期上标 * 的是应用按惯例给出的默认推测，请以学校通知为准，并在菜单的" +
                            "“节假日与调休”里改成本校的安排。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
    if (showCustomTimeEditor) {
        CustomClassTimeDialog(
            settings = classTimeSettings,
            onSave = {
                onCustomClassTimesSave(it)
                showCustomTimeEditor = false
            },
            onDismiss = { showCustomTimeEditor = false },
        )
    }
    selectedSession?.let { session ->
        ClassSessionDetailDialog(
            session = session,
            onDismiss = { selectedSession = null },
            onSave = {
                onSessionSave(it)
                selectedSession = null
            },
        )
    }
}

internal data class ScheduleWeekIndex(
    val weeks: List<Int>,
    val sessionsByWeek: Map<Int, List<ClassSessionEntity>>,
)

internal fun buildScheduleWeekIndex(
    sessions: List<ClassSessionEntity>,
    currentWeek: Int?,
): ScheduleWeekIndex {
    val parsedSessions = sessions.map { it to it.weekNumbers() }
    // Only explicit week ranges may lengthen the term. A `全周` course expands to every teachable
    // week, which would otherwise stretch the picker to the maximum instead of the term's real size.
    val maxWeek = maxOf(
        DEFAULT_TEACHING_WEEKS,
        currentWeek ?: 1,
        sessions.mapNotNull { it.explicitWeekNumbers()?.maxOrNull() }.maxOrNull() ?: 1,
    )
    val weeks = (1..maxWeek).toList()
    val buckets = weeks.associateWith { mutableListOf<ClassSessionEntity>() }
    parsedSessions.forEach { (session, sessionWeeks) ->
        sessionWeeks.forEach { week -> buckets[week]?.add(session) }
    }
    return ScheduleWeekIndex(
        weeks = weeks,
        sessionsByWeek = buckets.mapValues { (_, value) -> value.toList() },
    )
}

package com.studyflow.app.feature.tasks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CheckSquare
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.DateTimeField
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.FieldLabel
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.PickerField
import com.studyflow.app.core.ui.components.SegmentedToggle
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.StudyCalendar
import com.studyflow.app.domain.tasks.StudyCalendar.CalendarMode
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.domain.tasks.TaskStats
import com.studyflow.app.ui.appViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

private val TASK_TYPES = listOf(
    TaskType.UNIQUE,
    TaskType.DAILY,
    TaskType.WEEKLY,
    TaskType.MONTHLY,
    TaskType.WEEKLY_GOAL,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val vm: TasksViewModel = appViewModel { TasksViewModel(it.tasksRepository, it.projectsRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    var title by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(TaskType.UNIQUE.name) }
    var startDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var weekdayText by rememberSaveable { mutableStateOf("") }
    var monthDayText by rememberSaveable { mutableStateOf("") }
    var weeklyGoalText by rememberSaveable { mutableStateOf("") }

    val type = TASK_TYPES.firstOrNull { it.name == typeName } ?: TaskType.UNIQUE
    val recurrenceDays = remember(weekdayText) {
        weekdayText.split(',').mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.sorted()
    }

    LaunchedEffect(state.formOpen) {
        if (!state.formOpen) {
            title = ""
            typeName = TaskType.UNIQUE.name
            startDate = null
            endDate = null
            weekdayText = ""
            monthDayText = ""
            weeklyGoalText = ""
        }
    }

    LaunchedEffect(state.editingId) {
        val task = state.editingTask ?: return@LaunchedEffect
        title = task.title
        typeName = task.type.name
        startDate = ApiDates.localDateTimeOrNull(task.startDate)
        endDate = ApiDates.localDateTimeOrNull(task.endDate)
        weekdayText = task.recurrenceDays.sorted().joinToString(",")
        monthDayText = task.recurrenceDayOfMonth?.toString().orEmpty()
        weeklyGoalText = task.weeklyGoal?.toString().orEmpty()
    }

    val formBusy = state.creating || state.updating

    if (state.formOpen) {
        ModalBottomSheet(
            onDismissRequest = vm::toggleForm,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (state.editingId != null) s["editTaskTitle"] else s["addTaskAction"],
                        style = MaterialTheme.typography.titleLarge,
                    )
                    state.editingTask?.takeIf { state.editingId != null }?.let { task ->
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s["taskTitleLabel"]) },
                    placeholder = { Text(s["taskTitlePlaceholder"]) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PickerField(
                    label = s["taskTypeLabel"],
                    value = s[typeLabelKey(type)],
                    options = TASK_TYPES.map { s[typeLabelKey(it)] },
                    onSelect = { index -> typeName = TASK_TYPES[index].name },
                )
                if (type == TaskType.WEEKLY) {
                    WeekdayPicker(
                        selected = recurrenceDays,
                        onToggle = { day ->
                            val next = if (day in recurrenceDays) recurrenceDays - day else recurrenceDays + day
                            weekdayText = next.sorted().joinToString(",")
                        },
                    )
                }
                if (type == TaskType.MONTHLY) {
                    NumberField(
                        label = s["taskMonthDayLabel"],
                        value = monthDayText,
                        onChange = { monthDayText = it },
                    )
                }
                if (type == TaskType.WEEKLY_GOAL) {
                    NumberField(
                        label = s["taskWeeklyGoalLabel"],
                        value = weeklyGoalText,
                        onChange = { weeklyGoalText = it },
                    )
                }
                if (type != TaskType.DAILY || state.editingId != null) {
                    DateTimeField(
                        label = s["taskStartDateLabel"],
                        value = startDate,
                        onChange = { startDate = it },
                    )
                }
                DateTimeField(
                    label = s["taskEndDateLabel"],
                    value = endDate,
                    onChange = { endDate = it },
                )
                PickerField(
                    label = s["taskProjectLabel"],
                    value = state.projects.firstOrNull { it.id == state.formProjectId }?.title
                        ?: s["taskProjectNone"],
                    options = listOf(s["taskProjectNone"]) + state.projects.map { it.title },
                    onSelect = { index ->
                        vm.selectFormProject(if (index == 0) null else state.projects[index - 1].id)
                    },
                )
                if (state.formProjectId != null && state.formStages.isNotEmpty()) {
                    PickerField(
                        label = s["taskStageLabel"],
                        value = state.formStages.firstOrNull { it.id == state.formStageId }?.name
                            ?: s["taskStageNone"],
                        options = listOf(s["taskStageNone"]) + state.formStages.map { it.name },
                        onSelect = { index ->
                            vm.selectFormStage(if (index == 0) null else state.formStages[index - 1].id)
                        },
                    )
                }
                if (state.formErrorKey != null) {
                    ErrorBanner(message = s[state.formErrorKey!!])
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = vm::toggleForm,
                        enabled = !formBusy,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(s["cancelAction"]) }
                    Button(
                        onClick = {
                            vm.submitForm(
                                title = title,
                                start = startDate,
                                end = endDate,
                                type = type,
                                recurrenceDays = recurrenceDays,
                                recurrenceDayOfMonth = monthDayText.toIntOrNull(),
                                weeklyGoal = weeklyGoalText.toIntOrNull(),
                            )
                        },
                        enabled = !formBusy,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (formBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            when {
                                formBusy -> s["saving"]
                                state.editingId != null -> s["saveChangesAction"]
                                else -> s["taskSaveAction"]
                            },
                        )
                    }
                }
            }
        }
    }

    if (state.loading) {
        LoadingScreen(s["tasksLoading"])
        return
    }

    val pendingCount = state.items.count { !StudyCalendar.isTaskDone(it, state.todayKey) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Subtitle(s["tasksSubtitle"], Modifier.weight(1f))
                    TextButton(
                        enabled = state.total > 0,
                        onClick = {
                            val text = vm.copyTasksText() ?: return@TextButton
                            clipboard.setPrimaryClip(ClipData.newPlainText(s["tasks"], text))
                            copied = true
                        },
                    ) {
                        Text(
                            if (copied) s["tasksCopied"] else s["copyTasksAction"],
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            if (state.errorKey != null) {
                item {
                    ErrorBanner(
                        message = s[state.errorKey!!],
                        onRetry = vm::load,
                        retryLabel = s["retry"],
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PickerField(
                        label = s["taskFilterProject"],
                        value = state.projects.firstOrNull { it.id == state.projectFilter }?.title
                            ?: s["taskFilterAll"],
                        options = listOf(s["taskFilterAll"]) + state.projects.map { it.title },
                        onSelect = { index ->
                            vm.setProjectFilter(if (index == 0) null else state.projects[index - 1].id)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = vm::toggleStats,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(s["taskStatsToggle"]) }
                }
            }

            item {
                CalendarCard(
                    state = state,
                    onNavigate = vm::navigate,
                    onModeChange = vm::setMode,
                    onDayClick = vm::jumpTo,
                    onClearFilter = vm::clearDayFilter,
                )
            }

            if (state.statsOpen) {
                item {
                    StatsCard(
                        state = state,
                        onRetry = vm::loadStats,
                    )
                }
            }

            item {
                ContentCard {
                    if (state.items.isEmpty()) {
                        EmptyNote(s["noTasks"])
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.items.forEach { task ->
                                TaskRow(
                                    task = task,
                                    done = vm.isDone(task),
                                    saving = state.savingId == task.id,
                                    deleting = state.deletingId == task.id,
                                    enabled = vm.canToggle(task),
                                    selectedDay = state.selectedDay,
                                    todayKey = state.todayKey,
                                    projectName = state.projects.firstOrNull { it.id == task.projectId }?.title,
                                    stageName = task.projectId?.let { pid ->
                                        state.stagesByProject[pid]?.firstOrNull { it.id == task.stageId }?.name
                                    },
                                    onToggle = { vm.toggle(task.id) },
                                    onEdit = { vm.openEdit(task) },
                                    onDelete = { vm.remove(task.id) },
                                )
                            }
                        }
                    }
                }
            }

            if (state.lastPage > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(
                            onClick = { vm.goToPage(state.page - 1) },
                            enabled = state.canGoPrev,
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(s["prevPage"]) }
                        Text(
                            s.format("pageOf", "page" to state.page + 1, "total" to state.lastPage + 1),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { vm.goToPage(state.page + 1) },
                            enabled = state.canGoNext,
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(s["nextPage"]) }
                    }
                }
            }

            item {
                Text(
                    buildString {
                        append(s.format("taskPendingCount", "count" to pendingCount, "total" to state.total))
                        append(" · ")
                        append(s.format("taskRecurringCount", "count" to state.recurringCount))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudyFlowTheme.colors.subtle,
                )
            }
        }

        FloatingActionButton(
            onClick = vm::toggleForm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = s["addTaskAction"])
        }
    }
}

private fun typeLabelKey(type: TaskType): String = when (type) {
    TaskType.UNIQUE -> "taskTypeUnique"
    TaskType.DAILY -> "taskTypeDaily"
    TaskType.WEEKLY -> "taskTypeWeekly"
    TaskType.MONTHLY -> "taskTypeMonthly"
    TaskType.WEEKLY_GOAL -> "taskTypeWeeklyGoal"
}

@Composable
private fun WeekdayPicker(selected: List<Int>, onToggle: (Int) -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(s["taskWeekdaysLabel"])
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val monday = LocalDate.of(2024, 1, 1)
            (0..6).forEach { offset ->
                val day = offset + 1
                val active = day in selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            if (active) StudyFlowTheme.colors.primarySoft else MaterialTheme.colorScheme.background,
                            RoundedCornerShape(8.dp),
                        )
                        .border(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary else StudyFlowTheme.colors.border,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { onToggle(day) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        monday.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, s.locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            if (text.length <= 2 && text.all { it.isDigit() }) onChange(text)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatsCard(state: TasksUiState, onRetry: () -> Unit) {
    val s = LocalStrings.current
    ContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s["taskStatsTitle"], style = MaterialTheme.typography.titleMedium)
            Text(
                s["taskStatsPeriod"],
                style = MaterialTheme.typography.labelSmall,
                color = StudyFlowTheme.colors.subtle,
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            state.statsErrorKey != null -> ErrorBanner(
                message = s[state.statsErrorKey],
                onRetry = onRetry,
                retryLabel = s["retry"],
            )
            state.statsLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            state.statsTasks.isEmpty() -> EmptyNote(s["taskStatsEmpty"])
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.statsTasks.forEach { entry ->
                    StatsRow(entry)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(entry: TaskStats) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                entry.task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Tag(
                text = s[typeLabelKey(entry.task.type)],
                color = MaterialTheme.colorScheme.primary,
                background = StudyFlowTheme.colors.primarySoft,
            )
        }
        if (entry.task.type == TaskType.WEEKLY_GOAL) {
            Text(
                listOf(
                    s.format("taskStatsWeeksOk", "count" to entry.weeksOk),
                    s.format("taskStatsWeeksFailed", "count" to entry.weeksFailed),
                    s.format(
                        "taskStatsCurrentWeek",
                        "done" to entry.currentWeekDone,
                        "goal" to (entry.currentWeekGoal ?: 0),
                    ),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                s[if (entry.currentWeekMet) "taskStatsGoalMet" else "taskStatsGoalOpen"],
                style = MaterialTheme.typography.labelMedium,
                color = if (entry.currentWeekMet) StudyFlowTheme.colors.success else StudyFlowTheme.colors.subtle,
            )
        } else {
            Text(
                listOf(
                    s.format("taskStatsCompleted", "count" to entry.completed),
                    s.format("taskStatsFailures", "count" to entry.failed),
                    s.format("taskStatsRate", "rate" to entry.rate),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarCard(
    state: TasksUiState,
    onNavigate: (Int) -> Unit,
    onModeChange: (CalendarMode) -> Unit,
    onDayClick: (String) -> Unit,
    onClearFilter: () -> Unit,
) {
    val s = LocalStrings.current
    val calendar = remember(state.visibleTasks, state.anchor, state.mode, state.todayKey) {
        StudyCalendar.buildCalendarGrid(state.visibleTasks, state.anchor, state.mode, state.todayKey)
    }
    val locale = s.locale

    ContentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onNavigate(-1) }, enabled = !state.calendarLoading) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = s["calendarPrev"])
            }
            Text(
                text = periodLabel(state, calendar.firstOrNull()?.date ?: state.anchor, locale),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onNavigate(1) }, enabled = !state.calendarLoading) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = s["calendarNext"])
            }
        }
        Spacer(Modifier.height(8.dp))
        SegmentedToggle(
            options = listOf(s["calendarWeek"], s["calendarMonth"]),
            selectedIndex = if (state.mode == CalendarMode.WEEK) 0 else 1,
            onSelect = { index -> onModeChange(if (index == 0) CalendarMode.WEEK else CalendarMode.MONTH) },
        )
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            val monday = LocalDate.of(2024, 1, 1)
            (0..6).forEach { offset ->
                Text(
                    text = monday.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = StudyFlowTheme.colors.subtle,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        calendar.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        today = day.key == state.todayKey,
                        selected = day.key == state.selectedDay,
                        onClick = { onDayClick(day.key) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "dayStatus_done" to StudyFlowTheme.colors.success,
                "dayStatus_partial" to StudyFlowTheme.colors.warning,
                "dayStatus_missed" to StudyFlowTheme.colors.danger,
            ).forEach { (key, color) ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape),
                )
                Text(
                    s[key],
                    style = MaterialTheme.typography.labelSmall,
                    color = StudyFlowTheme.colors.subtle,
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        if (state.selectedDay != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = s.format(
                        "calendarFilteredBy",
                        "date" to TaskFormat.formatDayChip(state.selectedDay, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClearFilter) {
                    Text(s["calendarClearFilter"], style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: StudyCalendar.CalendarDay,
    today: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StudyFlowTheme.colors
    val background = when (day.status) {
        StudyCalendar.DayStatus.DONE -> colors.successSoft
        StudyCalendar.DayStatus.PARTIAL -> colors.warningSoft
        StudyCalendar.DayStatus.MISSED -> colors.dangerSoft
        StudyCalendar.DayStatus.EMPTY -> Color.Transparent
    }
    val accent = when (day.status) {
        StudyCalendar.DayStatus.DONE -> colors.success
        StudyCalendar.DayStatus.PARTIAL -> colors.warning
        StudyCalendar.DayStatus.MISSED -> colors.danger
        StudyCalendar.DayStatus.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        today -> colors.borderStrong
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .height(50.dp)
            .background(background, RoundedCornerShape(8.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = day.dayNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = accent.copy(alpha = if (day.outside) 0.45f else 1f),
                fontWeight = if (today) FontWeight.Bold else FontWeight.Medium,
            )
            if (day.total > 0) {
                Text(
                    text = "${day.done}/${day.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = if (day.outside) 0.45f else 1f),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    done: Boolean,
    saving: Boolean,
    deleting: Boolean,
    enabled: Boolean,
    selectedDay: String?,
    todayKey: String,
    projectName: String?,
    stageName: String?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalStrings.current
    val rowDay = selectedDay
        ?: if (task.type != TaskType.UNIQUE && StudyCalendar.scheduledOn(task, todayKey)) todayKey else ""
    val recurring = task.type != TaskType.UNIQUE

    val statsText = when {
        task.type == TaskType.WEEKLY_GOAL -> {
            val progress = StudyCalendar.weeklyGoalProgress(task, todayKey)
            s.format("taskWeeklyGoalProgress", "done" to progress.done, "goal" to progress.goal) +
                if (progress.met) " · ${s["taskStatsGoalMet"]}" else ""
        }
        recurring -> task.dailyCompletions.size.takeIf { it > 0 }
            ?.let { s.format("taskDailyDoneCount", "count" to it) }
            .orEmpty()
        else -> ""
    }
    val meta = listOfNotNull(
        "${s["endDateLabel"]} ${TaskFormat.formatTaskPeriod(task.startDate, task.endDate, s.locale)}",
        listOfNotNull(projectName, stageName).joinToString(" · ").ifBlank { null },
        statsText.ifBlank { null },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = enabled && !saving && !deleting, onClick = onToggle)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CheckSquare(checked = done)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        task.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (rowDay.isNotBlank()) {
                        Tag(
                            text = TaskFormat.formatDayChip(rowDay, s.locale),
                            color = if (rowDay == todayKey) MaterialTheme.colorScheme.primary else StudyFlowTheme.colors.subtle,
                            background = if (rowDay == todayKey) StudyFlowTheme.colors.primarySoft else MaterialTheme.colorScheme.background,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        meta,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyFlowTheme.colors.subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (recurring) {
                        Tag(
                            text = s[typeLabelKey(task.type)],
                            color = MaterialTheme.colorScheme.primary,
                            background = StudyFlowTheme.colors.primarySoft,
                        )
                    }
                    if (task.status == TaskStatus.DELAYED) {
                        Tag(text = s["taskDelayed"])
                    }
                }
            }
        }
        IconButton(onClick = onEdit, enabled = !deleting, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = s["taskEditAction"],
                tint = StudyFlowTheme.colors.subtle,
                modifier = Modifier.size(15.dp),
            )
        }
        IconButton(onClick = onDelete, enabled = !deleting, modifier = Modifier.size(34.dp)) {
            if (deleting) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = s["taskDeleteAction"],
                    tint = StudyFlowTheme.colors.subtle,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

private fun periodLabel(
    state: TasksUiState,
    firstDay: LocalDate,
    locale: java.util.Locale,
): String {
    return if (state.mode == CalendarMode.WEEK) {
        firstDay.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM", locale))
    } else {
        state.anchor.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
}

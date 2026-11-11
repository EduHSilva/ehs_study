package com.studyflow.app.feature.kanban

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.SegmentedToggle
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.kanban.KanbanBoard
import com.studyflow.app.domain.kanban.KanbanEntity
import com.studyflow.app.domain.kanban.ProjectColumn
import com.studyflow.app.domain.kanban.ProjectColumnView
import com.studyflow.app.domain.kanban.TaskColumn
import com.studyflow.app.domain.kanban.TaskColumnView
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.ui.appViewModel
import kotlin.math.roundToInt

private val ColumnWidth = 272.dp
private val CardWidth = ColumnWidth - 24.dp

private data class DragState(
    val task: Task? = null,
    val project: Project? = null,
    val center: Offset = Offset.Zero,
)

private fun columnKey(prefix: String, column: Enum<*>): String = "$prefix:${column.name}"

private fun taskColumnLabelKey(column: TaskColumn): String = when (column) {
    TaskColumn.PENDING -> "kanbanTasksPending"
    TaskColumn.WORKING -> "kanbanTasksWorking"
    TaskColumn.DELAYED -> "kanbanTasksDelayed"
    TaskColumn.DONE -> "kanbanTasksDone"
}

private fun projectColumnLabelKey(column: ProjectColumn): String = when (column) {
    ProjectColumn.WAITING -> "kanbanProjectsWaiting"
    ProjectColumn.WORKING -> "kanbanProjectsInProgress"
    ProjectColumn.DONE -> "kanbanProjectsDone"
}

@Composable
fun KanbanScreen() {
    val vm: KanbanViewModel = appViewModel { KanbanViewModel(it.tasksRepository, it.projectsRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current

    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val columnBounds = remember { mutableStateMapOf<String, Rect>() }
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    val drag = remember { mutableStateOf<DragState?>(null) }

    val endDrag = {
        val current = drag.value
        drag.value = null
        if (current != null) {
            if (current.task != null) {
                val column = TaskColumn.values().firstOrNull { option ->
                    columnBounds[columnKey("TASK", option)]?.contains(current.center) == true
                }
                if (column != null) vm.moveTask(current.task.id, column)
            } else if (current.project != null) {
                val column = ProjectColumn.values().firstOrNull { option ->
                    columnBounds[columnKey("PROJECT", option)]?.contains(current.center) == true
                }
                if (column != null) vm.moveProject(current.project.id, column)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.boundsInRoot().topLeft },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Subtitle(s["kanbanSubtitle"])

            SegmentedToggle(
                options = listOf(s["kanbanSwitchTasks"], s["kanbanSwitchProjects"]),
                selectedIndex = if (state.entity == KanbanEntity.TASKS) 0 else 1,
                onSelect = { index ->
                    vm.setEntity(if (index == 0) KanbanEntity.TASKS else KanbanEntity.PROJECTS)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorKey?.let { key ->
                ErrorBanner(message = s[key], onRetry = vm::load, retryLabel = s["retry"])
            }

            if (state.loading) {
                LoadingScreen(s["kanbanLoading"])
            } else if (state.entity == KanbanEntity.TASKS) {
                TaskBoard(
                    columns = state.taskColumns,
                    today = state.todayKey,
                    savingId = state.savingId,
                    projectName = state::projectName,
                    onColumnBounds = { column, bounds ->
                        columnBounds[columnKey("TASK", column)] = bounds
                    },
                    onCardBounds = { id, bounds -> cardBounds[id] = bounds },
                    onDragStart = { task ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        drag.value = DragState(
                            task = task,
                            center = cardBounds[task.id]?.center ?: Offset.Zero,
                        )
                    },
                    onDrag = { amount ->
                        drag.value = drag.value?.let { it.copy(center = it.center + amount) }
                    },
                    onDragEnd = endDrag,
                    onDragCancel = { drag.value = null },
                    onMove = vm::moveTask,
                )
            } else {
                ProjectBoard(
                    columns = state.projectColumns,
                    savingId = state.savingId,
                    onColumnBounds = { column, bounds ->
                        columnBounds[columnKey("PROJECT", column)] = bounds
                    },
                    onCardBounds = { id, bounds -> cardBounds[id] = bounds },
                    onDragStart = { project ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        drag.value = DragState(
                            project = project,
                            center = cardBounds[project.id]?.center ?: Offset.Zero,
                        )
                    },
                    onDrag = { amount ->
                        drag.value = drag.value?.let { it.copy(center = it.center + amount) }
                    },
                    onDragEnd = endDrag,
                    onDragCancel = { drag.value = null },
                    onMove = vm::moveProject,
                )
            }
        }

        DragOverlayHost(
            drag = drag,
            rootOffset = rootOffset,
            projectName = state::projectName,
        )
    }
}

@Composable
private fun TaskBoard(
    columns: List<TaskColumnView>,
    today: String,
    savingId: String?,
    projectName: (Task) -> String?,
    onColumnBounds: (TaskColumn, Rect) -> Unit,
    onCardBounds: (String, Rect) -> Unit,
    onDragStart: (Task) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMove: (String, TaskColumn) -> Unit,
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        columns.forEach { column ->
            BoardColumn(
                title = s[column.labelKey],
                count = column.items.size,
                tone = taskTone(column.column),
                onBounds = { onColumnBounds(column.column, it) },
            ) {
                if (column.items.isEmpty()) {
                    EmptyColumn(s["kanbanEmptyColumn"])
                }
                column.items.forEach { task ->
                    TaskCard(
                        task = task,
                        column = column.column,
                        today = today,
                        saving = savingId == task.id,
                        projectName = projectName(task),
                        onBounds = { onCardBounds(task.id, it) },
                        onDragStart = { onDragStart(task) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onMove = { option -> onMove(task.id, option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectBoard(
    columns: List<ProjectColumnView>,
    savingId: String?,
    onColumnBounds: (ProjectColumn, Rect) -> Unit,
    onCardBounds: (String, Rect) -> Unit,
    onDragStart: (Project) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMove: (String, ProjectColumn) -> Unit,
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        columns.forEach { column ->
            BoardColumn(
                title = s[column.labelKey],
                count = column.items.size,
                tone = projectTone(column.column),
                onBounds = { onColumnBounds(column.column, it) },
            ) {
                if (column.items.isEmpty()) {
                    EmptyColumn(s["kanbanEmptyColumn"])
                }
                column.items.forEach { project ->
                    ProjectCard(
                        project = project,
                        column = column.column,
                        saving = savingId == project.id,
                        onBounds = { onCardBounds(project.id, it) },
                        onDragStart = { onDragStart(project) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onMove = { option -> onMove(project.id, option) },
                    )
                }
            }
        }
    }
}

private data class ColumnTone(
    val background: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
)

@Composable
private fun taskTone(column: TaskColumn): ColumnTone {
    val colors = StudyFlowTheme.colors
    return when (column) {
        TaskColumn.PENDING -> ColumnTone(MaterialTheme.colorScheme.background, colors.border)
        TaskColumn.WORKING -> ColumnTone(colors.primarySoft, MaterialTheme.colorScheme.primary)
        TaskColumn.DELAYED -> ColumnTone(colors.warningSoft, colors.warning)
        TaskColumn.DONE -> ColumnTone(colors.successSoft, colors.success)
    }
}

@Composable
private fun projectTone(column: ProjectColumn): ColumnTone {
    val colors = StudyFlowTheme.colors
    return when (column) {
        ProjectColumn.WAITING -> ColumnTone(MaterialTheme.colorScheme.background, colors.border)
        ProjectColumn.WORKING -> ColumnTone(colors.primarySoft, MaterialTheme.colorScheme.primary)
        ProjectColumn.DONE -> ColumnTone(colors.successSoft, colors.success)
    }
}

@Composable
private fun BoardColumn(
    title: String,
    count: Int,
    tone: ColumnTone,
    onBounds: (Rect) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(ColumnWidth)
            .background(tone.background, RoundedCornerShape(16.dp))
            .border(1.dp, tone.border, RoundedCornerShape(16.dp))
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Tag(
                text = count.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surface,
            )
        }
        content()
    }
}

@Composable
private fun EmptyColumn(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    column: TaskColumn,
    today: String,
    saving: Boolean,
    projectName: String?,
    onBounds: (Rect) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMove: (TaskColumn) -> Unit,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors
    var menuOpen by remember(task.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (column == TaskColumn.DONE) TextDecoration.LineThrough else null,
                    color = if (column == TaskColumn.DONE) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        enabled = !saving,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = s["kanbanMoveAction"],
                            modifier = Modifier.size(18.dp),
                            tint = colors.subtle,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        // A DAILY nunca atrasa e a UNIQUE só entra em DELAYED se já venceu, pois o
                        // back recalcula o atraso pelo prazo.
                        val overdue = KanbanBoard.isTaskOverdue(task, today)
                        TaskColumn.values()
                            .filter { it != TaskColumn.DELAYED || column == TaskColumn.DELAYED || overdue }
                            .forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(s[taskColumnLabelKey(option)]) },
                                    onClick = {
                                        menuOpen = false
                                        onMove(option)
                                    },
                                    enabled = option != column,
                                )
                            }
                    }
                }
            }

            Text(
                text = "${s["endDateLabel"]} ${TaskFormat.formatTaskPeriod(task.startDate, task.endDate, s.locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtle,
            )
            if (!projectName.isNullOrBlank()) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (task.type == TaskType.DAILY) {
                    Tag(
                        text = s["taskTypeDaily"],
                        color = MaterialTheme.colorScheme.primary,
                        background = colors.primarySoft,
                    )
                }
                TaskColumnTag(column)
                Spacer(Modifier.weight(1f))
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun TaskColumnTag(column: TaskColumn) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors
    val (color, background) = when (column) {
        TaskColumn.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant to colors.border
        TaskColumn.WORKING -> MaterialTheme.colorScheme.primary to colors.primarySoft
        TaskColumn.DELAYED -> colors.warning to colors.warningSoft
        TaskColumn.DONE -> colors.success to colors.successSoft
    }
    Tag(text = s[taskColumnLabelKey(column)], color = color, background = background)
}

@Composable
private fun ProjectCard(
    project: Project,
    column: ProjectColumn,
    saving: Boolean,
    onBounds: (Rect) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMove: (ProjectColumn) -> Unit,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors
    var menuOpen by remember(project.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .pointerInput(project.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = project.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (column == ProjectColumn.DONE) TextDecoration.LineThrough else null,
                    color = if (column == ProjectColumn.DONE) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        enabled = !saving,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = s["kanbanMoveAction"],
                            modifier = Modifier.size(18.dp),
                            tint = colors.subtle,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ProjectColumn.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(s[projectColumnLabelKey(option)]) },
                                onClick = {
                                    menuOpen = false
                                    onMove(option)
                                },
                                enabled = option != column,
                            )
                        }
                    }
                }
            }

            Text(
                text = "${s["advisorLabel"]} ${project.advisor.ifBlank { s["noAdvisor"] }}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = s.format("percentCompleted", "progress" to project.progress),
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtle,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val (tagColor, tagBackground) = when (column) {
                    ProjectColumn.WAITING -> MaterialTheme.colorScheme.onSurfaceVariant to colors.border
                    ProjectColumn.WORKING -> MaterialTheme.colorScheme.primary to colors.primarySoft
                    ProjectColumn.DONE -> colors.success to colors.successSoft
                }
                Tag(
                    text = s[projectColumnLabelKey(column)],
                    color = tagColor,
                    background = tagBackground,
                )
                Spacer(Modifier.weight(1f))
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun DragOverlayHost(
    drag: MutableState<DragState?>,
    rootOffset: Offset,
    projectName: (Task) -> String?,
) {
    val density = LocalDensity.current
    val cardWidthPx = with(density) { CardWidth.toPx() }
    val cardHeightPx = with(density) { 96.dp.toPx() }

    drag.value?.let { current ->
        val center = current.center - rootOffset
        DragOverlay(
            task = current.task,
            project = current.project,
            projectName = current.task?.let(projectName),
            modifier = Modifier.offset {
                IntOffset(
                    (center.x - cardWidthPx / 2f).roundToInt(),
                    (center.y - cardHeightPx / 2f).roundToInt(),
                )
            },
        )
    }
}

@Composable
private fun DragOverlay(
    task: Task?,
    project: Project?,
    projectName: String?,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors
    Surface(
        modifier = modifier.width(CardWidth),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = task?.title ?: project?.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (task != null) {
                Text(
                    text = "${s["endDateLabel"]} ${TaskFormat.formatTaskPeriod(task.startDate, task.endDate, s.locale)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtle,
                )
                if (!projectName.isNullOrBlank()) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else if (project != null) {
                Text(
                    text = "${s["advisorLabel"]} ${project.advisor.ifBlank { s["noAdvisor"] }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = s.format("percentCompleted", "progress" to project.progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtle,
                )
            }
        }
    }
}

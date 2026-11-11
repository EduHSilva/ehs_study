package com.studyflow.app.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.CheckSquare
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.DateTimeField
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LinkButton
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.ProgressRing
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.ProjectStage
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.data.model.ProjectVersion
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.projects.VersionDeadline
import com.studyflow.app.domain.projects.VersionDeadline.Tone
import com.studyflow.app.domain.tasks.StudyCalendar
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.ui.appViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(projectId: String, onBack: () -> Unit, onManageTasks: () -> Unit) {
    val vm: ProjectDetailViewModel = appViewModel {
        ProjectDetailViewModel(projectId, it.projectsRepository, it.tasksRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    var newStageTitle by rememberSaveable { mutableStateOf("") }
    var newVersionTitle by rememberSaveable { mutableStateOf("") }
    var newVersionDue by remember { mutableStateOf<LocalDateTime?>(null) }

    var editingProject by rememberSaveable { mutableStateOf(false) }
    var editProjectTitle by rememberSaveable { mutableStateOf("") }
    var editProjectAdvisor by rememberSaveable { mutableStateOf("") }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    var showCompleted by rememberSaveable { mutableStateOf(true) }
    val expandedStages = remember { mutableStateMapOf<String, Boolean>() }
    var unassignedExpanded by rememberSaveable { mutableStateOf(false) }

    var editing by remember { mutableStateOf<ProjectVersion?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editDue by remember { mutableStateOf<LocalDateTime?>(null) }

    val details = state.details

    LaunchedEffect(state.projectDeleted) {
        if (state.projectDeleted) onBack()
    }

    if (state.loading && details == null) {
        LoadingScreen(s["projectDetailsLoadFailed"])
        return
    }

    if (details == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ErrorBanner(
                message = s[state.errorKey ?: "projectDetailsLoadFailed"],
                onRetry = vm::load,
                retryLabel = s["retry"],
            )
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp)) { Text(s["goBack"]) }
        }
        return
    }

    if (editingProject) {
        AlertDialog(
            onDismissRequest = { editingProject = false },
            title = { Text(s["editProjectAction"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editProjectTitle,
                        onValueChange = { editProjectTitle = it },
                        label = { Text(s["projectTitleLabel"]) },
                        placeholder = { Text(s["projectTitlePlaceholder"]) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editProjectAdvisor,
                        onValueChange = { editProjectAdvisor = it },
                        label = { Text(s["projectAdvisorLabel"]) },
                        placeholder = { Text(s["projectAdvisorPlaceholder"]) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.saveProject(editProjectTitle, editProjectAdvisor)
                        editingProject = false
                    },
                    enabled = !state.savingProject && editProjectTitle.isNotBlank(),
                ) { Text(if (state.savingProject) s["saving"] else s["save"]) }
            },
            dismissButton = {
                TextButton(onClick = { editingProject = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(s["deleteProjectAction"]) },
            text = { Text(s["deleteProjectConfirm"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.removeProject()
                        confirmingDelete = false
                    },
                    enabled = !state.savingProject,
                ) { Text(s["deleteAction"], color = StudyFlowTheme.colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(s["editVersionAction"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(s["versionTitleLabel"]) },
                        singleLine = true,
                    )
                    DateTimeField(
                        label = s["versionDueDateLabel"],
                        value = editDue,
                        onChange = { editDue = it },
                        allowClear = false,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = editing ?: return@TextButton
                        vm.updateVersion(target.id, editTitle, editDue)
                        editing = null
                    },
                    enabled = editTitle.isNotBlank() && editDue != null,
                ) { Text(s["save"]) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(s["cancelAction"]) }
            },
        )
    }

    val isTaskDone: (Task) -> Boolean = { StudyCalendar.isTaskDone(it, state.todayKey) }
    val doneStageCount = details.stages.count { it.done }
    val visibleStages = if (showCompleted) details.stages else details.stages.filterNot { it.done }
    val hiddenCompletedCount = if (showCompleted) {
        0
    } else {
        doneStageCount + state.tasks.count(isTaskDone)
    }
    val visibleUnassigned = state.tasks
        .filter { it.stageId.isNullOrBlank() }
        .let { list -> if (showCompleted) list else list.filterNot(isTaskDone) }
    val stageTasksFor = { stage: ProjectStage ->
        state.tasks
            .filter { it.stageId == stage.id }
            .let { list -> if (showCompleted) list else list.filterNot(isTaskDone) }
    }
    val allStagesExpanded = visibleStages.isNotEmpty() && visibleStages.all { expandedStages[it.id] == true }
    val timeline = details.versions
        .mapIndexed { index, version -> Triple(index + 1, version, VersionDeadline.meta(version.dueDate)) }
        .sortedBy { it.third.sortValue }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
            ContentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(details.project.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${s["advisorLabel"]} ${details.project.advisor.ifBlank { s["noAdvisor"] }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        when (details.project.status) {
                            ProjectStatus.DONE -> Tag(
                                text = s["projectStatusDone"],
                                color = StudyFlowTheme.colors.success,
                                background = StudyFlowTheme.colors.successSoft,
                            )
                            ProjectStatus.WORKING -> Tag(
                                text = s["projectStatusInProgress"],
                                color = MaterialTheme.colorScheme.primary,
                                background = StudyFlowTheme.colors.primarySoft,
                            )
                            ProjectStatus.WAITING -> Tag(
                                text = s["projectStatusPending"],
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                background = StudyFlowTheme.colors.border,
                            )
                        }
                    }
                    ProgressRing(progress = details.project.progress, size = 78.dp)
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            editProjectTitle = details.project.title
                            editProjectAdvisor = details.project.advisor
                            editingProject = true
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s["editProjectAction"], style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = vm::toggleProjectStatus,
                        enabled = !state.savingProject,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            if (details.project.status == ProjectStatus.DONE) {
                                s["reopenProjectAction"]
                            } else {
                                s["archiveProjectAction"]
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    OutlinedButton(
                        onClick = { confirmingDelete = true },
                        enabled = !state.savingProject,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            s["deleteProjectAction"],
                            style = MaterialTheme.typography.labelMedium,
                            color = StudyFlowTheme.colors.danger,
                        )
                    }
                }
            }
        }

        item {
            ContentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        s["stagesTitle"],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(
                        onClick = { showCompleted = !showCompleted },
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text(
                            if (showCompleted) s["hideCompleted"] else s["showCompleted"],
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (doneStageCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Text("$doneStageCount", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (details.stages.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val next = !allStagesExpanded
                                visibleStages.forEach { expandedStages[it.id] = next }
                            },
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        ) {
                            Text(
                                if (allStagesExpanded) s["collapseAllStages"] else s["expandAllStages"],
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (hiddenCompletedCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.format("hiddenCompletedCount", "count" to hiddenCompletedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyFlowTheme.colors.subtle,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.tasksLoading) {
                    LoadingScreen(s["projectTasksLoading"])
                } else if (state.tasksErrorKey != null) {
                    ErrorBanner(
                        message = s[state.tasksErrorKey!!],
                        onRetry = vm::loadTasks,
                        retryLabel = s["retry"],
                    )
                    Spacer(Modifier.height(8.dp))
                }

                when {
                    details.stages.isEmpty() -> EmptyNote(s["noStages"])
                    visibleStages.isEmpty() -> EmptyNote(s["allStagesHidden"])
                    else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        visibleStages.forEach { stage ->
                            StageSection(
                                stage = stage,
                                expanded = expandedStages[stage.id] == true,
                                saving = state.savingStageId == stage.id,
                                tasks = stageTasksFor(stage),
                                hasAnyTask = state.tasks.any { it.stageId == stage.id },
                                todayKey = state.todayKey,
                                onToggleDone = { vm.toggleStage(stage.id) },
                                onToggleExpanded = { expandedStages[stage.id] = !(expandedStages[stage.id] == true) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newStageTitle,
                        onValueChange = { newStageTitle = it },
                        placeholder = { Text(s["stageTitlePlaceholder"]) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            vm.createStage(newStageTitle)
                            newStageTitle = ""
                        },
                        enabled = !state.savingNewStage && newStageTitle.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (state.savingNewStage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(s["addStage"], style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                val allUnassigned = state.tasks.filter { it.stageId.isNullOrBlank() }
                if (!state.tasksLoading && allUnassigned.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unassignedExpanded = !unassignedExpanded }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = StudyFlowTheme.colors.subtle,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (unassignedExpanded) 0f else -90f),
                        )
                        Text(
                            s["unassignedTasksTitle"],
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            s.format("stageTaskCount", "count" to visibleUnassigned.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = StudyFlowTheme.colors.subtle,
                        )
                    }
                    if (unassignedExpanded) {
                        if (visibleUnassigned.isEmpty()) {
                            EmptyNote(s["noVisibleTasks"])
                        } else {
                            visibleUnassigned.forEach { task ->
                                ProjectTaskRow(
                                    title = task.title,
                                    done = isTaskDone(task),
                                    daily = task.type == TaskType.DAILY,
                                )
                            }
                        }
                    }
                }

                if (!state.tasksLoading && details.stages.isEmpty() && allUnassigned.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    EmptyNote(s["noProjectTasks"])
                }

                Spacer(Modifier.height(8.dp))
                LinkButton(text = s["manageTasks"], onClick = onManageTasks)
            }
        }

        item {
            ContentCard {
                CardTitle(s["versionsTitle"])
                Spacer(Modifier.height(12.dp))
                if (details.versions.isEmpty()) {
                    EmptyNote(s["noVersions"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        timeline.forEach { (position, version, meta) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            s.format("versionLabel", "index" to position),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudyFlowTheme.colors.subtle,
                                        )
                                        VersionBadge(meta)
                                    }
                                    Text(
                                        version.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        TaskFormat.formatVersionDue(version.dueDate, s.locale),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudyFlowTheme.colors.subtle,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editing = version
                                        editTitle = version.title
                                        editDue = ApiDates.localDateTimeOrNull(version.dueDate)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = s["editVersionAction"],
                                        tint = StudyFlowTheme.colors.subtle,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newVersionTitle,
                        onValueChange = { newVersionTitle = it },
                        label = { Text(s["versionTitleLabel"]) },
                        placeholder = { Text(s["versionTitlePlaceholder"]) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateTimeField(
                        label = s["versionDueDateLabel"],
                        value = newVersionDue,
                        onChange = { newVersionDue = it },
                        allowClear = false,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                vm.createVersion(newVersionTitle, newVersionDue)
                                newVersionTitle = ""
                                newVersionDue = null
                            },
                            enabled = !state.savingNewVersion &&
                                newVersionTitle.isNotBlank() &&
                                newVersionDue != null,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (state.savingNewVersion) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (state.savingNewVersion) s["saving"] else s["addVersion"])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageSection(
    stage: ProjectStage,
    expanded: Boolean,
    saving: Boolean,
    tasks: List<Task>,
    hasAnyTask: Boolean,
    todayKey: String,
    onToggleDone: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(enabled = !saving, onClick = onToggleDone),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    CheckSquare(checked = stage.done)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) s["collapseAllStages"] else s["expandAllStages"],
                    tint = StudyFlowTheme.colors.subtle,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (expanded) 90f else 0f),
                )
                Text(
                    stage.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (stage.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (stage.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    s.format("stageTaskCount", "count" to tasks.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = StudyFlowTheme.colors.subtle,
                )
            }
        }
        if (expanded) {
            Column(Modifier.padding(start = 20.dp, top = 2.dp, bottom = 8.dp)) {
                when {
                    tasks.isNotEmpty() -> tasks.forEach { task ->
                        ProjectTaskRow(
                            title = task.title,
                            done = StudyCalendar.isTaskDone(task, todayKey),
                            daily = task.type == TaskType.DAILY,
                        )
                    }
                    hasAnyTask -> EmptyNote(s["noVisibleTasks"])
                    else -> EmptyNote(s["noStageTasks"])
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(meta: VersionDeadline.Meta) {
    val s = LocalStrings.current
    val label = when (meta.tone) {
        Tone.OVERDUE -> s.format("versionPast", "days" to (meta.days ?: 0))
        Tone.SOON -> if (meta.days == 0) {
            s["versionDueToday"]
        } else {
            s.format("versionDueIn", "days" to (meta.days ?: 0))
        }
        Tone.NORMAL -> return
    }
    val color = if (meta.tone == Tone.OVERDUE) StudyFlowTheme.colors.danger else StudyFlowTheme.colors.warning
    val background = if (meta.tone == Tone.OVERDUE) {
        StudyFlowTheme.colors.dangerSoft
    } else {
        StudyFlowTheme.colors.warningSoft
    }
    Tag(text = label, color = color, background = background)
}

@Composable
private fun ProjectTaskRow(title: String, done: Boolean, daily: Boolean) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CheckSquare(checked = done, size = 16.dp)
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (daily) {
            Tag(
                text = s["taskTypeDaily"],
                color = MaterialTheme.colorScheme.primary,
                background = StudyFlowTheme.colors.primarySoft,
            )
        }
    }
}

package com.studyflow.app.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.ProgressRing
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.ProjectMilestone
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.ui.appViewModel
import java.time.LocalDateTime

@Composable
fun ProjectDetailScreen(projectId: String, onBack: () -> Unit) {
    val vm: ProjectDetailViewModel = appViewModel {
        ProjectDetailViewModel(projectId, it.projectsRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    var newStageTitle by rememberSaveable { mutableStateOf("") }
    var newMilestoneTitle by rememberSaveable { mutableStateOf("") }
    var newMilestoneDue by remember { mutableStateOf<LocalDateTime?>(null) }

    var editing by remember { mutableStateOf<ProjectMilestone?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editDue by remember { mutableStateOf<LocalDateTime?>(null) }

    val details = state.details

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

    if (editing != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(s["editMilestone"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(s["milestoneTitleLabel"]) },
                        singleLine = true,
                    )
                    DateTimeField(
                        label = s["milestoneDueDateLabel"],
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
                        vm.updateMilestone(target.id, editTitle, editDue)
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
                            "${s["advisorLabel"]} ${details.project.advisor.ifBlank { "—" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (details.project.status == ProjectStatus.DONE) {
                            Tag(
                                text = s["projectStatusDone"],
                                color = StudyFlowTheme.colors.success,
                                background = StudyFlowTheme.colors.successSoft,
                            )
                        } else {
                            Tag(
                                text = s["projectStatusInProgress"],
                                color = MaterialTheme.colorScheme.primary,
                                background = StudyFlowTheme.colors.primarySoft,
                            )
                        }
                    }
                    ProgressRing(progress = details.project.progress, size = 78.dp)
                }
            }
        }

        item {
            ContentCard {
                CardTitle(s["stagesTitle"])
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    details.stages.forEach { stage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .clickable(
                                    enabled = state.savingStageId != stage.id,
                                    onClick = { vm.toggleStage(stage.id) },
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CheckSquare(checked = stage.done)
                            Text(
                                stage.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (stage.done) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (stage.done) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (state.savingStageId == stage.id) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
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
            }
        }

        item {
            ContentCard {
                CardTitle(s["milestonesTitle"])
                Spacer(Modifier.height(12.dp))
                if (details.milestones.isEmpty()) {
                    EmptyNote(s["noMilestones"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        details.milestones.forEachIndexed { index, milestone ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        s.format("milestoneLabel", "index" to index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudyFlowTheme.colors.subtle,
                                    )
                                    Text(
                                        milestone.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        s.format(
                                            "dueDateLabel",
                                            "date" to TaskFormat.formatCompactDate(milestone.dueDate, s.locale),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudyFlowTheme.colors.subtle,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editing = milestone
                                        editTitle = milestone.title
                                        editDue = ApiDates.localDateTimeOrNull(milestone.dueDate)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = s["editMilestone"],
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
                        value = newMilestoneTitle,
                        onValueChange = { newMilestoneTitle = it },
                        label = { Text(s["milestoneTitleLabel"]) },
                        placeholder = { Text(s["milestoneTitlePlaceholder"]) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateTimeField(
                        label = s["milestoneDueDateLabel"],
                        value = newMilestoneDue,
                        onChange = { newMilestoneDue = it },
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
                                vm.createMilestone(newMilestoneTitle, newMilestoneDue)
                                newMilestoneTitle = ""
                                newMilestoneDue = null
                            },
                            enabled = !state.savingNewMilestone &&
                                newMilestoneTitle.isNotBlank() &&
                                newMilestoneDue != null,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (state.savingNewMilestone) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (state.savingNewMilestone) s["saving"] else s["addMilestone"])
                        }
                    }
                }
            }
        }
    }
}

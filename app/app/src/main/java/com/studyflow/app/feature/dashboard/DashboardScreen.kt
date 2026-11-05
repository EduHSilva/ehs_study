package com.studyflow.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.CheckSquare
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LinkButton
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.ProgressRing
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.domain.tasks.TaskFormat
import com.studyflow.app.navigation.Routes
import com.studyflow.app.ui.LocalContainer
import com.studyflow.app.ui.appViewModel

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val vm: DashboardViewModel = appViewModel {
        DashboardViewModel(it.dashboardRepository, it.focusRepository, it.tasksRepository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by LocalContainer.current.sessionManager.session.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    val firstName = session?.user?.name?.trim()?.split(Regex("\\s+"))?.firstOrNull()
        ?.takeIf { it.isNotBlank() } ?: s["studentFallback"]

    if (state.loading && state.data == null) {
        LoadingScreen(s["dashboardLoading"])
        return
    }

    val data = state.data
    if (data == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ErrorBanner(
                message = s[state.errorKey ?: "dashboardError"],
                onRetry = vm::load,
                retryLabel = s["retry"],
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    s.format("greeting", "name" to firstName),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    s["dashboardWelcome"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            ContentCard {
                CardTitle(s["weeklyProgressTitle"])
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProgressRing(progress = state.progress)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            buildString {
                                append(s["weeklyProgressBefore"])
                                append(' ')
                                append(data.weeklyProgress.completed)
                                append(' ')
                                append(s["of"])
                                append(' ')
                                append(data.weeklyProgress.total)
                                append(' ')
                                append(s["weeklyProgressAfter"])
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            s["weeklyProgressCheer"],
                            style = MaterialTheme.typography.labelMedium,
                            color = StudyFlowTheme.colors.success,
                        )
                    }
                }
            }
        }

        item {
            ContentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s["focusTitle"], style = MaterialTheme.typography.titleMedium)
                    Tag(
                        text = s["focusTag"],
                        color = MaterialTheme.colorScheme.primary,
                        background = StudyFlowTheme.colors.primarySoft,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = ApiDates.formatTime(state.focus?.completedAt, s.locale) ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column {
                        state.focus?.let {
                            Text(it.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                s.format("focusMinutes", "minutes" to it.minutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudyFlowTheme.colors.subtle,
                            )
                        }
                    }
                }
                if (state.focus == null) {
                    Spacer(Modifier.height(10.dp))
                    EmptyNote(s["noFocusToday"])
                }
                Spacer(Modifier.height(6.dp))
                LinkButton(text = s["goToTimer"], onClick = { onNavigate(Routes.POMODORO) })
            }
        }

        item {
            ContentCard {
                CardTitle(
                    title = s["todayTasks"],
                    action = {
                        LinkButton(text = s["seeAll"], onClick = { onNavigate(Routes.TASKS) })
                    },
                )
                if (data.tasks.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    EmptyNote(s["noTasks"])
                } else {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.tasks.forEach { task ->
                            DashboardTaskRow(
                                task = task,
                                saving = state.savingTaskId == task.id,
                                onClick = { vm.toggleTask(task.id) },
                            )
                        }
                    }
                }
            }
        }

        item {
            ContentCard {
                Text(s["recentNotes"], style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (data.notes.isEmpty()) {
                    EmptyNote(s["noNotesInFolder"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.notes.forEach { note ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudyFlowTheme.colors.subtle,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTaskRow(task: Task, saving: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    val done = task.status == TaskStatus.DONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable(enabled = !saving, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckSquare(checked = done)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${s["endDateLabel"]} ${TaskFormat.formatEndDate(task.endDate, s.locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.subtle,
            )
        }
        if (task.type == TaskType.DAILY) {
            Tag(
                text = s["taskTypeDaily"],
                color = MaterialTheme.colorScheme.primary,
                background = StudyFlowTheme.colors.primarySoft,
            )
        }
        if (task.status == TaskStatus.DELAYED) {
            Tag(text = s["taskDelayed"])
        }
    }
}

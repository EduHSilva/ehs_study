package com.studyflow.app.feature.projects

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.RefreshableLazyColumn
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.ui.appViewModel

// Mesmo limite do back (POST /project/stage/bulk).
private const val MAX_MODULES = 50

@Composable
fun ProjectsScreen(onOpenProject: (String) -> Unit) {
    val vm: ProjectsViewModel = appViewModel { ProjectsViewModel(it.projectsRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    var showCreate by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var advisor by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var modules by rememberSaveable { mutableStateOf("") }

    val resetCreateForm = {
        showCreate = false
        title = ""
        advisor = ""
        link = ""
        modules = ""
    }

    LaunchedEffect(state.newProjectId) {
        val id = state.newProjectId ?: return@LaunchedEffect
        vm.consumeNewProject()
        resetCreateForm()
        onOpenProject(id)
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { resetCreateForm() },
            title = { Text(s["createProject"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(s["projectTitleLabel"]) },
                        placeholder = { Text(s["projectTitlePlaceholder"]) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = advisor,
                        onValueChange = { advisor = it },
                        label = { Text(s["projectAdvisorLabel"]) },
                        placeholder = { Text(s["projectAdvisorPlaceholder"]) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it.take(500) },
                        label = { Text(s["projectLinkLabel"]) },
                        placeholder = { Text(s["projectLinkPlaceholder"]) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = modules,
                        onValueChange = { value -> modules = value.filter { it.isDigit() }.take(2) },
                        label = { Text(s["moduleCountLabel"]) },
                        placeholder = { Text(s["moduleCountPlaceholder"]) },
                        supportingText = { Text(s["moduleCountHint"]) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    if (state.errorKey == "projectCreateFailed") {
                        ErrorBanner(message = s["projectCreateFailed"])
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val count = modules.toIntOrNull()?.coerceIn(0, MAX_MODULES) ?: 0
                        // Só a quantidade é perguntada; os títulos nascem numerados e são
                        // renomeados depois na tela do projeto.
                        val moduleTitles = (1..count).map { index -> s.format("moduleStageName", "index" to index) }
                        vm.create(title, advisor, link, moduleTitles)
                    },
                    enabled = !state.savingProject,
                ) {
                    Text(if (state.savingProject) s["saving"] else s["createProject"])
                }
            },
            dismissButton = {
                TextButton(onClick = { resetCreateForm() }) { Text(s["cancelAction"]) }
            },
        )
    }

    if (state.loading) {
        LoadingScreen(s["projectsLoading"])
        return
    }

    RefreshableLazyColumn(
        isRefreshing = state.refreshing,
        onRefresh = { vm.load(silent = true) },
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Subtitle(s["projectsSubtitle"]) }
        item {
            Text(
                s["projectsHint"],
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.subtle,
            )
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
                CardTitle(
                    title = s["myProjects"],
                    action = {
                        Button(
                            onClick = { showCreate = true },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s["newProject"], style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    s["projectProgressHint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = StudyFlowTheme.colors.subtle,
                )
                Spacer(Modifier.height(12.dp))
                if (state.projects.isEmpty()) {
                    EmptyNote(s["noProjects"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.projects.forEach { project ->
                            ProjectRow(project = project, onClick = { onOpenProject(project.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: Project, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.status == ProjectStatus.DONE) {
                    Tag(
                        text = s["projectStatusDone"],
                        color = StudyFlowTheme.colors.success,
                        background = StudyFlowTheme.colors.successSoft,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(StudyFlowTheme.colors.border, CircleShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((project.progress.coerceIn(0, 100)) / 100f)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Text(
                s.format("percentCompleted", "progress" to project.progress),
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.subtle,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = StudyFlowTheme.colors.subtle,
            modifier = Modifier.size(18.dp),
        )
    }
}

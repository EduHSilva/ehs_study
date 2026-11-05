package com.studyflow.app.feature.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.ProgressRing
import com.studyflow.app.core.ui.components.SegmentedToggle
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.components.Tag
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.model.ReferenceStatus
import com.studyflow.app.data.model.ReviewReport
import com.studyflow.app.ui.appViewModel

@Composable
fun ReviewScreen() {
    val vm: ReviewViewModel = appViewModel { ReviewViewModel(it.reviewRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    if (state.loading) {
        LoadingScreen(s["reviewLoading"])
        return
    }

    val report = state.report
    if (report == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ErrorBanner(
                message = s[state.errorKey ?: "reviewLoadFailed"],
                onRetry = vm::load,
                retryLabel = s["retry"],
            )
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Subtitle(s["reviewSubtitle"]) }

        item { DocumentPreview(report) }

        item {
            ContentCard {
                SegmentedToggle(
                    options = listOf(s["tabPlagiarism"], s["tabAbnt"], s["tabReferences"]),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
                Spacer(Modifier.height(20.dp))
                when (tab) {
                    0 -> SimilaritySection(report)
                    1 -> AbntSection(report)
                    else -> ReferencesSection(report)
                }
            }
        }
    }
}

@Composable
private fun DocumentPreview(report: ReviewReport) {
    val s = LocalStrings.current
    ContentCard {
        CardTitle(s["previewA4"])
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(report.documentTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                report.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                report.flaggedExcerpt,
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.danger,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
private fun SimilaritySection(report: ReviewReport) {
    val s = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProgressRing(
            progress = report.similarityPercent,
            size = 92.dp,
            color = StudyFlowTheme.colors.warning,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s["similarityTitle"], style = MaterialTheme.typography.titleMedium)
            Text(
                s.format("similarityText", "percent" to report.similarityPercent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AbntSection(report: ReviewReport) {
    val s = LocalStrings.current
    if (report.abntChecks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        report.abntChecks.forEach { check ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (check.passed) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (check.passed) StudyFlowTheme.colors.success else StudyFlowTheme.colors.danger,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    check.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (check.passed) MaterialTheme.colorScheme.onSurface else StudyFlowTheme.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun ReferencesSection(report: ReviewReport) {
    val s = LocalStrings.current
    if (report.references.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        report.references.forEach { reference ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    reference.citation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (reference.status == ReferenceStatus.VALID) {
                    Tag(
                        text = s["referenceValid"],
                        color = StudyFlowTheme.colors.success,
                        background = StudyFlowTheme.colors.successSoft,
                    )
                } else {
                    Tag(text = s["referenceAttention"])
                }
            }
        }
    }
}

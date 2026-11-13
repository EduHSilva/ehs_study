package com.studyflow.app.feature.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.studyflow.app.core.ui.components.SegmentedToggle
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.domain.ApiDates
import com.studyflow.app.ui.appViewModel

@Composable
fun PomodoroScreen() {
    val vm: PomodoroViewModel = appViewModel {
        PomodoroViewModel(it.focusRepository, it.settings, it.pomodoroTimer)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    if (state.loading) {
        LoadingScreen(s["pomodoroLoading"])
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Subtitle(s["timerSubtitle"]) }

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
                SegmentedToggle(
                    options = listOf(s["focusMode"], s["breakMode"], s["longBreakMode"]),
                    selectedIndex = state.mode.ordinal,
                    onSelect = { index -> vm.setMode(TimerMode.entries[index]) },
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimerRing(
                        seconds = state.timer.remainingSeconds,
                        total = state.timer.durationSeconds,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = state.time,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = s.format(
                                    "sessionCounter",
                                    "current" to state.sessionNumber,
                                    "total" to state.settings.cycles,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudyFlowTheme.colors.subtle,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (state.completed) {
                    val completedKey = when (state.mode) {
                        TimerMode.FOCUS -> "focusComplete"
                        TimerMode.BREAK -> "breakComplete"
                        TimerMode.LONG_BREAK -> "longBreakComplete"
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            s[completedKey],
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = StudyFlowTheme.colors.success,
                        )
                        Text(
                            s[if (state.mode == TimerMode.FOCUS) "registerFocusHint" else "breakCompleteHint"],
                            style = MaterialTheme.typography.bodySmall,
                            color = StudyFlowTheme.colors.subtle,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = vm::reset,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(s["reset"]) }
                    if (state.completed && state.mode != TimerMode.FOCUS) {
                        Button(
                            onClick = { vm.setMode(TimerMode.FOCUS) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(s["backToFocus"]) }
                    }
                    if (!state.completed) {
                        Button(
                            onClick = vm::toggleTimer,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(if (state.running) s["pause"] else s["start"]) }
                    }
                    if (state.focusStarted && state.mode == TimerMode.FOCUS) {
                        Button(
                            onClick = vm::finishSession,
                            enabled = !state.saving,
                            modifier = Modifier.weight(1.2f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (state.saving) s["saving"] else s["finishSession"],
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            PomodoroSettingsCard(
                settings = state.settings,
                onChange = vm::updateSettings,
            )
        }

        item {
            ContentCard {
                CardTitle(s["sessions"])
                Spacer(Modifier.height(14.dp))
                if (state.sessions.isEmpty()) {
                    EmptyNote(s["noSessions"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.sessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(StudyFlowTheme.colors.successSoft, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = StudyFlowTheme.colors.success,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        session.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        sessionTime(session.completedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudyFlowTheme.colors.subtle,
                                    )
                                }
                                Text(
                                    s.format("focusMinutes", "minutes" to session.minutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun PomodoroSettingsCard(settings: PomodoroSettings, onChange: (PomodoroSettings) -> Unit) {
    val s = LocalStrings.current
    ContentCard {
        CardTitle(s["pomodoroSettingsTitle"])
        Spacer(Modifier.height(14.dp))
        NumberSettingField(
            label = s["pomodoroFocusMinutes"],
            value = settings.focusMinutes,
            onChange = { onChange(settings.copy(focusMinutes = it)) },
        )
        Spacer(Modifier.height(10.dp))
        NumberSettingField(
            label = s["pomodoroBreakMinutes"],
            value = settings.breakMinutes,
            onChange = { onChange(settings.copy(breakMinutes = it)) },
        )
        Spacer(Modifier.height(10.dp))
        NumberSettingField(
            label = s["pomodoroLongBreakMinutes"],
            value = settings.longBreakMinutes,
            onChange = { onChange(settings.copy(longBreakMinutes = it)) },
        )
        Spacer(Modifier.height(10.dp))
        NumberSettingField(
            label = s["pomodoroCycles"],
            value = settings.cycles,
            onChange = { onChange(settings.copy(cycles = it)) },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            s["pomodoroSettingsHint"],
            style = MaterialTheme.typography.bodySmall,
            color = StudyFlowTheme.colors.subtle,
        )
    }
}

@Composable
private fun NumberSettingField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var text by rememberSaveable(label) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            if (raw.length <= 3 && raw.all { it.isDigit() }) {
                text = raw
                raw.toIntOrNull()?.let(onChange)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimerRing(
    seconds: Int,
    total: Int,
    content: @Composable () -> Unit,
) {
    val progress = if (total <= 0) 0f else ((total - seconds).toFloat() / total).coerceIn(0f, 1f)
    val track = StudyFlowTheme.colors.border
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                )
            }
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun sessionTime(completedAt: String?): String {
    val s = LocalStrings.current
    val time = ApiDates.formatTime(completedAt, s.locale) ?: return s["sessionCompleted"]
    return s.format("sessionCompletedAt", "time" to time)
}

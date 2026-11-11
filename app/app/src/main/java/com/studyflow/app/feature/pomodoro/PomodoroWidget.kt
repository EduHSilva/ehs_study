package com.studyflow.app.feature.pomodoro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.data.repository.FocusRepository
import com.studyflow.app.ui.appViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Finaliza a sessão pelo widget flutuante, sem depender da tela do Pomodoro. */
class PomodoroWidgetViewModel(
    private val timer: PomodoroTimer,
    private val focusRepository: FocusRepository,
    private val settings: AppSettings,
) : ViewModel() {

    val timerState: StateFlow<PomodoroTimerState> = timer.state

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun toggle() = timer.toggle()

    fun dismiss() = timer.reset()

    fun finish() {
        val current = timer.state.value
        if (!current.started || current.mode != TimerMode.FOCUS || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                focusRepository.createSession(settings.strings()["focusSessionTitle"], current.elapsedMinutes)
                timer.reset()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Widget silencioso: a tela do Pomodoro exibe o erro completo.
            } finally {
                _saving.value = false
            }
        }
    }
}

/** Equivalente ao `PomodoroWidget.vue`: flutua nas telas que não são o Pomodoro. */
@Composable
fun PomodoroWidget(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val vm: PomodoroWidgetViewModel = appViewModel {
        PomodoroWidgetViewModel(it.pomodoroTimer, it.focusRepository, it.settings)
    }
    val timer by vm.timerState.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    if (!timer.isActive) return

    val colors = StudyFlowTheme.colors
    val borderColor = when {
        timer.completed -> colors.success
        timer.mode == TimerMode.BREAK -> MaterialTheme.colorScheme.primary
        else -> colors.border
    }
    val modeLabel = s[if (timer.mode == TimerMode.FOCUS) "focusModeShort" else "breakModeShort"]
    val stateLabel = when {
        timer.completed -> s[if (timer.mode == TimerMode.FOCUS) "focusComplete" else "breakComplete"]
        timer.running -> s["timerRunning"]
        else -> s["timerPaused"]
    }
    val canFinish = timer.mode == TimerMode.FOCUS && timer.started

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudyFlowTheme.colors.subtle,
                )
                Text(
                    timer.timeLeft,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    stateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (timer.completed) colors.success else colors.subtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!timer.completed) {
                TextButton(onClick = vm::toggle) {
                    Text(
                        if (timer.running) s["pause"] else s["start"],
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
            if (canFinish) {
                TextButton(onClick = vm::finish, enabled = !saving) {
                    Text(
                        if (saving) s["saving"] else s["finishSession"],
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            TextButton(onClick = vm::dismiss) {
                Text(
                    "×",
                    style = MaterialTheme.typography.titleMedium,
                    color = StudyFlowTheme.colors.subtle,
                )
            }
        }
    }
}

package com.studyflow.app.feature.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.data.model.FocusSession
import com.studyflow.app.data.repository.FocusRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val sessions: List<FocusSession> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorKey: String? = null,
    val timer: PomodoroTimerState = PomodoroTimerState(),
) {
    val sessionNumber: Int get() = timer.sessionNumber
    val time: String get() = timer.timeLeft
    val running: Boolean get() = timer.running
    val mode: TimerMode get() = timer.mode
    val breakMode: Boolean get() = timer.mode != TimerMode.FOCUS
    val completed: Boolean get() = timer.completed
    val focusStarted: Boolean get() = timer.started
    val settings: PomodoroSettings get() = timer.settings
}

private data class SessionsState(
    val sessions: List<FocusSession> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorKey: String? = null,
)

class PomodoroViewModel(
    private val focusRepository: FocusRepository,
    private val settings: AppSettings,
    private val timer: PomodoroTimer,
) : ViewModel() {

    private val _sessions = MutableStateFlow(SessionsState())

    val state: StateFlow<PomodoroUiState> = combine(_sessions, timer.state) { sessions, timerState ->
        PomodoroUiState(
            sessions = sessions.sessions,
            loading = sessions.loading,
            saving = sessions.saving,
            errorKey = sessions.errorKey,
            timer = timerState,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _sessions.update { it.copy(loading = true, errorKey = null) }
            try {
                val sessions = focusRepository.getTodaySessions()
                _sessions.update { it.copy(sessions = sessions, loading = false, errorKey = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _sessions.update { it.copy(loading = false, errorKey = "pomodoroLoadFailed") }
            }
        }
    }

    fun toggleTimer() = timer.toggle()

    fun reset() = timer.reset()

    fun setMode(mode: TimerMode) = timer.setMode(mode)

    fun updateSettings(settings: PomodoroSettings) = timer.updateSettings(settings)

    fun finishSession() {
        val timerState = timer.state.value
        if (!timerState.started || timerState.mode != TimerMode.FOCUS || _sessions.value.saving) return
        val minutes = timerState.elapsedMinutes
        if (minutes <= 0) return
        _sessions.update { it.copy(saving = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val session = focusRepository.createSession(settings.strings()["focusSessionTitle"], minutes)
                _sessions.update {
                    it.copy(sessions = listOf(session) + it.sessions, saving = false)
                }
                timer.advanceAfterFocus()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _sessions.update { it.copy(saving = false, errorKey = "sessionSaveFailed") }
            }
        }
    }
}

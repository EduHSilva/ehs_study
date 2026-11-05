package com.studyflow.app.feature.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.data.model.FocusSession
import com.studyflow.app.data.repository.FocusRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

const val FOCUS_SECONDS = 25 * 60
const val BREAK_SECONDS = 5 * 60

data class PomodoroUiState(
    val sessions: List<FocusSession> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorKey: String? = null,
    val running: Boolean = false,
    val breakMode: Boolean = false,
    val seconds: Int = FOCUS_SECONDS,
    val focusStarted: Boolean = false,
) {
    val sessionNumber: Int get() = (sessions.size % 4) + 1
    val time: String get() = "%02d:%02d".format(seconds / 60, seconds % 60)
}

class PomodoroViewModel(
    private val focusRepository: FocusRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(PomodoroUiState())
    val state: StateFlow<PomodoroUiState> = _state.asStateFlow()

    init {
        load()
        // Cronômetro roda no cliente (como no web); só "Finalizar sessão" persiste no back.
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                val sessions = focusRepository.getTodaySessions()
                _state.update { it.copy(sessions = sessions, loading = false, errorKey = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "pomodoroLoadFailed") }
            }
        }
    }

    private fun tick() {
        _state.update { current ->
            if (!current.running || current.seconds <= 0) return@update current
            val next = current.seconds - 1
            if (next <= 0) current.copy(seconds = 0, running = false) else current.copy(seconds = next)
        }
    }

    fun toggleTimer() {
        _state.update { current ->
            if (current.running) {
                current.copy(running = false)
            } else {
                current.copy(running = true, focusStarted = current.focusStarted || !current.breakMode)
            }
        }
    }

    fun reset() {
        _state.update {
            it.copy(
                running = false,
                focusStarted = false,
                seconds = if (it.breakMode) BREAK_SECONDS else FOCUS_SECONDS,
            )
        }
    }

    fun setMode(breakMode: Boolean) {
        _state.update {
            it.copy(
                breakMode = breakMode,
                running = false,
                focusStarted = false,
                seconds = if (breakMode) BREAK_SECONDS else FOCUS_SECONDS,
            )
        }
    }

    fun finishSession() {
        val current = _state.value
        if (!current.focusStarted || current.breakMode || current.saving) return
        val elapsedSeconds = FOCUS_SECONDS - current.seconds
        if (elapsedSeconds <= 0) return
        val minutes = max(1, (elapsedSeconds / 60.0).roundToInt())
        _state.update { it.copy(running = false, saving = true, errorKey = null) }
        viewModelScope.launch {
            try {
                val session = focusRepository.createSession(settings.strings()["focusSessionTitle"], minutes)
                _state.update {
                    it.copy(
                        sessions = listOf(session) + it.sessions,
                        saving = false,
                        focusStarted = false,
                        seconds = FOCUS_SECONDS,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(saving = false, errorKey = "sessionSaveFailed") }
            }
        }
    }
}

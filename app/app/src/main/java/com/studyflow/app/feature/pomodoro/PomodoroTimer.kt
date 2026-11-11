package com.studyflow.app.feature.pomodoro

import com.studyflow.app.core.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.roundToInt

const val FOCUS_SECONDS = 25 * 60
const val BREAK_SECONDS = 5 * 60

enum class TimerMode { FOCUS, BREAK }

data class PomodoroTimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val running: Boolean = false,
    val remainingSeconds: Int = FOCUS_SECONDS,
    val started: Boolean = false,
    val completed: Boolean = false,
) {
    val durationSeconds: Int get() = if (mode == TimerMode.FOCUS) FOCUS_SECONDS else BREAK_SECONDS
    val isActive: Boolean
        get() = running || started || completed || remainingSeconds < durationSeconds
    val timeLeft: String
        get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    val elapsedMinutes: Int
        get() = if (mode != TimerMode.FOCUS) 0
        else max(1, ((FOCUS_SECONDS - remainingSeconds) / 60.0).roundToInt())
}

@Serializable
private data class StoredTimer(
    val mode: String = TimerMode.FOCUS.name,
    val running: Boolean = false,
    val endsAt: Long? = null,
    val remaining: Int = FOCUS_SECONDS,
    val started: Boolean = false,
    val completed: Boolean = false,
)

/**
 * Cronômetro Pomodoro compartilhado pelo app inteiro (como o composable global do web):
 * sobrevive à navegação e ao processo via `SharedPreferences` na chave `studyflow-pomodoro`.
 */
class PomodoroTimer(
    private val preferences: AppPreferences,
    private val sound: PomodoroSound = PomodoroChime,
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(PomodoroTimerState())
    val state: StateFlow<PomodoroTimerState> = _state.asStateFlow()

    private var endsAt: Long? = null

    init {
        restore()
        scope.launch {
            while (isActive) {
                delay(500)
                tick()
            }
        }
    }

    fun toggle() {
        if (_state.value.running) pause() else start()
    }

    fun start() {
        val current = _state.value
        if (current.running || current.completed || current.remainingSeconds <= 0) return
        endsAt = clock() + current.remainingSeconds * 1000L
        _state.update { it.copy(running = true, started = it.started || it.mode == TimerMode.FOCUS) }
        persist()
    }

    fun pause() {
        if (!_state.value.running) return
        val left = endsAt?.let(::secondsLeft) ?: _state.value.remainingSeconds
        endsAt = null
        if (left <= 0) {
            complete()
        } else {
            _state.update { it.copy(running = false, remainingSeconds = left) }
            persist()
        }
    }

    fun reset() {
        endsAt = null
        _state.update {
            it.copy(
                running = false,
                remainingSeconds = it.durationSeconds,
                started = false,
                completed = false,
            )
        }
        persist()
    }

    fun setMode(mode: TimerMode) {
        if (_state.value.mode == mode) return
        endsAt = null
        _state.update {
            it.copy(
                mode = mode,
                running = false,
                remainingSeconds = if (mode == TimerMode.FOCUS) FOCUS_SECONDS else BREAK_SECONDS,
                started = false,
                completed = false,
            )
        }
        persist()
    }

    private fun tick() {
        if (!_state.value.running) return
        val end = endsAt ?: return
        val left = secondsLeft(end)
        if (left <= 0) {
            complete()
            return
        }
        if (left != _state.value.remainingSeconds) {
            _state.update { it.copy(remainingSeconds = left) }
        }
    }

    private fun complete() {
        endsAt = null
        _state.update {
            it.copy(running = false, remainingSeconds = 0, completed = true)
        }
        sound.play()
        persist()
    }

    private fun secondsLeft(value: Long): Int =
        max(0, ((value - clock()) + 999) / 1000).toInt()

    private fun persist() {
        val current = _state.value
        val stored = StoredTimer(
            mode = current.mode.name,
            running = current.running,
            endsAt = endsAt,
            remaining = current.remainingSeconds,
            started = current.started,
            completed = current.completed,
        )
        runCatching { preferences.writeString(STORAGE_KEY, json.encodeToString(StoredTimer.serializer(), stored)) }
    }

    private fun restore() {
        val raw = preferences.readString(STORAGE_KEY) ?: return
        val stored = runCatching { json.decodeFromString(StoredTimer.serializer(), raw) }.getOrNull() ?: return
        val mode = runCatching { TimerMode.valueOf(stored.mode) }.getOrNull() ?: return
        val duration = if (mode == TimerMode.FOCUS) FOCUS_SECONDS else BREAK_SECONDS
        val remaining = stored.remaining.coerceIn(0, duration)
        _state.value = PomodoroTimerState(
            mode = mode,
            remainingSeconds = remaining,
            started = stored.started,
            completed = stored.completed,
        )
        if (stored.running && stored.endsAt != null) {
            endsAt = stored.endsAt
            _state.update { it.copy(running = true) }
            tick()
        }
    }

    private companion object {
        const val STORAGE_KEY = "studyflow-pomodoro"
    }
}

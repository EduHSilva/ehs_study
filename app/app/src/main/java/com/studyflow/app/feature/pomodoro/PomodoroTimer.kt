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

const val DEFAULT_FOCUS_MINUTES = 25
const val DEFAULT_BREAK_MINUTES = 5
const val DEFAULT_LONG_BREAK_MINUTES = 15
const val DEFAULT_CYCLES = 4

enum class TimerMode { FOCUS, BREAK, LONG_BREAK }

/** Mesmos limites do web: os ajustes são salvos localmente e valem a partir do próximo ciclo. */
data class PomodoroSettings(
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val cycles: Int = DEFAULT_CYCLES,
) {
    fun minutesFor(mode: TimerMode): Int = when (mode) {
        TimerMode.FOCUS -> focusMinutes
        TimerMode.BREAK -> breakMinutes
        TimerMode.LONG_BREAK -> longBreakMinutes
    }

    fun durationSeconds(mode: TimerMode): Int = minutesFor(mode) * 60

    fun sanitized(): PomodoroSettings = PomodoroSettings(
        focusMinutes = focusMinutes.coerceIn(1, 180),
        breakMinutes = breakMinutes.coerceIn(1, 60),
        longBreakMinutes = longBreakMinutes.coerceIn(1, 120),
        cycles = cycles.coerceIn(1, 12),
    )
}

data class PomodoroTimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val running: Boolean = false,
    val remainingSeconds: Int = DEFAULT_FOCUS_MINUTES * 60,
    val started: Boolean = false,
    val completed: Boolean = false,
    val focusCount: Int = 0,
    val settings: PomodoroSettings = PomodoroSettings(),
) {
    val durationSeconds: Int get() = settings.durationSeconds(mode)
    val isActive: Boolean
        get() = running || started || completed || remainingSeconds < durationSeconds
    val timeLeft: String
        get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    val elapsedMinutes: Int
        get() = if (mode != TimerMode.FOCUS) 0
        else max(1, ((durationSeconds - remainingSeconds) / 60.0).roundToInt())
    /** Contador "Sessão X de N" do ciclo atual. */
    val sessionNumber: Int get() = (focusCount % settings.cycles) + 1
}

@Serializable
private data class StoredTimer(
    val mode: String = TimerMode.FOCUS.name,
    val running: Boolean = false,
    val endsAt: Long? = null,
    val remaining: Int = DEFAULT_FOCUS_MINUTES * 60,
    val started: Boolean = false,
    val completed: Boolean = false,
    val focusCount: Int = 0,
)

@Serializable
private data class StoredSettings(
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val cycles: Int = DEFAULT_CYCLES,
)

/**
 * Cronômetro Pomodoro compartilhado pelo app inteiro (como o composable global do web):
 * sobrevive à navegação e ao processo via `SharedPreferences` nas chaves `studyflow-pomodoro`
 * e `studyflow-pomodoro-settings`.
 */
class PomodoroTimer(
    private val preferences: AppPreferences,
    private val sound: PomodoroSound = PomodoroChime,
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(PomodoroTimerState(settings = restoreSettings()))
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
                remainingSeconds = it.settings.durationSeconds(mode),
                started = false,
                completed = false,
            )
        }
        persist()
    }

    // Os ajustes valem na hora: se o ciclo ainda não começou, o tempo em tela já reflete o novo
    // valor; se estiver rodando, a mudança entra no próximo ciclo.
    fun updateSettings(next: PomodoroSettings) {
        val settings = next.sanitized()
        _state.update {
            val updated = it.copy(settings = settings)
            if (it.running || it.started) {
                updated
            } else {
                updated.copy(
                    remainingSeconds = settings.durationSeconds(it.mode),
                    completed = false,
                )
            }
        }
        persistSettings()
        persist()
    }

    // Fecha um bloco de foco: a cada `cycles` focos o próximo intervalo vira descanso longo.
    fun advanceAfterFocus() {
        val current = _state.value
        setMode(if (current.focusCount % current.settings.cycles == 0) TimerMode.LONG_BREAK else TimerMode.BREAK)
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
            it.copy(
                running = false,
                remainingSeconds = 0,
                completed = true,
                focusCount = if (it.mode == TimerMode.FOCUS) it.focusCount + 1 else it.focusCount,
            )
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
            focusCount = current.focusCount,
        )
        runCatching { preferences.writeString(STORAGE_KEY, json.encodeToString(StoredTimer.serializer(), stored)) }
    }

    private fun persistSettings() {
        val settings = _state.value.settings
        val stored = StoredSettings(
            focusMinutes = settings.focusMinutes,
            breakMinutes = settings.breakMinutes,
            longBreakMinutes = settings.longBreakMinutes,
            cycles = settings.cycles,
        )
        runCatching {
            preferences.writeString(SETTINGS_KEY, json.encodeToString(StoredSettings.serializer(), stored))
        }
    }

    private fun restoreSettings(): PomodoroSettings {
        val raw = preferences.readString(SETTINGS_KEY) ?: return PomodoroSettings()
        val stored = runCatching { json.decodeFromString(StoredSettings.serializer(), raw) }.getOrNull()
            ?: return PomodoroSettings()
        return PomodoroSettings(
            focusMinutes = stored.focusMinutes,
            breakMinutes = stored.breakMinutes,
            longBreakMinutes = stored.longBreakMinutes,
            cycles = stored.cycles,
        ).sanitized()
    }

    private fun restore() {
        val raw = preferences.readString(STORAGE_KEY) ?: return
        val stored = runCatching { json.decodeFromString(StoredTimer.serializer(), raw) }.getOrNull() ?: return
        val mode = runCatching { TimerMode.valueOf(stored.mode) }.getOrNull() ?: return
        val duration = _state.value.settings.durationSeconds(mode)
        val remaining = stored.remaining.coerceIn(0, duration)
        _state.update {
            it.copy(
                mode = mode,
                remainingSeconds = remaining,
                started = stored.started,
                completed = stored.completed,
                focusCount = stored.focusCount.coerceAtLeast(0),
            )
        }
        if (stored.running && stored.endsAt != null) {
            endsAt = stored.endsAt
            _state.update { it.copy(running = true) }
            tick()
        }
    }

    private companion object {
        const val STORAGE_KEY = "studyflow-pomodoro"
        const val SETTINGS_KEY = "studyflow-pomodoro-settings"
    }
}

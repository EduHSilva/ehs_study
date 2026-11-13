import { computed, ref } from 'vue'
import { playChime, primeAudio } from './pomodoroSound'

export type TimerMode = 'focus' | 'break' | 'longBreak'

export interface PomodoroSettings {
  focusMinutes: number
  breakMinutes: number
  longBreakMinutes: number
  cycles: number
}

export const DEFAULT_SETTINGS: PomodoroSettings = {
  focusMinutes: 25,
  breakMinutes: 5,
  longBreakMinutes: 15,
  cycles: 4
}

interface StoredTimer {
  mode: TimerMode
  running: boolean
  endsAt: number | null
  remaining: number
  started: boolean
  completed: boolean
  focusCount: number
}

const STORAGE_KEY = 'studyflow-pomodoro'
const SETTINGS_KEY = 'studyflow-pomodoro-settings'

const settings = ref<PomodoroSettings>({ ...DEFAULT_SETTINGS })

const clamp = (value: number, min: number, max: number, fallback: number) =>
  Number.isFinite(value) ? Math.min(max, Math.max(min, Math.round(value))) : fallback

const durationFor = (value: TimerMode) => {
  const minutes = value === 'focus'
    ? settings.value.focusMinutes
    : value === 'longBreak' ? settings.value.longBreakMinutes : settings.value.breakMinutes
  return minutes * 60
}

const mode = ref<TimerMode>('focus')
const running = ref(false)
const endsAt = ref<number | null>(null)
const remaining = ref(durationFor('focus'))
const started = ref(false)
const completed = ref(false)
const focusCount = ref(0)
let ticker: number | undefined

const snapshot = (): StoredTimer => ({
  mode: mode.value,
  running: running.value,
  endsAt: endsAt.value,
  remaining: remaining.value,
  started: started.value,
  completed: completed.value,
  focusCount: focusCount.value
})

function persist(): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot()))
  } catch {
    // Armazenamento indisponível (modo privado): o cronômetro segue funcionando só na memória.
  }
}

function persistSettings(): void {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings.value))
  } catch {
    // Idem: sem localStorage os ajustes valem só para esta sessão.
  }
}

function restoreSettings(): void {
  let stored: unknown
  try {
    stored = JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? 'null')
  } catch {
    return
  }
  if (!stored || typeof stored !== 'object') return
  const data = stored as Partial<PomodoroSettings>
  settings.value = {
    focusMinutes: clamp(Number(data.focusMinutes), 1, 180, DEFAULT_SETTINGS.focusMinutes),
    breakMinutes: clamp(Number(data.breakMinutes), 1, 60, DEFAULT_SETTINGS.breakMinutes),
    longBreakMinutes: clamp(Number(data.longBreakMinutes), 1, 120, DEFAULT_SETTINGS.longBreakMinutes),
    cycles: clamp(Number(data.cycles), 1, 12, DEFAULT_SETTINGS.cycles)
  }
}

function secondsLeft(value: number): number {
  return Math.max(0, Math.ceil((value - Date.now()) / 1000))
}

function restore(): void {
  let stored: unknown
  try {
    stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
  } catch {
    return
  }
  if (!stored || typeof stored !== 'object') return
  const data = stored as Partial<StoredTimer>
  if (data.mode !== 'focus' && data.mode !== 'break' && data.mode !== 'longBreak') return
  const savedMode = data.mode
  const savedRemaining = typeof data.remaining === 'number' && Number.isFinite(data.remaining)
    ? Math.min(durationFor(savedMode), Math.max(0, Math.round(data.remaining)))
    : durationFor(savedMode)
  mode.value = savedMode
  remaining.value = savedRemaining
  started.value = data.started === true
  completed.value = data.completed === true
  focusCount.value = typeof data.focusCount === 'number' && Number.isFinite(data.focusCount)
    ? Math.max(0, Math.round(data.focusCount))
    : 0
  if (data.running === true && typeof data.endsAt === 'number' && Number.isFinite(data.endsAt)) {
    endsAt.value = data.endsAt
    running.value = true
    ensureTicker()
    tick()
  }
}

function stopTicker(): void {
  if (ticker !== undefined) {
    window.clearInterval(ticker)
    ticker = undefined
  }
}

function ensureTicker(): void {
  if (ticker === undefined) ticker = window.setInterval(tick, 1000)
}

function complete(): void {
  running.value = false
  endsAt.value = null
  remaining.value = 0
  completed.value = true
  if (mode.value === 'focus') focusCount.value += 1
  stopTicker()
  playChime()
  persist()
}

function tick(): void {
  if (!running.value || endsAt.value === null) {
    stopTicker()
    return
  }
  const left = secondsLeft(endsAt.value)
  remaining.value = left
  if (left <= 0) complete()
}

function start(): void {
  if (running.value || completed.value || remaining.value <= 0) return
  if (mode.value === 'focus') started.value = true
  primeAudio()
  endsAt.value = Date.now() + remaining.value * 1000
  running.value = true
  ensureTicker()
  persist()
}

function pause(): void {
  if (!running.value) return
  if (endsAt.value !== null) remaining.value = secondsLeft(endsAt.value)
  running.value = false
  endsAt.value = null
  stopTicker()
  if (remaining.value <= 0) complete()
  else persist()
}

function toggle(): void {
  if (running.value) pause()
  else start()
}

function reset(): void {
  running.value = false
  endsAt.value = null
  remaining.value = durationFor(mode.value)
  started.value = false
  completed.value = false
  stopTicker()
  persist()
}

function setMode(next: TimerMode): void {
  if (next === mode.value) return
  mode.value = next
  reset()
}

// Os ajustes valem na hora: se o cronômetro ainda não começou, o tempo em tela já reflete o novo
// valor; se estiver rodando, a mudança só entra no próximo ciclo.
function updateSettings(next: Partial<PomodoroSettings>): void {
  settings.value = {
    focusMinutes: clamp(next.focusMinutes ?? settings.value.focusMinutes, 1, 180, DEFAULT_SETTINGS.focusMinutes),
    breakMinutes: clamp(next.breakMinutes ?? settings.value.breakMinutes, 1, 60, DEFAULT_SETTINGS.breakMinutes),
    longBreakMinutes: clamp(next.longBreakMinutes ?? settings.value.longBreakMinutes, 1, 120, DEFAULT_SETTINGS.longBreakMinutes),
    cycles: clamp(next.cycles ?? settings.value.cycles, 1, 12, DEFAULT_SETTINGS.cycles)
  }
  persistSettings()
  if (!running.value && !started.value) {
    remaining.value = durationFor(mode.value)
    completed.value = false
  }
  persist()
}

// Fecha um bloco de foco: a cada `cycles` focos o próximo intervalo vira descanso longo.
function advanceAfterFocus(): void {
  mode.value = focusCount.value % settings.value.cycles === 0 ? 'longBreak' : 'break'
  reset()
}

const duration = computed(() => durationFor(mode.value))

const isActive = computed(
  () => running.value || started.value || completed.value || remaining.value < durationFor(mode.value)
)

const elapsedMinutes = computed(() => {
  if (mode.value !== 'focus') return 0
  return Math.max(1, Math.round((durationFor('focus') - remaining.value) / 60))
})

const timeLeft = computed(
  () => `${String(Math.floor(remaining.value / 60)).padStart(2, '0')}:${String(remaining.value % 60).padStart(2, '0')}`
)

if (typeof window !== 'undefined') {
  restoreSettings()
  restore()
}

export function usePomodoroTimer() {
  return {
    mode,
    running,
    remaining,
    started,
    completed,
    focusCount,
    settings,
    duration,
    isActive,
    elapsedMinutes,
    timeLeft,
    start,
    pause,
    toggle,
    reset,
    setMode,
    updateSettings,
    advanceAfterFocus
  }
}

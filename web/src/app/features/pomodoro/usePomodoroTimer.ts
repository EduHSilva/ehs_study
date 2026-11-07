import { computed, ref } from 'vue'
import { playChime, primeAudio } from './pomodoroSound'

export const FOCUS_SECONDS = 25 * 60
export const BREAK_SECONDS = 5 * 60

export type TimerMode = 'focus' | 'break'

interface StoredTimer {
  mode: TimerMode
  running: boolean
  endsAt: number | null
  remaining: number
  started: boolean
  completed: boolean
}

const STORAGE_KEY = 'studyflow-pomodoro'

const durationFor = (mode: TimerMode) => (mode === 'focus' ? FOCUS_SECONDS : BREAK_SECONDS)

const mode = ref<TimerMode>('focus')
const running = ref(false)
const endsAt = ref<number | null>(null)
const remaining = ref(FOCUS_SECONDS)
const started = ref(false)
const completed = ref(false)
let ticker: number | undefined

const snapshot = (): StoredTimer => ({
  mode: mode.value,
  running: running.value,
  endsAt: endsAt.value,
  remaining: remaining.value,
  started: started.value,
  completed: completed.value
})

function persist(): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot()))
  } catch {
    // Armazenamento indisponível (modo privado): o cronômetro segue funcionando só na memória.
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
  if (data.mode !== 'focus' && data.mode !== 'break') return
  const savedMode = data.mode
  const savedRemaining = typeof data.remaining === 'number' && Number.isFinite(data.remaining)
    ? Math.min(durationFor(savedMode), Math.max(0, Math.round(data.remaining)))
    : durationFor(savedMode)
  mode.value = savedMode
  remaining.value = savedRemaining
  started.value = data.started === true
  completed.value = data.completed === true
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

const isActive = computed(
  () => running.value || started.value || completed.value || remaining.value < durationFor(mode.value)
)

const elapsedMinutes = computed(() => {
  if (mode.value !== 'focus') return 0
  return Math.max(1, Math.round((FOCUS_SECONDS - remaining.value) / 60))
})

const timeLeft = computed(
  () => `${String(Math.floor(remaining.value / 60)).padStart(2, '0')}:${String(remaining.value % 60).padStart(2, '0')}`
)

if (typeof window !== 'undefined') restore()

export function usePomodoroTimer() {
  return {
    mode,
    running,
    remaining,
    started,
    completed,
    isActive,
    elapsedMinutes,
    timeLeft,
    start,
    pause,
    toggle,
    reset,
    setMode
  }
}

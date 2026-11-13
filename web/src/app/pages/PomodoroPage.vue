<script setup lang="ts">
import { computed, onMounted } from 'vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { usePomodoro } from '../features/pomodoro/usePomodoro'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'

defineEmits<{ navigate: [page: AppPage] }>()
const { t, locale } = useI18n()
const { sessions, loading, saving, error, load, registerSession } = usePomodoro()
const { mode, running, remaining, started, completed, focusCount, settings, duration, elapsedMinutes, toggle, reset, setMode, updateSettings, advanceAfterFocus } = usePomodoroTimer()
onMounted(load)

const sessionNumber = computed(() => (focusCount.value % settings.value.cycles) + 1)
const time = computed(
  () => `${String(Math.floor(remaining.value / 60)).padStart(2, '0')}:${String(remaining.value % 60).padStart(2, '0')}`
)
const ringProgress = computed(() => Math.round(((duration.value - remaining.value) / duration.value) * 100))

const finishSession = async () => {
  if (mode.value !== 'focus' || !started.value) return
  if (await registerSession(t('focusSessionTitle'), elapsedMinutes.value)) advanceAfterFocus()
}

const completionText = computed(
  () => t(mode.value === 'focus' ? 'focusComplete' : mode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete')
)
const completionHint = computed(
  () => t(mode.value === 'focus' ? 'registerFocusHint' : mode.value === 'longBreak' ? 'breakCompleteHint' : 'breakCompleteHint')
)

const inputNumber = (event: Event) => Number((event.target as HTMLInputElement).value)
const setFocusMinutes = (event: Event) => updateSettings({ focusMinutes: inputNumber(event) })
const setBreakMinutes = (event: Event) => updateSettings({ breakMinutes: inputNumber(event) })
const setLongBreakMinutes = (event: Event) => updateSettings({ longBreakMinutes: inputNumber(event) })
const setCycles = (event: Event) => updateSettings({ cycles: inputNumber(event) })

const timeFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }))
const sessionTime = (value: string | null) => {
  if (!value) return t('sessionCompleted')
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? t('sessionCompleted') : t('sessionCompletedAt', { time: timeFormatter.value.format(date) })
}
</script>
<template>
  <section class="content"><header class="page-header"><div><h1>{{ t('timerTitle') }}</h1><p>{{ t('timerSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <div class="pomodoro-layout">
      <article class="card timer-card" :class="{ finished: completed }"><div class="mode-switch"><button :class="{selected:mode==='focus'}" @click="setMode('focus')">{{ t('focusMode') }}</button><button :class="{selected:mode==='break'}" @click="setMode('break')">{{ t('breakMode') }}</button><button :class="{selected:mode==='longBreak'}" @click="setMode('longBreak')">{{ t('longBreakMode') }}</button></div><div class="timer-ring" :style="{ '--timer-progress': `${ringProgress}%` }"><div><strong>{{ time }}</strong><small>{{ t('sessionCounter', { current: sessionNumber, total: settings.cycles }) }}</small></div></div>
        <p v-if="completed" class="timer-complete" role="status">{{ completionText }} <span>{{ completionHint }}</span></p>
        <div class="timer-actions"><button class="button secondary" @click="reset">{{ t('reset') }}</button><button v-if="completed && mode !== 'focus'" class="button" @click="setMode('focus')">{{ t('backToFocus') }}</button><button v-if="!completed" class="button" @click="toggle">{{ running ? t('pause') : t('start') }}</button><button v-if="started && mode === 'focus'" class="button finish-session" :disabled="saving" @click="finishSession">{{ saving ? t('saving') : t('finishSession') }}</button></div></article>
      <div class="right-column">
        <article class="card sessions"><h2>{{ t('sessions') }}</h2>
          <template v-if="loading">
            <span class="sr-only">{{ t('pomodoroLoading') }}</span>
            <span v-for="n in 3" :key="n" class="skeleton skeleton-row-line" />
          </template>
          <p v-else-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
          <p v-else-if="!sessions.length" class="empty-note">{{ t('noSessions') }}</p>
          <div v-for="session in sessions" :key="session.id" class="session"><span>✓</span><div><strong>{{ session.title }}</strong><small>{{ sessionTime(session.completedAt) }}</small></div><em>{{ session.minutes }} min</em></div>
        </article>
        <article class="card pomodoro-settings"><h2>{{ t('pomodoroSettingsTitle') }}</h2>
          <div class="settings-grid">
            <label class="field"><span>{{ t('pomodoroFocusMinutes') }}</span><input type="number" min="1" max="180" :value="settings.focusMinutes" @change="setFocusMinutes" /></label>
            <label class="field"><span>{{ t('pomodoroBreakMinutes') }}</span><input type="number" min="1" max="60" :value="settings.breakMinutes" @change="setBreakMinutes" /></label>
            <label class="field"><span>{{ t('pomodoroLongBreakMinutes') }}</span><input type="number" min="1" max="120" :value="settings.longBreakMinutes" @change="setLongBreakMinutes" /></label>
            <label class="field"><span>{{ t('pomodoroCycles') }}</span><input type="number" min="1" max="12" :value="settings.cycles" @change="setCycles" /></label>
          </div>
          <p class="settings-hint">{{ t('pomodoroSettingsHint') }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

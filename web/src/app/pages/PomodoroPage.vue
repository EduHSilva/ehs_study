<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { usePomodoro } from '../features/pomodoro/usePomodoro'
import { BREAK_SECONDS, FOCUS_SECONDS, usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'

defineEmits<{ navigate: [page: AppPage] }>()
const { t, locale } = useI18n()
const { student } = useStudentProfile()
const { sessions, loading, saving, error, load, registerSession } = usePomodoro()
const { mode, running, remaining, started, completed, elapsedMinutes, toggle, reset, setMode } = usePomodoroTimer()
onMounted(load)

const sessionNumber = computed(() => (sessions.value.length % 4) + 1)
const time = computed(
  () => `${String(Math.floor(remaining.value / 60)).padStart(2, '0')}:${String(remaining.value % 60).padStart(2, '0')}`
)
const duration = computed(() => (mode.value === 'focus' ? FOCUS_SECONDS : BREAK_SECONDS))
const ringProgress = computed(() => Math.round(((duration.value - remaining.value) / duration.value) * 100))

const finishSession = async () => {
  if (mode.value !== 'focus' || !started.value) return
  if (await registerSession(t('focusSessionTitle'), elapsedMinutes.value)) reset()
}

const timeFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }))
const sessionTime = (value: string | null) => {
  if (!value) return t('sessionCompleted')
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? t('sessionCompleted') : t('sessionCompletedAt', { time: timeFormatter.value.format(date) })
}
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="pomodoro" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('timerTitle') }}</h1><p>{{ t('timerSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <div class="pomodoro-layout">
      <article class="card timer-card" :class="{ finished: completed }"><div class="mode-switch"><button :class="{selected:mode==='focus'}" @click="setMode('focus')">{{ t('focusMode') }}</button><button :class="{selected:mode==='break'}" @click="setMode('break')">{{ t('breakMode') }}</button></div><div class="timer-ring" :style="{ '--timer-progress': `${ringProgress}%` }"><div><strong>{{ time }}</strong><small>{{ t('sessionCounter', { current: sessionNumber, total: 4 }) }}</small></div></div>
        <p v-if="completed" class="timer-complete" role="status">{{ mode === 'focus' ? t('focusComplete') : t('breakComplete') }} <span>{{ mode === 'focus' ? t('registerFocusHint') : t('breakCompleteHint') }}</span></p>
        <div class="timer-actions"><button class="button secondary" @click="reset">{{ t('reset') }}</button><button v-if="!completed" class="button" @click="toggle">{{ running ? t('pause') : t('start') }}</button><button v-if="started && mode === 'focus'" class="button finish-session" :disabled="saving" @click="finishSession">{{ saving ? t('saving') : t('finishSession') }}</button></div></article>
      <article class="card sessions"><h2>{{ t('sessions') }}</h2>
        <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
        <p v-else-if="!sessions.length" class="empty-note">{{ t('noSessions') }}</p>
        <div v-for="session in sessions" :key="session.id" class="session"><span>✓</span><div><strong>{{ session.title }}</strong><small>{{ sessionTime(session.completedAt) }}</small></div><em>{{ session.minutes }} min</em></div>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('pomodoroLoading') }}</main>
</template>

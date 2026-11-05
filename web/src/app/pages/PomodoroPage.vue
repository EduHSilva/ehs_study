<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { usePomodoro } from '../features/pomodoro/usePomodoro'

defineEmits<{ navigate: [page: AppPage] }>()
const { t, locale } = useI18n()
const { student } = useStudentProfile()
const { sessions, loading, saving, error, load, registerSession } = usePomodoro()
onMounted(load)

const FOCUS_SECONDS = 25 * 60
const BREAK_SECONDS = 5 * 60
const running = ref(false); const breakMode = ref(false); const seconds = ref(FOCUS_SECONDS)
const focusStarted = ref(false)
let timer: number | undefined

const time = () => `${String(Math.floor(seconds.value / 60)).padStart(2,'0')}:${String(seconds.value % 60).padStart(2,'0')}`
const sessionNumber = computed(() => (sessions.value.length % 4) + 1)
const reset = () => { running.value=false; focusStarted.value=false; seconds.value = breakMode.value ? BREAK_SECONDS : FOCUS_SECONDS }
const setMode = (isBreak: boolean) => { breakMode.value=isBreak; reset() }
const toggleTimer = () => {
  if (running.value) {
    running.value = false
    return
  }
  if (!breakMode.value) focusStarted.value = true
  running.value = true
}
const finishSession = async () => {
  if (!focusStarted.value || breakMode.value) return
  const elapsedSeconds = FOCUS_SECONDS - seconds.value
  if (elapsedSeconds <= 0) return
  running.value = false
  const minutes = Math.max(1, Math.round(elapsedSeconds / 60))
  if (await registerSession(t('focusSessionTitle'), minutes)) {
    focusStarted.value = false
    seconds.value = FOCUS_SECONDS
  }
}

timer = window.setInterval(() => {
  if (!running.value || seconds.value === 0) return
  seconds.value -= 1
  if (seconds.value > 0) return
  running.value = false
  // O cronômetro é controlado no cliente; só Finalizar sessão persiste a sessão de foco.
}, 1000)
onBeforeUnmount(() => window.clearInterval(timer))

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
      <article class="card timer-card"><div class="mode-switch"><button :class="{selected:!breakMode}" @click="setMode(false)">{{ t('focusMode') }}</button><button :class="{selected:breakMode}" @click="setMode(true)">{{ t('breakMode') }}</button></div><div class="timer-ring"><div><strong>{{ time() }}</strong><small>{{ t('sessionCounter', { current: sessionNumber, total: 4 }) }}</small></div></div><div class="timer-actions"><button class="button secondary" @click="reset">{{ t('reset') }}</button><button class="button" @click="toggleTimer">{{ running ? t('pause') : t('start') }}</button><button v-if="focusStarted && !breakMode" class="button finish-session" :disabled="saving" @click="finishSession">{{ saving ? t('saving') : t('finishSession') }}</button></div></article>
      <article class="card sessions"><h2>{{ t('sessions') }}</h2>
        <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
        <p v-else-if="!sessions.length" class="empty-note">{{ t('noSessions') }}</p>
        <div v-for="session in sessions" :key="session.id" class="session"><span>✓</span><div><strong>{{ session.title }}</strong><small>{{ sessionTime(session.completedAt) }}</small></div><em>{{ session.minutes }} min</em></div>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('pomodoroLoading') }}</main>
</template>

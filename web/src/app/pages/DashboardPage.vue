<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'

defineEmits<{ navigate: [page: AppPage] }>()
import { useStudentProfile } from '../features/auth/useAuth'
import { useDashboard } from '../features/dashboard/useDashboard'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'
import { formatEndDate } from '../features/tasks/taskFormat'

const { t, locale } = useI18n()
const { student } = useStudentProfile()
const firstName = computed(() => student.value.name.split(' ')[0])
const { data, focus, loading, error, progress, savingTaskId, load, toggleTask } = useDashboard()
const {
  mode: timerMode,
  running: timerRunning,
  completed: timerCompleted,
  started: timerStarted,
  isActive: timerActive,
  timeLeft: timerTimeLeft
} = usePomodoroTimer()
onMounted(load)

const timeFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }))
const focusTime = computed(() => {
  if (!focus.value?.completedAt) return '—'
  const date = new Date(focus.value.completedAt)
  return Number.isNaN(date.getTime()) ? '—' : timeFormatter.value.format(date)
})
</script>

<template>
  <main v-if="data" class="app-shell">
    <AppSidebar v-bind="student" active="dashboard" @navigate="$emit('navigate', $event)" />
    <section class="content"><header class="page-header"><div><h1>{{ t('greeting', { name: firstName }) }}</h1><p>{{ t('dashboardWelcome') }}</p></div><div class="header-actions"><LanguageToggle /><ThemeToggle /></div></header>
      <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
      <div class="dashboard-grid">
        <div class="left-column">
          <article class="card progress-card"><h2>{{ t('weeklyProgressTitle') }}</h2><div class="progress-content"><div class="progress-ring" :style="{ '--progress': `${progress * 3.6}deg` }"><strong>{{ progress }}%</strong></div><div><p>{{ t('weeklyProgressBefore') }} <b>{{ data.weeklyProgress.completed }} {{ t('of') }} {{ data.weeklyProgress.total }}</b> {{ t('weeklyProgressAfter') }}</p><small>{{ t('weeklyProgressCheer') }}</small></div></div></article>
          <article class="card focus-card"><div class="card-title"><h2>{{ t('focusTitle') }}</h2><span v-if="timerActive" class="tag daily">{{ t('timerActiveTag') }}</span><span v-else class="tag">{{ t('focusTag') }}</span></div>
            <template v-if="timerActive">
              <div class="focus-row"><strong class="focus-time">{{ timerTimeLeft }}</strong><div class="focus-meta"><span>{{ timerMode === 'focus' ? t('focusModeShort') : t('breakModeShort') }}</span><small>{{ timerCompleted ? t(timerMode === 'focus' ? 'focusComplete' : 'breakComplete') : timerRunning ? t('timerRunning') : t('timerPaused') }}</small></div></div>
              <p v-if="timerStarted && timerMode === 'focus'" class="timer-hint">{{ t('registerFocusHint') }}</p>
              <button class="link" @click="$emit('navigate','pomodoro')">{{ t('goToTimer') }}</button>
            </template>
            <template v-else><div class="focus-row"><strong class="focus-time">{{ focusTime }}</strong><div class="focus-meta"><span v-if="focus">{{ focus.title }}</span><small>{{ t('focusMinutes', { minutes: focus?.minutes ?? 0 }) }}</small></div></div><p v-if="!focus" class="empty-note">{{ t('noFocusToday') }}</p><button class="link" @click="$emit('navigate','pomodoro')">{{ t('goToTimer') }}</button></template>
          </article>
        </div>
        <div class="right-column">
          <article class="card"><div class="card-title"><h2>{{ t('todayTasks') }}</h2><button class="link" @click="$emit('navigate','tasks')">{{ t('seeAll') }}</button></div><div class="task-list"><button v-for="item in data.tasks" :key="item.id" class="task-row" :disabled="savingTaskId === item.id" @click="toggleTask(item.id)"><span class="checkbox" :class="{ checked: item.status === 'DONE' }">✓</span><span class="task-info"><strong :class="{ done: item.status === 'DONE' }">{{ item.title }}</strong><small>{{ t('endDateLabel') }} {{ formatEndDate(item.endDate) }}</small></span><span v-if="item.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span><span v-if="item.status === 'DELAYED'" class="tag">{{ t('taskDelayed') }}</span></button></div></article>
          <article class="card notes"><h2>{{ t('recentNotes') }}</h2><div class="notes-grid"><article v-for="note in data.notes" :key="note.id" class="note"><strong>{{ note.title }}</strong><small>{{ note.content }}</small></article></div></article>
        </div>
      </div>
    </section>
  </main>
  <main v-else class="loading">{{ loading ? t('dashboardLoading') : t('dashboardError') }}</main>
</template>

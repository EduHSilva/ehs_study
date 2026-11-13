<script setup lang="ts">
import { computed, onMounted } from 'vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'

const emit = defineEmits<{ navigate: [page: AppPage] }>()
import { useStudentProfile } from '../features/auth/useAuth'
import { useDashboard } from '../features/dashboard/useDashboard'
import { requestOpenNote } from '../features/notes/useNotes'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'
import { formatEndDate } from '../features/tasks/taskFormat'

const { t, locale } = useI18n()
const { student } = useStudentProfile()
const firstName = computed(() => student.value.name.split(' ')[0])
const { data, focus, tasks, loading, error, progress, savingTaskId, load, toggleTask } = useDashboard()
const openNote = (id: string) => {
  requestOpenNote(id)
  emit('navigate', 'notes')
}
const {
  mode: timerMode,
  running: timerRunning,
  completed: timerCompleted,
  started: timerStarted,
  isActive: timerActive,
  timeLeft: timerTimeLeft
} = usePomodoroTimer()
onMounted(load)

const timerModeLabel = computed(() => t(timerMode.value === 'focus' ? 'focusModeShort' : timerMode.value === 'longBreak' ? 'longBreakModeShort' : 'breakModeShort'))
const timerCompletionLabel = computed(() => t(timerMode.value === 'focus' ? 'focusComplete' : timerMode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete'))

const timeFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }))
const focusTime = computed(() => {
  if (!focus.value?.completedAt) return '—'
  const date = new Date(focus.value.completedAt)
  return Number.isNaN(date.getTime()) ? '—' : timeFormatter.value.format(date)
})
</script>

<template>
  <section class="content">
    <header class="page-header"><div><h1>{{ t('greeting', { name: firstName }) }}</h1><p>{{ t('dashboardWelcome') }}</p></div><div class="header-actions"><LanguageToggle /><ThemeToggle /></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <PageSkeleton v-if="loading" layout="dashboard" :label="t('dashboardLoading')" />
    <div v-else-if="data" class="dashboard-grid">
        <div class="left-column">
          <article class="card progress-card"><h2>{{ t('weeklyProgressTitle') }}</h2><div class="progress-content"><div class="progress-ring" :style="{ '--progress': `${progress * 3.6}deg` }"><strong>{{ progress }}%</strong></div><div><p>{{ t('weeklyProgressBefore') }} <b>{{ data.weeklyProgress.completed }} {{ t('of') }} {{ data.weeklyProgress.total }}</b> {{ t('weeklyProgressAfter') }}</p><small>{{ t('weeklyProgressCheer') }}</small></div></div></article>
          <article class="card focus-card"><div class="card-title"><h2>{{ t('focusTitle') }}</h2><span v-if="timerActive" class="tag daily">{{ t('timerActiveTag') }}</span><span v-else class="tag">{{ t('focusTag') }}</span></div>
            <template v-if="timerActive">
              <div class="focus-row"><strong class="focus-time">{{ timerTimeLeft }}</strong><div class="focus-meta"><span>{{ timerModeLabel }}</span><small>{{ timerCompleted ? timerCompletionLabel : timerRunning ? t('timerRunning') : t('timerPaused') }}</small></div></div>
              <p v-if="timerStarted && timerMode === 'focus'" class="timer-hint">{{ t('registerFocusHint') }}</p>
              <button class="link" @click="$emit('navigate','pomodoro')">{{ t('goToTimer') }}</button>
            </template>
            <template v-else><div class="focus-row"><strong class="focus-time">{{ focusTime }}</strong><div class="focus-meta"><span v-if="focus">{{ focus.title }}</span><small>{{ t('focusMinutes', { minutes: focus?.minutes ?? 0 }) }}</small></div></div><p v-if="!focus" class="empty-note">{{ t('noFocusToday') }}</p><button class="link" @click="$emit('navigate','pomodoro')">{{ t('goToTimer') }}</button></template>
          </article>
        </div>
        <div class="right-column">
          <article class="card"><div class="card-title"><h2>{{ t('todayTasks') }}</h2><button class="link" @click="$emit('navigate','tasks')">{{ t('seeAll') }}</button></div><div class="task-list"><button v-for="item in tasks" :key="item.id" class="task-row" :disabled="savingTaskId === item.id" @click="toggleTask(item.id)"><span class="checkbox" :class="{ checked: item.status === 'DONE' }">✓</span><span class="task-info"><strong :class="{ done: item.status === 'DONE' }">{{ item.title }}</strong><small>{{ t('endDateLabel') }} {{ formatEndDate(item.endDate) }}</small></span><span v-if="item.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span><span v-if="item.status === 'DELAYED'" class="tag">{{ t('taskDelayed') }}</span></button></div></article>
          <article class="card notes"><h2>{{ t('recentNotes') }}</h2><div class="notes-list"><button v-for="note in data.notes" :key="note.id" type="button" class="note" @click="openNote(note.id)"><strong>{{ note.title }}</strong></button></div></article>
        </div>
      </div>
    <p v-else class="empty-note">{{ t('dashboardError') }}</p>
  </section>
</template>

<style scoped>
.note{width:100%;text-align:left;color:var(--color-text);font:inherit;cursor:pointer}
.note:hover{border-color:var(--color-border-strong)}
</style>

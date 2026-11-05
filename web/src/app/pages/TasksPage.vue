<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useTasks } from '../features/tasks/useTasks'
import { formatTaskPeriod } from '../features/tasks/taskFormat'
import { parseDayKey } from '../features/tasks/calendar'
import type { Task } from '../features/tasks/types'

defineEmits<{ navigate: [page: AppPage] }>()
const { t, locale } = useI18n()
const { student } = useStudentProfile()
const { items, loading, pageLoading, calendarLoading, page, total, lastPage, canGoPrev, canGoNext, error, savingId, deletingId, pendingCount, dailyCount, anchor, mode, calendar, selectedDay, formOpen, title, startDate, endDate, type, creating, formError, todayKey, isDone, load, goToPage, navigate, jumpTo, setMode, toggle, remove, toggleForm, create } = useTasks()
onMounted(load)

const WEEKDAYS = computed(() => {
  const fmt = new Intl.DateTimeFormat(locale.value, { weekday: 'narrow' })
  return Array.from({ length: 7 }, (_, i) => fmt.format(new Date(2024, 0, 1 + i)))
})

const periodLabel = computed(() => {
  if (mode.value === 'week') {
    const fmt = new Intl.DateTimeFormat(locale.value, { day: '2-digit', month: 'short' })
    return fmt.format(calendar.value[0]?.date ?? anchor.value)
  }
  return new Intl.DateTimeFormat(locale.value, { month: 'long', year: 'numeric' }).format(anchor.value)
})

const statusLabel = (status: string) => t(`dayStatus_${status}`)
const isToday = (key: string) => key === todayKey.value
const selectedDateLabel = computed(() => (selectedDay.value ? formatTaskPeriod(parseDayKey(selectedDay.value).toISOString(), null) : ''))
const clearDay = () => { if (selectedDay.value) void jumpTo(selectedDay.value) }
// O dia que a linha representa — e que o checkbox vai marcar. Sem dia selecionado a `UNIQUE` cobre
// um intervalo, então a data fica por conta da linha de período; a `DAILY` cai em hoje.
const rowDay = (item: Task): string => (selectedDay.value ?? (item.type === 'DAILY' ? todayKey.value : ''))
const dayFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { weekday: 'short', day: '2-digit', month: '2-digit' }))
const rowDayLabel = (item: Task): string => {
  const key = rowDay(item)
  return key ? dayFormatter.value.format(parseDayKey(key)) : ''
}
// A lista (`GET /tasks`) traz `dailyCompletions` e deixa `dailyStatistics` nulo; o detalhe é o
// contrário. Então o resumo de dias concluídos sai da lista de dias, que é o que a tela tem.
const dailyStats = (item: { dailyCompletions: unknown[] }) => {
  const count = item.dailyCompletions?.length ?? 0
  return count ? t('taskDailyDoneCount', { count }) : ''
}
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="tasks" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('tasks') }}</h1><p>{{ t('tasksSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>

    <article class="card calendar-card" :aria-busy="calendarLoading">
      <div class="calendar-head">
        <div class="calendar-nav">
          <button class="round" :disabled="calendarLoading" :aria-label="t('calendarPrev')" @click="navigate(-1)">‹</button>
          <strong>{{ periodLabel }}</strong>
          <button class="round" :disabled="calendarLoading" :aria-label="t('calendarNext')" @click="navigate(1)">›</button>
        </div>
        <div class="mode-switch calendar-mode">
          <button :class="{ selected: mode === 'week' }" @click="setMode('week')">{{ t('calendarWeek') }}</button>
          <button :class="{ selected: mode === 'month' }" @click="setMode('month')">{{ t('calendarMonth') }}</button>
        </div>
      </div>
      <div class="calendar-grid" :class="{ month: mode === 'month' }">
        <span v-for="(label, i) in WEEKDAYS" :key="i" class="calendar-weekday">{{ label }}</span>
        <button v-for="day in calendar" :key="day.key" class="calendar-day" :class="[day.status, { outside: day.outside, today: isToday(day.key), selected: day.key === selectedDay }]" :title="statusLabel(day.status)" @click="jumpTo(day.key)">
          <span class="calendar-daynum">{{ day.dayNumber }}</span>
          <small v-if="day.total">{{ day.done }}/{{ day.total }}</small>
        </button>
      </div>
      <p class="calendar-legend"><span class="dot done"/>{{ t('dayStatus_done') }}<span class="dot partial"/>{{ t('dayStatus_partial') }}<span class="dot missed"/>{{ t('dayStatus_missed') }}</p>
      <p v-if="selectedDay" class="calendar-filter">{{ t('calendarFilteredBy', { date: selectedDateLabel }) }} <button class="link" @click="clearDay">{{ t('calendarClearFilter') }}</button></p>
    </article>

    <article class="card task-page">
      <form v-if="formOpen" class="task-form" @submit.prevent="create">
        <label class="field"><span>{{ t('taskTitleLabel') }}</span><input v-model="title" type="text" :placeholder="t('taskTitlePlaceholder')" required /></label>
        <div class="task-form-row">
          <label class="field"><span>{{ t('taskTypeLabel') }}</span><select v-model="type" class="select"><option value="UNIQUE">{{ t('taskTypeUnique') }}</option><option value="DAILY">{{ t('taskTypeDaily') }}</option></select></label>
          <label v-if="type === 'UNIQUE'" class="field"><span>{{ t('taskStartDateLabel') }}</span><input v-model="startDate" type="datetime-local" /></label>
          <label class="field"><span>{{ t('taskEndDateLabel') }}</span><input v-model="endDate" type="datetime-local" /></label>
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <div class="task-form-actions"><button class="button" type="submit" :disabled="creating">{{ creating ? t('saving') : t('taskSaveAction') }}</button><button class="button secondary" type="button" :disabled="creating" @click="toggleForm">{{ t('cancelAction') }}</button></div>
      </form>
      <p v-if="!items.length && !error" class="empty-note">{{ t('noTasks') }}</p>
      <div v-for="item in items" :key="item.id" class="task-line">
        <button class="task-row" :disabled="savingId === item.id" @click="toggle(item.id)"><span class="checkbox" :class="{checked:isDone(item)}">✓</span><span class="task-info"><strong :class="{done:isDone(item)}">{{ item.title }}</strong><small>{{ t('endDateLabel') }} {{ formatTaskPeriod(item.startDate, item.endDate) }}</small><small v-if="dailyStats(item)" class="task-stats">{{ dailyStats(item) }}</small></span><span v-if="rowDayLabel(item)" class="task-day" :class="{ today: rowDay(item) === todayKey }">{{ rowDayLabel(item) }}</span><span v-if="item.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span><span v-if="item.status === 'DELAYED'" class="tag">{{ t('taskDelayed') }}</span></button>
        <button class="task-remove" :disabled="deletingId === item.id" :aria-label="t('taskDeleteAction')" @click="remove(item.id)">✕</button>
      </div>
      <button class="floating-add" :aria-expanded="formOpen" @click="toggleForm">＋</button>
    </article>
    <nav v-if="lastPage > 0" class="task-pager" :aria-busy="pageLoading">
      <button class="button secondary" :disabled="!canGoPrev" @click="goToPage(page - 1)">{{ t('prevPage') }}</button>
      <span>{{ t('pageOf', { page: page + 1, total: lastPage + 1 }) }}</span>
      <button class="button secondary" :disabled="!canGoNext" @click="goToPage(page + 1)">{{ t('nextPage') }}</button>
    </nav>
    <p class="task-summary">{{ t('taskPendingCount', { count: pendingCount, total }) }} · {{ t('taskDailyCount', { count: dailyCount }) }}</p>
  </section></main>
  <main v-else class="loading">{{ t('tasksLoading') }}</main>
</template>
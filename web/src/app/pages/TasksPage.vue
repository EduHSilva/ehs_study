<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useTasks } from '../features/tasks/useTasks'
import { MAX_DETAILS_LENGTH, renderTaskDetails } from '../features/tasks/taskDetails'
import { formatEndDate, formatTaskPeriod } from '../features/tasks/taskFormat'
import { parseDayKey, scheduledOn, weeklyGoalProgress } from '../features/tasks/calendar'
import { isRecurringType } from '../features/tasks/types'
import type { Task, TaskType } from '../features/tasks/types'

defineEmits<{ navigate: [page: AppPage] }>()
const { t, locale } = useI18n()
const { items, loading, pageLoading, calendarLoading, page, total, lastPage, canGoPrev, canGoNext, error, savingId, deletingId, copied, pendingCount, recurringCount, anchor, mode, calendar, selectedDay, projectFilter, formOpen, title, details, startDate, endDate, type, recurrenceDays, recurrenceDayOfMonth, weeklyGoal, projectId, stageId, projectOptions, stages, projectName, stageName, creating, updating, editingId, editingTask, formError, todayKey, isDone, canToggle, statsOpen, statsLoading, statsTasks, statsError, toggleStats, load, goToPage, navigate, jumpTo, setMode, toggle, toggleDetailsCheck, copyTasks, remove, toggleForm, create, openEdit, submitForm } = useTasks()
onMounted(load)

// Os checkboxes vêm do `v-html`, então o clique é delegado: o índice do item no texto vai junto
// no `data-check-index` e a marcação real acontece no estado da tarefa.
const onDetailsClick = (event: MouseEvent, item: Task) => {
  const target = event.target as HTMLElement | null
  const input = target?.closest<HTMLInputElement>('input[type="checkbox"][data-check-index]')
  if (!input) return
  event.preventDefault()
  toggleDetailsCheck(item.id, Number(input.dataset.checkIndex))
}

// Checklist pode ser longa: a linha nasce compacta e o olho abre os detalhes daquela tarefa.
const openDetails = ref<string[]>([])
const isDetailsOpen = (id: string) => openDetails.value.includes(id)
const toggleDetails = (id: string) => {
  openDetails.value = isDetailsOpen(id)
    ? openDetails.value.filter(item => item !== id)
    : [...openDetails.value, id]
}

const formEl = ref<HTMLFormElement | null>(null)
const startEdit = async (task: Task) => {
  openEdit(task)
  await nextTick()
  formEl.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

// 2024-01-01 foi uma segunda, então o índice i (0..6) já casa com o dia ISO (1..7).
const CALENDAR_WEEKDAYS = computed(() => {
  const fmt = new Intl.DateTimeFormat(locale.value, { weekday: 'narrow' })
  return Array.from({ length: 7 }, (_, i) => fmt.format(new Date(2024, 0, 1 + i)))
})
const WEEKDAY_OPTIONS = computed(() => {
  const fmt = new Intl.DateTimeFormat(locale.value, { weekday: 'short' })
  return Array.from({ length: 7 }, (_, i) => ({ value: i + 1, label: fmt.format(new Date(2024, 0, 1 + i)) }))
})

const TYPE_KEYS: Record<TaskType, string> = {
  UNIQUE: 'taskTypeUnique',
  DAILY: 'taskTypeDaily',
  WEEKLY: 'taskTypeWeekly',
  MONTHLY: 'taskTypeMonthly',
  WEEKLY_GOAL: 'taskTypeWeeklyGoal'
}
const typeLabel = (value: TaskType) => t(TYPE_KEYS[value] ?? 'taskTypeUnique')

const toggleWeekday = (value: number) => {
  recurrenceDays.value = recurrenceDays.value.includes(value)
    ? recurrenceDays.value.filter(day => day !== value)
    : [...recurrenceDays.value, value].sort((a, b) => a - b)
}
const numberFromInput = (value: string): number | null => (value === '' ? null : Number(value))
const setMonthDay = (event: Event) => { recurrenceDayOfMonth.value = numberFromInput((event.target as HTMLInputElement).value) }
const setWeeklyGoal = (event: Event) => { weeklyGoal.value = numberFromInput((event.target as HTMLInputElement).value) }

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
// um intervalo, então a data fica por conta da linha de período; numa recorrente só faz sentido
// mostrar hoje quando a task realmente cai hoje (uma semanal de terça não tem dia na sexta).
const rowDay = (item: Task): string => {
  if (selectedDay.value) return selectedDay.value
  if (!isRecurringType(item.type)) return ''
  return scheduledOn(item, todayKey.value) ? todayKey.value : ''
}
const dayFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { weekday: 'short', day: '2-digit', month: '2-digit' }))
const rowDayLabel = (item: Task): string => {
  const key = rowDay(item)
  return key ? dayFormatter.value.format(parseDayKey(key)) : ''
}
// A lista (`GET /tasks`) traz `dailyCompletions` e deixa `dailyStatistics` nulo; o detalhe é o
// contrário. Então o resumo de dias concluídos sai da lista de dias, que é o que a tela tem.
const dailyStats = (item: Task) => {
  if (item.type === 'WEEKLY_GOAL' && item.weeklyGoal) {
    const { done, goal, met } = weeklyGoalProgress(item, todayKey.value)
    return t('taskWeeklyGoalProgress', { done, goal }) + (met ? ` · ${t('taskStatsGoalMet')}` : '')
  }
  const count = item.dailyCompletions?.length ?? 0
  return count ? t('taskDailyDoneCount', { count }) : ''
}
// `Prazo` só existe quando há `endDate`; recorrentes sem fim mostram quando foram criadas.
const taskPeriodLabel = (item: Task): string => {
  if (item.endDate) return `${t('endDateLabel')} ${formatTaskPeriod(item.startDate, item.endDate)}`
  return item.createdDate ? t('createdAtLabel', { date: formatEndDate(item.createdDate) }) : ''
}
</script>
<template>
  <section class="content"><header class="page-header"><div><h1>{{ t('tasks') }}</h1><p>{{ t('tasksSubtitle') }}</p></div><div class="header-actions"><button class="button secondary" :disabled="!total" @click="copyTasks">{{ copied ? t('tasksCopied') : t('copyTasksAction') }}</button><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <PageSkeleton v-if="loading" layout="list" :label="t('tasksLoading')" />
    <template v-else>

    <div class="task-toolbar">
      <label class="field task-filter"><span>{{ t('taskFilterProject') }}</span><select v-model="projectFilter" class="select"><option value="">{{ t('taskFilterAll') }}</option><option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.title }}</option></select></label>
      <button class="button secondary" :aria-expanded="statsOpen" @click="toggleStats">{{ t('taskStatsToggle') }}</button>
    </div>

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
        <span v-for="(label, i) in CALENDAR_WEEKDAYS" :key="i" class="calendar-weekday">{{ label }}</span>
        <button v-for="day in calendar" :key="day.key" class="calendar-day" :class="[day.status, { outside: day.outside, today: isToday(day.key), selected: day.key === selectedDay }]" :title="statusLabel(day.status)" @click="jumpTo(day.key)">
          <span class="calendar-daynum">{{ day.dayNumber }}</span>
          <small v-if="day.total">{{ day.done }}/{{ day.total }}</small>
        </button>
      </div>
      <p class="calendar-legend"><span class="dot done"/>{{ t('dayStatus_done') }}<span class="dot partial"/>{{ t('dayStatus_partial') }}<span class="dot missed"/>{{ t('dayStatus_missed') }}</p>
      <p v-if="selectedDay" class="calendar-filter">{{ t('calendarFilteredBy', { date: selectedDateLabel }) }} <button class="link" @click="clearDay">{{ t('calendarClearFilter') }}</button></p>
    </article>

    <article v-if="statsOpen" class="card task-stats-card" :aria-busy="statsLoading">
      <header class="stats-head">
        <strong>{{ t('taskStatsTitle') }}</strong>
        <small>{{ t('taskStatsPeriod') }}</small>
      </header>
      <p v-if="statsError" class="sync-error">{{ statsError }} <button @click="toggleStats">{{ t('retry') }}</button></p>
      <p v-else-if="!statsLoading && !statsTasks.length" class="empty-note">{{ t('taskStatsEmpty') }}</p>
      <div v-else class="stats-list">
        <div v-for="entry in statsTasks" :key="entry.task.id" class="stats-row">
          <div class="stats-title"><strong>{{ entry.task.title }}</strong><small>{{ typeLabel(entry.task.type) }}</small></div>
          <template v-if="entry.task.type === 'WEEKLY_GOAL'">
            <span>{{ t('taskStatsWeeksOk', { count: entry.weeksOk }) }}</span>
            <span>{{ t('taskStatsWeeksFailed', { count: entry.weeksFailed }) }}</span>
            <span>{{ t('taskStatsCurrentWeek', { done: entry.currentWeekDone, goal: entry.currentWeekGoal ?? 0 }) }}</span>
            <span class="stats-rate" :class="{ good: entry.currentWeekMet }">{{ entry.currentWeekMet ? t('taskStatsGoalMet') : t('taskStatsGoalOpen') }}</span>
          </template>
          <template v-else>
            <span>{{ t('taskStatsCompleted', { count: entry.completed }) }}</span>
            <span>{{ t('taskStatsFailures', { count: entry.failed }) }}</span>
            <span class="stats-rate">{{ t('taskStatsRate', { rate: entry.rate }) }}</span>
          </template>
        </div>
      </div>
    </article>

    <article class="card task-page">
      <form v-if="formOpen" ref="formEl" class="task-form" @submit.prevent="submitForm">
        <header v-if="editingId" class="task-form-header">
          <strong>{{ t('editTaskTitle') }}</strong>
          <small v-if="editingTask">{{ editingTask.title }}</small>
        </header>
        <label class="field"><span>{{ t('taskTitleLabel') }}</span><input v-model="title" type="text" :placeholder="t('taskTitlePlaceholder')" required /></label>
        <label class="field"><span>{{ t('taskDetailsLabel') }}</span><textarea v-model="details" rows="3" :maxlength="MAX_DETAILS_LENGTH" :placeholder="t('taskDetailsPlaceholder')"></textarea></label>
        <div class="task-form-row">
          <label class="field"><span>{{ t('taskTypeLabel') }}</span><select v-model="type" class="select"><option value="UNIQUE">{{ t('taskTypeUnique') }}</option><option value="DAILY">{{ t('taskTypeDaily') }}</option><option value="WEEKLY">{{ t('taskTypeWeekly') }}</option><option value="MONTHLY">{{ t('taskTypeMonthly') }}</option><option value="WEEKLY_GOAL">{{ t('taskTypeWeeklyGoal') }}</option></select></label>
          <label v-if="type !== 'DAILY' || editingId" class="field"><span>{{ t('taskStartDateLabel') }}</span><input v-model="startDate" type="datetime-local" /></label>
          <label class="field"><span>{{ t('taskEndDateLabel') }}</span><input v-model="endDate" type="datetime-local" /></label>
        </div>
        <div v-if="type === 'WEEKLY'" class="field">
          <span>{{ t('taskWeekdaysLabel') }}</span>
          <div class="weekday-picker">
            <button v-for="day in WEEKDAY_OPTIONS" :key="day.value" type="button" class="weekday" :class="{ selected: recurrenceDays.includes(day.value) }" :aria-pressed="recurrenceDays.includes(day.value)" @click="toggleWeekday(day.value)">{{ day.label }}</button>
          </div>
        </div>
        <div v-if="type === 'MONTHLY' || type === 'WEEKLY_GOAL'" class="task-form-row">
          <label v-if="type === 'MONTHLY'" class="field"><span>{{ t('taskMonthDayLabel') }}</span><input type="number" min="1" max="31" :value="recurrenceDayOfMonth ?? ''" @input="setMonthDay" /></label>
          <label v-if="type === 'WEEKLY_GOAL'" class="field"><span>{{ t('taskWeeklyGoalLabel') }}</span><input type="number" min="1" max="7" :value="weeklyGoal ?? ''" @input="setWeeklyGoal" /></label>
        </div>
        <div class="task-form-row">
          <label class="field"><span>{{ t('taskProjectLabel') }}</span><select v-model="projectId" class="select"><option value="">{{ t('taskProjectNone') }}</option><option v-for="project in projectOptions" :key="project.id" :value="project.id">{{ project.title }}</option></select></label>
          <label class="field"><span>{{ t('taskStageLabel') }}</span><select v-model="stageId" class="select" :disabled="!projectId || !stages.length"><option value="">{{ t('taskStageNone') }}</option><option v-for="stage in stages" :key="stage.id" :value="stage.id">{{ stage.name }}</option></select></label>
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <div class="task-form-actions"><button class="button" type="submit" :disabled="creating || updating">{{ creating || updating ? t('saving') : editingId ? t('saveChangesAction') : t('taskSaveAction') }}</button><button class="button secondary" type="button" :disabled="creating || updating" @click="toggleForm">{{ t('cancelAction') }}</button></div>
      </form>
      <p v-if="!items.length && !error" class="empty-note">{{ t('noTasks') }}</p>
      <div v-for="item in items" :key="item.id" class="task-line">
        <div class="task-main">
        <button class="task-row" :disabled="savingId === item.id || !canToggle(item)" @click="toggle(item.id)"><span class="checkbox" :class="{checked:isDone(item)}">✓</span><span class="task-info"><strong :class="{done:isDone(item)}">{{ item.title }}</strong><small v-if="taskPeriodLabel(item)">{{ taskPeriodLabel(item) }}</small><small v-if="dailyStats(item)" class="task-stats">{{ dailyStats(item) }}</small></span><span v-if="rowDayLabel(item)" class="task-day" :class="{ today: rowDay(item) === todayKey }">{{ rowDayLabel(item) }}</span><span v-if="projectName(item)" class="tag">{{ projectName(item) }}</span><span v-if="stageName(item)" class="tag">{{ stageName(item) }}</span><span v-if="isRecurringType(item.type)" class="tag daily">{{ typeLabel(item.type) }}</span><span v-if="item.status === 'DELAYED'" class="tag">{{ t('taskDelayed') }}</span></button>
          <div v-if="item.details && isDetailsOpen(item.id)" class="task-detail" @click="onDetailsClick($event, item)" v-html="renderTaskDetails(item.details)"></div>
        </div>
        <div class="task-actions">
          <button v-if="item.details" class="task-view" :class="{ open: isDetailsOpen(item.id) }" :aria-expanded="isDetailsOpen(item.id)" :aria-label="isDetailsOpen(item.id) ? t('taskDetailsHide') : t('taskDetailsShow')" :title="isDetailsOpen(item.id) ? t('taskDetailsHide') : t('taskDetailsShow')" @click="toggleDetails(item.id)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2.5 12S6 6 12 6s9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
          <button class="task-edit" :aria-label="t('taskEditAction')" @click="startEdit(item)">✎</button>
          <button class="task-remove" :disabled="deletingId === item.id" :aria-label="t('taskDeleteAction')" @click="remove(item.id)">✕</button>
        </div>
      </div>
      <button class="floating-add" :aria-expanded="formOpen" @click="toggleForm">＋</button>
    </article>
    <nav v-if="lastPage > 0" class="task-pager" :aria-busy="pageLoading">
      <button class="button secondary" :disabled="!canGoPrev" @click="goToPage(page - 1)">{{ t('prevPage') }}</button>
      <span>{{ t('pageOf', { page: page + 1, total: lastPage + 1 }) }}</span>
      <button class="button secondary" :disabled="!canGoNext" @click="goToPage(page + 1)">{{ t('nextPage') }}</button>
    </nav>
    <p class="task-summary">{{ t('taskPendingCount', { count: pendingCount, total }) }} · {{ t('taskRecurringCount', { count: recurringCount }) }}</p>
    </template>
  </section>
</template>
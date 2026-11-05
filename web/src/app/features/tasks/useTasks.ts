import { computed, ref, watch } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createTask, deleteTask, getTasksInRange, updateTask } from './tasks.api'
import { buildCalendarGrid, dayKey, dayKeyFrom, parseDayKey, rangeFor, gridRangeFor, startOfDay, taskEnd, taskStart, taskCoversDay, isTaskDone, isCompletedOn, addDays, addMonths } from './calendar'
import { localInputToIso } from './taskFormat'
import type { Task, TaskInput, TaskStatus, TaskType } from './types'

const TITLE_MIN_LENGTH = 1
export const PAGE_SIZE = 10

export function useTasks() {
  const { t } = useI18n()

  // Período e calendário navegável
  const anchor = ref(startOfDay(new Date()))
  const mode = ref<'week' | 'month'>('week')
  const calendarLoading = ref(false)

  // Tarefas do período visível, usadas para pintar o calendário
  const periodTasks = ref<Task[]>([])
  const selectedDay = ref<string | null>(null)

  const items = ref<Task[]>([])
  const loading = ref(true)
  const pageLoading = ref(false)
  const page = ref(0)
  const total = ref(0)
  const savingId = ref<string | null>(null)
  const deletingId = ref<string | null>(null)
  const error = ref<string | null>(null)

  const formOpen = ref(false)
  const title = ref('')
  const startDate = ref('')
  const endDate = ref('')
  const type = ref<TaskType>('UNIQUE')
  const creating = ref(false)
  const formError = ref<string | null>(null)

  const todayKey = ref(dayKey(new Date()))
  // O dia que o toggle vai atingir: o selecionado no calendário, senão hoje. O checkbox e a ação
  // usam o mesmo dia, senão a tela mostraria o estado de hoje enquanto o clique marcaria outro dia.
  const targetDay = computed(() => selectedDay.value ?? todayKey.value)
  const isDone = (task: Task) =>
    (task.type === 'DAILY' && isCompletedOn(task, targetDay.value, todayKey.value)) ||
    (task.type !== 'DAILY' && task.status === 'DONE')
  const range = computed(() => rangeFor(anchor.value, mode.value))
  const gridRange = computed(() => gridRangeFor(anchor.value, mode.value))
  const calendar = computed(() => buildCalendarGrid(periodTasks.value, anchor.value, mode.value, todayKey.value))
  const lastPage = computed(() => Math.max(0, Math.ceil(total.value / PAGE_SIZE) - 1))
  const canGoPrev = computed(() => page.value > 0)
  const canGoNext = computed(() => page.value < lastPage.value)
  const pendingCount = computed(() => items.value.filter(item => !isTaskDone(item, todayKey.value)).length)
  const dailyCount = computed(() => items.value.filter(item => item.type === 'DAILY').length)

  const paginate = (all: Task[]) => {
    total.value = all.length
    const maxPage = Math.max(0, Math.ceil(all.length / PAGE_SIZE) - 1)
    if (page.value > maxPage) page.value = maxPage
    items.value = all.slice(page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE)
  }

  const applyFilter = () => {
    const key = selectedDay.value
    if (key) {
      paginate(periodTasks.value.filter(task => taskCoversDay(task, key, todayKey.value)))
      return
    }
    const { from, to } = range.value
    const fromMs = startOfDay(from).getTime()
    const toMs = startOfDay(to).getTime()
    paginate(periodTasks.value.filter(task => {
      if (task.status === 'DELETED') return false
      if (task.type === 'DAILY') {
        const start = taskStart(task)
        return !start || startOfDay(start).getTime() <= toMs
      }
      const taskFrom = taskStart(task)
      const taskTo = taskEnd(task)
      if (!taskFrom || !taskTo) return false
      const a = startOfDay(taskFrom).getTime()
      const b = startOfDay(taskTo).getTime()
      return b >= fromMs && a <= toMs
    }))
  }

  const fetchPeriod = async () => {
    // Busca a grade completa (inclui dias vizinhos do mês); o filtro da lista usa o período estrito.
    const { from, to } = gridRange.value
    periodTasks.value = await getTasksInRange(from, to)
  }

  const refresh = async () => {
    pageLoading.value = true
    error.value = null
    try {
      await fetchPeriod()
      applyFilter()
    } catch {
      error.value = t('tasksLoadFailed')
    } finally {
      pageLoading.value = false
    }
  }

  const load = async () => {
    loading.value = true
    calendarLoading.value = true
    try {
      await refresh()
    } finally {
      loading.value = false
      calendarLoading.value = false
    }
  }

  const navigate = async (step: number) => {
    anchor.value = mode.value === 'week' ? addDays(anchor.value, step * 7) : addMonths(anchor.value, step)
    selectedDay.value = null
    await refresh()
  }

  const jumpTo = async (key: string) => {
    const day = parseDayKey(key)
    const { from, to } = range.value
    if (day.getTime() < from.getTime() || day.getTime() > to.getTime()) {
      anchor.value = day
      selectedDay.value = null
      await refresh()
      return
    }
    selectedDay.value = selectedDay.value === key ? null : key
    page.value = 0
    applyFilter()
  }

  const setMode = async (next: 'week' | 'month') => {
    if (mode.value === next) return
    mode.value = next
    selectedDay.value = null
    await refresh()
  }

  const goToPage = (target: number) => {
    const next = Math.min(Math.max(target, 0), lastPage.value)
    if (next === page.value) return
    page.value = next
    applyFilter()
  }

  watch([range, selectedDay], () => {
    page.value = 0
  })

  const resetForm = () => {
    title.value = ''
    startDate.value = ''
    endDate.value = ''
    type.value = 'UNIQUE'
    formError.value = null
  }

  const toggleForm = () => {
    formOpen.value = !formOpen.value
    if (!formOpen.value) resetForm()
  }

  const validate = (): boolean => {
    formError.value = null
    if (title.value.trim().length < TITLE_MIN_LENGTH) {
      formError.value = t('taskTitleRequired')
      return false
    }
    if (startDate.value && !localInputToIso(startDate.value)) {
      formError.value = t('taskStartDateInvalid')
      return false
    }
    if (endDate.value && !localInputToIso(endDate.value)) {
      formError.value = t('taskEndDateInvalid')
      return false
    }
    // Numa UNIQUE o fim não pode vir antes do início.
    if (type.value === 'UNIQUE' && startDate.value && endDate.value && startDate.value > endDate.value) {
      formError.value = t('taskRangeInverted')
      return false
    }
    return true
  }

  // Numa UNIQUE sem início explícito, o fim define o dia único. Numa DAILY o início é hoje,
  // porque a recorrência passa a valer a partir do cadastro.
  const buildInput = (): TaskInput => {
    const end = localInputToIso(endDate.value)
    if (type.value === 'DAILY') {
      return { title: title.value.trim(), startDate: localInputToIso(startDate.value) ?? new Date().toISOString(), endDate: end, type: 'DAILY' }
    }
    const start = localInputToIso(startDate.value) ?? end
    return { title: title.value.trim(), startDate: start, endDate: end, type: 'UNIQUE' }
  }

  const create = async (): Promise<boolean> => {
    if (creating.value || !validate()) return false
    creating.value = true
    try {
      const input = buildInput()
      const focusDay = dayKey(new Date(input.startDate ?? input.endDate ?? new Date()))
      await createTask(input)

      // Destaca o dia da task criada; se ele estiver fora do período visível, move o calendário até lá.
      selectedDay.value = focusDay
      const { from, to } = range.value
      const time = parseDayKey(focusDay).getTime()
      if (time < startOfDay(from).getTime() || time > startOfDay(to).getTime()) anchor.value = parseDayKey(focusDay)
      await fetchPeriod()
      page.value = 0
      applyFilter()
      resetForm()
      return true
    } catch {
      formError.value = t('taskCreateFailed')
      return false
    } finally {
      creating.value = false
    }
  }

const nextStatus = (status: TaskStatus): TaskStatus => (status === 'DONE' ? 'PENDING' : 'DONE')

  const toggle = async (id: string) => {
    const current = items.value.find(item => item.id === id)
    if (!current || savingId.value === id) return
    const day = targetDay.value
    error.value = null

    // O back recusa `completionDate` futuro, e sem `startDate`/`endDate` recusa qualquer data
    // explícita. Nesses casos a conclusão cairia no dia atual, o que silenciosamente marcaria
    // outro dia do que o usuário escolheu — então avisamos em vez de mandar a chamada.
    if (current.type === 'DAILY') {
      if (day > todayKey.value) {
        error.value = t('taskCompletionFuture')
        return
      }
      // Sem intervalo o back só conclui o dia corrente, então marcar outro dia é impossível —
      // mas marcar *hoje* continua valendo, mesmo com um dia selecionado no calendário.
      if (day !== todayKey.value && !(current.startDate || current.endDate)) {
        error.value = t('taskCompletionNoRange')
        return
      }
    }

    const wasDone = isDone(current)
    const next = nextStatus(wasDone ? 'DONE' : 'PENDING')
    const previous = { ...current }

    // Otimismo: numa DAILY o que muda é a lista de dias concluídos, porque é ela que pinta o
    // calendário. O back só aceita `completionDate` quando a task tem `startDate`/`endDate`
    // (sem intervalo ele rejeita qualquer data), então nesse caso deixa o dia corrente implícito.
    const completionDate = current.type === 'DAILY' && (current.startDate || current.endDate) ? day : null
    const optimistic: Task = {
      ...current,
      status: next,
      dailyStatistics: current.dailyStatistics
        ? { ...current.dailyStatistics, completedToday: next === 'DONE' && day === todayKey.value }
        : null,
      dailyCompletions: next === 'DONE'
        ? [...current.dailyCompletions.filter(e => dayKeyFrom(e.completionDate) !== day),
           { completionDate: day, completedAt: new Date().toISOString() }]
        : current.dailyCompletions.filter(e => dayKeyFrom(e.completionDate) !== day)
    }
    Object.assign(current, optimistic)
    savingId.value = id
    try {
      const saved = await updateTask({ id: current.id, status: next, completionDate })
      // A resposta do PUT vem sem `dailyCompletions` (só o detalhe as traz), então reaplicar ela
      // apagaria o histórico. Numa DAILY recarrega a lista, que é onde o back devolve os dias.
      if (current.type === 'DAILY') {
        Object.assign(current, { ...saved, dailyCompletions: optimistic.dailyCompletions })
        periodTasks.value = periodTasks.value.map(task =>
          task.id === saved.id ? { ...saved, dailyCompletions: optimistic.dailyCompletions } : task
        )
        await fetchPeriod()
        applyFilter()
      } else {
        Object.assign(current, saved)
        periodTasks.value = periodTasks.value.map(task => (task.id === saved.id ? saved : task))
      }
    } catch {
      Object.assign(current, previous)
      error.value = t('taskSaveFailed')
    } finally {
      savingId.value = null
    }
  }

  const remove = async (id: string) => {
    if (deletingId.value === id) return
    deletingId.value = id
    try {
      await deleteTask(id)
      periodTasks.value = periodTasks.value.filter(task => task.id !== id)
      items.value = items.value.filter(task => task.id !== id)
      applyFilter()
    } catch {
      error.value = t('taskDeleteFailed')
    } finally {
      deletingId.value = null
    }
  }

  return {
    items,
    loading,
    pageLoading,
    calendarLoading,
    page,
    total,
    lastPage,
    canGoPrev,
    canGoNext,
    error,
    savingId,
    deletingId,
    pendingCount,
    dailyCount,
    anchor,
    mode,
    calendar,
    selectedDay,
    formOpen,
    title,
    startDate,
    endDate,
    type,
    creating,
    formError,
    todayKey,
    isDone,
    load,
    refresh,
    goToPage,
    navigate,
    jumpTo,
    setMode,
    toggle,
    remove,
    toggleForm,
    create
  }
}

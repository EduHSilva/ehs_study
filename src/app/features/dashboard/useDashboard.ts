import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getDashboard } from './dashboard.api'
import { updateTask } from '../tasks/tasks.api'
import type { DashboardData } from './types'

export function useDashboard() {
  const { t } = useI18n()
  const data = ref<DashboardData | null>(null)
  const loading = ref(true)
  const savingTaskId = ref<string | null>(null)
  const error = ref<string | null>(null)
  let timer: number | undefined

  const progress = computed(() => data.value ? Math.round(data.value.weeklyProgress.completed / data.value.weeklyProgress.total * 100) : 0)
  const formattedTime = computed(() => {
    const seconds = data.value?.focus.remainingSeconds ?? 0
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
  })
  const load = async () => { try { loading.value = true; error.value = null; data.value = await getDashboard() } catch { error.value = t('dashboardLoadFailed') } finally { loading.value = false } }
  const toggleTask = async (id: string) => {
    if (!data.value) return
    const item = data.value.tasks.find(task => task.id === id)
    if (!item) return
    savingTaskId.value = id
    const previous = { ...item }
    item.status = item.status === 'DONE' ? 'PENDING' : 'DONE'
    try {
      Object.assign(item, await updateTask({ ...item }))
    } catch { Object.assign(item, previous); error.value = t('taskSaveFailed') } finally { savingTaskId.value = null }
  }
  const toggleFocus = () => { if (data.value) data.value.focus.running = !data.value.focus.running }
  const resetFocus = () => { if (data.value) data.value.focus.remainingSeconds = 25 * 60 }
  timer = window.setInterval(() => { if (data.value?.focus.running && data.value.focus.remainingSeconds > 0) data.value.focus.remainingSeconds-- }, 1000)
  onBeforeUnmount(() => window.clearInterval(timer))
  return { data, loading, error, progress, formattedTime, savingTaskId, load, toggleTask, toggleFocus, resetFocus }
}

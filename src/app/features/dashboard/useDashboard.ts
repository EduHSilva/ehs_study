import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getDashboard } from './dashboard.api'
import { updateReminder } from '../reminders/reminders.api'
import type { DashboardData } from './types'

export function useDashboard() {
  const { t } = useI18n()
  const data = ref<DashboardData | null>(null)
  const loading = ref(true)
  const savingReminderId = ref<string | null>(null)
  const error = ref<string | null>(null)
  let timer: number | undefined

  const progress = computed(() => data.value ? Math.round(data.value.weeklyProgress.completed / data.value.weeklyProgress.total * 100) : 0)
  const formattedTime = computed(() => {
    const seconds = data.value?.focus.remainingSeconds ?? 0
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
  })
  const load = async () => { try { loading.value = true; error.value = null; data.value = await getDashboard() } catch { error.value = t('dashboardLoadFailed') } finally { loading.value = false } }
  const toggleReminder = async (id: string) => {
    if (!data.value) return
    const item = data.value.reminders.find(reminder => reminder.id === id)
    if (!item) return
    savingReminderId.value = id
    try { Object.assign(item, await updateReminder(id, !item.completed)) } catch { error.value = t('reminderSaveFailed') } finally { savingReminderId.value = null }
  }
  const toggleFocus = () => { if (data.value) data.value.focus.running = !data.value.focus.running }
  const resetFocus = () => { if (data.value) data.value.focus.remainingSeconds = 25 * 60 }
  timer = window.setInterval(() => { if (data.value?.focus.running && data.value.focus.remainingSeconds > 0) data.value.focus.remainingSeconds-- }, 1000)
  onBeforeUnmount(() => window.clearInterval(timer))
  return { data, loading, error, progress, formattedTime, savingReminderId, load, toggleReminder, toggleFocus, resetFocus }
}

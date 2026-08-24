import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createReminder, getReminders, updateReminder } from './reminders.api'
import type { Reminder, ReminderInput } from './types'

export function useReminders() {
  const { t } = useI18n()
  const items = ref<Reminder[]>([])
  const loading = ref(true)
  const savingId = ref<string | null>(null)
  const error = ref<string | null>(null)

  const pendingCount = computed(() => items.value.filter(item => !item.completed).length)

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      items.value = await getReminders()
    } catch {
      error.value = t('remindersLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const toggle = async (id: string) => {
    const item = items.value.find(entry => entry.id === id)
    if (!item) return
    savingId.value = id
    try {
      Object.assign(item, await updateReminder(id, !item.completed))
    } catch {
      error.value = t('reminderSaveFailed')
    } finally {
      savingId.value = null
    }
  }

  const add = async (input: ReminderInput) => {
    error.value = null
    try {
      items.value.push(await createReminder(input))
    } catch {
      error.value = t('reminderCreateFailed')
    }
  }

  return { items, loading, error, pendingCount, savingId, load, toggle, add }
}

import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getTodaySessions } from './pomodoro.api'
import type { FocusSession } from './types'

export function usePomodoro() {
  const { t } = useI18n()
  const sessions = ref<FocusSession[]>([])
  const loading = ref(true)
  const error = ref<string | null>(null)

  const totalMinutes = computed(() => sessions.value.reduce((sum, session) => sum + session.minutes, 0))

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      sessions.value = await getTodaySessions()
    } catch {
      error.value = t('pomodoroLoadFailed')
    } finally {
      loading.value = false
    }
  }

  return { sessions, totalMinutes, loading, error, load }
}

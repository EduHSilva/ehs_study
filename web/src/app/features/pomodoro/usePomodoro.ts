import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createFocusSession, getTodaySessions } from './pomodoro.api'
import type { FocusSession } from './types'

// Estado de módulo: a página do Pomodoro e o widget flutuante compartilham a mesma lista de
// sessões, então finalizar o ciclo pelo widget atualiza os contadores em qualquer tela.
const sessions = ref<FocusSession[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

export function usePomodoro() {
  const { t } = useI18n()
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

  const registerSession = async (title: string, minutes: number): Promise<boolean> => {
    saving.value = true
    error.value = null
    try {
      const session = await createFocusSession(title, minutes)
      sessions.value = [session, ...sessions.value]
      return true
    } catch {
      error.value = t('sessionSaveFailed')
      return false
    } finally {
      saving.value = false
    }
  }

  return { sessions, totalMinutes, loading, saving, error, load, registerSession }
}

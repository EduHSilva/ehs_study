import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getDashboard } from './dashboard.api'
import { updateTask } from '../tasks/tasks.api'
import { dayKey, taskCoversDay } from '../tasks/calendar'
import { getTodaySessions } from '../pomodoro/pomodoro.api'
import type { DashboardData } from './types'
import type { FocusSession } from '../pomodoro/types'

export function useDashboard() {
  const { t } = useI18n()
  const data = ref<DashboardData | null>(null)
  const focus = ref<FocusSession | null>(null)
  const loading = ref(true)
  const savingTaskId = ref<string | null>(null)
  const error = ref<string | null>(null)

  const progress = computed(() => {
    const { completed, total } = data.value?.weeklyProgress ?? { completed: 0, total: 0 }
    if (!total) return 0
    return Math.round(completed / total * 100)
  })

  // O dashboard do back pode vir com a janela das próximas 24h; "Tarefas de Hoje" mostra só as
  // que ocupam o dia local atual, com o mesmo corte por dia (00:00–23:59) da tela de tarefas.
  const tasks = computed(() => {
    const today = dayKey(new Date())
    return (data.value?.tasks ?? []).filter(task => taskCoversDay(task, today, today))
  })

  // O foco não vive mais no dashboard: o cronômetro roda no cliente e o que persiste são as
  // sessões de `/focus`. O card "Foco Ativo" mostra o último foco concluído hoje (se houver).
  const load = async () => {
    try {
      loading.value = true
      error.value = null
      const [dashboard, sessions] = await Promise.all([
        getDashboard(),
        getTodaySessions().catch(() => [] as FocusSession[])
      ])
      data.value = dashboard
      focus.value = sessions[0] ?? null
    } catch {
      error.value = t('dashboardLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const toggleTask = async (id: string) => {
    if (!data.value) return
    const item = data.value.tasks.find(task => task.id === id)
    if (!item) return
    savingTaskId.value = id
    const previous = { ...item }
    item.status = item.status === 'DONE' ? 'PENDING' : 'DONE'
    // O dia vem do calendário local, como na tela de tarefas: sem isso a conclusão dependeria do
    // fuso do servidor e, perto da meia-noite, cairia no dia seguinte.
    const day = dayKey(new Date())
    const completionDate = item.type === 'DAILY' ? ((item.startDate || item.endDate) ? day : null) : day
    try {
      Object.assign(item, await updateTask({ ...item, completionDate }))
    } catch { Object.assign(item, previous); error.value = t('taskSaveFailed') } finally { savingTaskId.value = null }
  }

  return { data, focus, tasks, loading, error, progress, savingTaskId, load, toggleTask }
}

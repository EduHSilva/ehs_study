import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { onSessionCacheReset } from '../../composables/useSessionCache'
import { getDashboard } from './dashboard.api'
import { updateTask } from '../tasks/tasks.api'
import { dayKey, taskCoversDay } from '../tasks/calendar'
import { getTodaySessions } from '../pomodoro/pomodoro.api'
import type { DashboardData } from './types'
import type { FocusSession } from '../pomodoro/types'

// Cache de módulo: voltar ao dashboard mostra o que já veio antes e revalida em segundo plano,
// em vez de trocar a tela inteira por um "Carregando…".
const data = ref<DashboardData | null>(null)
const focus = ref<FocusSession | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
let loaded = false
let inFlight: Promise<void> | null = null
let cacheEpoch = 0

onSessionCacheReset(() => {
  cacheEpoch += 1
  loaded = false
  inFlight = null
  data.value = null
  focus.value = null
  loading.value = true
  error.value = null
})

export function useDashboard() {
  const { t } = useI18n()
  const savingTaskId = ref<string | null>(null)

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
  const load = (): Promise<void> => {
    if (inFlight) return inFlight
    const epoch = cacheEpoch
    loading.value = !loaded
    error.value = null
    inFlight = (async () => {
      try {
        const [dashboard, sessions] = await Promise.all([
          getDashboard(),
          getTodaySessions().catch(() => [] as FocusSession[])
        ])
        if (epoch !== cacheEpoch) return
        data.value = dashboard
        focus.value = sessions[0] ?? null
        loaded = true
      } catch {
        if (epoch !== cacheEpoch) return
        error.value = t('dashboardLoadFailed')
      } finally {
        if (epoch === cacheEpoch) {
          loading.value = false
          inFlight = null
        }
      }
    })()
    return inFlight
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

import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Task } from '../tasks/types'
import type { DashboardData } from './types'

interface DashboardPayload {
  weeklyProgress?: DashboardData['weeklyProgress']
  // O endpoint ainda pode entregar a lista como `reminders` (nome antigo do contrato).
  tasks?: unknown[]
  reminders?: unknown[]
  notes?: DashboardData['notes']
}

function readStatus(item: Record<string, unknown>): Task['status'] {
  if (item.status === 'DONE') return 'DONE'
  if (item.status === 'DELAYED') return 'DELAYED'
  if (item.status === 'WORKING') return 'WORKING'
  return item.completed === true ? 'DONE' : 'PENDING'
}

function toTask(raw: unknown, index: number): Task | null {
  if (!raw || typeof raw !== 'object') return null
  const item = raw as Record<string, unknown>
  const endDate = typeof item.endDate === 'string' ? item.endDate : null
  const startDate = typeof item.startDate === 'string' ? item.startDate : null
  const createdDate = typeof item.createdDate === 'string' ? item.createdDate : null
  const completedDate = typeof item.completedDate === 'string' ? item.completedDate : null
  const legacyDate = typeof item.dueDate === 'string' ? item.dueDate : null
  const schedule = typeof item.schedule === 'string' ? item.schedule : null
  return {
    id: typeof item.id === 'string' ? item.id : `dashboard-task-${index}`,
    title: typeof item.title === 'string' ? item.title : '',
    createdDate,
    startDate,
    endDate: endDate ?? legacyDate ?? schedule,
    type: item.type === 'DAILY' ? 'DAILY' : 'UNIQUE',
    status: readStatus(item),
    completedDate,
    projectId: typeof item.projectId === 'string' ? item.projectId : null,
    stageId: typeof item.stageId === 'string' ? item.stageId : null,
    dailyStatistics: null,
    dailyCompletions: [],
    recurrenceDays: [],
    recurrenceDayOfMonth: null,
    weeklyGoal: null
  }
}

export async function getDashboard(): Promise<DashboardData> {
  const payload = unwrap(await httpClient<DefaultResponse<DashboardPayload>>('/study/dashboard'))
  const list = payload.tasks ?? payload.reminders ?? []
  return {
    weeklyProgress: payload.weeklyProgress ?? { completed: 0, total: 0 },
    tasks: list.map(toTask).filter((task): task is Task => task !== null),
    notes: payload.notes ?? []
  }
}
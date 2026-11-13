export type TaskStatus = 'PENDING' | 'WORKING' | 'DONE' | 'DELAYED' | 'DELETED'
export type TaskType = 'UNIQUE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'WEEKLY_GOAL'

export const RECURRING_TYPES: TaskType[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'WEEKLY_GOAL']

export function isRecurringType(type: TaskType | null | undefined): boolean {
  return !!type && RECURRING_TYPES.includes(type)
}

// Agregados que o backend expõe para tarefas recorrentes: a conclusão é salva dia a dia
// no servidor, mas só chega no front como resumo.
export interface DailyTaskStatistics {
  scheduledDays: number
  completedDays: number
  skippedDays: number
  completedToday: boolean
}

// Um dia concluído de uma tarefa recorrente. É o que permite pintar o histórico no calendário.
export interface DailyTaskCompletion {
  completionDate: string
  completedAt: string | null
}

export interface Task {
  id: string
  title: string
  createdDate: string | null
  startDate: string | null
  endDate: string | null
  type: TaskType
  status: TaskStatus
  // Dia em que uma tarefa UNIQUE foi concluída. Enquanto pendente é nulo; quando concluída,
  // o calendário mostra a tarefa só nesse dia. Recorrentes não usam (têm `dailyCompletions`).
  completedDate: string | null
  // Dias da semana (1 = segunda .. 7 = domingo) usados pela WEEKLY.
  recurrenceDays: number[]
  // Dia do mês (1..31) usado pela MONTHLY; meses mais curtos usam o último dia.
  recurrenceDayOfMonth: number | null
  // Conclusões esperadas por semana na WEEKLY_GOAL.
  weeklyGoal: number | null
  projectId: string | null
  stageId: string | null
  dailyStatistics: DailyTaskStatistics | null
  dailyCompletions: DailyTaskCompletion[]
}

export interface TaskInput {
  title: string
  startDate: string | null
  endDate: string | null
  type: TaskType
  status?: TaskStatus
  projectId?: string | null
  stageId?: string | null
  recurrenceDays?: number[] | null
  recurrenceDayOfMonth?: number | null
  weeklyGoal?: number | null
}

// `UpdateTaskDTO`: todos os campos são opcionais. `completionDate` (date) diz qual dia da
// recorrência está sendo concluído ou desfeito; sem ele o back usa o dia atual.
export interface TaskUpdate {
  title?: string
  type?: TaskType
  startDate?: string | null
  endDate?: string | null
  status?: TaskStatus
  completionDate?: string | null
  projectId?: string | null
  stageId?: string | null
  recurrenceDays?: number[] | null
  recurrenceDayOfMonth?: number | null
  weeklyGoal?: number | null
}

export interface TaskPage {
  total: number
  page: number
  size: number
  items: Task[]
}

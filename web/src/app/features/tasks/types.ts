export type TaskStatus = 'PENDING' | 'WORKING' | 'DONE' | 'DELAYED' | 'DELETED'
export type TaskType = 'UNIQUE' | 'DAILY'

// Agregados que o backend expõe para tarefas DAILY: a conclusão é salva dia a dia
// no servidor, mas só chega no front como resumo.
export interface DailyTaskStatistics {
  scheduledDays: number
  completedDays: number
  skippedDays: number
  completedToday: boolean
}

// Um dia concluído de uma tarefa DAILY. É o que permite pintar o histórico no calendário.
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
  // o calendário mostra a tarefa só nesse dia. DAILY não usa (tem `dailyCompletions`).
  completedDate: string | null
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
}

// `UpdateTaskDTO`: todos os campos são opcionais. `completionDate` (date) diz qual dia da
// recorrência da DAILY está sendo concluído ou desfeito; sem ele o back usa o dia atual.
export interface TaskUpdate {
  title?: string
  type?: TaskType
  startDate?: string | null
  endDate?: string | null
  status?: TaskStatus
  completionDate?: string | null
  projectId?: string | null
  stageId?: string | null
}

export interface TaskPage {
  total: number
  page: number
  size: number
  items: Task[]
}

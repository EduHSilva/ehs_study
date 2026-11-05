import { getStoredUserId, httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import { addDays, dayKey, startOfDay, taskEnd, taskStart } from './calendar'
import type { DailyTaskCompletion, DailyTaskStatistics, Task, TaskInput, TaskPage, TaskStatus, TaskType, TaskUpdate } from './types'

const RANGE_PAGE_SIZE = 100
const MAX_RANGE_PAGES = 50

// O filtro de período é documentado como `yyyy-MM-dd`, então formatamos pela data local. Usar
// `toISOString()` aqui deslocaria o dia em fusos negativos (UTC-3 manda a data de "hoje" como
// ontem) e a busca voltaria truncada sem erro visível.
const dayParam = (date: Date): string => dayKey(date)

interface DailyStatsPayload {
  scheduledDays?: number | null
  completedDays?: number | null
  skippedDays?: number | null
  completedToday?: boolean | null
}

interface DailyCompletionPayload {
  completionDate?: string | null
  completedAt?: string | null
}

interface TaskPayload {
  id: string
  createdDate?: string | null
  title: string
  startDate?: string | null
  endDate?: string | null
  type?: string | null
  status?: string | null
  dailyStatistics?: DailyStatsPayload | null
  dailyCompletions?: DailyCompletionPayload[] | null
}

interface TaskPagePayload {
  total?: number
  page?: number
  size?: number
  items?: TaskPayload[]
}

const TASK_STATUSES: TaskStatus[] = ['PENDING', 'DONE', 'DELAYED', 'DELETED']
const TASK_TYPES: TaskType[] = ['UNIQUE', 'DAILY']

function taskHeaders(): Record<string, string> {
  const userId = getStoredUserId()
  return userId ? { 'X-User-ID': userId } : {}
}

function toStatus(value: string | null | undefined): TaskStatus {
  return TASK_STATUSES.find(status => status === value) ?? 'PENDING'
}

function toType(value: string | null | undefined): TaskType {
  return TASK_TYPES.find(type => type === value) ?? 'UNIQUE'
}

function toDailyStats(payload: DailyStatsPayload | null | undefined): DailyTaskStatistics | null {
  if (!payload) return null
  return {
    scheduledDays: payload.scheduledDays ?? 0,
    completedDays: payload.completedDays ?? 0,
    skippedDays: payload.skippedDays ?? 0,
    completedToday: payload.completedToday === true
  }
}

function toDailyCompletions(payload: DailyCompletionPayload[] | null | undefined): DailyTaskCompletion[] {
  if (!Array.isArray(payload)) return []
  const result: DailyTaskCompletion[] = []
  for (const entry of payload) {
    if (!entry?.completionDate) continue
    result.push({
      completionDate: entry.completionDate,
      completedAt: entry.completedAt ?? null
    })
  }
  return result
}

function toTask(payload: TaskPayload): Task {
  return {
    id: payload.id,
    title: payload.title,
    createdDate: payload.createdDate ?? null,
    startDate: payload.startDate ?? null,
    endDate: payload.endDate ?? null,
    type: toType(payload.type),
    status: toStatus(payload.status),
    dailyStatistics: toDailyStats(payload.dailyStatistics),
    dailyCompletions: toDailyCompletions(payload.dailyCompletions)
  }
}

export async function getTasks(page = 0, size = 10): Promise<TaskPage> {
  const res = await httpClient<DefaultResponse<TaskPagePayload>>(`/tasks?page=${page}&size=${size}`, {
    headers: taskHeaders()
  })
  const payload = unwrap(res)
  return {
    total: payload.total ?? 0,
    page: payload.page ?? page,
    size: payload.size ?? size,
    items: (payload.items ?? []).map(toTask)
  }
}

async function fetchAllPages(extraQuery: string, size: number): Promise<Task[]> {
  const collected: Task[] = []
  let page = 0
  for (;;) {
    const query = `page=${page}&size=${size}${extraQuery}`
    const res = await httpClient<DefaultResponse<TaskPagePayload>>(`/tasks?${query}`, { headers: taskHeaders() })
    const payload = unwrap(res)
    const items = (payload.items ?? []).map(toTask)
    collected.push(...items)
    if (items.length === 0 || collected.length >= (payload.total ?? collected.length) || page >= MAX_RANGE_PAGES) break
    page += 1
  }
  return collected
}

// Filtro por período: `GET /tasks` aceita `start`/`end` (date-time) além de `page`/`size`.
// O front mantém o filtro no cliente também, porque a `UNIQUE` ocupa um intervalo e o filtro do
// back é por um campo só. Percorre todas as páginas porque o calendário precisa do período inteiro.
//
// Hoje o back responde 500 sempre que `start`/`end` recebem valor (`could not determine data type
// of parameter`), então a chamada com filtro é tentada primeiro e, se falhar, o front refaz sem os
// params e filtra no cliente — o calendário continua funcionando antes do back ser corrigido.
export async function getTasksInRange(from: Date, to: Date, size = RANGE_PAGE_SIZE): Promise<Task[]> {
  // O back escolhe o dia pelo calendário UTC e o front pelo calendário local. Uma task criada às
  // 00:30 de 02/10 UTC é "ontem" para quem está em UTC-3, e o filtro do back a responde em 02/10
  // enquanto o dia local dela é 01/10. Se pedido pelo dia local, o back não devolveria essa task
  // e ela sumiria do calendário. Por isso a consulta sai com um dia de folga de cada lado, e quem
  // decide o dia é o filtro local abaixo — que usa o calendário que o usuário enxerga.
  const rangeQuery = `&start=${dayParam(addDays(from, -1))}&end=${dayParam(addDays(to, 1))}`
  let collected: Task[]
  try {
    collected = await fetchAllPages(rangeQuery, size)
  } catch {
    // O filtro é opcional e o back assume `[hoje, +7 dias]` quando ele não vem, o que
    // truncaria a lista e o histórico da DAILY. Na reserva pedimos uma janela larga e explícita
    // e deixamos o recorte por conta do filtro local abaixo.
    const wideFrom = dayKey(new Date(from.getFullYear() - 2, 0, 1))
    const wideTo = dayKey(new Date(to.getFullYear() + 2, 11, 31))
    collected = await fetchAllPages(`&start=${wideFrom}&end=${wideTo}`, size)
  }
  const fromMs = startOfDay(from).getTime()
  const toMs = startOfDay(to).getTime()
  return collected.filter(task => {
    if (task.status === 'DELETED') return false
    if (task.type === 'DAILY') {
      const start = taskStart(task)
      return !start || startOfDay(start).getTime() <= toMs
    }
    const taskFrom = taskStart(task)
    const taskTo = taskEnd(task)
    if (!taskFrom || !taskTo) return false
    const a = startOfDay(taskFrom).getTime()
    const b = startOfDay(taskTo).getTime()
    return b >= fromMs && a <= toMs
  })
}

export async function createTask(input: TaskInput): Promise<Task> {
  const res = await httpClient<DefaultResponse<TaskPayload>>('/tasks', {
    method: 'POST',
    headers: taskHeaders(),
    body: JSON.stringify({
      title: input.title,
      startDate: input.startDate,
      endDate: input.endDate,
      type: input.type
    })
  })
  return toTask(unwrap(res))
}

// Atualização parcial (`UpdateTaskDTO`). Só os campos enviados vão no corpo.
export async function updateTask(input: TaskUpdate & { id: string }): Promise<Task> {
  const body: Record<string, unknown> = {}
  if (input.title !== undefined) body.title = input.title
  if (input.type !== undefined) body.type = input.type
  if (input.startDate !== undefined) body.startDate = input.startDate
  if (input.endDate !== undefined) body.endDate = input.endDate
  if (input.status !== undefined) body.status = input.status
  if (input.completionDate) body.completionDate = input.completionDate
  const res = await httpClient<DefaultResponse<TaskPayload>>(`/tasks/${input.id}`, {
    method: 'PUT',
    headers: taskHeaders(),
    body: JSON.stringify(body)
  })
  return toTask(unwrap(res))
}

export async function deleteTask(id: string): Promise<void> {
  await httpClient<DefaultResponse<null>>(`/tasks/${id}`, { method: 'DELETE', headers: taskHeaders() })
}
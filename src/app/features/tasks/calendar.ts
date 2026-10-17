import type { Task } from './types'

export type CalendarMode = 'week' | 'month'
export type DayStatus = 'empty' | 'done' | 'partial' | 'missed'

const WEEK_STARTS_ON = 1

export interface DaySummary {
  key: string
  date: Date
  total: number
  done: number
  status: DayStatus
}

export interface CalendarDay extends DaySummary {
  dayNumber: number
  outside: boolean
}

// Uma task do dia, com a conclusão apurada *para aquele dia*.
export interface DayTask {
  task: Task
  done: boolean
}

export function dayKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function parseDayKey(key: string): Date {
  const [year, month, day] = key.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function addDays(date: Date, days: number): Date {
  const next = startOfDay(date)
  next.setDate(next.getDate() + days)
  return next
}

export function addMonths(date: Date, months: number): Date {
  return new Date(date.getFullYear(), date.getMonth() + months, 1)
}

export function startOfWeek(date: Date): Date {
  const day = startOfDay(date)
  return addDays(day, -((day.getDay() - WEEK_STARTS_ON + 7) % 7))
}

export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function endOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0)
}

function statusOf(total: number, done: number): DayStatus {
  if (total === 0) return 'empty'
  if (done === total) return 'done'
  if (done === 0) return 'missed'
  return 'partial'
}

function parse(value: string | null | undefined): Date | null {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

// `completionDate` vem como `date` (ex.: "2026-10-07"), mas pode vir como date-time. Devolve a
// chave do dia; strings `YYYY-MM-DD` são usadas direto, senão `new Date` interpretaria como UTC
// e o dia local poderia virar o anterior em fusos negativos.
export function dayKeyFrom(value: string | null | undefined): string | null {
  if (!value) return null
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value
  const date = parse(value)
  return date ? dayKey(date) : null
}

function completionKeys(task: Task): string[] {
  const keys: string[] = []
  for (const entry of task.dailyCompletions ?? []) {
    const key = dayKeyFrom(entry.completionDate)
    if (key) keys.push(key)
  }
  return keys
}

// Conclusão de uma DAILY em um dia específico. O backend manda `dailyCompletions` com um registro
// por dia concluído; `completedToday` só cobre hoje e fica como reserva quando a lista vem vazia.
export function isCompletedOn(task: Task, key: string, today = ''): boolean {
  const reference = today || dayKey(new Date())
  const keys = completionKeys(task)
  if (keys.length > 0) return keys.includes(key)
  return key === reference && task.dailyStatistics?.completedToday === true
}

// Início válido da task: `startDate`, senão a data de criação, senão o próprio `endDate`
// (dado legado, em que a task ocupa um único dia).
export function taskStart(task: Task): Date | null {
  return parse(task.startDate) ?? parse(task.createdDate) ?? parse(task.endDate)
}

// Fim da task: `endDate` quando houver, senão o próprio início (virou dia único).
export function taskEnd(task: Task): Date | null {
  return parse(task.endDate) ?? taskStart(task)
}

// Tarefas que valem para um dia, já com a conclusão apurada para aquele dia.
//
// - `UNIQUE` ocupa todos os dias entre `startDate` e `endDate`; concluída = `status === 'DONE'`.
// - `DAILY` está agendada todos os dias a partir do início e concluída quando o dia está em
//   `dailyCompletions` (o backend salva a conclusão dia a dia).
export function tasksForDay(tasks: Task[], key: string, today: string): DayTask[] {
  if (today === '') today = dayKey(new Date())
  const dayMs = parseDayKey(key).getTime()
  const result: DayTask[] = []
  for (const task of tasks) {
    if (task.status === 'DELETED') continue
    if (task.type === 'DAILY') {
      const start = taskStart(task)
      if (start && startOfDay(start).getTime() > dayMs) continue
      result.push({ task, done: isCompletedOn(task, key, today) })
      continue
    }
    const from = taskStart(task)
    const to = taskEnd(task)
    if (!from || !to) continue
    const fromMs = startOfDay(from).getTime()
    const toMs = startOfDay(to).getTime()
    if (dayMs < fromMs || dayMs > toMs) continue
    result.push({ task, done: task.status === 'DONE' })
  }
  return result
}

export function buildDaySummaries(tasks: Task[], from: Date, to: Date, today = ''): DaySummary[] {
  const reference = today || dayKey(new Date())
  const summaries: DaySummary[] = []
  const cursor = startOfDay(from)
  const last = startOfDay(to)
  while (cursor.getTime() <= last.getTime()) {
    const key = dayKey(cursor)
    const list = tasksForDay(tasks, key, reference)
    const done = list.filter(entry => entry.done).length
    summaries.push({ key, date: new Date(cursor), total: list.length, done, status: statusOf(list.length, done) })
    cursor.setDate(cursor.getDate() + 1)
  }
  return summaries
}

export function buildCalendarGrid(tasks: Task[], anchor: Date, mode: CalendarMode, today = ''): CalendarDay[] {
  const reference = today || dayKey(new Date())
  if (mode === 'month') {
    const monthStart = startOfMonth(anchor)
    const month = monthStart.getMonth()
    const from = startOfWeek(monthStart)
    const to = addDays(startOfWeek(endOfMonth(anchor)), 6)
    return buildDaySummaries(tasks, from, to, reference).map(day => ({
      ...day,
      dayNumber: day.date.getDate(),
      outside: day.date.getMonth() !== month
    }))
  }
  const from = startOfWeek(anchor)
  const to = addDays(from, 6)
  return buildDaySummaries(tasks, from, to, reference).map(day => ({
    ...day,
    dayNumber: day.date.getDate(),
    outside: false
  }))
}

export function rangeFor(anchor: Date, mode: CalendarMode): { from: Date; to: Date } {
  if (mode === 'month') return { from: startOfMonth(anchor), to: endOfMonth(anchor) }
  const from = startOfWeek(anchor)
  return { from, to: addDays(from, 6) }
}

// A grade do mês é preenchida com dias vizinhos para fechar as 7 colunas, então a busca
// precisa cobrir esse padding também; senão as bordas ficam sem tarefas.
export function gridRangeFor(anchor: Date, mode: CalendarMode): { from: Date; to: Date } {
  if (mode === 'month') {
    return { from: startOfWeek(startOfMonth(anchor)), to: addDays(startOfWeek(endOfMonth(anchor)), 6) }
  }
  const from = startOfWeek(anchor)
  return { from, to: addDays(from, 6) }
}

// Um dia "casa" com o filtro da lista quando a task ocupa aquele dia.
export function taskCoversDay(task: Task, key: string, today = ''): boolean {
  return tasksForDay([task], key, today).length > 0
}

// Conclusão de uma task no dia de referência. Para `DAILY` isso é o `dailyCompletions`; para
// `UNIQUE`, o `status` (que vale para todos os dias do intervalo).
export function isTaskDone(task: Task, today = ''): boolean {
  const reference = today || dayKey(new Date())
  if (task.type === 'DAILY') return isCompletedOn(task, reference, reference)
  return task.status === 'DONE'
}


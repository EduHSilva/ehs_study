import { addDays, dayKey, dayKeyFrom, parseDayKey, scheduledOn, startOfDay, startOfWeek, taskEnd, taskStart } from './calendar'
import type { Task } from './types'

export interface TaskStats {
  task: Task
  completed: number
  failed: number
  scheduled: number
  rate: number
  weeksOk: number
  weeksFailed: number
  currentWeekDone: number
  currentWeekGoal: number | null
  currentWeekMet: boolean
}

const completionSet = (task: Task): Set<string> => {
  const keys = new Set<string>()
  for (const entry of task.dailyCompletions ?? []) {
    const key = dayKeyFrom(entry.completionDate)
    if (key) keys.add(key)
  }
  return keys
}

const later = (first: Date, second: Date): Date =>
  startOfDay(first).getTime() > startOfDay(second).getTime() ? startOfDay(first) : startOfDay(second)

const earlier = (first: Date, second: Date): Date =>
  startOfDay(first).getTime() < startOfDay(second).getTime() ? startOfDay(first) : startOfDay(second)

// Estatísticas de uma tarefa recorrente dentro do intervalo pedido.
//
// - DAILY/WEEKLY/MONTHLY: conta só os dias agendados; `failed` são os dias passados que não
//   tiveram conclusão (hoje ainda não conta como falha).
// - WEEKLY_GOAL: avalia semanas fechadas (meta atingida ou não) e mostra o progresso da semana
//   corrente separadamente.
export function buildTaskStats(tasks: Task[], from: Date, to: Date, today = ''): TaskStats[] {
  const reference = today || dayKey(new Date())
  const yesterdayKey = dayKey(addDays(parseDayKey(reference), -1))
  const result: TaskStats[] = []

  for (const task of tasks) {
    if (task.status === 'DELETED' || task.type === 'UNIQUE') continue
    const start = taskStart(task)
    if (!start) continue
    const end = taskEnd(task) ?? to
    const first = later(start, from)
    const last = earlier(end, to)
    if (first.getTime() > last.getTime()) continue

    const done = completionSet(task)
    const firstKey = dayKey(first)
    const lastKey = dayKey(last)

    let completed = 0
    for (const key of done) {
      if (key >= firstKey && key <= lastKey) completed += 1
    }

    if (task.type === 'WEEKLY_GOAL') {
      const goal = task.weeklyGoal ?? 1
      let weeksOk = 0
      let weeksFailed = 0
      let currentWeekDone = 0
      let currentWeekMet = false
      for (let cursor = startOfWeek(first); cursor.getTime() <= startOfWeek(last).getTime(); cursor = addDays(cursor, 7)) {
        const weekFromKey = dayKey(cursor) < firstKey ? firstKey : dayKey(cursor)
        const weekToKey = dayKey(addDays(cursor, 6)) > lastKey ? lastKey : dayKey(addDays(cursor, 6))
        // A primeira semana pode começar antes do início da task e sobrar sem nenhum dia válido.
        if (weekFromKey > weekToKey) continue
        let weekDone = 0
        for (const key of done) {
          if (key >= weekFromKey && key <= weekToKey) weekDone += 1
        }
        const isCurrentWeek = weekFromKey <= reference && reference <= weekToKey
        if (isCurrentWeek) {
          currentWeekDone = weekDone
          currentWeekMet = weekDone >= goal
        } else if (weekToKey < reference) {
          if (weekDone >= goal) weeksOk += 1
          else weeksFailed += 1
        }
      }
      const evaluated = weeksOk + weeksFailed
      result.push({
        task,
        completed,
        failed: weeksFailed,
        scheduled: evaluated * goal,
        rate: evaluated ? Math.round((weeksOk / evaluated) * 100) : 0,
        weeksOk,
        weeksFailed,
        currentWeekDone,
        currentWeekGoal: goal,
        currentWeekMet
      })
      continue
    }

    let scheduled = 0
    let scheduledPast = 0
    let scheduledToDate = 0
    let completedPast = 0
    let completedToDate = 0
    for (let cursor = new Date(first); cursor.getTime() <= last.getTime(); cursor = addDays(cursor, 1)) {
      const key = dayKey(cursor)
      if (!scheduledOn(task, key)) continue
      scheduled += 1
      if (key <= yesterdayKey) {
        scheduledPast += 1
        if (done.has(key)) completedPast += 1
      }
      // A adesão considera o dia de hoje, mas uma falha só é contada depois que o dia fecha.
      if (key <= reference) {
        scheduledToDate += 1
        if (done.has(key)) completedToDate += 1
      }
    }
    result.push({
      task,
      completed,
      failed: Math.max(0, scheduledPast - completedPast),
      scheduled,
      rate: scheduledToDate ? Math.round((completedToDate / scheduledToDate) * 100) : 0,
      weeksOk: 0,
      weeksFailed: 0,
      currentWeekDone: 0,
      currentWeekGoal: null,
      currentWeekMet: false
    })
  }

  return result
}

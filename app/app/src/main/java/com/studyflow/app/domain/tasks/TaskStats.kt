package com.studyflow.app.domain.tasks

import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import java.time.LocalDate

data class TaskStats(
    val task: Task,
    val completed: Int,
    val failed: Int,
    val scheduled: Int,
    val rate: Int,
    val weeksOk: Int,
    val weeksFailed: Int,
    val currentWeekDone: Int,
    val currentWeekGoal: Int?,
    val currentWeekMet: Boolean,
)

/**
 * Estatísticas de tarefas recorrentes, portadas de `src/app/features/tasks/taskStats.ts`.
 *
 * - DAILY/WEEKLY/MONTHLY: conta só os dias agendados; `failed` são os dias passados sem
 *   conclusão (hoje ainda não conta como falha).
 * - WEEKLY_GOAL: avalia semanas fechadas (meta atingida ou não) e mostra o progresso da semana
 *   corrente separadamente.
 */
object TaskStatsBuilder {
    fun build(tasks: List<Task>, from: LocalDate, to: LocalDate, today: String): List<TaskStats> {
        val reference = if (today.isBlank()) StudyCalendar.dayKey(LocalDate.now()) else today
        val referenceDay = StudyCalendar.parseDayKey(reference)
        val yesterday = StudyCalendar.dayKey(referenceDay.minusDays(1))
        val result = ArrayList<TaskStats>()

        for (task in tasks) {
            if (task.status == TaskStatus.DELETED || task.type == TaskType.UNIQUE) continue
            val start = StudyCalendar.taskStart(task) ?: continue
            val end = StudyCalendar.taskEnd(task) ?: to
            val first = if (start.isAfter(from)) start else from
            val last = if (end.isBefore(to)) end else to
            if (first.isAfter(last)) continue

            val done = completionKeys(task)
            val firstKey = StudyCalendar.dayKey(first)
            val lastKey = StudyCalendar.dayKey(last)
            val completed = done.count { it in firstKey..lastKey }

            if (task.type == TaskType.WEEKLY_GOAL) {
                val goal = task.weeklyGoal ?: 1
                var weeksOk = 0
                var weeksFailed = 0
                var currentWeekDone = 0
                var currentWeekMet = false
                var cursor = StudyCalendar.startOfWeek(first)
                while (!cursor.isAfter(StudyCalendar.startOfWeek(last))) {
                    val weekEnd = cursor.plusDays(6)
                    val weekFromKey = maxOf(StudyCalendar.dayKey(cursor), firstKey)
                    val weekToKey = minOf(StudyCalendar.dayKey(weekEnd), lastKey)
                    if (weekFromKey > weekToKey) {
                        cursor = cursor.plusWeeks(1)
                        continue
                    }
                    val weekDone = done.count { it in weekFromKey..weekToKey }
                    val isCurrentWeek = weekFromKey <= reference && reference <= weekToKey
                    when {
                        isCurrentWeek -> {
                            currentWeekDone = weekDone
                            currentWeekMet = weekDone >= goal
                        }
                        weekToKey < reference -> if (weekDone >= goal) weeksOk++ else weeksFailed++
                    }
                    cursor = cursor.plusWeeks(1)
                }
                val evaluated = weeksOk + weeksFailed
                result += TaskStats(
                    task = task,
                    completed = completed,
                    failed = weeksFailed,
                    scheduled = evaluated * goal,
                    rate = if (evaluated > 0) Math.round(weeksOk.toFloat() / evaluated * 100) else 0,
                    weeksOk = weeksOk,
                    weeksFailed = weeksFailed,
                    currentWeekDone = currentWeekDone,
                    currentWeekGoal = goal,
                    currentWeekMet = currentWeekMet,
                )
                continue
            }

            var scheduled = 0
            var scheduledPast = 0
            var scheduledToDate = 0
            var completedPast = 0
            var completedToDate = 0
            var cursor = first
            while (!cursor.isAfter(last)) {
                val key = StudyCalendar.dayKey(cursor)
                cursor = cursor.plusDays(1)
                if (!StudyCalendar.scheduledOn(task, key)) continue
                scheduled++
                if (key <= yesterday) {
                    scheduledPast++
                    if (key in done) completedPast++
                }
                // A adesão considera o dia de hoje, mas uma falha só é contada depois que ele fecha.
                if (key <= reference) {
                    scheduledToDate++
                    if (key in done) completedToDate++
                }
            }
            result += TaskStats(
                task = task,
                completed = completed,
                failed = maxOf(0, scheduledPast - completedPast),
                scheduled = scheduled,
                rate = if (scheduledToDate > 0) Math.round(completedToDate.toFloat() / scheduledToDate * 100) else 0,
                weeksOk = 0,
                weeksFailed = 0,
                currentWeekDone = 0,
                currentWeekGoal = null,
                currentWeekMet = false,
            )
        }

        return result
    }

    private fun completionKeys(task: Task): Set<String> =
        task.dailyCompletions.mapNotNull { ApiDates.dayKeyFrom(it.completionDate) }.toSet()
}

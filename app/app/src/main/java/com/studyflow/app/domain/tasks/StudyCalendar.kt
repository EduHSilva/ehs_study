package com.studyflow.app.domain.tasks

import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import com.studyflow.app.domain.ApiDates
import java.time.LocalDate

/**
 * Regras de calendário/lista portadas de `src/app/features/tasks/calendar.ts`.
 * Tudo é puro (sem Android) para poder ser testado na JVM.
 */
object StudyCalendar {
    enum class CalendarMode { WEEK, MONTH }

    enum class DayStatus(val key: String) {
        EMPTY("empty"),
        DONE("done"),
        PARTIAL("partial"),
        MISSED("missed"),
    }

    data class DaySummary(
        val key: String,
        val date: LocalDate,
        val total: Int,
        val done: Int,
        val status: DayStatus,
    )

    data class CalendarDay(
        val key: String,
        val date: LocalDate,
        val total: Int,
        val done: Int,
        val status: DayStatus,
        val dayNumber: Int,
        val outside: Boolean,
    )

    data class DayTask(val task: Task, val done: Boolean)

    data class WeeklyGoalProgress(val done: Int, val goal: Int, val met: Boolean)

    fun dayKey(date: LocalDate): String = date.toString()

    fun parseDayKey(key: String): LocalDate = LocalDate.parse(key)

    fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays(((date.dayOfWeek.value - 1) + 7) % 7L)

    fun startOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(1)

    fun endOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(date.lengthOfMonth())

    private fun statusOf(total: Int, done: Int): DayStatus = when {
        total == 0 -> DayStatus.EMPTY
        done == total -> DayStatus.DONE
        done == 0 -> DayStatus.MISSED
        else -> DayStatus.PARTIAL
    }

    private fun completionKeys(task: Task): List<String> =
        task.dailyCompletions.mapNotNull { ApiDates.dayKeyFrom(it.completionDate) }

    /**
     * Conclusão de uma DAILY em um dia específico. `completedToday` só cobre hoje e fica como
     * reserva quando o back não devolve a lista de dias.
     */
    fun isCompletedOn(task: Task, key: String, today: String): Boolean {
        val reference = today.ifBlank { dayKey(LocalDate.now()) }
        val keys = completionKeys(task)
        if (keys.isNotEmpty()) return key in keys
        return key == reference && task.dailyStatistics?.completedToday == true
    }

    /** Início válido: `startDate`, senão a criação, senão o próprio `endDate` (dado legado). */
    fun taskStart(task: Task): LocalDate? =
        ApiDates.localDateOrNull(task.startDate)
            ?: ApiDates.localDateOrNull(task.createdDate)
            ?: ApiDates.localDateOrNull(task.endDate)

    /** Fim: `endDate` quando houver, senão o próprio início (vira dia único). */
    fun taskEnd(task: Task): LocalDate? =
        ApiDates.localDateOrNull(task.endDate) ?: taskStart(task)

    /** ISO weekday: 1 = segunda .. 7 = domingo. */
    fun isoWeekday(date: LocalDate): Int = date.dayOfWeek.value

    /**
     * Se a tarefa está agendada para o dia. UNIQUE e DAILY ocupam todo o intervalo; WEEKLY segue
     * os dias da semana escolhidos; MONTHLY cai no dia do mês (meses curtos usam o último dia);
     * WEEKLY_GOAL pode ser concluída em qualquer dia do intervalo.
     */
    fun scheduledOn(task: Task, key: String): Boolean {
        val day = parseDayKey(key)
        taskStart(task)?.let { if (it.isAfter(day)) return false }
        ApiDates.localDateOrNull(task.endDate)?.let { if (it.isBefore(day)) return false }
        return when (task.type) {
            TaskType.UNIQUE, TaskType.DAILY -> true
            TaskType.WEEKLY -> isoWeekday(day) in task.recurrenceDays
            TaskType.MONTHLY -> {
                val configured = task.recurrenceDayOfMonth ?: return false
                day.dayOfMonth == minOf(configured, day.lengthOfMonth())
            }
            TaskType.WEEKLY_GOAL -> true
        }
    }

    /** Progresso da meta semanal na semana que contém [key] (segunda a domingo). */
    fun weeklyGoalProgress(task: Task, key: String): WeeklyGoalProgress {
        val goal = task.weeklyGoal ?: 1
        val monday = startOfWeek(parseDayKey(key))
        val from = dayKey(monday)
        val to = dayKey(monday.plusDays(6))
        val done = completionKeys(task).count { it in from..to }
        return WeeklyGoalProgress(done = done, goal = goal, met = done >= goal)
    }

    /** Só dá para concluir/desfazer no dia quando ele faz parte da recorrência. */
    fun canComplete(task: Task, key: String): Boolean =
        task.type == TaskType.UNIQUE || scheduledOn(task, key)

    /**
     * - `UNIQUE` pendente ocupa todos os dias entre início e fim; concluída, ocupa só o dia de
     *   `completedDate` (tarefas antigas sem a data continuam ocupando o intervalo todo).
     * - Recorrentes (DAILY/WEEKLY/MONTHLY) aparecem só nos dias agendados, concluídas quando o
     *   dia está em `dailyCompletions`.
     * - `WEEKLY_GOAL` aparece pendente em todos os dias da semana até a meta ser atingida; depois
     *   disso sobram apenas os dias efetivamente concluídos, e a semana seguinte recomeça cheia.
     */
    fun tasksForDay(tasks: List<Task>, key: String, today: String): List<DayTask> {
        val day = parseDayKey(key)
        val result = ArrayList<DayTask>()
        for (task in tasks) {
            if (task.status == TaskStatus.DELETED) continue
            if (task.type != TaskType.UNIQUE) {
                if (!scheduledOn(task, key)) continue
                val done = isCompletedOn(task, key, today)
                if (task.type == TaskType.WEEKLY_GOAL && !done && weeklyGoalProgress(task, key).met) continue
                result += DayTask(task, done)
                continue
            }
            val from = taskStart(task) ?: continue
            val to = taskEnd(task) ?: continue
            if (day.isBefore(from) || day.isAfter(to)) continue
            if (task.status == TaskStatus.DONE) {
                val completedOn = ApiDates.dayKeyFrom(task.completedDate)
                if (completedOn != null && completedOn != key) continue
            }
            result += DayTask(task, task.status == TaskStatus.DONE)
        }
        return result
    }

    fun buildDaySummaries(tasks: List<Task>, from: LocalDate, to: LocalDate, today: String): List<DaySummary> {
        val summaries = ArrayList<DaySummary>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            val key = dayKey(cursor)
            val list = tasksForDay(tasks, key, today)
            val done = list.count { it.done }
            summaries += DaySummary(key, cursor, list.size, done, statusOf(list.size, done))
            cursor = cursor.plusDays(1)
        }
        return summaries
    }

    fun buildCalendarGrid(
        tasks: List<Task>,
        anchor: LocalDate,
        mode: CalendarMode,
        today: String,
    ): List<CalendarDay> {
        if (mode == CalendarMode.MONTH) {
            val month = anchor.month
            val from = startOfWeek(startOfMonth(anchor))
            val to = startOfWeek(endOfMonth(anchor)).plusDays(6)
            return buildDaySummaries(tasks, from, to, today).map { day ->
                CalendarDay(
                    key = day.key,
                    date = day.date,
                    total = day.total,
                    done = day.done,
                    status = day.status,
                    dayNumber = day.date.dayOfMonth,
                    outside = day.date.month != month,
                )
            }
        }
        val from = startOfWeek(anchor)
        val to = from.plusDays(6)
        return buildDaySummaries(tasks, from, to, today).map { day ->
            CalendarDay(
                key = day.key,
                date = day.date,
                total = day.total,
                done = day.done,
                status = day.status,
                dayNumber = day.date.dayOfMonth,
                outside = false,
            )
        }
    }

    fun rangeFor(anchor: LocalDate, mode: CalendarMode): Pair<LocalDate, LocalDate> =
        if (mode == CalendarMode.MONTH) startOfMonth(anchor) to endOfMonth(anchor)
        else startOfWeek(anchor).let { it to it.plusDays(6) }

    /** A grade do mês inclui dias vizinhos para fechar as 7 colunas; a busca cobre o padding. */
    fun gridRangeFor(anchor: LocalDate, mode: CalendarMode): Pair<LocalDate, LocalDate> =
        if (mode == CalendarMode.MONTH) {
            startOfWeek(startOfMonth(anchor)) to startOfWeek(endOfMonth(anchor)).plusDays(6)
        } else {
            startOfWeek(anchor).let { it to it.plusDays(6) }
        }

    /** Um dia "casa" com o filtro da lista quando a task ocupa aquele dia. */
    fun taskCoversDay(task: Task, key: String, today: String): Boolean =
        tasksForDay(listOf(task), key, today).isNotEmpty()

    fun isTaskDone(task: Task, today: String): Boolean {
        val reference = today.ifBlank { dayKey(LocalDate.now()) }
        return if (task.type != TaskType.UNIQUE) {
            isCompletedOn(task, reference, reference)
        } else {
            task.status == TaskStatus.DONE
        }
    }
}

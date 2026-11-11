package com.studyflow.app.domain.kanban

import com.studyflow.app.data.model.DailyTaskCompletion
import com.studyflow.app.data.model.DailyTaskStatistics
import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStatus
import com.studyflow.app.data.model.Task
import com.studyflow.app.data.model.TaskStatus
import com.studyflow.app.data.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KanbanBoardTest {

    private val today = "2026-11-11"
    private val now = "2026-11-11T12:00:00Z"

    private fun task(
        id: String = "task",
        status: TaskStatus = TaskStatus.PENDING,
        type: TaskType = TaskType.UNIQUE,
        startDate: String? = "2026-11-01T09:00:00Z",
        endDate: String? = "2026-11-30T09:00:00Z",
        completedDate: String? = null,
        dailyStatistics: DailyTaskStatistics? = null,
        dailyCompletions: List<DailyTaskCompletion> = emptyList(),
    ) = Task(
        id = id,
        title = id,
        createdDate = null,
        startDate = startDate,
        endDate = endDate,
        type = type,
        status = status,
        completedDate = completedDate,
        projectId = null,
        stageId = null,
        dailyStatistics = dailyStatistics,
        dailyCompletions = dailyCompletions,
    )

    private fun project(id: String, status: ProjectStatus) = Project(
        id = id,
        title = id,
        progress = 0,
        status = status,
        advisor = "",
    )

    @Test
    fun `conclusao efetiva da diaria vence o status pendente`() {
        val daily = task(
            id = "diaria",
            type = TaskType.DAILY,
            status = TaskStatus.PENDING,
            dailyCompletions = listOf(DailyTaskCompletion(today, null)),
        )

        assertEquals(TaskColumn.DONE, KanbanBoard.taskColumn(daily, today))
    }

    @Test
    fun `tarefa atrasada fica na coluna de atrasadas`() {
        val delayed = task(id = "atrasada", status = TaskStatus.DELAYED)

        assertEquals(TaskColumn.DELAYED, KanbanBoard.taskColumn(delayed, today))
    }

    @Test
    fun `tarefa pendente fica na coluna de pendentes`() {
        val pending = task(id = "pendente")

        assertEquals(TaskColumn.PENDING, KanbanBoard.taskColumn(pending, today))
    }

    @Test
    fun `tarefa em andamento fica na coluna de em andamento`() {
        val working = task(id = "andamento", status = TaskStatus.WORKING)

        assertEquals(TaskColumn.WORKING, KanbanBoard.taskColumn(working, today))
    }

    @Test
    fun `colunas de tarefas seguem a ordem pendente andamento atrasada concluida`() {
        val columns = KanbanBoard.taskColumns(
            listOf(
                task(id = "feita", status = TaskStatus.DONE),
                task(id = "atrasada", status = TaskStatus.DELAYED),
                task(id = "andamento", status = TaskStatus.WORKING),
                task(id = "pendente"),
            ),
            today,
        )

        assertEquals(
            listOf(TaskColumn.PENDING, TaskColumn.WORKING, TaskColumn.DELAYED, TaskColumn.DONE),
            columns.map { it.column },
        )
        assertEquals(listOf("pendente"), columns[0].items.map { it.id })
        assertEquals(listOf("andamento"), columns[1].items.map { it.id })
        assertEquals(listOf("atrasada"), columns[2].items.map { it.id })
        assertEquals(listOf("feita"), columns[3].items.map { it.id })
    }

    @Test
    fun `projetos sao agrupados por status`() {
        val columns = KanbanBoard.projectColumns(
            listOf(
                project("concluido", ProjectStatus.DONE),
                project("andamento", ProjectStatus.WORKING),
                project("pendente", ProjectStatus.WAITING),
            ),
        )

        assertEquals(
            listOf(ProjectColumn.WAITING, ProjectColumn.WORKING, ProjectColumn.DONE),
            columns.map { it.column },
        )
        assertEquals(listOf("pendente"), columns[0].items.map { it.id })
        assertEquals(listOf("andamento"), columns[1].items.map { it.id })
        assertEquals(listOf("concluido"), columns[2].items.map { it.id })
    }

    @Test
    fun `mover tarefa unica para concluida envia o dia de hoje`() {
        val unique = task(id = "unica")

        val move = KanbanBoard.planTaskMove(unique, TaskColumn.DONE, today, now)!!

        assertEquals(TaskStatus.DONE, move.status)
        assertEquals(today, move.completionDate)
        assertEquals(today, move.optimistic.completedDate)
    }

    @Test
    fun `mover tarefa unica para atrasada nao envia conclusao`() {
        val unique = task(
            id = "unica",
            status = TaskStatus.DONE,
            endDate = "2026-11-01T09:00:00Z",
            completedDate = today,
        )

        val move = KanbanBoard.planTaskMove(unique, TaskColumn.DELAYED, today, now)!!

        assertEquals(TaskStatus.DELAYED, move.status)
        assertNull(move.completionDate)
        assertNull(move.optimistic.completedDate)
    }

    @Test
    fun `mover tarefa unica fora do prazo para atrasada e invalido`() {
        val unique = task(id = "unica", endDate = "2026-11-30T09:00:00Z")

        assertNull(KanbanBoard.planTaskMove(unique, TaskColumn.DELAYED, today, now))
    }

    @Test
    fun `mover tarefa unica para em andamento limpa a conclusao`() {
        val unique = task(id = "unica", status = TaskStatus.DONE, completedDate = today)

        val move = KanbanBoard.planTaskMove(unique, TaskColumn.WORKING, today, now)!!

        assertEquals(TaskStatus.WORKING, move.status)
        assertNull(move.optimistic.completedDate)
    }

    @Test
    fun `mover diaria sem intervalo nao envia completionDate`() {
        val daily = task(
            id = "diaria",
            type = TaskType.DAILY,
            startDate = null,
            endDate = null,
        )

        val move = KanbanBoard.planTaskMove(daily, TaskColumn.DONE, today, now)!!

        assertEquals(TaskStatus.DONE, move.status)
        assertNull(move.completionDate)
        assertEquals(listOf(today), move.optimistic.dailyCompletions.map { it.completionDate })
    }

    @Test
    fun `mover diaria com intervalo registra a conclusao do dia`() {
        val daily = task(
            id = "diaria",
            type = TaskType.DAILY,
            dailyStatistics = DailyTaskStatistics(
                scheduledDays = 10,
                completedDays = 0,
                skippedDays = 0,
                completedToday = false,
            ),
        )

        val move = KanbanBoard.planTaskMove(daily, TaskColumn.DONE, today, now)!!

        assertEquals(today, move.completionDate)
        assertEquals(listOf(today), move.optimistic.dailyCompletions.map { it.completionDate })
        assertTrue(move.optimistic.dailyStatistics?.completedToday == true)
    }

    @Test
    fun `mover diaria para atrasada e invalido`() {
        val daily = task(id = "diaria", type = TaskType.DAILY)

        assertNull(KanbanBoard.planTaskMove(daily, TaskColumn.DELAYED, today, now))
    }

    @Test
    fun `mover diaria para em andamento limpa a conclusao de hoje`() {
        val daily = task(
            id = "diaria",
            type = TaskType.DAILY,
            dailyStatistics = DailyTaskStatistics(
                scheduledDays = 10,
                completedDays = 1,
                skippedDays = 0,
                completedToday = true,
            ),
            dailyCompletions = listOf(DailyTaskCompletion(today, null)),
        )

        val move = KanbanBoard.planTaskMove(daily, TaskColumn.WORKING, today, now)!!

        assertEquals(TaskStatus.WORKING, move.status)
        assertEquals(today, move.completionDate)
        assertTrue(move.optimistic.dailyCompletions.isEmpty())
        assertTrue(move.optimistic.dailyStatistics?.completedToday == false)
    }

    @Test
    fun `mover projeto para concluido muda o status`() {
        val current = project("projeto", ProjectStatus.WORKING)

        val moved = KanbanBoard.planProjectMove(current, ProjectColumn.DONE)

        assertEquals(ProjectStatus.DONE, moved.status)
    }

    @Test
    fun `mover projeto para andamento muda o status`() {
        val current = project("projeto", ProjectStatus.WAITING)

        val moved = KanbanBoard.planProjectMove(current, ProjectColumn.WORKING)

        assertEquals(ProjectStatus.WORKING, moved.status)
    }

    @Test
    fun `mover projeto para pendente muda o status`() {
        val current = project("projeto", ProjectStatus.DONE)

        val moved = KanbanBoard.planProjectMove(current, ProjectColumn.WAITING)

        assertEquals(ProjectStatus.WAITING, moved.status)
    }
}

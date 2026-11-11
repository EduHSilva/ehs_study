import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getProjects, updateProject } from '../projects/projects.api'
import { dayKey, dayKeyFrom, isTaskDone } from '../tasks/calendar'
import { getAllTasks, updateTask } from '../tasks/tasks.api'
import type { Project, ProjectStatus } from '../projects/types'
import type { Task, TaskStatus } from '../tasks/types'

export type KanbanEntity = 'tasks' | 'projects'
export type TaskColumn = 'PENDING' | 'WORKING' | 'DELAYED' | 'DONE'
export type ProjectColumn = 'WAITING' | 'WORKING' | 'DONE'

export interface TaskColumnView {
  key: TaskColumn
  labelKey: string
  tone: string
  items: Task[]
}

export interface ProjectColumnView {
  key: ProjectColumn
  labelKey: string
  tone: string
  items: Project[]
}

// O Kanban carrega o acervo inteiro (sem recorte de período) e agrupa no cliente. A coluna da
// tarefa prioriza a conclusão efetiva — numa DAILY isso depende do dia, não do `status` — e só
// depois olha o atraso, o que mantém a mesma semântica da lista/calendário.
export function useKanban() {
  const { t } = useI18n()

  const entity = ref<KanbanEntity>('tasks')
  const tasks = ref<Task[]>([])
  const projects = ref<Project[]>([])
  const loading = ref(true)
  const error = ref<string | null>(null)
  const savingId = ref<string | null>(null)
  const tasksLoaded = ref(false)
  const projectsLoaded = ref(false)
  const todayKey = ref(dayKey(new Date()))

  // O atraso é derivado do prazo no backend: só faz sentido mover para DELAYED quando a tarefa
  // já venceu. DAILY nunca tem atraso.
  const isTaskOverdue = (task: Task): boolean => {
    if (task.type === 'DAILY') return false
    const end = dayKeyFrom(task.endDate)
    return end !== null && end < todayKey.value
  }

  const taskColumn = (task: Task): TaskColumn => {
    if (isTaskDone(task, todayKey.value)) return 'DONE'
    if (task.status === 'DELAYED') return 'DELAYED'
    if (task.status === 'WORKING') return 'WORKING'
    return 'PENDING'
  }

  const projectColumn = (project: Project): ProjectColumn => {
    if (project.status === 'Concluído') return 'DONE'
    return project.status === 'Em andamento' ? 'WORKING' : 'WAITING'
  }

  const taskColumns = computed<TaskColumnView[]>(() => {
    const groups: Record<TaskColumn, Task[]> = { PENDING: [], WORKING: [], DELAYED: [], DONE: [] }
    for (const task of tasks.value) groups[taskColumn(task)].push(task)
    return [
      { key: 'PENDING', labelKey: 'kanbanTasksPending', tone: 'pending', items: groups.PENDING },
      { key: 'WORKING', labelKey: 'kanbanTasksWorking', tone: 'progress', items: groups.WORKING },
      { key: 'DELAYED', labelKey: 'kanbanTasksDelayed', tone: 'delayed', items: groups.DELAYED },
      { key: 'DONE', labelKey: 'kanbanTasksDone', tone: 'done', items: groups.DONE }
    ]
  })

  const projectColumns = computed<ProjectColumnView[]>(() => {
    const groups: Record<ProjectColumn, Project[]> = { WAITING: [], WORKING: [], DONE: [] }
    for (const project of projects.value) groups[projectColumn(project)].push(project)
    return [
      { key: 'WAITING', labelKey: 'kanbanProjectsWaiting', tone: 'pending', items: groups.WAITING },
      { key: 'WORKING', labelKey: 'kanbanProjectsInProgress', tone: 'progress', items: groups.WORKING },
      { key: 'DONE', labelKey: 'kanbanProjectsDone', tone: 'done', items: groups.DONE }
    ]
  })

  const load = async () => {
    loading.value = true
    error.value = null
    try {
      // As duas listas são carregadas juntas para a troca de aba ser instantânea e para os cartões
      // de tarefa mostrarem o nome do projeto. Uma falha não derruba a outra.
      const [taskResult, projectResult] = await Promise.allSettled([getAllTasks(), getProjects()])
      if (taskResult.status === 'fulfilled') {
        tasks.value = taskResult.value
        tasksLoaded.value = true
      }
      if (projectResult.status === 'fulfilled') {
        projects.value = projectResult.value
        projectsLoaded.value = true
      }
      const currentFailed = entity.value === 'tasks'
        ? taskResult.status === 'rejected'
        : projectResult.status === 'rejected'
      if (currentFailed) error.value = t('kanbanLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const setEntity = async (next: KanbanEntity) => {
    if (entity.value === next) return
    entity.value = next
    error.value = null
    const loaded = next === 'tasks' ? tasksLoaded.value : projectsLoaded.value
    if (!loaded) await load()
  }

  const applyTask = (id: string, next: Task) => {
    tasks.value = tasks.value.map(task => (task.id === id ? next : task))
  }

  const moveTask = async (id: string, column: TaskColumn) => {
    const task = tasks.value.find(item => item.id === id)
    if (!task || savingId.value === id) return
    if (taskColumn(task) === column) return
    // A coluna DELAYED só aceita tarefa vencida (e a DAILY nunca atrasa): o back recalcula o
    // atraso pelo prazo, então um movimento manual fora do prazo voltaria para PENDING.
    if (column === 'DELAYED' && !isTaskOverdue(task)) return

    const previous = { ...task }
    const today = todayKey.value
    const done = column === 'DONE'
    const status: TaskStatus = column === 'DELAYED'
      ? 'DELAYED'
      : column === 'WORKING' ? 'WORKING' : done ? 'DONE' : 'PENDING'
    // O back só aceita `completionDate` explícito numa DAILY quando ela tem intervalo; sem ele,
    // o dia corrente fica implícito. Numa UNIQUE o dia acompanha a conclusão/reação.
    const completionDate = task.type === 'DAILY'
      ? ((task.startDate || task.endDate) ? today : null)
      : (column === 'DELAYED' ? null : today)

    const optimistic: Task = {
      ...task,
      status,
      completedDate: task.type === 'DAILY' ? task.completedDate : (done ? today : null),
      dailyStatistics: task.type === 'DAILY' && task.dailyStatistics
        ? { ...task.dailyStatistics, completedToday: done }
        : task.dailyStatistics,
      dailyCompletions: task.type === 'DAILY'
        ? (done
          ? [...task.dailyCompletions.filter(entry => dayKeyFrom(entry.completionDate) !== today),
             { completionDate: today, completedAt: new Date().toISOString() }]
          : task.dailyCompletions.filter(entry => dayKeyFrom(entry.completionDate) !== today))
        : task.dailyCompletions
    }

    applyTask(id, optimistic)
    savingId.value = id
    error.value = null
    try {
      const saved = await updateTask({ id, status, completionDate })
      // O PUT não devolve o histórico completo da DAILY; preserva o que o quadro já tinha.
      applyTask(id, task.type === 'DAILY'
        ? { ...saved, dailyCompletions: optimistic.dailyCompletions }
        : saved)
    } catch {
      applyTask(id, previous)
      error.value = t('kanbanMoveFailed')
    } finally {
      savingId.value = null
    }
  }

  const moveProject = async (id: string, column: ProjectColumn) => {
    const project = projects.value.find(item => item.id === id)
    if (!project || savingId.value === id) return
    if (projectColumn(project) === column) return

    const previous = { ...project }
    const status: ProjectStatus = column === 'DONE'
      ? 'Concluído'
      : column === 'WORKING' ? 'Em andamento' : 'Pendente'
    projects.value = projects.value.map(item => (item.id === id ? { ...item, status } : item))
    savingId.value = id
    error.value = null
    try {
      const saved = await updateProject(id, {
        title: project.title,
        advisor: project.advisor,
        status: column === 'DONE' ? 'DONE' : column === 'WORKING' ? 'WORKING' : 'WAITING'
      })
      projects.value = projects.value.map(item => (item.id === id ? saved.project : item))
    } catch {
      projects.value = projects.value.map(item => (item.id === id ? previous : item))
      error.value = t('kanbanMoveFailed')
    } finally {
      savingId.value = null
    }
  }

  return {
    entity,
    tasks,
    projects,
    loading,
    error,
    savingId,
    todayKey,
    taskColumns,
    projectColumns,
    taskColumn,
    projectColumn,
    isTaskOverdue,
    load,
    setEntity,
    moveTask,
    moveProject
  }
}

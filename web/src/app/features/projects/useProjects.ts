import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createProject as createProjectRequest, createStage as createStageRequest, createVersion as createVersionRequest, deleteProject as deleteProjectRequest, getProjectDetails, getProjects, updateProject as updateProjectRequest, updateStage, updateVersion as updateVersionRequest } from './projects.api'
import { getTasksForProject } from '../tasks/tasks.api'
import { isTaskDone } from '../tasks/calendar'
import type { Task } from '../tasks/types'
import type { Project, ProjectDetails } from './types'

// Concluídos ficam no fim; os em andamento em ordem alfabética.
const sortProjects = (list: Project[]): Project[] =>
  [...list].sort((a, b) => {
    const aDone = a.status === 'Concluído'
    const bDone = b.status === 'Concluído'
    if (aDone !== bDone) return aDone ? 1 : -1
    return a.title.localeCompare(b.title, undefined, { sensitivity: 'base' })
  })

export function useProjects() {
  const { t } = useI18n()
  const projects = ref<Project[]>([])
  const details = ref<ProjectDetails | null>(null)
  const selectedId = ref<string | null>(null)
  const loading = ref(true)
  const savingStageId = ref<string | null>(null)
  const savingVersionId = ref<string | null>(null)
  const savingNewStage = ref(false)
  const savingNewVersion = ref(false)
  const savingProject = ref(false)
  const error = ref<string | null>(null)
  const projectTasks = ref<Task[]>([])
  const tasksLoading = ref(false)
  const tasksError = ref<string | null>(null)

  const tasksByStage = computed(() => {
    const map = new Map<string, Task[]>()
    for (const task of projectTasks.value) {
      const key = task.stageId ?? ''
      const list = map.get(key)
      if (list) list.push(task)
      else map.set(key, [task])
    }
    return map
  })

  const stageTasks = (stageId: string) => tasksByStage.value.get(stageId) ?? []
  const unassignedTasks = computed(() => tasksByStage.value.get('') ?? [])

  const loadTasks = async (id: string) => {
    tasksLoading.value = true
    tasksError.value = null
    try {
      projectTasks.value = await getTasksForProject(id)
    } catch {
      projectTasks.value = []
      tasksError.value = t('projectTasksLoadFailed')
    } finally {
      tasksLoading.value = false
    }
  }

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      projects.value = sortProjects(await getProjects())
      if (projects.value[0]) await selectProject(projects.value[0].id)
    } catch {
      error.value = t('projectsLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const selectProject = async (id: string) => {
    selectedId.value = id
    projectTasks.value = []
    tasksError.value = null
    try {
      details.value = await getProjectDetails(id)
    } catch {
      error.value = t('projectDetailsLoadFailed')
      return
    }
    await loadTasks(id)
  }

  const syncProgress = () => {
    if (!details.value) return
    const total = details.value.stages.length
    const progress = total ? Math.round(details.value.stages.filter(stage => stage.done).length / total * 100) : 0
    details.value.project.progress = progress
    const listProject = projects.value.find(project => project.id === details.value?.project.id)
    if (listProject) listProject.progress = progress
  }

  const createProject = async (title: string, advisor: string) => {
    if (!title.trim()) {
      error.value = t('projectTitleRequired')
      return false
    }
    savingProject.value = true
    error.value = null
    try {
      const created = await createProjectRequest(title.trim(), advisor.trim())
      projects.value = sortProjects([created.project, ...projects.value])
      details.value = created
      selectedId.value = created.project.id
      projectTasks.value = []
      return true
    } catch {
      error.value = t('projectCreateFailed')
      return false
    } finally {
      savingProject.value = false
    }
  }

  const applyProjectUpdate = (updated: ProjectDetails) => {
    details.value = updated
    projects.value = sortProjects(projects.value.map(project => (project.id === updated.project.id ? updated.project : project)))
  }

  const saveProject = async (title: string, advisor: string): Promise<boolean> => {
    if (!selectedId.value) return false
    if (!title.trim()) {
      error.value = t('projectTitleRequired')
      return false
    }
    savingProject.value = true
    error.value = null
    try {
      applyProjectUpdate(await updateProjectRequest(selectedId.value, { title: title.trim(), advisor: advisor.trim() }))
      return true
    } catch {
      error.value = t('projectEditFailed')
      return false
    } finally {
      savingProject.value = false
    }
  }

  const toggleProjectStatus = async (): Promise<boolean> => {
    if (!details.value || !selectedId.value) return false
    const done = details.value.project.status === 'Concluído'
    savingProject.value = true
    error.value = null
    try {
      applyProjectUpdate(await updateProjectRequest(selectedId.value, {
        title: details.value.project.title,
        advisor: details.value.project.advisor,
        status: done ? 'WAITING' : 'DONE'
      }))
      return true
    } catch {
      error.value = t('projectUpdateFailed')
      return false
    } finally {
      savingProject.value = false
    }
  }

  const removeProject = async (): Promise<boolean> => {
    if (!selectedId.value) return false
    const id = selectedId.value
    savingProject.value = true
    error.value = null
    try {
      await deleteProjectRequest(id)
      projects.value = projects.value.filter(project => project.id !== id)
      details.value = null
      selectedId.value = null
      projectTasks.value = []
      const next = projects.value[0]
      if (next) await selectProject(next.id)
      return true
    } catch {
      error.value = t('projectDeleteFailed')
      return false
    } finally {
      savingProject.value = false
    }
  }

  const toggleStage = async (stageId: string) => {
    if (!details.value || !selectedId.value) return
    const stage = details.value.stages.find(entry => entry.id === stageId)
    if (!stage) return
    savingStageId.value = stageId
    try {
      Object.assign(stage, await updateStage(selectedId.value, stageId, stage.name, !stage.done))
      syncProgress()
    } catch {
      error.value = t('stageSaveFailed')
    } finally {
      savingStageId.value = null
    }
  }

  const createStage = async (title: string) => {
    if (!details.value || !selectedId.value || !title.trim()) return false
    savingNewStage.value = true
    error.value = null
    try {
      const stage = await createStageRequest(selectedId.value, title.trim())
      details.value.stages.push(stage)
      syncProgress()
      return true
    } catch {
      error.value = t('stageCreateFailed')
      return false
    } finally {
      savingNewStage.value = false
    }
  }

  const createVersion = async (title: string, dueDate: string) => {
    if (!details.value || !selectedId.value || !title.trim() || !dueDate) return false
    savingNewVersion.value = true
    error.value = null
    try {
      const version = await createVersionRequest(selectedId.value, title.trim(), new Date(dueDate).toISOString())
      details.value.versions.push(version)
      return true
    } catch {
      error.value = t('versionCreateFailed')
      return false
    } finally {
      savingNewVersion.value = false
    }
  }

  const updateVersion = async (versionId: string, title: string, dueDate: string) => {
    if (!details.value || !selectedId.value || !title.trim() || !dueDate) return false
    const version = details.value.versions.find(entry => entry.id === versionId)
    if (!version) return false
    savingVersionId.value = versionId
    error.value = null
    try {
      Object.assign(version, await updateVersionRequest(selectedId.value, versionId, title.trim(), new Date(dueDate).toISOString()))
      return true
    } catch {
      error.value = t('versionSaveFailed')
      return false
    } finally {
      savingVersionId.value = null
    }
  }

  return {
    projects,
    details,
    selectedId,
    loading,
    savingStageId,
    savingVersionId,
    savingNewStage,
    savingNewVersion,
    savingProject,
    error,
    projectTasks,
    tasksLoading,
    tasksError,
    stageTasks,
    unassignedTasks,
    isTaskDone,
    load,
    selectProject,
    loadTasks,
    createProject,
    saveProject,
    toggleProjectStatus,
    removeProject,
    createStage,
    createVersion,
    toggleStage,
    updateVersion
  }
}

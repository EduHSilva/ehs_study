import { ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createMilestone as createMilestoneRequest, createProject as createProjectRequest, createStage as createStageRequest, getProjectDetails, getProjects, updateMilestone as updateMilestoneRequest, updateStage } from './projects.api'
import type { Project, ProjectDetails } from './types'

export function useProjects() {
  const { t } = useI18n()
  const projects = ref<Project[]>([])
  const details = ref<ProjectDetails | null>(null)
  const selectedId = ref<string | null>(null)
  const loading = ref(true)
  const savingStageId = ref<string | null>(null)
  const savingMilestoneId = ref<string | null>(null)
  const savingNewStage = ref(false)
  const savingNewMilestone = ref(false)
  const savingProject = ref(false)
  const error = ref<string | null>(null)

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      projects.value = await getProjects()
      if (projects.value[0]) await selectProject(projects.value[0].id)
    } catch {
      error.value = t('projectsLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const selectProject = async (id: string) => {
    selectedId.value = id
    try {
      details.value = await getProjectDetails(id)
    } catch {
      error.value = t('projectDetailsLoadFailed')
    }
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
      projects.value.unshift(created.project)
      details.value = created
      selectedId.value = created.project.id
      return true
    } catch {
      error.value = t('projectCreateFailed')
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

  const createMilestone = async (title: string, dueDate: string) => {
    if (!details.value || !selectedId.value || !title.trim() || !dueDate) return false
    savingNewMilestone.value = true
    error.value = null
    try {
      const milestone = await createMilestoneRequest(selectedId.value, title.trim(), new Date(dueDate).toISOString())
      details.value.milestones.push(milestone)
      return true
    } catch {
      error.value = t('milestoneCreateFailed')
      return false
    } finally {
      savingNewMilestone.value = false
    }
  }

  const updateMilestone = async (milestoneId: string, title: string, dueDate: string) => {
    if (!details.value || !selectedId.value || !title.trim() || !dueDate) return false
    const milestone = details.value.milestones.find(entry => entry.id === milestoneId)
    if (!milestone) return false
    savingMilestoneId.value = milestoneId
    error.value = null
    try {
      Object.assign(milestone, await updateMilestoneRequest(selectedId.value, milestoneId, title.trim(), new Date(dueDate).toISOString()))
      return true
    } catch {
      error.value = t('milestoneSaveFailed')
      return false
    } finally {
      savingMilestoneId.value = null
    }
  }

  return { projects, details, selectedId, loading, savingStageId, savingMilestoneId, savingNewStage, savingNewMilestone, savingProject, error, load, selectProject, createProject, createStage, createMilestone, toggleStage, updateMilestone }
}

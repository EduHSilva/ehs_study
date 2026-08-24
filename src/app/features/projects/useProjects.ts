import { ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getProjectDetails, getProjects, updateStage } from './projects.api'
import type { Project, ProjectDetails } from './types'

export function useProjects() {
  const { t } = useI18n()
  const projects = ref<Project[]>([])
  const details = ref<ProjectDetails | null>(null)
  const selectedId = ref<string | null>(null)
  const loading = ref(true)
  const savingStageId = ref<string | null>(null)
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

  const toggleStage = async (stageId: string) => {
    if (!details.value || !selectedId.value) return
    const stage = details.value.stages.find(entry => entry.id === stageId)
    if (!stage) return
    savingStageId.value = stageId
    try {
      Object.assign(stage, await updateStage(selectedId.value, stageId, !stage.done))
      const doneCount = details.value.stages.filter(entry => entry.done).length
      details.value.project.progress = Math.round(doneCount / details.value.stages.length * 100)
    } catch {
      error.value = t('stageSaveFailed')
    } finally {
      savingStageId.value = null
    }
  }

  return { projects, details, selectedId, loading, savingStageId, error, load, selectProject, toggleStage }
}

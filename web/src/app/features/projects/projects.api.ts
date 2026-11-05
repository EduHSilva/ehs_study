import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Project, ProjectDetails, ProjectStage } from './types'

const LIST_PAGE_SIZE = 100

interface ProjectMilestonePayload {
  id: string
  title: string
  dueDate: string
}

interface ProjectStagePayload {
  id: string
  name?: string
  title?: string
  done: boolean
}

interface ProjectPayload {
  id: string
  title: string
  status?: string | null
  progress?: number | null
  advisor?: string | null
  stages?: ProjectStagePayload[] | null
  milestone?: ProjectMilestonePayload[] | null
}

interface ProjectPagePayload {
  items?: ProjectPayload[]
}

const STATUS_LABELS: Record<string, Project['status']> = {
  WAITING: 'Em andamento',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluído'
}

function toStatus(value: string | null | undefined): Project['status'] {
  return (value && STATUS_LABELS[value]) || 'Em andamento'
}

function toProject(payload: ProjectPayload): Project {
  return {
    id: payload.id,
    title: payload.title,
    progress: payload.progress ?? 0,
    status: toStatus(payload.status),
    advisor: payload.advisor ?? ''
  }
}

function toProjectDetails(payload: ProjectPayload): ProjectDetails {
  return {
    project: toProject(payload),
    stages: (payload.stages ?? []).map(stage => ({ id: stage.id, name: stage.name ?? stage.title ?? '', done: stage.done === true })),
    milestones: (payload.milestone ?? []).map(milestone => ({
      id: milestone.id,
      title: milestone.title,
      dueDate: milestone.dueDate
    }))
  }
}

export async function getProjects(): Promise<Project[]> {
  const res = await httpClient<DefaultResponse<ProjectPagePayload>>(`/study/projects?page=0&size=${LIST_PAGE_SIZE}`)
  return (unwrap(res).items ?? []).map(toProject)
}

export async function getProjectDetails(id: string): Promise<ProjectDetails> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectPayload>>(`/study/projects/${id}`))
  return toProjectDetails(payload)
}

export async function createProject(title: string, advisor: string): Promise<ProjectDetails> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectPayload>>('/study/projects/new', {
    method: 'POST',
    body: JSON.stringify({ title, status: 'WAITING', advisor: advisor || null })
  }))
  return toProjectDetails(payload)
}

export async function createStage(projectId: string, title: string): Promise<ProjectStage> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectStagePayload>>('/study/project/stage', {
    method: 'POST',
    body: JSON.stringify({ idProject: projectId, title, done: false })
  }))
  return { id: payload.id, name: payload.name ?? payload.title ?? title, done: payload.done === true }
}

export async function createMilestone(projectId: string, title: string, dueDate: string): Promise<ProjectMilestonePayload> {
  return unwrap(await httpClient<DefaultResponse<ProjectMilestonePayload>>('/study/project/milestone', {
    method: 'POST',
    body: JSON.stringify({ idProject: projectId, title, dueDate })
  }))
}

export async function updateStage(projectId: string, stageId: string, title: string, done: boolean): Promise<ProjectStage> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectStagePayload>>(`/study/project/stage/${stageId}`, {
    method: 'PUT',
    body: JSON.stringify({ idProject: projectId, title, done })
  }))
  return { id: payload.id, name: payload.name ?? payload.title ?? title, done: payload.done === true }
}

export async function updateMilestone(projectId: string, milestoneId: string, title: string, dueDate: string): Promise<ProjectMilestonePayload> {
  return unwrap(await httpClient<DefaultResponse<ProjectMilestonePayload>>(`/study/project/milestone/${milestoneId}`, {
    method: 'PUT',
    body: JSON.stringify({ idProject: projectId, title, dueDate })
  }))
}

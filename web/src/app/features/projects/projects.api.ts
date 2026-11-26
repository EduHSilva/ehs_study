import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Project, ProjectDetails, ProjectStage } from './types'

const LIST_PAGE_SIZE = 100

interface ProjectVersionPayload {
  id: string
  title: string
  dueDate: string
}

interface ProjectStagePayload {
  id: string
  name?: string
  title?: string
  done: boolean
  projectProgress?: number | null
}

interface ProjectPayload {
  id: string
  title: string
  status?: string | null
  progress?: number | null
  advisor?: string | null
  link?: string | null
  stages?: ProjectStagePayload[] | null
  versions?: ProjectVersionPayload[] | null
}

interface ProjectStageBulkPayload {
  stages?: ProjectStagePayload[] | null
  projectProgress?: number | null
}

interface ProjectPagePayload {
  items?: ProjectPayload[]
}

const STATUS_LABELS: Record<string, Project['status']> = {
  WAITING: 'Pendente',
  WORKING: 'Em andamento',
  DONE: 'Concluído'
}

function toStatus(value: string | null | undefined): Project['status'] {
  return (value && STATUS_LABELS[value]) || 'Pendente'
}

function toProject(payload: ProjectPayload): Project {
  return {
    id: payload.id,
    title: payload.title,
    progress: payload.progress ?? 0,
    status: toStatus(payload.status),
    advisor: payload.advisor ?? '',
    link: payload.link ?? ''
  }
}

function toStage(payload: ProjectStagePayload, fallbackTitle = ''): ProjectStage {
  return {
    id: payload.id,
    name: payload.name ?? payload.title ?? fallbackTitle,
    done: payload.done === true,
    projectProgress: payload.projectProgress ?? undefined
  }
}

function toProjectDetails(payload: ProjectPayload): ProjectDetails {
  return {
    project: toProject(payload),
    stages: (payload.stages ?? []).map(stage => ({ id: stage.id, name: stage.name ?? stage.title ?? '', done: stage.done === true })),
    versions: (payload.versions ?? []).map(version => ({
      id: version.id,
      title: version.title,
      dueDate: version.dueDate
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

export async function createProject(title: string, advisor: string, link = ''): Promise<ProjectDetails> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectPayload>>('/study/projects', {
    method: 'POST',
    body: JSON.stringify({ title, status: 'WAITING', advisor: advisor || null, link: link || null })
  }))
  return toProjectDetails(payload)
}

// `status` e `link` ausentes preservam os valores atuais no back; link vazio limpa o endereço
// (o formulário de edição envia sempre).
export async function updateProject(
  id: string,
  input: { title: string; advisor: string; link?: string; status?: 'WAITING' | 'WORKING' | 'DONE' }
): Promise<ProjectDetails> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectPayload>>(`/study/projects/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      title: input.title,
      advisor: input.advisor || null,
      ...(input.link !== undefined ? { link: input.link } : {}),
      ...(input.status ? { status: input.status } : {})
    })
  }))
  return toProjectDetails(payload)
}

export async function deleteProject(id: string): Promise<void> {
  await httpClient<DefaultResponse<null>>(`/study/projects/${id}`, { method: 'DELETE' })
}

export async function createStage(projectId: string, title: string): Promise<ProjectStage> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectStagePayload>>('/study/project/stage', {
    method: 'POST',
    body: JSON.stringify({ idProject: projectId, title, done: false })
  }))
  return toStage(payload, title)
}

// Cria as etapas numeradas dos módulos de uma vez; o back devolve todas com o progresso novo.
export async function createStages(
  projectId: string,
  titles: string[]
): Promise<{ stages: ProjectStage[]; projectProgress?: number }> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectStageBulkPayload>>('/study/project/stage/bulk', {
    method: 'POST',
    body: JSON.stringify({ idProject: projectId, titles })
  }))
  return {
    stages: (payload.stages ?? []).map(stage => toStage(stage)),
    projectProgress: payload.projectProgress ?? undefined
  }
}

export async function createVersion(projectId: string, title: string, dueDate: string): Promise<ProjectVersionPayload> {
  return unwrap(await httpClient<DefaultResponse<ProjectVersionPayload>>('/study/project/version', {
    method: 'POST',
    body: JSON.stringify({ idProject: projectId, title, dueDate })
  }))
}

export async function updateStage(projectId: string, stageId: string, title: string, done: boolean): Promise<ProjectStage> {
  const payload = unwrap(await httpClient<DefaultResponse<ProjectStagePayload>>(`/study/project/stage/${stageId}`, {
    method: 'PUT',
    body: JSON.stringify({ idProject: projectId, title, done })
  }))
  return toStage(payload, title)
}

export async function updateVersion(projectId: string, versionId: string, title: string, dueDate: string): Promise<ProjectVersionPayload> {
  return unwrap(await httpClient<DefaultResponse<ProjectVersionPayload>>(`/study/project/version/${versionId}`, {
    method: 'PUT',
    body: JSON.stringify({ idProject: projectId, title, dueDate })
  }))
}

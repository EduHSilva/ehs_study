import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Project, ProjectDetails, ProjectStage } from './types'

export async function getProjects(): Promise<Project[]> {
  return unwrap(await httpClient<DefaultResponse<Project[]>>('/projects'))
}

export async function getProjectDetails(id: string): Promise<ProjectDetails> {
  return unwrap(await httpClient<DefaultResponse<ProjectDetails>>(`/projects/${id}`))
}

export async function updateStage(projectId: string, stageId: string, done: boolean): Promise<ProjectStage> {
  return unwrap(
    await httpClient<DefaultResponse<ProjectStage>>(`/projects/${projectId}/stages/${stageId}`, {
      method: 'PATCH',
      body: JSON.stringify({ done })
    })
  )
}

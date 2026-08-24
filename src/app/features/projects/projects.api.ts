import { httpClient } from '../../composables/httpClient'
import type { Project, ProjectDetails, ProjectStage } from './types'

export async function getProjects(): Promise<Project[]> {
  return httpClient<Project[]>('/projects')
}

export async function getProjectDetails(id: string): Promise<ProjectDetails> {
  return httpClient<ProjectDetails>(`/projects/${id}`)
}

export async function updateStage(projectId: string, stageId: string, done: boolean): Promise<ProjectStage> {
  return httpClient<ProjectStage>(`/projects/${projectId}/stages/${stageId}`, { method: 'PATCH', body: JSON.stringify({ done }) })
}

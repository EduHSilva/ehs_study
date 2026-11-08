import type { APIRequestContext, APIResponse } from '@playwright/test'

export const API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost'
export const E2E_EMAIL = process.env.E2E_EMAIL ?? 'admin@example.com'
export const E2E_PASSWORD = process.env.E2E_PASSWORD ?? 'change-me'

interface Envelope<T> {
  data: T
  message?: string | null
}

interface PagePayload<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface ApiNote {
  id: string
  folderId: string | null
  title: string
  content: string
}

export interface ApiFolder {
  id: string
  name: string
  notes: ApiNote[]
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}` }
}

async function body<T>(res: APIResponse): Promise<T> {
  if (!res.ok()) {
    throw new Error(`API ${res.status()} ${res.url()}: ${await res.text()}`)
  }
  const payload = (await res.json()) as Envelope<T>
  return payload.data
}

export async function login(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${API_BASE_URL}/users/auth/login`, {
    data: { email: E2E_EMAIL, password: E2E_PASSWORD }
  })
  const session = await body<{ token: string | null }>(res)
  if (!session.token) throw new Error('Login real não retornou token')
  return session.token
}

export async function listFolders(request: APIRequestContext, token: string): Promise<ApiFolder[]> {
  return (await body<PagePayload<ApiFolder>>(await request.get(`${API_BASE_URL}/study/notes/folders?page=0&size=100`, { headers: auth(token) }))).items
}

export async function createFolder(request: APIRequestContext, token: string, name: string): Promise<ApiFolder> {
  return body<ApiFolder>(await request.post(`${API_BASE_URL}/study/notes/folders`, {
    headers: auth(token),
    data: { name }
  }))
}

export async function getFolder(request: APIRequestContext, token: string, id: string): Promise<ApiFolder> {
  return body<ApiFolder>(await request.get(`${API_BASE_URL}/study/notes/folders/${id}`, { headers: auth(token) }))
}

export async function listFocusSessions(request: APIRequestContext, token: string, date?: string): Promise<APIResponse> {
  const query = date ? `&date=${encodeURIComponent(date)}` : ''
  return request.get(`${API_BASE_URL}/study/focus?page=0&size=100${query}`, { headers: auth(token) })
}

export async function createFocusSession(request: APIRequestContext, token: string, title: string, minutes: number): Promise<APIResponse> {
  return request.post(`${API_BASE_URL}/study/focus`, { headers: auth(token), data: { title, minutes } })
}

export async function createNote(
  request: APIRequestContext,
  token: string,
  folderId: string,
  note: { title: string; content: string }
): Promise<ApiNote> {
  return body<ApiNote>(await request.post(`${API_BASE_URL}/study/notes/${folderId}`, {
    headers: auth(token),
    data: note
  }))
}

export async function deleteFolderQuietly(request: APIRequestContext, token: string, id: string): Promise<void> {
  await request.delete(`${API_BASE_URL}/study/notes/folders/${id}`, { headers: auth(token) })
}

export interface ApiTask {
  id: string
  title: string
  startDate: string | null
  endDate: string | null
  type: string
  status: string
  completedDate: string | null
  projectId: string | null
  stageId: string | null
  dailyCompletions?: Array<{ completionDate: string; completedAt: string | null }>
}

export interface ApiTaskInput {
  title: string
  startDate: string | null
  endDate: string | null
  type: string
  projectId: string | null
  stageId: string | null
}

export async function createTask(request: APIRequestContext, token: string, data: ApiTaskInput): Promise<ApiTask> {
  return body<ApiTask>(await request.post(`${API_BASE_URL}/tasks`, { headers: auth(token), data }))
}

export async function getTask(request: APIRequestContext, token: string, id: string, timeZone?: string): Promise<ApiTask> {
  const headers = Object.assign(auth(token), timeZone ? { 'X-Time-Zone': timeZone } : {})
  return body<ApiTask>(await request.get(`${API_BASE_URL}/tasks/${id}`, { headers }))
}

export async function deleteTaskQuietly(request: APIRequestContext, token: string, id: string): Promise<void> {
  await request.delete(`${API_BASE_URL}/tasks/${id}`, { headers: auth(token) })
}

export function uniqueSuffix(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`
}

export interface ApiProject {
  id: string
  title: string
  status: string
  advisor: string
  progress?: number
  stages?: Array<{ id: string; name: string; done: boolean }>
  versions?: Array<{ id: string; title: string; dueDate: string }>
}

export async function createProject(
  request: APIRequestContext,
  token: string,
  title: string,
  advisor: string
): Promise<ApiProject> {
  return body<ApiProject>(await request.post(`${API_BASE_URL}/study/projects`, {
    headers: auth(token),
    data: { title, status: 'WAITING', advisor: advisor || null }
  }))
}

export async function getProject(request: APIRequestContext, token: string, id: string): Promise<ApiProject> {
  return body<ApiProject>(await request.get(`${API_BASE_URL}/study/projects/${id}`, { headers: auth(token) }))
}

export async function deleteProjectQuietly(request: APIRequestContext, token: string, id: string): Promise<void> {
  await request.delete(`${API_BASE_URL}/study/projects/${id}`, { headers: auth(token) })
}

import { resetSessionCache } from './useSessionCache'

export const AUTH_STORAGE_KEY = 'studyflow-auth'
export const UNAUTHORIZED_EVENT = 'studyflow:unauthorized'

export function clearStoredSession() {
  try {
    localStorage.removeItem(AUTH_STORAGE_KEY)
  } catch {
    /* noop */
  }
}

export function notifyUnauthorized() {
  clearStoredSession()
  resetSessionCache()
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
  }
}

export function getAuthToken(): string | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    const token = parsed && typeof parsed === 'object' ? (parsed as { token?: unknown }).token : undefined
    return typeof token === 'string' && token ? token : null
  } catch {
    return null
  }
}

export function getStoredUserId(): string | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    const id = parsed && typeof parsed === 'object' ? (parsed as { user?: { id?: unknown } }).user?.id : undefined
    return typeof id === 'string' && id ? id : null
  } catch {
    return null
  }
}

// Padrão de resposta da API: tudo chega embrulhado em DefaultResponse e quem consome
// desembrulha com `unwrap`. Listas paginadas (quando existem) vêm em `data.items`.
export interface DefaultResponse<T> {
  message?: string | null
  data?: T | null
  links?: Record<string, string> | null
}

export function unwrap<T>(res: DefaultResponse<T>): T {
  if (res && res.data !== undefined && res.data !== null) return res.data
  throw new Error(res?.message ?? 'Resposta inesperada da API')
}

function clientTimeZone(): string | null {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || null
  } catch {
    return null
  }
}

// O back resolve os dias de calendário no fuso do cliente (`X-Time-Zone`); sem isso o
// servidor usa o próprio fuso (UTC) e uma diária criada à noite no Brasil começa no dia
// seguinte, fazendo a conclusão de "hoje" ser recusada com 400.
function timeZoneHeader(): Record<string, string> {
  const zone = clientTimeZone()
  return zone ? { 'X-Time-Zone': zone } : {}
}

export async function httpClient<T>(path: string, init?: RequestInit): Promise<T> {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL
  if (!configuredBaseUrl) throw new Error('VITE_API_BASE_URL não configurada')
  // "/" significa same-origin: o nginx dos microservices encaminha /users, /tasks
  // e /study para o gateway. Sem remover a barra final evita "//users".
  const baseUrl = configuredBaseUrl.replace(/\/+$/, '')
  const token = getAuthToken()
  const headers = {
    'Content-Type': 'application/json',
    ...timeZoneHeader(),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...init?.headers
  }
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers })
  if (response.status === 401) notifyUnauthorized()
  if (!response.ok) throw new Error(`Falha na API (${response.status})`)
  return response.json() as Promise<T>
}

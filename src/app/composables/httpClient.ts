export const AUTH_STORAGE_KEY = 'studyflow-auth'

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

export async function httpClient<T>(path: string, init?: RequestInit): Promise<T> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  if (!baseUrl) throw new Error('VITE_API_BASE_URL não configurada')
  const token = getAuthToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...init?.headers
  }
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers })
  if (!response.ok) throw new Error(`Falha na API (${response.status})`)
  return response.json() as Promise<T>
}

import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { FocusSession } from './types'

const LIST_PAGE_SIZE = 100

interface FocusSessionPayload {
  id: string
  title: string
  minutes: number
  completedAt?: string | null
}

interface FocusPagePayload {
  items?: FocusSessionPayload[]
}

function toSession(payload: FocusSessionPayload): FocusSession {
  return { id: payload.id, title: payload.title, minutes: payload.minutes, completedAt: payload.completedAt ?? null }
}

// Focus usa um dia de calendário (yyyy-MM-dd), resolvido no fuso do cliente (`X-Time-Zone`).
export async function getTodaySessions(): Promise<FocusSession[]> {
  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const res = await httpClient<DefaultResponse<FocusPagePayload>>(`/study/focus?page=0&size=${LIST_PAGE_SIZE}&date=${today}`)
  return (unwrap(res).items ?? []).map(toSession)
}

export async function createFocusSession(title: string, minutes: number): Promise<FocusSession> {
  const res = await httpClient<DefaultResponse<FocusSessionPayload>>('/study/focus', {
    method: 'POST',
    body: JSON.stringify({ title, minutes })
  })
  return toSession(unwrap(res))
}

import { httpClient } from '../../composables/httpClient'
import type { FocusSession } from './types'

export async function getTodaySessions(): Promise<FocusSession[]> {
  return httpClient<FocusSession[]>('/pomodoro/sessions')
}

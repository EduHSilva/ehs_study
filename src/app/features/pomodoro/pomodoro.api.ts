import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { FocusSession } from './types'

export async function getTodaySessions(): Promise<FocusSession[]> {
  return unwrap(await httpClient<DefaultResponse<FocusSession[]>>('/pomodoro/sessions'))
}

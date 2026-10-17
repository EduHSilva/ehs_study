import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { ReviewReport } from './types'

export async function getReviewReport(): Promise<ReviewReport> {
  return unwrap(await httpClient<DefaultResponse<ReviewReport>>('/review/report'))
}

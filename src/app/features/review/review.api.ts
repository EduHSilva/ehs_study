import { httpClient } from '../../composables/httpClient'
import type { ReviewReport } from './types'

export async function getReviewReport(): Promise<ReviewReport> {
  return httpClient<ReviewReport>('/review/report')
}

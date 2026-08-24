import { httpClient } from '../../composables/httpClient'
import type { DashboardData } from './types'

export async function getDashboard(): Promise<DashboardData> {
  return httpClient<DashboardData>('/dashboard')
}

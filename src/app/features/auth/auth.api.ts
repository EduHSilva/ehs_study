import { httpClient } from '../../composables/httpClient'
import type { AuthSession, Credentials } from './types'

export async function login(credentials: Credentials): Promise<AuthSession> {
  return httpClient<AuthSession>('/users/auth/login', { method: 'POST', body: JSON.stringify(credentials) })
}

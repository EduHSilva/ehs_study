import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { AuthSession, LoginPayload, RegisterInput, RegisterPayload } from './types'

function toAuthUser(payload: LoginPayload): AuthSession['user'] {
  return {
    id: payload.id,
    name: payload.username,
    email: payload.email
  }
}

function toSession(res: DefaultResponse<LoginPayload>): AuthSession {
  const payload = unwrap(res)
  if (!payload.token) throw new Error('Login sem token')
  return { token: payload.token, user: toAuthUser(payload) }
}

export async function login(credentials: { email: string; password: string }): Promise<AuthSession> {
  const res = await httpClient<DefaultResponse<LoginPayload>>('/users/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials)
  })
  return toSession(res)
}

// O mesmo `POST /auth/login`: o `AuthRequestDTO` do back aceita `tokenGmail` no lugar de
// email/senha. O front manda o ID token (JWT) que o Google Identity Services devolve.
export async function loginWithGoogle(idToken: string): Promise<AuthSession> {
  const res = await httpClient<DefaultResponse<LoginPayload>>('/users/auth/login', {
    method: 'POST',
    body: JSON.stringify({ tokenGmail: idToken })
  })
  return toSession(res)
}

export async function me(): Promise<AuthSession> {
  const res = await httpClient<DefaultResponse<LoginPayload>>('/users/auth/me')
  const payload = unwrap(res)
  return { token: '', user: toAuthUser(payload) }
}

export async function register(input: RegisterInput): Promise<RegisterPayload> {
  const res = await httpClient<DefaultResponse<RegisterPayload>>('/users/auth/register', {
    method: 'POST',
    body: JSON.stringify(input)
  })
  return unwrap(res)
}

export async function updateUserName(id: string, username: string): Promise<RegisterPayload> {
  const res = await httpClient<DefaultResponse<RegisterPayload>>(`/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ username })
  })
  return unwrap(res)
}

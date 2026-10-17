export interface Credentials { email: string; password: string }

export interface AuthUser {
  id: string
  name: string
  email: string
  course?: string
  avatarUrl?: string
}

export interface AuthSession { token: string; user: AuthUser }

export interface RegisterInput {
  email: string
  username: string
  password: string
  role: string
}

export interface LoginPayload {
  id: string
  username: string
  email: string
  lastLogin: string | null
  token: string | null
}

export interface RegisterPayload {
  id: string
  createdDate: string | null
  email: string
  username: string
  status: string
  roles: string[]
  companyName: string | null
}

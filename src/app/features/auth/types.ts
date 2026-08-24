export interface Credentials { email: string; password: string }
export interface AuthUser { name: string; email: string; course?: string; avatarUrl?: string }
export interface AuthSession { token: string; user: AuthUser }

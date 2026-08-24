import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { AUTH_STORAGE_KEY } from '../../composables/httpClient'
import { currentPage } from '../../composables/useNavigation'
import { login as loginRequest } from './auth.api'
import type { AuthSession, Credentials } from './types'

function readStoredSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as AuthSession
    return parsed && typeof parsed.token === 'string' && !!parsed.user ? parsed : null
  } catch {
    return null
  }
}

function persistSession(session: AuthSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

function clearStoredSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const validateCredentials = (credentials: Credentials): string | null => {
  const { t } = useI18n()
  if (!EMAIL_PATTERN.test(credentials.email)) return t('invalidEmail')
  if (credentials.password.length < 6) return t('passwordTooShort')
  return null
}

const session = ref<AuthSession | null>(readStoredSession())
const pending = ref(false)
const error = ref<string | null>(null)
const isAuthenticated = computed(() => session.value !== null)

async function login(email: string, password: string): Promise<boolean> {
  const credentials = { email: email.trim(), password }
  const invalidReason = validateCredentials(credentials)
  if (invalidReason) {
    error.value = invalidReason
    return false
  }
  pending.value = true
  error.value = null
  try {
    session.value = await loginRequest(credentials)
    persistSession(session.value)
    currentPage.value = 'dashboard'
    return true
  } catch {
    error.value = useI18n().t('loginFailed')
    return false
  } finally {
    pending.value = false
  }
}

function logout() {
  clearStoredSession()
  session.value = null
  currentPage.value = 'dashboard'
}

export function useAuth() {
  return { user: computed(() => session.value?.user ?? null), isAuthenticated, pending, error, login, logout }
}

const FALLBACK_AVATAR = '/assets/avatar.png'

export function useStudentProfile() {
  const { user } = useAuth()
  const { t } = useI18n()
  const student = computed(() => ({
    name: user.value?.name ?? t('studentFallback'),
    course: user.value?.course ?? '—',
    avatarUrl: user.value?.avatarUrl || FALLBACK_AVATAR
  }))
  return { student }
}

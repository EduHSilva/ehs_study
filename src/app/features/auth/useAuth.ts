import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import {
  AUTH_STORAGE_KEY,
  notifyUnauthorized,
  UNAUTHORIZED_EVENT
} from '../../composables/httpClient'
import { currentPage } from '../../composables/useNavigation'
import { login as loginRequest, loginWithGoogle as googleLoginRequest, me as meRequest, register as registerRequest, updateUserName } from './auth.api'
import type { AuthSession, Credentials, RegisterInput } from './types'

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

const registerPending = ref(false)
const registerError = ref<string | null>(null)
const registerSuccess = ref(false)

function doLogout() {
  notifyUnauthorized()
  session.value = null
  currentPage.value = 'dashboard'
}

if (typeof window !== 'undefined') {
  window.addEventListener(UNAUTHORIZED_EVENT, () => {
    session.value = null
    currentPage.value = 'dashboard'
  })
}

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

async function loginWithGoogle(idToken: string): Promise<boolean> {
  pending.value = true
  error.value = null
  try {
    session.value = await googleLoginRequest(idToken)
    persistSession(session.value)
    currentPage.value = 'dashboard'
    return true
  } catch {
    error.value = useI18n().t('googleLoginFailed')
    return false
  } finally {
    pending.value = false
  }
}

function logout() {
  doLogout()
}

async function initAuth(): Promise<void> {
  const stored = readStoredSession()
  if (!stored) return
  try {
    const fresh = await meRequest()
    // `/users/auth/me` não tem schema documentado; se não devolver `id`, preserva o
    // que veio do login — sem ele o header X-User-ID some e a listagem de tasks volta vazia.
    session.value = { token: stored.token, user: { ...fresh.user, id: fresh.user.id || stored.user.id } }
    persistSession(session.value)
  } catch {
    doLogout()
  }
}

async function register(input: Omit<RegisterInput, 'role'>): Promise<boolean> {
  const { t } = useI18n()
  const email = input.email.trim()
  const username = input.username.trim()
  if (!EMAIL_PATTERN.test(email)) {
    registerError.value = t('invalidEmail')
    return false
  }
  if (username.length < 2) {
    registerError.value = t('usernameTooShort')
    return false
  }
  if (input.password.length < 6) {
    registerError.value = t('passwordTooShort')
    return false
  }
  registerPending.value = true
  registerError.value = null
  registerSuccess.value = false
  try {
    await registerRequest({ email, username, password: input.password, role: 'STUDY' })
    registerSuccess.value = true
    return true
  } catch {
    registerError.value = t('registerFailed')
    return false
  } finally {
    registerPending.value = false
  }
}

export function useAuth() {
  return {
    user: computed(() => session.value?.user ?? null),
    isAuthenticated,
    pending,
    error,
    login,
    loginWithGoogle,
    logout,
    initAuth,
    register,
    registerPending,
    registerError,
    registerSuccess
  }
}

const profilePending = ref(false)
const profileError = ref<string | null>(null)
const profileSaved = ref(false)

async function updateProfile(username: string): Promise<boolean> {
  const current = session.value
  if (!current) return false
  const name = username.trim()
  if (name.length < 2) {
    profileError.value = useI18n().t('usernameTooShort')
    return false
  }
  profilePending.value = true
  profileError.value = null
  profileSaved.value = false
  try {
    await updateUserName(current.user.id, name)
    session.value = { token: current.token, user: { ...current.user, name } }
    persistSession(session.value)
    profileSaved.value = true
    return true
  } catch {
    profileError.value = useI18n().t('profileSaveFailed')
    return false
  } finally {
    profilePending.value = false
  }
}

export function useProfileUpdate() {
  return { pending: profilePending, error: profileError, saved: profileSaved, updateProfile }
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

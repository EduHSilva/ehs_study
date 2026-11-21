import { ref } from 'vue'

export type AppPage = 'dashboard' | 'pomodoro' | 'notes' | 'tasks' | 'kanban' | 'projects' | 'review' | 'profile'

const APP_PAGES: AppPage[] = ['dashboard', 'pomodoro', 'notes', 'tasks', 'kanban', 'projects', 'review', 'profile']

const isAppPage = (value: string): value is AppPage => (APP_PAGES as string[]).includes(value)

// Sem router, a tela atual vive no hash (`#/kanban`) para o F5 e o botão voltar do navegador
// manterem a página. O hash não depende de rewrite no servidor, então funciona sob `/app/study/`.
function pageFromHash(): AppPage {
  if (typeof window === 'undefined') return 'dashboard'
  const value = window.location.hash.replace(/^#\/?/, '')
  return isAppPage(value) ? value : 'dashboard'
}

export const currentPage = ref<AppPage>(pageFromHash())

type NavigationGuard = (next: AppPage) => boolean | Promise<boolean>

// Guard global: a tela ativa registra uma checagem (ex.: alterações não salvas) e a
// navegação só acontece depois que ela liberar. Login/logout usam `navigate` direto.
let guard: NavigationGuard | null = null
export const setNavigationGuard = (next: NavigationGuard | null) => { guard = next }

const applyPage = (page: AppPage) => {
  currentPage.value = page
  if (typeof window === 'undefined') return
  const hash = `#/${page}`
  if (window.location.hash !== hash) window.location.hash = hash
}

const restoreHash = () => {
  if (typeof window === 'undefined') return
  const hash = `#/${currentPage.value}`
  if (window.location.hash !== hash) window.history.replaceState(null, '', hash)
}

export const navigate = (page: AppPage) => {
  applyPage(page)
}

export const requestNavigate = async (page: AppPage): Promise<boolean> => {
  if (page === currentPage.value) return true
  if (guard && !(await guard(page))) return false
  applyPage(page)
  return true
}

// Voltar/avançar ou editar o hash à mão também troca a tela, respeitando o guard.
if (typeof window !== 'undefined') {
  window.addEventListener('hashchange', async () => {
    const next = pageFromHash()
    if (next === currentPage.value) return
    if (guard && !(await guard(next))) {
      restoreHash()
      return
    }
    currentPage.value = next
  })
}

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

export const navigate = (page: AppPage) => {
  currentPage.value = page
  if (typeof window === 'undefined') return
  const hash = `#/${page}`
  if (window.location.hash !== hash) window.location.hash = hash
}

// Voltar/avançar ou editar o hash à mão também troca a tela.
if (typeof window !== 'undefined') {
  window.addEventListener('hashchange', () => {
    currentPage.value = pageFromHash()
  })
}

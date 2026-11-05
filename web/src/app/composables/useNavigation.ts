import { ref } from 'vue'
export type AppPage = 'dashboard' | 'pomodoro' | 'notes' | 'tasks' | 'projects' | 'review' | 'profile'
export const currentPage = ref<AppPage>('dashboard')
export const navigate = (page: AppPage) => { currentPage.value = page }

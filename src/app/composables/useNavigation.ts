import { ref } from 'vue'
export type AppPage = 'dashboard' | 'pomodoro' | 'notes' | 'reminders' | 'projects' | 'review'
export const currentPage = ref<AppPage>('dashboard')
export const navigate = (page: AppPage) => { currentPage.value = page }

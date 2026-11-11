<script setup lang="ts">
import { watchEffect } from 'vue'
import { useAuth } from './app/features/auth/useAuth'
import { currentPage, navigate } from './app/composables/useNavigation'
import LandingPage from './app/pages/LandingPage.vue'
import DashboardPage from './app/pages/DashboardPage.vue'
import PomodoroPage from './app/pages/PomodoroPage.vue'
import NotesPage from './app/pages/NotesPage.vue'
import TasksPage from './app/pages/TasksPage.vue'
import KanbanPage from './app/pages/KanbanPage.vue'
import ProjectsPage from './app/pages/ProjectsPage.vue'
import ReviewPage from './app/pages/ReviewPage.vue'
import ProfilePage from './app/pages/ProfilePage.vue'
import PomodoroWidget from './app/components/PomodoroWidget.vue'

const { isAuthenticated, initAuth } = useAuth()
void initAuth()

const pageTitles: Record<string, string> = {
  dashboard: 'Dashboard',
  pomodoro: 'Pomodoro',
  notes: 'Notas',
  tasks: 'Tarefas',
  kanban: 'Kanban',
  projects: 'Projetos',
  review: 'Revisão',
  profile: 'Perfil'
}

watchEffect(() => {
  if (typeof document === 'undefined') return

  document.title = isAuthenticated.value
    ? `${pageTitles[currentPage.value] ?? 'Painel'} · EHS Study`
    : 'EHS Study — Seus estudos, em fluxo'
})
</script>

<template>
  <LandingPage v-if="!isAuthenticated" />
  <template v-else>
    <DashboardPage v-if="currentPage === 'dashboard'" @navigate="navigate" />
    <PomodoroPage v-else-if="currentPage === 'pomodoro'" @navigate="navigate" />
    <NotesPage v-else-if="currentPage === 'notes'" @navigate="navigate" />
    <TasksPage v-else-if="currentPage === 'tasks'" @navigate="navigate" />
    <KanbanPage v-else-if="currentPage === 'kanban'" @navigate="navigate" />
    <ProjectsPage v-else-if="currentPage === 'projects'" @navigate="navigate" />
    <ReviewPage v-else-if="currentPage === 'review'" @navigate="navigate" />
    <ProfilePage v-else @navigate="navigate" />
    <PomodoroWidget />
  </template>
</template>

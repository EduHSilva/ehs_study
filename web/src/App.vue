<script setup lang="ts">
import { computed, ref, watch, watchEffect } from 'vue'
import type { Component } from 'vue'
import { useAuth, useStudentProfile } from './app/features/auth/useAuth'
import { currentPage, requestNavigate } from './app/composables/useNavigation'
import type { AppPage } from './app/composables/useNavigation'
import AppSidebar from './app/components/AppSidebar.vue'
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
import { useDashboard } from './app/features/dashboard/useDashboard'
import { useNotes } from './app/features/notes/useNotes'
import { usePomodoro } from './app/features/pomodoro/usePomodoro'
import { useTasks } from './app/features/tasks/useTasks'
import { useKanban } from './app/features/kanban/useKanban'
import { useProjects } from './app/features/projects/useProjects'

const { isAuthenticated, initAuth } = useAuth()
const { student } = useStudentProfile()
void initAuth()

const pageTitles: Record<string, string> = {
  dashboard: 'Dashboard',
  pomodoro: 'Pomodoro',
  notes: 'Notas',
  tasks: 'Tarefas',
  kanban: 'Kanban',
  projects: 'Projetos e cursos',
  review: 'Revisão',
  profile: 'Perfil'
}

// A sidebar vive aqui, fora da página, para continuar montada entre navegações: só o painel
// de conteúdo troca (e anima). Sem isso o shell inteiro piscava a cada clique no menu.
const pages: Record<AppPage, Component> = {
  dashboard: DashboardPage,
  pomodoro: PomodoroPage,
  notes: NotesPage,
  tasks: TasksPage,
  kanban: KanbanPage,
  projects: ProjectsPage,
  review: ReviewPage,
  profile: ProfilePage
}
const page = computed(() => pages[currentPage.value])

const menuOpen = ref(false)
const closeMenu = () => { menuOpen.value = false }
watch(currentPage, closeMenu)

const brandIcon = `${import.meta.env.BASE_URL}assets/graduation-cap.svg`

// Prefetch sob intenção: passar o mouse (ou focar pelo teclado) no item do menu já busca os
// dados da tela, então o clique abre o conteúdo em vez de um skeleton.
const prefetchers: Partial<Record<AppPage, () => void>> = {
  dashboard: () => void useDashboard().load(),
  pomodoro: () => void usePomodoro().load(),
  notes: () => void useNotes().load(),
  tasks: () => void useTasks().load(),
  kanban: () => void useKanban().load(),
  projects: () => void useProjects().load()
}
const prefetched = new Set<AppPage>()
const prefetchPage = (target: AppPage) => {
  if (prefetched.has(target)) return
  prefetched.add(target)
  prefetchers[target]?.()
}

watchEffect(() => {
  if (typeof document === 'undefined') return

  document.title = isAuthenticated.value
    ? `${pageTitles[currentPage.value] ?? 'Painel'} · Estudos`
    : 'Estudos — Seus estudos, em fluxo'
})
</script>

<template>
  <LandingPage v-if="!isAuthenticated" />
  <main v-else class="app-shell" :class="{ 'menu-open': menuOpen }">
    <header class="mobile-topbar">
      <button
        class="mobile-menu"
        type="button"
        :aria-label="menuOpen ? 'Fechar menu' : 'Abrir menu'"
        :aria-expanded="menuOpen"
        @click="menuOpen = !menuOpen"
      >
        <svg v-if="!menuOpen" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
        <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
      </button>
      <span class="mobile-brand"><span class="brand-icon"><img :src="brandIcon" alt="" /></span><strong>Estudos</strong></span>
    </header>
    <div class="sidebar-scrim" @click="closeMenu" />
    <AppSidebar v-bind="student" :active="currentPage" @navigate="requestNavigate" @prefetch="prefetchPage" />
    <component :is="page" :key="currentPage" @navigate="requestNavigate" />
    <PomodoroWidget />
  </main>
</template>

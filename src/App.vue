<script setup lang="ts">
import { useAuth } from './app/features/auth/useAuth'
import { currentPage, navigate } from './app/composables/useNavigation'
import LandingPage from './app/pages/LandingPage.vue'
import DashboardPage from './app/pages/DashboardPage.vue'
import PomodoroPage from './app/pages/PomodoroPage.vue'
import NotesPage from './app/pages/NotesPage.vue'
import RemindersPage from './app/pages/RemindersPage.vue'
import ProjectsPage from './app/pages/ProjectsPage.vue'
import ReviewPage from './app/pages/ReviewPage.vue'

const { isAuthenticated } = useAuth()
</script>

<template>
  <LandingPage v-if="!isAuthenticated" />
  <DashboardPage v-else-if="currentPage === 'dashboard'" @navigate="navigate" />
  <PomodoroPage v-else-if="currentPage === 'pomodoro'" @navigate="navigate" />
  <NotesPage v-else-if="currentPage === 'notes'" @navigate="navigate" />
  <RemindersPage v-else-if="currentPage === 'reminders'" @navigate="navigate" />
  <ProjectsPage v-else-if="currentPage === 'projects'" @navigate="navigate" />
  <ReviewPage v-else @navigate="navigate" />
</template>

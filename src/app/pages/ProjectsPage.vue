<script setup lang="ts">
import { onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useProjects } from '../features/projects/useProjects'
import type { ProjectStatus } from '../features/projects/types'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { projects, details, selectedId, loading, savingStageId, error, load, selectProject, toggleStage } = useProjects()
onMounted(load)

const statusClass = (status: ProjectStatus) => (status === 'Concluído' ? 'baixa' : 'média')
const statusLabel = (status: ProjectStatus) => t(status === 'Concluído' ? 'projectStatusDone' : 'projectStatusInProgress')
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="projects" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('projectsTitle') }}</h1><p>{{ t('projectsSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <div v-if="details" class="projects-layout">
      <article class="card project-list">
        <h2>{{ t('myProjects') }}</h2>
        <p v-if="!projects.length && !error" class="empty-note">{{ t('noProjects') }}</p>
        <button v-for="project in projects" :key="project.id" class="project" :class="{selected:project.id===selectedId}" @click="selectProject(project.id)"><strong>{{project.title}}</strong><span><i :style="{width:`${project.progress}%`}"/></span><small>{{ t('percentCompleted', { progress: project.progress }) }}</small></button>
      </article>
      <article class="card project-details">
        <div class="card-title"><div><h2>{{details.project.title}}</h2><p>{{ t('advisorLabel') }} {{details.project.advisor}}</p></div><span class="priority" :class="statusClass(details.project.status)">{{ statusLabel(details.project.status) }}</span></div>
        <h3>{{ t('stagesTitle') }}</h3>
        <button v-for="stage in details.stages" :key="stage.id" class="stage" :disabled="savingStageId===stage.id" @click="toggleStage(stage.id)"><span class="checkbox" :class="{checked:stage.done}">✓</span><span :class="{done:stage.done}">{{stage.name}}</span></button>
        <h3>{{ t('milestonesTitle') }}</h3>
        <p v-if="!details.milestones.length" class="empty-note">{{ t('noMilestones') }}</p>
        <div class="milestones"><div v-for="(milestone,i) in details.milestones" :key="milestone.id"><small>{{ t('milestoneLabel', { index: i + 1 }) }}</small><strong>{{milestone.title}}</strong><em>{{ t('dueDateLabel', { date: milestone.dueDate }) }}</em></div></div>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('projectsLoading') }}</main>
</template>

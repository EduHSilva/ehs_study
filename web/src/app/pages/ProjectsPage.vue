<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useProjects } from '../features/projects/useProjects'
import type { ProjectStatus } from '../features/projects/types'

defineEmits<{ navigate:[page:AppPage] }>()
const { t, locale } = useI18n()
const { student } = useStudentProfile()
const { projects, details, selectedId, loading, savingStageId, savingMilestoneId, savingNewStage, savingNewMilestone, savingProject, error, load, selectProject, createProject, createStage, createMilestone, toggleStage, updateMilestone } = useProjects()
onMounted(load)

const creatingProject = ref(false)
const projectTitle = ref('')
const projectAdvisor = ref('')
const newStageTitle = ref('')
const newMilestoneTitle = ref('')
const newMilestoneDueDate = ref('')
const submitProject = async () => {
  if (await createProject(projectTitle.value, projectAdvisor.value)) {
    creatingProject.value = false
    projectTitle.value = ''
    projectAdvisor.value = ''
  }
}
const submitStage = async () => {
  if (await createStage(newStageTitle.value)) newStageTitle.value = ''
}
const submitMilestone = async () => {
  if (await createMilestone(newMilestoneTitle.value, newMilestoneDueDate.value)) {
    newMilestoneTitle.value = ''
    newMilestoneDueDate.value = ''
  }
}

const statusClass = (status: ProjectStatus) => (status === 'Concluído' ? 'baixa' : 'média')
const statusLabel = (status: ProjectStatus) => t(status === 'Concluído' ? 'projectStatusDone' : 'projectStatusInProgress')

// `dueDate` chega em ISO 8601; formatamos pro padrão exibível do card (ex.: "12 nov").
const milestoneDateFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { day: '2-digit', month: 'short' }))
const milestoneDate = (value: string) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : milestoneDateFormatter.value.format(date)
}
const editingMilestoneId = ref<string | null>(null)
const milestoneTitle = ref('')
const milestoneDueDate = ref('')
const beginMilestoneEdit = (milestone: { id: string; title: string; dueDate: string }) => {
  editingMilestoneId.value = milestone.id
  milestoneTitle.value = milestone.title
  milestoneDueDate.value = milestone.dueDate.slice(0, 16)
}
const saveMilestone = async (id: string) => {
  if (await updateMilestone(id, milestoneTitle.value, milestoneDueDate.value)) editingMilestoneId.value = null
}
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="projects" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('projectsTitle') }}</h1><p>{{ t('projectsSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <div class="projects-layout">
      <article class="card project-list">
        <div class="card-title"><h2>{{ t('myProjects') }}</h2><button v-if="!creatingProject" class="button" @click="creatingProject=true">＋ {{ t('newProject') }}</button></div>
        <form v-if="creatingProject" class="project-create-form" @submit.prevent="submitProject">
          <label>{{ t('projectTitleLabel') }}<input v-model="projectTitle" :placeholder="t('projectTitlePlaceholder')" required maxlength="200" /></label>
          <label>{{ t('projectAdvisorLabel') }}<input v-model="projectAdvisor" :placeholder="t('projectAdvisorPlaceholder')" maxlength="200" /></label>
          <div class="project-create-actions"><button class="link" type="button" @click="creatingProject=false">{{ t('cancelAction') }}</button><button class="button" type="submit" :disabled="savingProject">{{ savingProject ? t('saving') : t('createProject') }}</button></div>
        </form>
        <p v-if="!projects.length && !error" class="empty-note">{{ t('noProjects') }}</p>
        <button v-for="project in projects" :key="project.id" class="project" :class="{selected:project.id===selectedId}" @click="selectProject(project.id)"><strong>{{project.title}}</strong><span><i :style="{width:`${project.progress}%`}"/></span><small>{{ t('percentCompleted', { progress: project.progress }) }}</small></button>
      </article>
      <article v-if="details" class="card project-details">
        <div class="card-title"><div><h2>{{details.project.title}}</h2><p>{{ t('advisorLabel') }} {{details.project.advisor}}</p></div><span class="priority" :class="statusClass(details.project.status)">{{ statusLabel(details.project.status) }}</span></div>
        <h3>{{ t('stagesTitle') }}</h3>
        <button v-for="stage in details.stages" :key="stage.id" class="stage" :disabled="savingStageId===stage.id" @click="toggleStage(stage.id)"><span class="checkbox" :class="{checked:stage.done}">✓</span><span :class="{done:stage.done}">{{stage.name}}</span></button>
        <form class="project-item-create" @submit.prevent="submitStage"><input v-model="newStageTitle" :placeholder="t('stageTitlePlaceholder')" :aria-label="t('stageTitleLabel')" required maxlength="200"/><button class="button" type="submit" :disabled="savingNewStage">{{ savingNewStage ? t('saving') : t('addStage') }}</button></form>
        <h3>{{ t('milestonesTitle') }}</h3>
        <p v-if="!details.milestones.length" class="empty-note">{{ t('noMilestones') }}</p>
        <div class="milestones"><div v-for="(milestone,i) in details.milestones" :key="milestone.id">
          <small>{{ t('milestoneLabel', { index: i + 1 }) }}</small>
          <form v-if="editingMilestoneId===milestone.id" class="milestone-edit" @submit.prevent="saveMilestone(milestone.id)"><input v-model="milestoneTitle" aria-label="Título do marco"/><input v-model="milestoneDueDate" type="datetime-local" aria-label="Prazo do marco"/><button class="button" :disabled="savingMilestoneId===milestone.id">{{ t('save') }}</button><button class="link" type="button" @click="editingMilestoneId=null">{{ t('cancelAction') }}</button></form>
          <template v-else><strong>{{milestone.title}}</strong><em>{{ t('dueDateLabel', { date: milestoneDate(milestone.dueDate) }) }}</em><button class="link" :aria-label="`Editar ${milestone.title}`" @click="beginMilestoneEdit(milestone)">✎</button></template>
        </div></div>
        <form class="project-item-create milestone-create" @submit.prevent="submitMilestone"><input v-model="newMilestoneTitle" :placeholder="t('milestoneTitlePlaceholder')" :aria-label="t('milestoneTitleLabel')" required maxlength="200"/><input v-model="newMilestoneDueDate" type="datetime-local" :aria-label="t('milestoneDueDateLabel')" required/><button class="button" type="submit" :disabled="savingNewMilestone">{{ savingNewMilestone ? t('saving') : t('addMilestone') }}</button></form>
      </article>
      <article v-else-if="!error && projects.length" class="card project-details">
        <p class="empty-note">{{ t('selectProjectPlaceholder') }}</p>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('projectsLoading') }}</main>
</template>
<style scoped>
.project-create-form{display:grid;gap:12px;padding:12px 0}
.project-create-form label{display:grid;gap:6px;color:var(--color-muted);font-size:13px}
.project-create-form input{width:100%;padding:10px 12px;border:1px solid var(--color-border);border-radius:var(--radius-md);background:var(--color-surface);color:var(--color-text)}
.project-create-actions{display:flex;justify-content:flex-end;align-items:center;gap:12px}
.project-item-create{display:flex;gap:8px;margin:10px 0 18px;flex-wrap:wrap}
.project-item-create input{min-width:0;flex:1;padding:10px 12px;border:1px solid var(--color-border);border-radius:var(--radius-md);background:var(--color-surface);color:var(--color-text)}
.milestone-create input[type="datetime-local"]{flex:0 1 220px}
</style>

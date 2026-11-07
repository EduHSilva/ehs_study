<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useProjects } from '../features/projects/useProjects'
import type { ProjectStatus } from '../features/projects/types'

defineEmits<{ navigate:[page:AppPage] }>()
const { t, locale } = useI18n()
const { student } = useStudentProfile()
const { projects, details, selectedId, loading, savingStageId, savingVersionId, savingNewStage, savingNewVersion, savingProject, error, tasksLoading, tasksError, projectTasks, stageTasks, unassignedTasks, isTaskDone, load, selectProject, createProject, createStage, createVersion, toggleStage, updateVersion } = useProjects()
onMounted(load)

const creatingProject = ref(false)
const projectTitle = ref('')
const projectAdvisor = ref('')
const newStageTitle = ref('')
const newVersionTitle = ref('')
const newVersionDueDate = ref('')
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
const submitVersion = async () => {
  if (await createVersion(newVersionTitle.value, newVersionDueDate.value)) {
    newVersionTitle.value = ''
    newVersionDueDate.value = ''
  }
}

const statusClass = (status: ProjectStatus) => (status === 'Concluído' ? 'baixa' : 'média')
const statusLabel = (status: ProjectStatus) => t(status === 'Concluído' ? 'projectStatusDone' : 'projectStatusInProgress')

const showCompleted = ref(true)
const expandedStages = ref<Record<string, boolean>>({})
const unassignedExpanded = ref(false)

const visibleStages = computed(() => {
  const stages = details.value?.stages ?? []
  return showCompleted.value ? stages : stages.filter(stage => !stage.done)
})
const completedStageCount = computed(() => details.value?.stages.filter(stage => stage.done).length ?? 0)
const hiddenCompletedCount = computed(() => {
  if (showCompleted.value) return 0
  return completedStageCount.value + projectTasks.value.filter(task => isTaskDone(task)).length
})
const stageTasksFor = (stageId: string) => {
  const tasks = stageTasks(stageId)
  return showCompleted.value ? tasks : tasks.filter(task => !isTaskDone(task))
}
const visibleUnassignedTasks = computed(() =>
  showCompleted.value ? unassignedTasks.value : unassignedTasks.value.filter(task => !isTaskDone(task))
)
const isStageExpanded = (id: string) => expandedStages.value[id] === true
const toggleStageExpanded = (id: string) => {
  expandedStages.value = { ...expandedStages.value, [id]: !isStageExpanded(id) }
}
const allStagesExpanded = computed(() =>
  visibleStages.value.length > 0 && visibleStages.value.every(stage => isStageExpanded(stage.id))
)
const toggleAllStages = () => {
  const next = !allStagesExpanded.value
  const expanded: Record<string, boolean> = {}
  for (const stage of visibleStages.value) expanded[stage.id] = next
  expandedStages.value = expanded
}
watch(selectedId, () => {
  expandedStages.value = {}
  unassignedExpanded.value = false
})

// `dueDate` chega em ISO 8601; formatamos pro padrão exibível do card (ex.: "12 nov. 2026 · 12:00").
const versionDateFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { day: '2-digit', month: 'short', year: 'numeric' }))
const versionTimeFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }))
const versionDate = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${versionDateFormatter.value.format(date)} · ${versionTimeFormatter.value.format(date)}`
}

const DAY_MS = 24 * 60 * 60 * 1000
const versionMeta = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return { tone: 'normal', label: '' }
  const days = Math.round((date.getTime() - Date.now()) / DAY_MS)
  if (days < 0) return { tone: 'overdue', label: t('versionPast', { days: Math.abs(days) }) }
  if (days === 0) return { tone: 'soon', label: t('versionDueToday') }
  if (days <= 3) return { tone: 'soon', label: t('versionDueIn', { days }) }
  return { tone: 'normal', label: '' }
}
const timelineVersions = computed(() => {
  const versions = details.value?.versions ?? []
  const time = (value: string) => {
    const ms = new Date(value).getTime()
    return Number.isNaN(ms) ? Number.POSITIVE_INFINITY : ms
  }
  return versions
    .map((version, index) => ({ ...version, position: index + 1, meta: versionMeta(version.dueDate) }))
    .sort((a, b) => time(a.dueDate) - time(b.dueDate))
})
const editingVersionId = ref<string | null>(null)
const versionTitle = ref('')
const versionDueDate = ref('')
const beginVersionEdit = (version: { id: string; title: string; dueDate: string }) => {
  editingVersionId.value = version.id
  versionTitle.value = version.title
  versionDueDate.value = version.dueDate.slice(0, 16)
}
const saveVersion = async (id: string) => {
  if (await updateVersion(id, versionTitle.value, versionDueDate.value)) editingVersionId.value = null
}
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="projects" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('projectsTitle') }}</h1><p>{{ t('projectsSubtitle') }}</p><p>{{ t('projectsHint') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
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
        <div class="card-title"><div><h2>{{details.project.title}}</h2><p>{{ t('advisorLabel') }} {{details.project.advisor || t('noAdvisor')}}</p></div><span class="priority" :class="statusClass(details.project.status)">{{ statusLabel(details.project.status) }}</span></div>
        <div class="stage-heading"><h3>{{ t('stagesTitle') }}</h3><div class="stages-toolbar"><button class="chip" :class="{ active: !showCompleted }" :aria-pressed="!showCompleted" @click="showCompleted = !showCompleted">{{ showCompleted ? t('hideCompleted') : t('showCompleted') }}<small v-if="completedStageCount">{{ completedStageCount }}</small></button><button v-if="details.stages.length" class="chip" @click="toggleAllStages">{{ allStagesExpanded ? t('collapseAllStages') : t('expandAllStages') }}</button></div></div>
        <p v-if="hiddenCompletedCount" class="stage-hidden-hint">{{ t('hiddenCompletedCount', { count: hiddenCompletedCount }) }}</p>
        <p v-if="!details.stages.length" class="empty-note">{{ t('noStages') }}</p>
        <p v-else-if="!visibleStages.length" class="empty-note">{{ t('allStagesHidden') }}</p>
        <div v-for="stage in visibleStages" :key="stage.id" class="stage-item" :class="{ done: stage.done }">
          <div class="stage-row">
            <button class="stage-check" :class="{ checked: stage.done }" :disabled="savingStageId===stage.id" :aria-label="stage.done ? t('reopenStage') : t('completeStage')" :title="stage.done ? t('reopenStage') : t('completeStage')" @click="toggleStage(stage.id)">✓</button>
            <button class="stage-toggle" :aria-expanded="isStageExpanded(stage.id)" @click="toggleStageExpanded(stage.id)"><span class="stage-chevron" :class="{ open: isStageExpanded(stage.id) }" aria-hidden="true">›</span><span class="stage-name" :class="{ done: stage.done }">{{ stage.name }}</span><small class="stage-count">{{ t('stageTaskCount', { count: stageTasksFor(stage.id).length }) }}</small></button>
          </div>
          <div v-if="isStageExpanded(stage.id)" class="stage-body">
            <ul v-if="stageTasksFor(stage.id).length" class="stage-tasks"><li v-for="task in stageTasksFor(stage.id)" :key="task.id" :class="{done:isTaskDone(task)}"><span class="checkbox" :class="{checked:isTaskDone(task)}">✓</span><span>{{ task.title }}</span><span v-if="task.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span></li></ul>
            <p v-else-if="stageTasks(stage.id).length" class="empty-note">{{ t('noVisibleTasks') }}</p>
            <p v-else class="empty-note">{{ t('noStageTasks') }}</p>
          </div>
        </div>
        <form class="project-item-create" @submit.prevent="submitStage"><input v-model="newStageTitle" :placeholder="t('stageTitlePlaceholder')" :aria-label="t('stageTitleLabel')" required maxlength="200"/><button class="button" type="submit" :disabled="savingNewStage">{{ savingNewStage ? t('saving') : t('addStage') }}</button></form>
        <p v-if="tasksLoading" class="empty-note">{{ t('projectTasksLoading') }}</p>
        <p v-else-if="tasksError" class="sync-error">{{ tasksError }}</p>
        <template v-else>
          <div v-if="unassignedTasks.length" class="stage-item">
            <div class="stage-row">
              <button class="stage-toggle" :aria-expanded="unassignedExpanded" @click="unassignedExpanded = !unassignedExpanded"><span class="stage-chevron" :class="{ open: unassignedExpanded }" aria-hidden="true">›</span><span class="stage-name">{{ t('unassignedTasksTitle') }}</span><small class="stage-count">{{ t('stageTaskCount', { count: visibleUnassignedTasks.length }) }}</small></button>
            </div>
            <div v-if="unassignedExpanded" class="stage-body">
              <ul v-if="visibleUnassignedTasks.length" class="stage-tasks"><li v-for="task in visibleUnassignedTasks" :key="task.id" :class="{done:isTaskDone(task)}"><span class="checkbox" :class="{checked:isTaskDone(task)}">✓</span><span>{{ task.title }}</span><span v-if="task.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span></li></ul>
              <p v-else class="empty-note">{{ t('noVisibleTasks') }}</p>
            </div>
          </div>
          <p v-if="!details.stages.length && !unassignedTasks.length" class="empty-note">{{ t('noProjectTasks') }}</p>
        </template>
        <button class="link project-tasks-link" @click="$emit('navigate','tasks')">{{ t('manageTasks') }}</button>
        <h3>{{ t('versionsTitle') }}</h3>
        <p v-if="!details.versions.length" class="empty-note">{{ t('noVersions') }}</p>
        <ol v-else class="version-timeline">
          <li v-for="version in timelineVersions" :key="version.id" class="version-item" :class="version.meta.tone">
            <span class="version-dot" aria-hidden="true"></span>
            <div class="version-card">
              <div class="version-top"><small>{{ t('versionLabel', { index: version.position }) }}</small><em v-if="version.meta.label" class="version-badge">{{ version.meta.label }}</em></div>
              <form v-if="editingVersionId===version.id" class="version-edit" @submit.prevent="saveVersion(version.id)"><input v-model="versionTitle" :aria-label="t('versionTitleLabel')"/><input v-model="versionDueDate" type="datetime-local" :aria-label="t('versionDueDateLabel')"/><button class="button" :disabled="savingVersionId===version.id">{{ t('save') }}</button><button class="link" type="button" @click="editingVersionId=null">{{ t('cancelAction') }}</button></form>
              <template v-else><strong>{{version.title}}</strong><div class="version-meta"><span>{{ versionDate(version.dueDate) }}</span><button class="link" :aria-label="`${t('editVersionAction')}: ${version.title}`" @click="beginVersionEdit(version)">✎</button></div></template>
            </div>
          </li>
        </ol>
        <form class="project-item-create version-create" @submit.prevent="submitVersion"><input v-model="newVersionTitle" :placeholder="t('versionTitlePlaceholder')" :aria-label="t('versionTitleLabel')" required maxlength="200"/><input v-model="newVersionDueDate" type="datetime-local" :aria-label="t('versionDueDateLabel')" required/><button class="button" type="submit" :disabled="savingNewVersion">{{ savingNewVersion ? t('saving') : t('addVersion') }}</button></form>
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
.version-create input[type="datetime-local"]{flex:0 1 220px}
.stage-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin:8px 0}
.stage-heading h3{margin:0}
.stages-toolbar{display:flex;gap:8px;flex-wrap:wrap}
.chip{display:inline-flex;align-items:center;gap:6px;padding:6px 12px;border:1px solid var(--color-border-strong);border-radius:999px;background:var(--color-surface);color:var(--color-muted);font:600 12px var(--font-body);cursor:pointer}
.chip:hover{border-color:var(--color-primary);color:var(--color-primary)}
.chip.active{border-color:transparent;background:var(--color-primary-soft);color:var(--color-primary)}
.chip small{display:grid;place-items:center;min-width:18px;height:18px;padding:0 4px;border-radius:999px;background:var(--color-bg);color:inherit;font-size:11px}
.stage-hidden-hint{margin:0 0 8px;color:var(--color-muted);font-size:12px}
.stage-item{margin-bottom:10px;border:1px solid var(--color-border);border-radius:12px;overflow:hidden}
.stage-row{display:flex;align-items:center;gap:4px;padding:4px 8px 4px 4px}
.stage-check{flex:none;width:26px;height:26px;border:2px solid var(--color-border-strong);border-radius:7px;background:transparent;color:transparent;font-size:13px;cursor:pointer}
.stage-check.checked{border-color:var(--color-success);background:var(--color-success);color:#fff}
.stage-check:disabled{opacity:.55;cursor:default}
.stage-toggle{flex:1;min-width:0;display:flex;align-items:center;gap:8px;padding:8px 6px;border:0;background:transparent;color:var(--color-text);font:500 14px var(--font-body);text-align:left;cursor:pointer}
.stage-toggle:hover .stage-name{color:var(--color-primary)}
.stage-chevron{flex:none;color:var(--color-muted);transition:transform .15s}
.stage-chevron.open{transform:rotate(90deg)}
.stage-name{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.stage-name.done{color:var(--color-muted);text-decoration:line-through}
.stage-count{margin-left:auto;color:var(--color-muted);font-size:12px;white-space:nowrap}
.stage-body{padding:10px 14px 12px 40px;border-top:1px solid var(--color-border)}
.stage-tasks{list-style:none;display:grid;gap:6px;margin:0;padding:0}
.stage-tasks li{display:flex;align-items:center;gap:8px;font-size:14px}
.stage-tasks li .checkbox{flex:none}
.stage-tasks li.done span{text-decoration:line-through;color:var(--color-muted)}
.stage-body .empty-note{padding:10px}
.project-tasks-link{margin:0 0 18px}
.version-timeline{list-style:none;display:grid;margin:0 0 8px;padding:0}
.version-item{position:relative;display:grid;grid-template-columns:18px minmax(0,1fr);gap:12px;padding-bottom:14px}
.version-item:not(:last-child):before{content:'';position:absolute;left:8px;top:20px;bottom:0;width:2px;background:var(--color-border)}
.version-dot{width:14px;height:14px;margin-top:16px;border:3px solid var(--color-border);border-radius:50%;background:var(--color-surface)}
.version-item.soon .version-dot{border-color:var(--color-warning)}
.version-item.overdue .version-dot{border-color:var(--color-danger)}
.version-card{display:grid;gap:8px;padding:12px 14px;border:1px solid var(--color-border);border-radius:12px;background:var(--color-bg)}
.version-item.overdue .version-card{border-color:var(--color-danger)}
.version-top{display:flex;align-items:center;justify-content:space-between;gap:8px}
.version-top small{color:var(--color-muted);font-size:11px;font-weight:600;letter-spacing:.04em}
.version-badge{padding:2px 8px;border:1px solid var(--color-border);border-radius:999px;background:var(--color-surface);color:var(--color-muted);font-style:normal;font-size:11px;font-weight:600}
.version-item.soon .version-badge{border-color:transparent;background:var(--color-warning-soft);color:var(--color-warning)}
.version-item.overdue .version-badge{border-color:transparent;background:var(--color-danger-soft);color:var(--color-danger)}
.version-card>strong{font-size:14px}
.version-meta{display:flex;align-items:center;justify-content:space-between;gap:8px;color:var(--color-muted);font-size:12px}
</style>

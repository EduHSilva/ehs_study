<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useKanban } from '../features/kanban/useKanban'
import type { ProjectColumn, TaskColumn } from '../features/kanban/useKanban'
import { formatTaskPeriod } from '../features/tasks/taskFormat'
import type { Project } from '../features/projects/types'
import type { Task } from '../features/tasks/types'

defineEmits<{ navigate: [page: AppPage] }>()
const { t } = useI18n()
const {
  entity,
  projects,
  loading,
  error,
  savingId,
  taskColumns,
  projectColumns,
  taskColumn,
  projectColumn,
  isTaskOverdue,
  load,
  setEntity,
  moveTask,
  moveProject
} = useKanban()
onMounted(load)

const dragging = ref<{ kind: 'task' | 'project'; id: string } | null>(null)
const projectName = (task: Task) => projects.value.find(project => project.id === task.projectId)?.title ?? ''

const startTaskDrag = (task: Task, event: DragEvent) => {
  dragging.value = { kind: 'task', id: task.id }
  if (!event.dataTransfer) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', task.id)
}

const startProjectDrag = (project: Project, event: DragEvent) => {
  dragging.value = { kind: 'project', id: project.id }
  if (!event.dataTransfer) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', project.id)
}

const endDrag = () => {
  dragging.value = null
}

const dropTask = (column: TaskColumn) => {
  const current = dragging.value
  dragging.value = null
  if (current?.kind === 'task') void moveTask(current.id, column)
}

const dropProject = (column: ProjectColumn) => {
  const current = dragging.value
  dragging.value = null
  if (current?.kind === 'project') void moveProject(current.id, column)
}

const onTaskStatusChange = (id: string, event: Event) => {
  void moveTask(id, (event.target as HTMLSelectElement).value as TaskColumn)
}

const onProjectStatusChange = (id: string, event: Event) => {
  void moveProject(id, (event.target as HTMLSelectElement).value as ProjectColumn)
}
</script>

<template>
  <section class="content">
      <header class="page-header">
        <div>
          <h1>{{ t('kanban') }}</h1>
          <p>{{ t('kanbanSubtitle') }}</p>
        </div>
        <div class="header-actions"><LanguageToggle /><ThemeToggle /></div>
      </header>

      <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
      <PageSkeleton v-if="loading" layout="board" :label="t('kanbanLoading')" />
      <template v-else>

      <div class="mode-switch kanban-switch">
        <button :class="{ selected: entity === 'tasks' }" @click="setEntity('tasks')">{{ t('kanbanSwitchTasks') }}</button>
        <button :class="{ selected: entity === 'projects' }" @click="setEntity('projects')">{{ t('kanbanSwitchProjects') }}</button>
      </div>

      <div v-if="entity === 'tasks'" class="kanban-board">
        <article
          v-for="column in taskColumns"
          :key="column.key"
          class="kanban-column"
          :class="column.tone"
          @dragover.prevent
          @dragenter.prevent
          @drop.prevent="dropTask(column.key)"
        >
          <header class="kanban-column-head">
            <h2>{{ t(column.labelKey) }}</h2>
            <span class="kanban-count">{{ column.items.length }}</span>
          </header>
          <p v-if="!column.items.length" class="empty-note">{{ t('kanbanEmptyColumn') }}</p>
          <article
            v-for="task in column.items"
            :key="task.id"
            class="kanban-card"
            :class="{ saving: savingId === task.id, dragging: dragging?.kind === 'task' && dragging.id === task.id }"
            draggable="true"
            @dragstart="startTaskDrag(task, $event)"
            @dragend="endDrag"
          >
            <header class="kanban-card-head">
              <strong :class="{ done: taskColumn(task) === 'DONE' }">{{ task.title }}</strong>
              <span v-if="task.type === 'DAILY'" class="tag daily">{{ t('taskTypeDaily') }}</span>
            </header>
            <p class="kanban-card-meta">{{ t('endDateLabel') }} {{ formatTaskPeriod(task.startDate, task.endDate) }}</p>
            <p v-if="projectName(task)" class="kanban-card-meta">{{ projectName(task) }}</p>
            <label class="kanban-status">
              <span>{{ t('kanbanTaskStatusLabel') }}</span>
              <select :value="taskColumn(task)" :disabled="savingId === task.id" @change="onTaskStatusChange(task.id, $event)">
                <option value="PENDING">{{ t('kanbanTasksPending') }}</option>
                <option value="WORKING">{{ t('kanbanTasksWorking') }}</option>
                <option v-if="taskColumn(task) === 'DELAYED' || isTaskOverdue(task)" value="DELAYED">{{ t('kanbanTasksDelayed') }}</option>
                <option value="DONE">{{ t('kanbanTasksDone') }}</option>
              </select>
            </label>
          </article>
        </article>
      </div>

      <div v-else class="kanban-board projects">
        <article
          v-for="column in projectColumns"
          :key="column.key"
          class="kanban-column"
          :class="column.tone"
          @dragover.prevent
          @dragenter.prevent
          @drop.prevent="dropProject(column.key)"
        >
          <header class="kanban-column-head">
            <h2>{{ t(column.labelKey) }}</h2>
            <span class="kanban-count">{{ column.items.length }}</span>
          </header>
          <p v-if="!column.items.length" class="empty-note">{{ t('kanbanEmptyColumn') }}</p>
          <article
            v-for="project in column.items"
            :key="project.id"
            class="kanban-card"
            :class="{ saving: savingId === project.id, dragging: dragging?.kind === 'project' && dragging.id === project.id }"
            draggable="true"
            @dragstart="startProjectDrag(project, $event)"
            @dragend="endDrag"
          >
            <header class="kanban-card-head">
              <strong :class="{ done: projectColumn(project) === 'DONE' }">{{ project.title }}</strong>
            </header>
            <p class="kanban-card-meta">{{ t('advisorLabel') }} {{ project.advisor || t('noAdvisor') }}</p>
            <p class="kanban-card-meta">{{ t('percentCompleted', { progress: project.progress }) }}</p>
            <label class="kanban-status">
              <span>{{ t('kanbanProjectStatusLabel') }}</span>
              <select :value="projectColumn(project)" :disabled="savingId === project.id" @change="onProjectStatusChange(project.id, $event)">
                <option value="WAITING">{{ t('kanbanProjectsWaiting') }}</option>
                <option value="WORKING">{{ t('kanbanProjectsInProgress') }}</option>
                <option value="DONE">{{ t('kanbanProjectsDone') }}</option>
              </select>
            </label>
          </article>
        </article>
      </div>
      </template>
  </section>
</template>

<style scoped>
.kanban-switch{display:inline-flex;margin:0 0 20px}
.kanban-board{display:grid;grid-template-columns:repeat(4,minmax(220px,1fr));gap:16px;align-items:start;overflow-x:auto;max-width:1420px;padding-bottom:8px}
.kanban-board.projects{grid-template-columns:repeat(3,minmax(240px,1fr));max-width:1080px}
.kanban-column{display:grid;align-content:start;gap:12px;min-height:220px;padding:14px;border:1px solid var(--color-border);border-radius:16px;background:var(--color-bg)}
.kanban-column.delayed{border-color:var(--color-warning);background:var(--color-warning-soft)}
.kanban-column.done{border-color:var(--color-success);background:var(--color-success-soft)}
.kanban-column.progress{border-color:var(--color-primary);background:var(--color-primary-soft)}
.kanban-column-head{display:flex;align-items:center;justify-content:space-between;gap:8px}
.kanban-column-head h2{margin:0;font-size:15px}
.kanban-count{display:grid;place-items:center;min-width:22px;height:22px;padding:0 6px;border:1px solid var(--color-border);border-radius:999px;background:var(--color-surface);color:var(--color-muted);font-size:11px;font-weight:700}
.kanban-card{display:grid;gap:8px;padding:12px;border:1px solid var(--color-border);border-radius:12px;background:var(--color-surface);cursor:grab;transition:border-color .15s,opacity .15s,box-shadow .15s}
.kanban-card:hover{border-color:var(--color-border-strong)}
.kanban-card:active{cursor:grabbing}
.kanban-card.saving{opacity:.6}
.kanban-card.dragging{opacity:.45;border-style:dashed}
.kanban-card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}
.kanban-card-head strong{font-size:14px;line-height:1.35;overflow-wrap:anywhere}
.kanban-card-head strong.done{color:var(--color-muted);text-decoration:line-through}
.kanban-card-meta{margin:0;color:var(--color-muted);font-size:12px;line-height:1.35}
.kanban-status{display:grid;gap:4px}
.kanban-status>span{color:var(--color-subtle);font-size:11px;font-weight:600}
.kanban-status select{width:100%;height:32px;padding:0 8px;border:1px solid var(--color-border-strong);border-radius:8px;background:var(--color-surface);color:var(--color-text);font:500 12px var(--font-body)}
.kanban-status select:disabled{opacity:.6;cursor:default}
.kanban-column .empty-note{padding:18px 10px;text-align:center}
@media(max-width:900px){.kanban-board{grid-template-columns:repeat(4,minmax(210px,1fr))}.kanban-board.projects{grid-template-columns:repeat(3,minmax(220px,1fr))}}
</style>

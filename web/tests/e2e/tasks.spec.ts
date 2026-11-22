import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'
import type { MockState, MockTask } from './support/mockApi'

const PROJECT_A = {
  id: 'project-1',
  title: 'Projeto Alfa',
  status: 'WORKING',
  progress: 0,
  advisor: '',
  stages: [{ id: 'stage-1', name: 'Introdução', done: false }],
  versions: []
}

const PROJECT_B = {
  id: 'project-2',
  title: 'Projeto Beta',
  status: 'WORKING',
  progress: 0,
  advisor: '',
  stages: [{ id: 'stage-2', name: 'Metodologia', done: false }],
  versions: []
}

const now = new Date()
const TASK: MockTask = {
  id: 'task-1',
  title: 'Revisar capítulo 2',
  details: 'Gravar aula: https://example.com/aula-1',
  createdDate: now.toISOString(),
  startDate: new Date(now.getTime() - 60 * 60 * 1000).toISOString(),
  endDate: new Date(now.getTime() + 2 * 86_400_000).toISOString(),
  type: 'UNIQUE',
  status: 'PENDING',
  projectId: 'project-1',
  stageId: 'stage-1',
  dailyStatistics: null,
  dailyCompletions: []
}

function stateWithTask(): MockState {
  return {
    folders: [],
    projects: [PROJECT_A, PROJECT_B],
    projectDetails: { 'project-1': PROJECT_A, 'project-2': PROJECT_B },
    tasks: [structuredClone(TASK)]
  }
}

async function openTasks(page: Parameters<typeof signIn>[0]) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Tarefas' }).click()
  await expect(page.getByRole('heading', { name: 'Tarefas', level: 1 })).toBeVisible()
}

test('edita a tarefa incluindo projeto e etapa', async ({ page }) => {
  const state = stateWithTask()
  await signIn(page)
  await mockApi(page, state)
  await openTasks(page)

  await page.getByRole('button', { name: 'Editar tarefa' }).click()
  await expect(page.getByText('Editando tarefa')).toBeVisible()

  const form = page.locator('.task-form')
  await expect(form.getByLabel('Título da tarefa')).toHaveValue(TASK.title)
  await expect(form.getByLabel('Detalhes')).toHaveValue(TASK.details)
  await expect(form.getByLabel('Projeto ou curso')).toHaveValue('project-1')
  await expect(form.getByLabel('Etapa')).toHaveValue('stage-1')

  await form.getByLabel('Título da tarefa').fill('Revisar capítulo 3')
  await form.getByLabel('Detalhes').fill('Aula 2: https://example.com/aula-2')
  await form.getByLabel('Projeto ou curso').selectOption('project-2')
  await expect(form.getByLabel('Etapa')).toHaveValue('')
  await form.getByLabel('Etapa').selectOption('stage-2')
  await form.getByRole('button', { name: 'Salvar alterações' }).click()

  await expect.poll(() => state.tasks?.[0]).toMatchObject({
    title: 'Revisar capítulo 3',
    details: 'Aula 2: https://example.com/aula-2',
    projectId: 'project-2',
    stageId: 'stage-2'
  })
  await expect(page.locator('.task-row')).toContainText('Revisar capítulo 3')
  await expect(page.locator('.task-row')).toContainText('Projeto Beta')
  await expect(page.locator('.task-row')).toContainText('Metodologia')
  await page.locator('.task-view').click()
  await expect(page.locator('.task-detail')).toContainText('Aula 2:')
  await expect(page.locator('.task-form')).toBeHidden()
})

test('mostra os detalhes da tarefa com o link clicável', async ({ page }) => {
  await signIn(page)
  await mockApi(page, stateWithTask())
  await openTasks(page)

  const view = page.locator('.task-view')
  await expect(page.locator('.task-detail')).toHaveCount(0)
  await view.click()
  await expect(view).toHaveAttribute('aria-expanded', 'true')
  await expect(view).toHaveAttribute('aria-label', 'Ocultar detalhes da tarefa')

  const detail = page.locator('.task-detail')
  await expect(detail).toContainText('Gravar aula:')
  await expect(detail.getByRole('link', { name: 'https://example.com/aula-1' }))
    .toHaveAttribute('href', 'https://example.com/aula-1')

  await view.click()
  await expect(page.locator('.task-detail')).toHaveCount(0)
})

test('marca subtarefas do checklist nos detalhes', async ({ page }) => {
  const state = stateWithTask()
  state.tasks = [{
    ...structuredClone(TASK),
    details: '- [ ] Aula 1\n- [ ] **Aula 2**'
  }]
  await signIn(page)
  await mockApi(page, state)
  await openTasks(page)

  await page.getByRole('button', { name: 'Ver detalhes da tarefa' }).click()
  const checks = page.locator('.task-detail input[type="checkbox"]')
  await expect(checks).toHaveCount(2)
  await expect(checks.nth(0)).not.toBeChecked()

  await checks.nth(1).click()
  await expect.poll(() => state.tasks?.[0].details).toBe('- [ ] Aula 1\n- [x] **Aula 2**')
  await expect(checks.nth(1)).toBeChecked()
  await expect(checks.nth(0)).not.toBeChecked()
})

test('tarefa única concluída aparece só no dia da execução no calendário', async ({ page }) => {
  const today = new Date()
  const weekStart = new Date(today.getFullYear(), today.getMonth(), today.getDate() - ((today.getDay() + 6) % 7))
  const weekEnd = new Date(weekStart.getFullYear(), weekStart.getMonth(), weekStart.getDate() + 6)
  const inWeek = (date: Date, hour: number) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate(), hour).toISOString()

  const task: MockTask = {
    ...TASK,
    id: 'task-week',
    title: 'Entregar capítulo',
    startDate: inWeek(weekStart, 9),
    endDate: inWeek(weekEnd, 18),
    projectId: null,
    stageId: null,
    completedDate: null
  }
  const state: MockState = { folders: [], tasks: [task] }
  await signIn(page)
  await mockApi(page, state)
  await openTasks(page)

  const days = page.locator('.calendar-day')
  await expect(days).toHaveCount(7)
  for (let index = 0; index < 7; index += 1) {
    await expect(days.nth(index)).toContainText('0/1')
  }

  await page.locator('.task-row').click()
  await expect.poll(() => state.tasks?.[0].completedDate).toBeTruthy()
  const todayIndex = (today.getDay() + 6) % 7
  await expect(days.nth(todayIndex)).toContainText('1/1')
  for (let index = 0; index < 7; index += 1) {
    if (index !== todayIndex) await expect(days.nth(index)).not.toContainText('/1')
  }

  await page.locator('.task-row').click()
  await expect.poll(() => state.tasks?.[0].completedDate).toBeNull()
  for (let index = 0; index < 7; index += 1) {
    await expect(days.nth(index)).toContainText('0/1')
  }
})

test('abre a lista no dia atual, sem as tarefas das próximas 24h', async ({ page }) => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(9, 0, 0, 0)

  const tomorrowTask: MockTask = {
    ...TASK,
    id: 'task-tomorrow',
    title: 'Entregar amanhã',
    startDate: tomorrow.toISOString(),
    endDate: tomorrow.toISOString(),
    projectId: null,
    stageId: null
  }
  const state = stateWithTask()
  state.tasks = [structuredClone(TASK), tomorrowTask]
  await signIn(page)
  await mockApi(page, state)
  await openTasks(page)

  await expect(page.locator('.calendar-filter')).toContainText('Filtrando por')
  await expect(page.locator('.task-row')).toContainText('Revisar capítulo 2')
  await expect(page.getByText('Entregar amanhã')).toBeHidden()
})

test('copia as tarefas do dia como checklist', async ({ page, context }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write'])
  await signIn(page)
  await mockApi(page, stateWithTask())
  await openTasks(page)

  await page.getByRole('button', { name: 'Copiar tarefas' }).click()
  await expect(page.getByRole('button', { name: 'Copiado!' })).toBeVisible()

  const clipboard = await page.evaluate(() => navigator.clipboard.readText())
  expect(clipboard).toBe('- [] Revisar capítulo 2')
})

test('cancelar a edição volta o formulário para o modo de criação', async ({ page }) => {
  await signIn(page)
  await mockApi(page, stateWithTask())
  await openTasks(page)

  await page.getByRole('button', { name: 'Editar tarefa' }).click()
  const form = page.locator('.task-form')
  await form.getByRole('button', { name: 'Cancelar' }).click()
  await expect(form).toBeHidden()

  await page.locator('.floating-add').click()
  await expect(page.getByText('Editando tarefa')).toBeHidden()
  await expect(page.locator('.task-form').getByRole('button', { name: 'Adicionar tarefa' })).toBeVisible()
})

const dayKeyAt = (offset: number) => {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

test('estatística de recorrente sem prazo final conta os dias da janela', async ({ page }) => {
  const start = new Date()
  start.setDate(start.getDate() - 10)
  const daily: MockTask = {
    id: 'task-daily-stats',
    title: 'Estudar diariamente',
    createdDate: start.toISOString(),
    startDate: start.toISOString(),
    endDate: null,
    type: 'DAILY',
    status: 'PENDING',
    projectId: null,
    stageId: null,
    dailyStatistics: null,
    dailyCompletions: [
      { completionDate: dayKeyAt(-5), completedAt: start.toISOString() },
      { completionDate: dayKeyAt(-3), completedAt: start.toISOString() }
    ]
  }
  await signIn(page)
  await mockApi(page, { folders: [], tasks: [daily] })
  await openTasks(page)

  await page.getByRole('button', { name: 'Estatísticas' }).click()

  const stats = page.locator('.task-stats-card')
  await expect(stats).toContainText('Últimos 30 dias')
  await expect(stats).toContainText('2 dia(s) concluído(s)')
  await expect(stats).toContainText('8 dia(s) não concluído(s)')
})

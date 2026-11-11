import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'
import type { MockState, MockTask } from './support/mockApi'

const now = new Date()
const todayKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

function task(id: string, title: string, status: string, extra: Partial<MockTask> = {}): MockTask {
  return {
    id,
    title,
    createdDate: now.toISOString(),
    startDate: now.toISOString(),
    endDate: now.toISOString(),
    type: 'UNIQUE',
    status,
    dailyStatistics: null,
    dailyCompletions: [],
    ...extra
  }
}

function stateWithTasks(): MockState {
  return {
    folders: [],
    tasks: [
      task('task-pending', 'Revisar capítulo 2', 'PENDING'),
      task('task-working', 'Escrever introdução', 'WORKING'),
      task('task-delayed', 'Entregar relatório', 'DELAYED'),
      task('task-done', 'Ler artigo', 'DONE', { completedDate: now.toISOString() }),
      task('task-daily', 'Praticar exercícios', 'PENDING', {
        type: 'DAILY',
        dailyStatistics: { scheduledDays: 1, completedDays: 1, skippedDays: 0, completedToday: true },
        dailyCompletions: [{ completionDate: todayKey, completedAt: now.toISOString() }]
      })
    ]
  }
}

async function openKanban(page: Parameters<typeof signIn>[0]) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Kanban' }).click()
  await expect(page.getByRole('heading', { name: 'Kanban', level: 1 })).toBeVisible()
}

const taskColumn = (page: Parameters<typeof signIn>[0], heading: string) =>
  page.locator('.kanban-column').filter({ has: page.getByRole('heading', { name: heading }) })

const projectColumn = (page: Parameters<typeof signIn>[0], heading: string) =>
  page.locator('.kanban-column').filter({ has: page.getByRole('heading', { name: heading }) })

test('agrupa tarefas em pendentes, em andamento, atrasadas e concluídas', async ({ page }) => {
  await signIn(page)
  await mockApi(page, stateWithTasks())
  await openKanban(page)

  const pending = taskColumn(page, 'Pendentes')
  const working = taskColumn(page, 'Em andamento')
  const delayed = taskColumn(page, 'Atrasadas')
  const done = taskColumn(page, 'Concluídas')

  await expect(pending.getByText('Revisar capítulo 2')).toBeVisible()
  await expect(working.getByText('Escrever introdução')).toBeVisible()
  await expect(delayed.getByText('Entregar relatório')).toBeVisible()
  await expect(done.getByText('Ler artigo')).toBeVisible()
  // A DAILY concluída hoje entra em Concluídas mesmo com `status` pendente.
  await expect(done.getByText('Praticar exercícios')).toBeVisible()
  await expect(done.locator('.kanban-count')).toHaveText('2')
})

test('mover tarefa para em andamento atualiza o status', async ({ page }) => {
  const state = stateWithTasks()
  await signIn(page)
  await mockApi(page, state)
  await openKanban(page)

  const card = taskColumn(page, 'Pendentes').locator('.kanban-card').filter({ hasText: 'Revisar capítulo 2' })
  await card.getByLabel('Status da tarefa').selectOption('WORKING')

  await expect.poll(() => state.tasks?.find(item => item.id === 'task-pending')?.status).toBe('WORKING')
  await expect(taskColumn(page, 'Em andamento').getByText('Revisar capítulo 2')).toBeVisible()
  await expect(taskColumn(page, 'Pendentes').getByText('Revisar capítulo 2')).toHaveCount(0)
})

test('mover tarefa para concluída atualiza o status', async ({ page }) => {
  const state = stateWithTasks()
  await signIn(page)
  await mockApi(page, state)
  await openKanban(page)

  const card = taskColumn(page, 'Pendentes').locator('.kanban-card').filter({ hasText: 'Revisar capítulo 2' })
  await card.getByLabel('Status da tarefa').selectOption('DONE')

  await expect.poll(() => state.tasks?.find(item => item.id === 'task-pending')?.status).toBe('DONE')
  await expect(taskColumn(page, 'Concluídas').getByText('Revisar capítulo 2')).toBeVisible()
  await expect(taskColumn(page, 'Pendentes').getByText('Revisar capítulo 2')).toHaveCount(0)
})

test('mover tarefa atrasada para pendente limpa o atraso', async ({ page }) => {
  const state = stateWithTasks()
  await signIn(page)
  await mockApi(page, state)
  await openKanban(page)

  const card = taskColumn(page, 'Atrasadas').locator('.kanban-card').filter({ hasText: 'Entregar relatório' })
  await card.getByLabel('Status da tarefa').selectOption('PENDING')

  await expect.poll(() => state.tasks?.find(item => item.id === 'task-delayed')?.status).toBe('PENDING')
  await expect(taskColumn(page, 'Pendentes').getByText('Entregar relatório')).toBeVisible()
})

test('tarefa diária não oferece a coluna de atrasadas', async ({ page }) => {
  await signIn(page)
  await mockApi(page, stateWithTasks())
  await openKanban(page)

  const card = taskColumn(page, 'Concluídas').locator('.kanban-card').filter({ hasText: 'Praticar exercícios' })
  const status = card.getByLabel('Status da tarefa')
  await expect(status.locator('option[value="DELAYED"]')).toHaveCount(0)
  await expect(status.locator('option[value="WORKING"]')).toHaveCount(1)
})

test('recarrega a página mantendo o kanban aberto', async ({ page }) => {
  await signIn(page)
  await mockApi(page, stateWithTasks())
  await openKanban(page)

  await expect(page).toHaveURL(/#\/kanban$/)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Kanban', level: 1 })).toBeVisible()
})

test('aba de projetos move o projeto entre pendente, em andamento e concluídos', async ({ page }) => {
  const alpha = { id: 'project-a', title: 'TCC de História', status: 'WAITING', progress: 40, advisor: 'Prof. Silva', stages: [], versions: [] }
  const beta = { id: 'project-b', title: 'Iniciação científica', status: 'DONE', progress: 100, advisor: '', stages: [], versions: [] }
  const state: MockState = { folders: [], projects: [alpha, beta] }
  await signIn(page)
  await mockApi(page, state)
  await openKanban(page)

  await page.locator('.kanban-switch').getByRole('button', { name: 'Projetos' }).click()
  const waiting = projectColumn(page, 'Pendentes')
  const working = projectColumn(page, 'Em andamento')
  const done = projectColumn(page, 'Concluídos')
  await expect(waiting.getByText('TCC de História')).toBeVisible()
  await expect(done.getByText('Iniciação científica')).toBeVisible()

  const card = waiting.locator('.kanban-card').filter({ hasText: 'TCC de História' })
  await card.getByLabel('Status do projeto').selectOption('WORKING')
  await expect.poll(() => (state.projects?.find(item => (item as { id?: string }).id === 'project-a') as { status?: string } | undefined)?.status).toBe('WORKING')
  await expect(working.getByText('TCC de História')).toBeVisible()

  await working.locator('.kanban-card').filter({ hasText: 'TCC de História' })
    .getByLabel('Status do projeto').selectOption('DONE')
  await expect.poll(() => (state.projects?.find(item => (item as { id?: string }).id === 'project-a') as { status?: string } | undefined)?.status).toBe('DONE')
  await expect(done.getByText('TCC de História')).toBeVisible()
  await expect(working.getByText('TCC de História')).toHaveCount(0)
})

import { expect, test } from '@playwright/test'
import type { APIRequestContext } from '@playwright/test'
import { API_BASE_URL, createTask, deleteTaskQuietly, getTask, login, uniqueSuffix } from './support/api'
import { signIn } from './support/ui'

interface ProjectSummary {
  id: string
  title: string
}

interface ProjectDetails {
  stages: Array<{ id: string; name: string }>
}

async function projectDetails(request: APIRequestContext, token: string, id: string): Promise<ProjectDetails> {
  const response = await request.get(`${API_BASE_URL}/study/projects/${id}`, { headers: { Authorization: `Bearer ${token}` } })
  expect(response.ok(), await response.text()).toBeTruthy()
  return (await response.json()).data as ProjectDetails
}

test('edita tarefa pela UI, incluindo projeto e etapa, e persiste no back real', async ({ page, request }) => {
  const token = await login(request)
  const projectsResponse = await request.get(`${API_BASE_URL}/study/projects?page=0&size=100`, { headers: { Authorization: `Bearer ${token}` } })
  expect(projectsResponse.ok(), await projectsResponse.text()).toBeTruthy()
  const projects = (await projectsResponse.json()).data.items as ProjectSummary[]
  test.skip(!projects.length, 'O usuário real não tem projetos para vincular à tarefa.')

  let source: ProjectSummary | null = null
  let sourceStage: { id: string; name: string } | null = null
  let target: ProjectSummary | null = null
  let targetStage: { id: string; name: string } | null = null
  for (const project of projects) {
    const details = await projectDetails(request, token, project.id)
    if (!details.stages?.length) continue
    if (!source) {
      source = project
      sourceStage = details.stages[0]
    } else {
      target = project
      targetStage = details.stages[0]
      break
    }
  }
  test.skip(!source || !sourceStage, 'Nenhum projeto com etapa para vincular à tarefa.')

  const originalTitle = `Tarefa E2E ${uniqueSuffix()}`
  const editedTitle = `${originalTitle} editada`
  const start = new Date()
  const end = new Date(start.getTime() + 2 * 86_400_000)
  const task = await createTask(request, token, {
    title: originalTitle,
    startDate: start.toISOString(),
    endDate: end.toISOString(),
    type: 'UNIQUE',
    projectId: source.id,
    stageId: sourceStage.id
  })

  try {
    await signIn(page)
    await page.getByRole('button', { name: 'Tarefas' }).click()

    const row = page.locator('.task-line').filter({ hasText: originalTitle })
    await row.getByRole('button', { name: 'Editar tarefa' }).click()
    const form = page.locator('.task-form')
    await form.getByLabel('Título da tarefa').fill(editedTitle)
    if (target && targetStage) {
      await form.getByLabel('Projeto').selectOption(target.id)
      await form.getByLabel('Etapa').selectOption(targetStage.id)
    }
    await form.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect.poll(async () => (await getTask(request, token, task.id))).toMatchObject({
      title: editedTitle,
      ...(target && targetStage ? { projectId: target.id, stageId: targetStage.id } : {})
    })
    await expect(row).toContainText(editedTitle)
  } finally {
    await deleteTaskQuietly(request, token, task.id)
  }
})

test('concluir tarefa única grava e limpa a data de execução no back real', async ({ page, request }) => {
  const token = await login(request)
  const start = new Date()
  const end = new Date(start.getTime() + 2 * 86_400_000)
  const title = `Tarefa única E2E ${uniqueSuffix()}`
  const task = await createTask(request, token, {
    title,
    startDate: start.toISOString(),
    endDate: end.toISOString(),
    type: 'UNIQUE',
    projectId: null,
    stageId: null
  })

  try {
    await signIn(page)
    await page.getByRole('button', { name: 'Tarefas' }).click()
    const row = page.locator('.task-line').filter({ hasText: title })
    await row.locator('.task-row').click()

    const expectedDay = await page.evaluate(() => {
      const now = new Date()
      const pad = (part: number) => String(part).padStart(2, '0')
      return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    })
    await expect.poll(async () => (await getTask(request, token, task.id)).completedDate).toBe(expectedDay)

    await row.locator('.task-row').click()
    await expect.poll(async () => (await getTask(request, token, task.id)).completedDate).toBeNull()
  } finally {
    await deleteTaskQuietly(request, token, task.id)
  }
})

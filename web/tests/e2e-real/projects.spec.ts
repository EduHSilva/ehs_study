import { expect, test } from '@playwright/test'
import { API_BASE_URL, login } from './support/api'
import { signIn } from './support/ui'

test('carrega a lista de projetos do back real', async ({ page }) => {
  await signIn(page)

  await page.getByRole('button', { name: 'Projetos' }).click()
  await expect(page.getByRole('heading', { name: 'Projetos', level: 1 })).toBeVisible()
  await expect(page.locator('.sync-error')).toBeHidden()
})

test('altera etapa pela UI e persiste no back real', async ({ page, request }) => {
  const token = await login(request)
  const projectsResponse = await request.get(`${API_BASE_URL}/study/projects?page=0&size=100`, { headers: { Authorization: `Bearer ${token}` } })
  expect(projectsResponse.ok(), await projectsResponse.text()).toBeTruthy()
  const projects = (await projectsResponse.json()).data.items as Array<{ id: string; title: string }>
  test.skip(!projects.length, 'O usuário real não tem projetos para editar.')

  const project = projects[0]
  const detailsResponse = await request.get(`${API_BASE_URL}/study/projects/${project.id}`, { headers: { Authorization: `Bearer ${token}` } })
  expect(detailsResponse.ok(), await detailsResponse.text()).toBeTruthy()
  const details = (await detailsResponse.json()).data as { stages: Array<{ id: string; name: string; done: boolean }> }
  test.skip(!details.stages.length, 'O primeiro projeto não tem etapas para editar.')
  const stage = details.stages[0]

  try {
    await signIn(page)
    await page.getByRole('button', { name: 'Projetos' }).click()
    await page.getByRole('button', { name: new RegExp(project.title) }).click()
    const stageRow = page.locator('.stage-item').filter({ hasText: stage.name }).first()
    await stageRow.getByRole('button', { name: stage.done ? 'Reabrir etapa' : 'Marcar etapa como concluída' }).click()
    await expect.poll(async () => {
      const response = await request.get(`${API_BASE_URL}/study/projects/${project.id}`, { headers: { Authorization: `Bearer ${token}` } })
      return ((await response.json()).data.stages as typeof details.stages).find(item => item.id === stage.id)?.done
    }).toBe(!stage.done)
  } finally {
    await request.put(`${API_BASE_URL}/study/project/stage/${stage.id}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { idProject: project.id, title: stage.name, done: stage.done }
    })
  }
})

test('edita versão pela UI e persiste no back real', async ({ page, request }) => {
  const token = await login(request)
  const projectsResponse = await request.get(`${API_BASE_URL}/study/projects?page=0&size=100`, { headers: { Authorization: `Bearer ${token}` } })
  const projects = (await projectsResponse.json()).data.items as Array<{ id: string; title: string }>
  test.skip(!projects?.length, 'O usuário real não tem projetos para editar.')
  const project = projects[0]
  const detailsResponse = await request.get(`${API_BASE_URL}/study/projects/${project.id}`, { headers: { Authorization: `Bearer ${token}` } })
  const details = (await detailsResponse.json()).data as { versions: Array<{ id: string; title: string; dueDate: string }> }
  test.skip(!details.versions?.length, 'O primeiro projeto não tem versões para editar.')
  const version = details.versions[0]
  const temporaryTitle = `${version.title} E2E`

  try {
    await signIn(page)
    await page.getByRole('button', { name: 'Projetos' }).click()
    await page.getByRole('button', { name: new RegExp(project.title) }).click()
    await page.getByRole('button', { name: `Editar versão: ${version.title}` }).click()
    const editForm = page.locator('.version-edit')
    await editForm.getByLabel('Título da versão').fill(temporaryTitle)
    await editForm.getByRole('button', { name: 'Salvar' }).click()
    await expect.poll(async () => {
      const response = await request.get(`${API_BASE_URL}/study/projects/${project.id}`, { headers: { Authorization: `Bearer ${token}` } })
      return ((await response.json()).data.versions as typeof details.versions).find(item => item.id === version.id)?.title
    }).toBe(temporaryTitle)
  } finally {
    await request.put(`${API_BASE_URL}/study/project/version/${version.id}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { idProject: project.id, title: version.title, dueDate: version.dueDate }
    })
  }
})

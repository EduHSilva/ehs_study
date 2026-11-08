import { expect, test } from '@playwright/test'
import { API_BASE_URL, createProject, deleteProjectQuietly, getProject, login, uniqueSuffix } from './support/api'
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

test('cria, edita, conclui e exclui projeto pelo back real', async ({ page, request }) => {
  const token = await login(request)
  const suffix = uniqueSuffix()
  const activeTitle = `aaa E2E ${suffix}`
  const editedTitle = `aaa E2E ${suffix} editado`
  const otherTitle = `zzz E2E ${suffix}`
  const first = await createProject(request, token, activeTitle, 'Orientador Inicial')
  const second = await createProject(request, token, otherTitle, '')
  const projectButtons = page.locator('.project-list button.project')

  const order = async (title = editedTitle) => {
    const texts = await projectButtons.allTextContents()
    return {
      first: texts.findIndex(text => text.includes(title)),
      second: texts.findIndex(text => text.includes(otherTitle))
    }
  }

  try {
    await signIn(page)
    await page.getByRole('button', { name: 'Projetos' }).click()

    await expect(page.getByRole('button', { name: new RegExp(activeTitle) })).toBeVisible()
    await expect(page.getByRole('button', { name: new RegExp(otherTitle) })).toBeVisible()
    const initial = await order(activeTitle)
    expect(initial.first).toBeGreaterThanOrEqual(0)
    expect(initial.second).toBeGreaterThan(initial.first)

    await page.getByRole('button', { name: new RegExp(activeTitle) }).click()
    await page.getByRole('button', { name: 'Editar projeto' }).click()
    const form = page.locator('.project-edit-form')
    await form.getByLabel('Título do projeto').fill(editedTitle)
    await form.getByLabel('Responsável (opcional)').fill('Orientador Editado')
    await form.getByRole('button', { name: 'Salvar' }).click()
    await expect.poll(async () => (await getProject(request, token, first.id)).title).toBe(editedTitle)
    await expect.poll(async () => (await getProject(request, token, first.id)).advisor).toBe('Orientador Editado')

    await page.getByRole('button', { name: 'Concluir projeto' }).click()
    await expect.poll(async () => (await getProject(request, token, first.id)).status).toBe('DONE')
    await expect.poll(async () => {
      const current = await order()
      return current.first > current.second && current.second >= 0
    }).toBe(true)

    await page.getByRole('button', { name: 'Excluir projeto' }).click()
    await page.getByRole('button', { name: 'Excluir', exact: true }).click()
    await expect(page.getByRole('button', { name: new RegExp(editedTitle) })).toHaveCount(0)
    await expect.poll(async () => {
      const response = await request.get(`${API_BASE_URL}/study/projects/${first.id}`, { headers: { Authorization: `Bearer ${token}` } })
      return response.ok()
    }).toBe(false)
  } finally {
    await deleteProjectQuietly(request, token, first.id)
    await deleteProjectQuietly(request, token, second.id)
  }
})

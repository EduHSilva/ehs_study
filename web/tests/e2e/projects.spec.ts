import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

const PROJECT = {
  id: 'project-1',
  title: 'TCC: História do Brasil',
  status: 'WORKING',
  progress: 50,
  advisor: 'Prof. Silva',
  stages: [{ id: 'stage-1', name: 'Introdução', done: false }],
  versions: [{ id: 'version-1', title: 'Entrega do capítulo 1', dueDate: '2026-11-12T12:00:00' }]
}

test('carrega os projetos pelo prefixo /study', async ({ page }) => {
  await signIn(page)
  await mockApi(page, {
    folders: [],
    projects: [PROJECT],
    projectDetails: { 'project-1': PROJECT }
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()

  await expect(page.getByRole('heading', { name: 'Projetos', level: 1 })).toBeVisible()
  await expect(page.getByRole('button', { name: /TCC: História do Brasil/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /Introdução/ })).toBeVisible()
  await expect(page.getByText('Entrega do capítulo 1')).toBeVisible()
})

const PROJECT_WITH_HISTORY = {
  ...PROJECT,
  stages: [
    { id: 'stage-1', name: 'Introdução', done: true },
    { id: 'stage-2', name: 'Metodologia', done: false }
  ],
  versions: [
    { id: 'version-1', title: 'Versão antiga', dueDate: new Date(Date.now() - 2 * 86_400_000).toISOString() },
    { id: 'version-2', title: 'Próxima entrega', dueDate: new Date(Date.now() + 2 * 86_400_000).toISOString() }
  ]
}

test('cadastra projeto pela tela de projetos', async ({ page }) => {
  const state = { folders: [], projects: [] }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: /Novo projeto/ }).click()
  await page.getByRole('textbox', { name: 'Título do projeto' }).fill('Pesquisa de campo')
  await page.getByRole('textbox', { name: 'Responsável (opcional)' }).fill('Prof. Costa')
  await page.getByRole('button', { name: 'Cadastrar projeto' }).click()

  await expect(page.getByRole('heading', { name: 'Pesquisa de campo' })).toBeVisible()
  expect(state.projects).toHaveLength(1)
  expect(state.projects[0]).toMatchObject({ title: 'Pesquisa de campo', advisor: 'Prof. Costa', status: 'WAITING' })
})

test('recolhe etapas, filtra concluídos e destaca prazos das versões', async ({ page }) => {
  await signIn(page)
  await mockApi(page, {
    folders: [],
    projects: [PROJECT_WITH_HISTORY],
    projectDetails: { 'project-1': PROJECT_WITH_HISTORY }
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()

  const stageHeader = page.getByRole('button', { name: /Metodologia/ })
  await expect(stageHeader).toHaveAttribute('aria-expanded', 'false')
  await stageHeader.click()
  await expect(stageHeader).toHaveAttribute('aria-expanded', 'true')
  await expect(page.getByText('Nenhuma tarefa nesta etapa.')).toBeVisible()

  await page.getByRole('button', { name: 'Ocultar concluídos' }).click()
  await expect(page.getByRole('button', { name: /Introdução/ })).toBeHidden()
  await expect(page.getByRole('button', { name: /Metodologia/ })).toBeVisible()

  await page.getByRole('button', { name: 'Mostrar concluídos' }).click()
  await expect(page.getByRole('button', { name: /Introdução/ })).toBeVisible()

  await expect(page.getByText('Atrasada há 2 dia(s)')).toBeVisible()
  await expect(page.getByText('Entrega em 2 dia(s)')).toBeVisible()
})

test('edita título e responsável do projeto', async ({ page }) => {
  const project = structuredClone(PROJECT)
  const state = { folders: [], projects: [project], projectDetails: { 'project-1': project } }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: /TCC: História do Brasil/ }).click()
  await page.getByRole('button', { name: 'Editar projeto' }).click()
  const form = page.locator('.project-edit-form')
  await form.getByLabel('Título do projeto').fill('TCC revisado')
  await form.getByLabel('Responsável (opcional)').fill('Prof. Lima')
  await form.getByRole('button', { name: 'Salvar' }).click()

  await expect(page.getByRole('heading', { name: 'TCC revisado' })).toBeVisible()
  await expect(page.getByText('Prof. Lima')).toBeVisible()
  expect(state.projectDetails['project-1']).toMatchObject({ title: 'TCC revisado', advisor: 'Prof. Lima', status: 'WORKING' })
})

test('conclui projeto e ele vai para o fim da lista', async ({ page }) => {
  const alpha = { id: 'project-a', title: 'Alpha', status: 'WORKING', progress: 0, advisor: '', stages: [], versions: [] }
  const zeta = { id: 'project-z', title: 'Zeta', status: 'WORKING', progress: 0, advisor: '', stages: [], versions: [] }
  const state = { folders: [], projects: [alpha, zeta], projectDetails: { 'project-a': alpha, 'project-z': zeta } }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  const list = page.locator('.project-list button.project')
  await expect(list.nth(0)).toContainText('Alpha')
  await expect(list.nth(1)).toContainText('Zeta')

  await list.nth(1).click()
  await page.getByRole('button', { name: 'Concluir projeto' }).click()

  await expect(list.nth(0)).toContainText('Alpha')
  await expect(list.nth(1)).toContainText('Zeta')
  await expect(list.nth(1)).toContainText('Concluído')
  expect(state.projectDetails['project-z'].status).toBe('DONE')
})

test('exclui projeto após confirmar', async ({ page }) => {
  const project = structuredClone(PROJECT)
  const state = { folders: [], projects: [project], projectDetails: { 'project-1': project } }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: /TCC: História do Brasil/ }).click()
  await page.getByRole('button', { name: 'Excluir projeto' }).click()
  await expect(page.getByText('Excluir este projeto?')).toBeVisible()
  await page.getByRole('button', { name: 'Excluir', exact: true }).click()

  await expect(page.getByRole('button', { name: /TCC: História do Brasil/ })).toHaveCount(0)
  await expect(page.getByText('Nenhum projeto ainda.')).toBeVisible()
  expect(state.projects).toHaveLength(0)
})

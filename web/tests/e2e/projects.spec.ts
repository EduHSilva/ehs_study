import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

const PROJECT = {
  id: 'project-1',
  title: 'TCC: História do Brasil',
  status: 'IN_PROGRESS',
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

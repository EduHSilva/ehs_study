import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

const PROJECT = {
  id: 'project-1',
  title: 'TCC: História do Brasil',
  status: 'IN_PROGRESS',
  progress: 50,
  advisor: 'Prof. Silva',
  stages: [{ id: 'stage-1', name: 'Introdução', done: false }],
  milestone: [{ id: 'milestone-1', title: 'Entrega do capítulo 1', dueDate: '2026-11-12T12:00:00' }]
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

  await expect(page.getByRole('heading', { name: 'Projetos Acadêmicos' })).toBeVisible()
  await expect(page.getByRole('button', { name: /TCC: História do Brasil/ })).toBeVisible()
  await expect(page.getByText('Introdução')).toBeVisible()
  await expect(page.getByText('Entrega do capítulo 1')).toBeVisible()
})

test('cadastra projeto pela tela de projetos', async ({ page }) => {
  const state = { folders: [], projects: [] }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: /Novo projeto/ }).click()
  await page.getByRole('textbox', { name: 'Título do projeto' }).fill('Pesquisa de campo')
  await page.getByRole('textbox', { name: 'Orientador (opcional)' }).fill('Prof. Costa')
  await page.getByRole('button', { name: 'Cadastrar projeto' }).click()

  await expect(page.getByRole('heading', { name: 'Pesquisa de campo' })).toBeVisible()
  expect(state.projects).toHaveLength(1)
  expect(state.projects[0]).toMatchObject({ title: 'Pesquisa de campo', advisor: 'Prof. Costa', status: 'WAITING' })
})

import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'
import type { MockTask } from './support/mockApi'

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

  await expect(page.getByRole('heading', { name: 'Projetos e cursos', level: 1 })).toBeVisible()
  await expect(page.getByRole('button', { name: /TCC: História do Brasil/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /Introdução/ })).toBeVisible()
  await expect(page.getByText('Entrega do capítulo 1')).toBeVisible()
})

test('exibe o progresso calculado pelo back', async ({ page }) => {
  const project = {
    id: 'project-1',
    title: 'TCC: História do Brasil',
    status: 'WORKING',
    progress: 75,
    advisor: 'Prof. Silva',
    stages: [
      { id: 'stage-1', name: 'Introdução', done: true },
      { id: 'stage-2', name: 'Metodologia', done: true },
      { id: 'stage-3', name: 'Resultados', done: false }
    ],
    versions: []
  }
  const doneTask: MockTask = {
    id: 'task-1',
    title: 'Ler capítulo 1',
    type: 'UNIQUE',
    status: 'DONE',
    projectId: 'project-1',
    stageId: 'stage-1',
    dailyStatistics: null,
    dailyCompletions: []
  }
  const pendingTask: MockTask = {
    id: 'task-2',
    title: 'Escrever metodologia',
    type: 'UNIQUE',
    status: 'PENDING',
    projectId: 'project-1',
    stageId: 'stage-2',
    dailyStatistics: null,
    dailyCompletions: []
  }
  await signIn(page)
  await mockApi(page, {
    folders: [],
    projects: [project],
    projectDetails: { 'project-1': project },
    tasks: [doneTask, pendingTask]
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()

  // 3 etapas (2 concluídas) + 2 tarefas (1 concluída) dariam 60% no front; o valor exibido é o do back.
  await expect(page.getByText('75% Concluído')).toBeVisible()
})

test('aplica o progresso devolvido pelo back ao concluir etapa', async ({ page }) => {
  const project = {
    id: 'project-1',
    title: 'TCC: História do Brasil',
    status: 'WORKING',
    progress: 25,
    advisor: 'Prof. Silva',
    stages: [{ id: 'stage-1', name: 'Introdução', done: false, projectProgress: 80 }],
    versions: []
  }
  await signIn(page)
  await mockApi(page, {
    folders: [],
    projects: [project],
    projectDetails: { 'project-1': project }
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: 'Marcar etapa como concluída' }).click()

  await expect(page.getByText('80% Concluído')).toBeVisible()
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
  await page.getByRole('button', { name: /Novo projeto ou curso/ }).click()
  await page.getByRole('textbox', { name: 'Título do projeto ou curso' }).fill('Pesquisa de campo')
  await page.getByRole('textbox', { name: 'Responsável (opcional)' }).fill('Prof. Costa')
  await page.getByRole('textbox', { name: 'Link do projeto ou curso (opcional)' }).fill('https://udemy.com/estatistica')
  await page.getByRole('spinbutton', { name: 'Quantidade de módulos (opcional)' }).fill('3')
  await page.getByRole('button', { name: 'Cadastrar projeto ou curso' }).click()

  await expect(page.getByRole('heading', { name: 'Pesquisa de campo' })).toBeVisible()
  await expect(page.getByRole('link', { name: /Abrir link do projeto ou curso/ }))
    .toHaveAttribute('href', 'https://udemy.com/estatistica')
  await expect(page.getByRole('button', { name: /Módulo 3/ })).toBeVisible()
  expect(state.projects).toHaveLength(1)
  expect(state.projects[0]).toMatchObject({ title: 'Pesquisa de campo', advisor: 'Prof. Costa', status: 'WAITING' })
  const created = state.projects[0] as unknown as { link: string; stages: Array<{ name: string }> }
  expect(created.link).toBe('https://udemy.com/estatistica')
  expect(created.stages.map(stage => stage.name)).toEqual(['Módulo 1', 'Módulo 2', 'Módulo 3'])
})

test('renomeia o título de uma etapa', async ({ page }) => {
  const project = structuredClone(PROJECT)
  const state = { folders: [], projects: [project], projectDetails: { 'project-1': project } }
  await signIn(page)
  await mockApi(page, state)

  await page.goto('/')
  await page.getByRole('button', { name: 'Projetos' }).click()
  await page.getByRole('button', { name: /TCC: História do Brasil/ }).click()
  const stageRow = page.locator('.stage-item').filter({ hasText: 'Introdução' })
  await stageRow.getByRole('button', { name: 'Editar nome da etapa' }).click()
  const form = page.locator('.stage-edit-form')
  await form.getByRole('textbox', { name: 'Nome da etapa' }).fill('Introdução revisada')
  await form.getByRole('button', { name: 'Salvar' }).click()

  await expect(page.getByRole('button', { name: /Introdução revisada/ })).toBeVisible()
  const saved = state.projectDetails['project-1'] as { stages: Array<{ name: string }> }
  expect(saved.stages[0].name).toBe('Introdução revisada')
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
  await page.getByRole('button', { name: 'Editar projeto ou curso' }).click()
  const form = page.locator('.project-edit-form')
  await form.getByLabel('Título do projeto ou curso').fill('TCC revisado')
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
  await page.getByRole('button', { name: 'Concluir projeto ou curso' }).click()

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
  await page.getByRole('button', { name: 'Excluir projeto ou curso' }).click()
  await expect(page.getByText('Excluir este projeto ou curso?')).toBeVisible()
  await page.getByRole('button', { name: 'Excluir', exact: true }).click()

  await expect(page.getByRole('button', { name: /TCC: História do Brasil/ })).toHaveCount(0)
  await expect(page.getByText('Nenhum projeto ou curso ainda.')).toBeVisible()
  expect(state.projects).toHaveLength(0)
})

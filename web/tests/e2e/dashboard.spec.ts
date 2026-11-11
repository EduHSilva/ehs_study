import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

test.beforeEach(async ({ page }) => {
  await signIn(page)
  await mockApi(page, {
    folders: [
      {
        id: 'folder-1',
        name: 'Semana 1',
        notes: [{ id: 'note-1', folderId: 'folder-1', title: 'Resumo da aula', content: 'Conteúdo da anotação' }]
      }
    ],
    dashboard: {
      weeklyProgress: { completed: 4, total: 10 },
      tasks: [
        {
          id: 'task-1',
          title: 'Revisar capítulo 2',
          endDate: new Date().toISOString(),
          type: 'UNIQUE',
          status: 'PENDING'
        },
        {
          id: 'task-2',
          title: 'Entregar amanhã',
          endDate: new Date(Date.now() + 86_400_000).toISOString(),
          type: 'UNIQUE',
          status: 'PENDING'
        }
      ],
      notes: [{ id: 'note-1', folderId: 'folder-1', title: 'Resumo da aula', content: 'Conteúdo da anotação' }]
    },
    sessions: [{ id: 'focus-1', title: 'Leitura', minutes: 25, completedAt: new Date().toISOString() }]
  })
})

test('exibe progresso semanal, tarefas e notas recentes', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: /Olá, Ana!/ })).toBeVisible()
  await expect(page.getByText('40%')).toBeVisible()
  await expect(page.getByText('Revisar capítulo 2')).toBeVisible()
  await expect(page.getByText('Resumo da aula')).toBeVisible()
  await expect(page.getByText('Conteúdo da anotação')).toBeVisible()
})

test('não lista na dashboard tarefas que não são do dia atual', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByText('Revisar capítulo 2')).toBeVisible()
  await expect(page.getByText('Entregar amanhã')).toBeHidden()
})

test('abre a nota recente na tela de anotações', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: /Resumo da aula/ }).click()

  await expect(page.getByRole('heading', { name: 'Anotações de Aula' })).toBeVisible()
  await expect(page.locator('.editor input')).toHaveValue('Resumo da aula')
  await expect(page.locator('.editor textarea')).toHaveValue('Conteúdo da anotação')
})

import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

test.beforeEach(async ({ page }) => {
  await signIn(page)
  await mockApi(page, {
    folders: [],
    dashboard: {
      weeklyProgress: { completed: 4, total: 10 },
      tasks: [
        {
          id: 'task-1',
          title: 'Revisar capítulo 2',
          endDate: '2026-10-30T15:00:00',
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

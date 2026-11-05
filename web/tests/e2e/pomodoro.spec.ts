import { expect, test } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'

test('o contador usa as sessões concluídas de hoje', async ({ page }) => {
  await signIn(page)
  await mockApi(page, {
    folders: [],
    sessions: [
      { id: 'session-1', title: 'Uma', minutes: 25, completedAt: new Date().toISOString() },
      { id: 'session-2', title: 'Duas', minutes: 25, completedAt: new Date().toISOString() }
    ]
  })
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()

  await expect(page.getByText('Sessão 3 de 4')).toBeVisible()
})

test('só salva a sessão quando o estudante finaliza o ciclo', async ({ page }) => {
  const state: { folders: []; sessions: unknown[] } = { folders: [], sessions: [] }
  await signIn(page)
  await mockApi(page, state)
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()

  await expect(page.getByText('Sessão 1 de 4')).toBeVisible()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(25 * 60 * 1000)

  await expect(page.getByText('00:00')).toBeVisible()
  expect(state.sessions).toHaveLength(0)
  await page.getByRole('button', { name: 'Finalizar sessão' }).click()

  await expect(page.getByText('Sessão de foco')).toBeVisible()
  expect(state.sessions).toHaveLength(1)
  expect(state.sessions[0]).toMatchObject({ minutes: 25 })
  await expect(page.getByText('Sessão 2 de 4')).toBeVisible()
})

import { expect, test } from '@playwright/test'
import { mockApi } from './support/mockApi'

test('faz login e abre o dashboard', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear())
  await mockApi(page, {
    folders: [],
    dashboard: { weeklyProgress: { completed: 0, total: 0 }, tasks: [], notes: [] }
  })

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Acessar conta' })).toBeVisible()

  await page.locator('input[type="email"]').fill('ana@uni.br')
  await page.locator('input[autocomplete="current-password"]').fill('123456')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page.getByRole('heading', { name: /Olá, Ana!/ })).toBeVisible()
})

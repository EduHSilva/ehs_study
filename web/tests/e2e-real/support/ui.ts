import { expect } from '@playwright/test'
import type { Page } from '@playwright/test'
import { E2E_EMAIL, E2E_PASSWORD } from './api'

export async function signIn(page: Page): Promise<void> {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Acessar conta' })).toBeVisible()
  await page.locator('input[type="email"]').fill(E2E_EMAIL)
  await page.locator('input[autocomplete="current-password"]').fill(E2E_PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { name: /Olá, / })).toBeVisible()
}

export async function openNotes(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Anotações' }).click()
  await expect(page.getByRole('heading', { name: 'Anotações de Aula' })).toBeVisible()
}

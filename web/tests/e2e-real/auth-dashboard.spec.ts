import { expect, test } from '@playwright/test'
import { signIn } from './support/ui'

test('faz login no back real e carrega o dashboard', async ({ page }) => {
  await signIn(page)

  await expect(page.getByRole('heading', { name: 'Progresso da Semana' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Anotações Recentes' })).toBeVisible()
  await expect(page.getByText('Não foi possível carregar o dashboard.')).toBeHidden()
})

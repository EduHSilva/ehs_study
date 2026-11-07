import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import { mockApi, signIn } from './support/mockApi'
import type { Folder } from './support/mockApi'

const FOLDERS: Folder[] = [
  {
    id: 'folder-1',
    name: 'Semana 1',
    notes: [
      { id: 'note-1', folderId: 'folder-1', title: 'Introdução', content: 'Primeira anotação' },
      { id: 'note-2', folderId: 'folder-1', title: 'Revolução', content: 'Segunda anotação' }
    ]
  },
  {
    id: 'folder-2',
    name: 'Semana 2',
    notes: [{ id: 'note-3', folderId: 'folder-2', title: 'Iluminismo', content: 'Terceira anotação' }]
  },
  {
    id: 'folder-3',
    name: 'Semana 3',
    notes: []
  }
]

test.beforeEach(async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: structuredClone(FOLDERS) })
})

async function openNotes(page: Page) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Anotações' }).click()
  await expect(page.getByRole('heading', { name: 'Anotações de Aula' })).toBeVisible()
}

test('lista as pastas e mostra as notas da pasta selecionada', async ({ page }) => {
  await openNotes(page)

  await expect(page.getByRole('button', { name: /Semana 1/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /Semana 2/ })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Introdução' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Revolução' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Iluminismo' })).toBeHidden()

  await page.getByRole('button', { name: /Semana 2/ }).click()

  await expect(page.getByRole('button', { name: 'Iluminismo' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Introdução' })).toBeHidden()
})

test('mostra as notas da pasta como cartões com trecho e contagem', async ({ page }) => {
  await openNotes(page)

  const cards = page.locator('.note-cards .note-card')
  await expect(cards).toHaveCount(2)
  await expect(cards.first()).toContainText('Introdução')
  await expect(cards.first()).toContainText('Primeira anotação')
  await expect(page.getByRole('button', { name: /Semana 1/ })).toContainText('2')
})

test('salva a nota selecionada', async ({ page }) => {
  await openNotes(page)

  await page.locator('.editor input').fill('Introdução revisada')
  await page.locator('.editor textarea').fill('Conteúdo atualizado')
  await page.getByRole('button', { name: 'Salvar' }).click()

  await expect(page.getByText('Salvo agora')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Introdução revisada' })).toBeVisible()
})

test('mostra pasta vazia e cria uma anotação nela', async ({ page }) => {
  await openNotes(page)
  await page.getByRole('button', { name: /Semana 3/ }).click()

  await expect(page.getByText('Não há notas nesta pasta.')).toBeVisible()
  await page.getByRole('button', { name: /Nova nota/ }).click()
  await page.getByRole('textbox', { name: 'Título da anotação' }).fill('Resumo da Semana 3')
  await page.locator('.editor textarea').fill('Notas criadas dentro da pasta selecionada.')
  await page.getByRole('button', { name: 'Criar anotação' }).click()

  await expect(page.getByRole('button', { name: 'Resumo da Semana 3' })).toBeVisible()
  await expect(page.locator('.editor textarea')).toHaveValue('Notas criadas dentro da pasta selecionada.')
  await expect(page.getByText('Salvo agora')).toBeVisible()
})

test('exclui a nota selecionada e seleciona a próxima', async ({ page }) => {
  await openNotes(page)

  await page.getByRole('button', { name: 'Excluir nota' }).click()

  await expect(page.getByRole('button', { name: 'Introdução' })).toBeHidden()
  await expect(page.getByRole('button', { name: 'Revolução' })).toBeVisible()
  await expect(page.locator('.editor input')).toHaveValue('Revolução')
})

test('cria uma pasta', async ({ page }) => {
  await openNotes(page)

  await page.getByRole('button', { name: '＋', exact: true }).click()

  await expect(page.getByRole('button', { name: /Nova pasta 4/ })).toBeVisible()
  await expect(page.getByText('Não há notas nesta pasta.')).toBeVisible()
  await expect(page.getByRole('button', { name: /Nova nota/ })).toBeVisible()
})

test('exclui a pasta selecionada e volta para a primeira', async ({ page }) => {
  await openNotes(page)

  await page.getByRole('button', { name: /Semana 2/ }).click()
  await expect(page.getByRole('button', { name: 'Iluminismo' })).toBeVisible()

  await page.getByRole('button', { name: 'Excluir pasta' }).click()

  await expect(page.getByRole('button', { name: /Semana 2/ })).toBeHidden()
  await expect(page.getByRole('button', { name: /Semana 1/ })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Introdução' })).toBeVisible()
})

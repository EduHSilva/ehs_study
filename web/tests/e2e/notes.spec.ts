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

test('arrasta uma nota para outra pasta', async ({ page }) => {
  await openNotes(page)

  await page.getByRole('button', { name: 'Introdução' }).dragTo(page.getByRole('button', { name: /Semana 2/ }))

  await expect(page.getByRole('button', { name: /Semana 1/ })).toContainText('1')
  await expect(page.getByRole('button', { name: /Semana 2/ })).toContainText('2')
  await expect(page.locator('.note-cards .note-card')).toHaveCount(2)
  await expect(page.locator('.editor input')).toHaveValue('Introdução')
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

test('formata o texto pela barra com markdown', async ({ page }) => {
  await openNotes(page)
  const textarea = page.locator('.editor textarea')

  await textarea.fill('Primeira linha\nSegunda linha')
  await textarea.focus()
  await page.keyboard.press('ControlOrMeta+a')
  await page.getByRole('button', { name: 'Lista com marcadores' }).click()
  await expect(textarea).toHaveValue('- Primeira linha\n- Segunda linha')

  await textarea.fill('texto importante')
  await textarea.focus()
  await page.keyboard.press('ControlOrMeta+a')
  await page.getByRole('button', { name: 'Título 1' }).click()
  await expect(textarea).toHaveValue('# texto importante')

  await textarea.fill('texto importante')
  await textarea.evaluate(element => {
    const area = element as HTMLTextAreaElement
    area.focus()
    area.setSelectionRange(0, 5)
  })
  await page.getByRole('button', { name: 'Negrito' }).click()
  await expect(textarea).toHaveValue('**texto** importante')
})

test('prévia renderiza o markdown e volta ao texto cru', async ({ page }) => {
  await openNotes(page)
  const textarea = page.locator('.editor textarea')
  const markdown = '# Título da aula\n\n**Importante** e lista:\n\n- um\n- dois'

  await textarea.fill(markdown)
  await page.getByRole('button', { name: 'Visualizar' }).click()

  const preview = page.locator('.note-preview')
  await expect(preview.locator('h1')).toHaveText('Título da aula')
  await expect(preview.locator('strong')).toHaveText('Importante')
  await expect(preview.locator('li')).toHaveCount(2)
  await expect(textarea).toBeHidden()

  await page.getByRole('button', { name: 'Editar' }).click()
  await expect(textarea).toHaveValue(markdown)
})

test('expande o editor em tela cheia e restaura com Esc', async ({ page }) => {
  await openNotes(page)
  await page.getByRole('button', { name: 'Expandir editor' }).click()

  const editor = page.locator('.editor.expanded')
  await expect(editor).toBeVisible()
  await expect(page.locator('.note-cards')).toBeHidden()
  await expect(page.getByRole('button', { name: 'Restaurar editor' })).toBeVisible()

  const viewport = page.viewportSize()
  const box = await editor.boundingBox()
  expect(box?.width).toBeGreaterThanOrEqual((viewport?.width ?? 0) - 1)
  expect(box?.height).toBeGreaterThanOrEqual((viewport?.height ?? 0) - 1)

  await page.keyboard.press('Escape')
  await expect(page.locator('.editor.expanded')).toHaveCount(0)
  await expect(page.locator('.note-cards')).toBeVisible()
})

test('pede confirmação ao trocar de tela com alterações não salvas', async ({ page }) => {
  await openNotes(page)
  const textarea = page.locator('.editor textarea')
  await textarea.fill('Conteúdo pendente')

  await page.getByRole('button', { name: 'Tarefas' }).click()
  const dialog = page.getByRole('dialog', { name: 'Alterações não salvas' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: 'Cancelar' }).click()

  await expect(dialog).toBeHidden()
  await expect(page.getByRole('heading', { name: 'Anotações de Aula' })).toBeVisible()
  await expect(textarea).toHaveValue('Conteúdo pendente')

  await page.getByRole('button', { name: 'Tarefas' }).click()
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: 'Descartar' }).click()

  await expect(page.getByRole('heading', { name: 'Tarefas' })).toBeVisible()
})

test('salva a anotação antes de sair quando pedido', async ({ page }) => {
  await openNotes(page)
  await page.locator('.editor textarea').fill('Conteúdo salvo ao sair')

  await page.getByRole('button', { name: 'Tarefas' }).click()
  const dialog = page.getByRole('dialog', { name: 'Alterações não salvas' })
  await dialog.getByRole('button', { name: 'Salvar' }).click()

  await expect(page.getByRole('heading', { name: 'Tarefas' })).toBeVisible()

  await page.getByRole('button', { name: 'Anotações' }).click()
  await expect(page.locator('.editor textarea')).toHaveValue('Conteúdo salvo ao sair')
  await expect(page.locator('.note-cards .note-card').first()).toContainText('Conteúdo salvo ao sair')
})

test('protege também a navegação pelo botão voltar', async ({ page }) => {
  await openNotes(page)
  await page.locator('.editor textarea').fill('Pendente no voltar')

  await page.goBack()
  const dialog = page.getByRole('dialog', { name: 'Alterações não salvas' })
  await expect(dialog).toBeVisible()

  await dialog.getByRole('button', { name: 'Cancelar' }).click()
  await expect(page.getByRole('heading', { name: 'Anotações de Aula' })).toBeVisible()
  await expect(page.locator('.editor textarea')).toHaveValue('Pendente no voltar')
})

test('pede confirmação ao trocar de nota com alterações não salvas', async ({ page }) => {
  await openNotes(page)
  await page.locator('.editor textarea').fill('Rascunho')

  await page.getByRole('button', { name: 'Revolução' }).click()
  const dialog = page.getByRole('dialog', { name: 'Alterações não salvas' })
  await expect(dialog).toBeVisible()

  await dialog.getByRole('button', { name: 'Descartar' }).click()
  await expect(page.locator('.editor input')).toHaveValue('Revolução')
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

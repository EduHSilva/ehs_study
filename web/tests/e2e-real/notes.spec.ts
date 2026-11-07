import { expect, test } from '@playwright/test'
import {
  createFolder,
  createNote,
  deleteFolderQuietly,
  getFolder,
  listFolders,
  login,
  uniqueSuffix
} from './support/api'
import { openNotes, signIn } from './support/ui'

test('cria pasta, edita e exclui nota usando o back real', async ({ page, request }) => {
  const suffix = uniqueSuffix()
  const token = await login(request)
  const folder = await createFolder(request, token, `E2E ${suffix}`)
  const note = await createNote(request, token, folder.id, {
    title: `Nota ${suffix}`,
    content: 'Conteúdo inicial'
  })

  try {
    await signIn(page)
    await openNotes(page)

    await page.getByRole('button', { name: new RegExp(folder.name) }).click()
    await expect(page.getByRole('button', { name: note.title })).toBeVisible()

    await page.locator('.editor input').fill(`Nota ${suffix} revisada`)
    await page.locator('.editor textarea').fill('Conteúdo atualizado')
    await page.getByRole('button', { name: 'Salvar' }).click()
    await expect(page.getByText('Salvo agora')).toBeVisible()

    await expect
      .poll(async () => (await getFolder(request, token, folder.id)).notes[0]?.title)
      .toBe(`Nota ${suffix} revisada`)

    await page.getByRole('button', { name: 'Excluir nota' }).click()
    await expect(page.getByRole('button', { name: `Nota ${suffix} revisada` })).toBeHidden()

    await expect
      .poll(async () => (await getFolder(request, token, folder.id)).notes.length)
      .toBe(0)

    await page.getByRole('button', { name: 'Excluir pasta' }).click()
    await expect(page.getByRole('button', { name: new RegExp(folder.name) })).toBeHidden()

    await expect
      .poll(async () => (await listFolders(request, token)).some(item => item.id === folder.id))
      .toBe(false)
  } finally {
    await deleteFolderQuietly(request, token, folder.id)
  }
})

test('cria pasta pela UI e persiste no back real', async ({ page, request }) => {
  const token = await login(request)
  const before = new Set((await listFolders(request, token)).map(folder => folder.id))
  let createdId: string | undefined

  try {
    await signIn(page)
    await openNotes(page)

    await page.getByRole('button', { name: '＋', exact: true }).click()
    // Espera a pasta aparecer no back: uma pasta "Nova pasta N" deixada por execução anterior
    // faria a checagem visual passar antes do POST concluir.
    await expect
      .poll(async () => (await listFolders(request, token)).find(folder => !before.has(folder.id))?.id)
      .toBeTruthy()

    const newFolder = (await listFolders(request, token)).find(folder => !before.has(folder.id))
    expect(newFolder).toBeTruthy()
    if (!newFolder) return
    createdId = newFolder.id
    await expect(page.getByRole('button', { name: new RegExp(newFolder.name) })).toBeVisible()
    await page.getByRole('button', { name: 'Renomear pasta' }).last().click()
    await page.getByRole('textbox', { name: 'Nome da pasta' }).fill(`Renomeada ${uniqueSuffix()}`)
    const renamed = await page.getByRole('textbox', { name: 'Nome da pasta' }).inputValue()
    await page.locator('.folder-rename button[type="submit"]').click()
    await expect(page.getByRole('button', { name: new RegExp(renamed) })).toBeVisible()
    expect((await listFolders(request, token)).find(folder => folder.id === createdId)?.name).toBe(renamed)

    await expect
      .poll(async () => (await listFolders(request, token)).find(folder => !before.has(folder.id))?.id)
      .toBeTruthy()
  } finally {
    if (createdId) await deleteFolderQuietly(request, token, createdId)
  }
})

test('excluir pasta remove do back as notas contidas nela (cascade)', async ({ request }) => {
  const suffix = uniqueSuffix()
  const token = await login(request)
  const folder = await createFolder(request, token, `E2E cascade ${suffix}`)
  const note = await createNote(request, token, folder.id, { title: `Nota ${suffix}`, content: 'Cascade test' })
  try {
    const response = await request.delete(`${process.env.E2E_API_BASE_URL ?? 'http://localhost'}/study/notes/folders/${folder.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(response.ok(), await response.text()).toBeTruthy()
    const noteResponse = await request.get(`${process.env.E2E_API_BASE_URL ?? 'http://localhost'}/study/notes/${note.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(noteResponse.status()).toBe(404)
  } finally {
    await deleteFolderQuietly(request, token, folder.id)
  }
})

test('mostra a nota criada no back real entre as anotações recentes do dashboard', async ({ page, request }) => {
  const suffix = uniqueSuffix()
  const token = await login(request)
  const folder = await createFolder(request, token, `E2E dashboard ${suffix}`)
  const note = await createNote(request, token, folder.id, {
    title: `Resumo ${suffix}`,
    content: `Conteúdo recente ${suffix}`
  })

  try {
    await signIn(page)

    await expect(page.getByText(note.title)).toBeVisible()
    await expect(page.getByText(note.content)).toBeVisible()
  } finally {
    await deleteFolderQuietly(request, token, folder.id)
  }
})

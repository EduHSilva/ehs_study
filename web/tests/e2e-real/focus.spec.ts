import { expect, test } from '@playwright/test'
import { createFocusSession, listFocusSessions, login, uniqueSuffix } from './support/api'

test('POST /study/focus registra uma sessão no back real', async ({ request }) => {
  const token = await login(request)
  const title = `E2E focus ${uniqueSuffix()}`
  const createdResponse = await createFocusSession(request, token, title, 1)
  expect(createdResponse.ok(), await createdResponse.text()).toBeTruthy()
  const created = (await createdResponse.json()).data as { id: string; title: string; minutes: number }
  expect(created.title).toBe(title)
  expect(created.minutes).toBe(1)
  expect(created.id).toBeTruthy()
})

test('GET /study/focus filtra as sessões pelo dia no back real', async ({ request }) => {
  const token = await login(request)
  const now = new Date()
  const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const response = await listFocusSessions(request, token, date)
  expect(response.ok(), await response.text()).toBeTruthy()
  const result = (await response.json()).data as { items: Array<{ id: string }> }
  expect(Array.isArray(result.items)).toBeTruthy()
})

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

test('mantém o cronômetro rodando e mostra o widget flutuante ao navegar', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(60 * 1000)

  await expect(page.getByText('24:00')).toBeVisible()
  await page.getByRole('button', { name: 'Dashboard' }).click()

  const widget = page.locator('.pomodoro-widget')
  await expect(widget).toBeVisible()
  await expect(widget.getByText('24:00')).toBeVisible()
  await expect(page.getByRole('heading', { name: /Olá, / })).toBeVisible()

  await widget.getByRole('button', { name: 'Pausar' }).click()
  await page.clock.runFor(5 * 1000)
  await expect(widget.getByText('24:00')).toBeVisible()
})

test('retoma o cronômetro após recarregar a página', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()

  await page.reload()

  // A página aberta também sobrevive ao F5, então o cronômetro continua na tela cheia.
  await expect(page.getByRole('heading', { name: 'Cronômetro Pomodoro' })).toBeVisible()
  await expect(page.getByText(/2[45]:\d{2}/)).toBeVisible()

  await page.getByRole('button', { name: 'Dashboard' }).click()
  const widget = page.locator('.pomodoro-widget')
  await expect(widget).toBeVisible()
  await expect(widget.getByText(/2[45]:\d{2}/)).toBeVisible()
})

test('finaliza a sessão pelo widget e registra no servidor', async ({ page }) => {
  const state: { folders: []; sessions: unknown[] } = { folders: [], sessions: [] }
  await signIn(page)
  await mockApi(page, state)
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(25 * 60 * 1000)
  await page.getByRole('button', { name: 'Dashboard' }).click()

  const widget = page.locator('.pomodoro-widget')
  await expect(widget.locator('.widget-time')).toContainText('Sessão de foco concluída!')
  await widget.getByRole('button', { name: 'Finalizar sessão' }).click()

  await expect.poll(() => state.sessions.length).toBe(1)
  expect(state.sessions[0]).toMatchObject({ minutes: 25 })
  await expect(widget).toBeHidden()
})

test('toca o alerta sonoro quando o ciclo termina', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.addInitScript(() => {
    const scope = window as unknown as { __chimes: number[] }
    scope.__chimes = []
    class FakeOscillator {
      frequency = { value: 0 }
      type = 'sine'
      connect() { return this }
      start() { scope.__chimes.push(this.frequency.value) }
      stop() {}
    }
    class FakeGain {
      gain = { setValueAtTime() {}, exponentialRampToValueAtTime() {} }
      connect() { return this }
    }
    class FakeContext {
      currentTime = 0
      destination = {}
      resume() { return Promise.resolve() }
      createOscillator() { return new FakeOscillator() }
      createGain() { return new FakeGain() }
    }
    Object.defineProperty(window, 'AudioContext', { value: FakeContext })
  })
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(25 * 60 * 1000)

  await expect(page.getByRole('status').filter({ hasText: 'Sessão de foco concluída!' })).toBeVisible()
  await expect
    .poll(() => page.evaluate(() => (window as unknown as { __chimes: number[] }).__chimes.length))
    .toBe(3)
})

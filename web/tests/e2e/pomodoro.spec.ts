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

test('mostra o tempo restante no título da aba e o devolve ao reiniciar', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(60 * 1000)

  await expect.poll(() => page.title()).toBe('24:00 · Foco')

  await page.getByRole('button', { name: 'Reiniciar' }).click()
  await expect.poll(() => page.title()).toContain('Estudos')
})

test('abre o cronômetro em janela flutuante pelo botão manter visível', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(60 * 1000)
  await page.getByRole('button', { name: 'Dashboard' }).click()

  const supported = await page.evaluate(() => 'documentPictureInPicture' in window)
  test.skip(!supported, 'Document Picture-in-Picture indisponível neste navegador')

  const widget = page.locator('.pomodoro-widget')
  await widget.getByRole('button', { name: 'Manter visível' }).click()
  const pin = widget.getByRole('button', { name: 'Fechar janela flutuante' })
  await expect(pin).toHaveAttribute('aria-pressed', 'true')

  const pipText = () => page.evaluate(() => {
    const api = (window as unknown as { documentPictureInPicture?: { window: Window | null } }).documentPictureInPicture
    return api?.window && !api.window.closed ? api.window.document.body.innerText : ''
  })
  await expect.poll(pipText).toContain('24:00')
  await expect.poll(pipText).toContain('Pausar')

  await pin.click()
  await expect(widget.getByRole('button', { name: 'Manter visível' })).toHaveAttribute('aria-pressed', 'false')
})

test('abre a janela flutuante direto da tela do Pomodoro', async ({ page }) => {
  await signIn(page)
  await mockApi(page, { folders: [] })
  await page.clock.install()
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()
  await page.clock.runFor(60 * 1000)

  const supported = await page.evaluate(() => 'documentPictureInPicture' in window)
  test.skip(!supported, 'Document Picture-in-Picture indisponível neste navegador')

  await page.getByRole('button', { name: 'Manter visível' }).click()
  await expect(page.getByRole('button', { name: 'Fechar janela flutuante' })).toHaveAttribute('aria-pressed', 'true')

  const pipText = () => page.evaluate(() => {
    const api = (window as unknown as { documentPictureInPicture?: { window: Window | null } }).documentPictureInPicture
    return api?.window && !api.window.closed ? api.window.document.body.innerText : ''
  })
  await expect.poll(pipText).toContain('24:00')

  // A janela sobrevive à navegação e o widget passa a refletir o mesmo estado.
  await page.getByRole('button', { name: 'Dashboard' }).click()
  await expect.poll(pipText).toContain('24:00')
  const widgetPin = page.locator('.pomodoro-widget').getByRole('button', { name: 'Fechar janela flutuante' })
  await expect(widgetPin).toHaveAttribute('aria-pressed', 'true')
  await widgetPin.click()
  await expect(page.locator('.pomodoro-widget').getByRole('button', { name: 'Manter visível' })).toHaveAttribute('aria-pressed', 'false')
})

test('a janela flutuante acompanha o tema escuro', async ({ page }) => {
  await signIn(page)
  await page.addInitScript(() => localStorage.setItem('studyflow-theme', 'dark'))
  await mockApi(page, { folders: [] })
  await page.goto('/')
  await page.getByRole('button', { name: 'Pomodoro' }).click()
  await page.getByRole('button', { name: 'Começar' }).click()

  const supported = await page.evaluate(() => 'documentPictureInPicture' in window)
  test.skip(!supported, 'Document Picture-in-Picture indisponível neste navegador')

  await page.getByRole('button', { name: 'Manter visível' }).click()
  await expect(page.getByRole('button', { name: 'Fechar janela flutuante' })).toHaveAttribute('aria-pressed', 'true')

  const pipBackground = () => page.evaluate(() => {
    const api = (window as unknown as { documentPictureInPicture?: { window: Window | null } }).documentPictureInPicture
    const win = api?.window
    return win ? win.getComputedStyle(win.document.body).backgroundColor : ''
  })
  await expect.poll(pipBackground).toBe('rgb(39, 39, 42)')
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

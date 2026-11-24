// Janela flutuante do cronômetro (Document Picture-in-Picture, Chromium desktop). A janela vive
// fora do DOM da SPA e continua visível ao trocar de aba, então o estado é espelhado a cada tick.
export interface PipView {
  label: string
  time: string
  status: string
  completed: boolean
  canToggle: boolean
  toggleLabel: string
  canFinish: boolean
  finishLabel: string
  finishDisabled: boolean
}

export interface PipActions {
  toggle: () => void
  finish: () => void
  closed: () => void
}

interface DocumentPictureInPictureApi {
  requestWindow(options?: { width?: number; height?: number }): Promise<Window>
}

const WIDTH = 320
const HEIGHT = 176

const THEME_TOKENS = [
  '--font-body',
  '--font-heading',
  '--color-surface',
  '--color-text',
  '--color-muted',
  '--color-subtle',
  '--color-border',
  '--color-primary',
  '--color-primary-soft',
  '--color-on-primary',
  '--color-success'
]

const PIP_STYLE = `
*{box-sizing:border-box}
html,body{height:100%}
body{margin:0;background:var(--color-surface);color:var(--color-text);font-family:var(--font-body);display:grid;place-items:center;overflow:hidden}
.pip{width:100%;display:grid;gap:8px;padding:16px}
.pip-head{display:flex;justify-content:space-between;align-items:baseline;gap:8px}
.pip-label{font-size:11px;font-weight:600;letter-spacing:.04em;text-transform:uppercase;color:var(--color-muted)}
.pip-status{font-size:11px;color:var(--color-subtle)}
.pip-time{font:700 42px/1 var(--font-heading);font-variant-numeric:tabular-nums}
.pip.completed .pip-time{color:var(--color-success)}
.pip-actions{display:flex;gap:6px;flex-wrap:wrap}
.pip-btn{height:32px;padding:0 12px;border:1px solid var(--color-border);border-radius:8px;background:var(--color-surface);color:var(--color-text);font:600 12px var(--font-body);white-space:nowrap;cursor:pointer}
.pip-btn.primary{border-color:transparent;background:var(--color-primary);color:var(--color-on-primary)}
.pip-btn:disabled{opacity:.6;cursor:default}
`

function pipApi(): DocumentPictureInPictureApi | null {
  if (typeof window === 'undefined') return null
  const api = (window as unknown as { documentPictureInPicture?: DocumentPictureInPictureApi }).documentPictureInPicture
  return api ?? null
}

export function isPipSupported(): boolean {
  return pipApi() !== null
}

interface PipElements {
  label: HTMLElement
  status: HTMLElement
  time: HTMLElement
  toggle: HTMLButtonElement
  finish: HTMLButtonElement
}

export class PomodoroPipWindow {
  private win: Window | null = null
  private root: HTMLElement | null = null
  private els: PipElements | null = null
  private actions: PipActions | null = null

  get isOpen(): boolean {
    return this.win !== null && !this.win.closed
  }

  async open(actions: PipActions, view: PipView): Promise<boolean> {
    if (this.isOpen) {
      this.update(view)
      this.win?.focus()
      return true
    }
    const api = pipApi()
    if (!api) return false
    let win: Window
    try {
      win = await api.requestWindow({ width: WIDTH, height: HEIGHT })
    } catch {
      return false
    }
    this.win = win
    this.actions = actions
    this.build(win)
    win.addEventListener('pagehide', this.handlePageHide)
    this.update(view)
    return true
  }

  update(view: PipView): void {
    if (!this.win || !this.root || !this.els) return
    const { label, time, status, toggle, finish } = this.els
    label.textContent = view.label
    time.textContent = view.time
    status.textContent = view.status
    status.hidden = view.status === ''
    toggle.textContent = view.toggleLabel
    toggle.hidden = !view.canToggle
    finish.textContent = view.finishLabel
    finish.hidden = !view.canFinish
    finish.disabled = view.finishDisabled
    this.root.classList.toggle('completed', view.completed)
    this.applyTheme()
  }

  refreshTheme(): void {
    this.applyTheme()
  }

  close(): void {
    if (!this.win) return
    if (!this.win.closed) this.win.close()
    this.handlePageHide()
  }

  private handlePageHide = (): void => {
    this.win = null
    this.root = null
    this.els = null
    const actions = this.actions
    this.actions = null
    actions?.closed()
  }

  private build(win: Window): void {
    const doc = win.document
    doc.head.replaceChildren()
    doc.body.replaceChildren()
    const style = doc.createElement('style')
    style.textContent = PIP_STYLE
    doc.head.append(style)

    const root = doc.createElement('div')
    root.className = 'pip'
    root.innerHTML = `
      <div class="pip-head">
        <span class="pip-label" data-el="label"></span>
        <span class="pip-status" data-el="status"></span>
      </div>
      <strong class="pip-time" data-el="time"></strong>
      <div class="pip-actions">
        <button type="button" class="pip-btn primary" data-el="toggle"></button>
        <button type="button" class="pip-btn" data-el="finish"></button>
      </div>`
    doc.body.append(root)
    doc.title = 'Pomodoro'

    const pick = <T extends HTMLElement>(name: string) => root.querySelector<T>(`[data-el="${name}"]`) as T
    this.root = root
    this.els = {
      label: pick('label'),
      status: pick('status'),
      time: pick('time'),
      toggle: pick('toggle'),
      finish: pick('finish')
    }
    this.els.toggle.addEventListener('click', () => this.actions?.toggle())
    this.els.finish.addEventListener('click', () => this.actions?.finish())
  }

  private applyTheme(): void {
    if (!this.win) return
    // Os tokens precisam ficar no <html> da janela flutuante: o body é quem define o fundo,
    // então aplicá-los só no card deixaria fundo claro com texto do tema escuro.
    const target = this.win.document.documentElement
    const source = window.getComputedStyle(document.documentElement)
    for (const token of THEME_TOKENS) {
      const value = source.getPropertyValue(token).trim()
      if (value) target.style.setProperty(token, value)
    }
    target.style.colorScheme = document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
  }
}

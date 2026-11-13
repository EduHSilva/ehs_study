import { ref, watch, type Ref } from 'vue'
import { useTheme } from './useTheme'

const GSI_SRC = 'https://accounts.google.com/gsi/client'

interface GoogleCredentialResponse {
  credential?: string
}

interface GoogleAccountsId {
  initialize(config: { client_id: string; callback: (response: GoogleCredentialResponse) => void }): void
  renderButton(parent: HTMLElement, options: Record<string, unknown>): void
}

interface GoogleNamespace {
  accounts: { id: GoogleAccountsId }
}

declare global {
  interface Window {
    google?: GoogleNamespace
  }
}

// O script do Google entra uma vez por sessão; o `promise` fica em cache para que varias
// montagens compartilhem a mesma requisicao em vez de injetar a tag duas vezes.
let scriptPromise: Promise<GoogleNamespace> | null = null

function loadGoogleIdentity(): Promise<GoogleNamespace> {
  if (typeof window === 'undefined') return Promise.reject(new Error('sem window'))
  if (window.google?.accounts?.id) return Promise.resolve(window.google)
  if (scriptPromise) return scriptPromise

  scriptPromise = new Promise<GoogleNamespace>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${GSI_SRC}"]`)
    const script = existing ?? document.createElement('script')
    const done = () => (window.google ? resolve(window.google) : reject(new Error('Google indisponivel')))
    script.addEventListener('load', done)
    script.addEventListener('error', () => reject(new Error('Falha ao carregar o Google')))
    if (!existing) {
      script.src = GSI_SRC
      script.async = true
      script.defer = true
      document.head.appendChild(script)
    }
  }).catch((error: unknown) => {
    // Libera o cache para uma nova tentativa na proxima montagem.
    scriptPromise = null
    throw error
  })

  return scriptPromise
}

// Desenha o botao oficial do Google dentro do `container` que o consumidor passa por template
// ref. Sem `VITE_GOOGLE_CLIENT_ID` nada e desenhado e `enabled` fica falso, para a tela nao
// abrir um espaco vazio.
export function useGoogleIdentity(container: Ref<HTMLElement | null>, onCredential: (idToken: string) => void) {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''
  const enabled = clientId.length > 0
  const failed = ref(false)
  const { theme } = useTheme()

  let renderedTheme = ''
  let renderedWidth = 0

  const render = async (el: HTMLElement) => {
    if (!enabled) return
    try {
      const google = await loadGoogleIdentity()
      // A tela pode ter desmontado enquanto o script carregava.
      if (container.value !== el) return
      // O GSI nao aceita largura percentual e o tema e fixado no desenho do botao,
      // entao cada mudanca de largura ou de tema exige um novo render.
      const mode = theme.value === 'dark' ? 'filled_black' : 'outline'
      const width = Math.max(200, Math.min(400, Math.floor(el.getBoundingClientRect().width) || 300))
      if (mode === renderedTheme && width === renderedWidth) return
      renderedTheme = mode
      renderedWidth = width
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          if (response.credential) onCredential(response.credential)
        }
      })
      el.replaceChildren()
      google.accounts.id.renderButton(el, {
        type: 'standard',
        theme: mode,
        size: 'large',
        text: 'continue_with',
        shape: 'rectangular',
        logo_alignment: 'left',
        width
      })
    } catch {
      failed.value = true
    }
  }

  // `flush: 'post'` garante que o elemento ja esteja no documento quando o Google tentar
  // medir a largura do botao.
  watch(container, (el) => { if (el) void render(el) }, { flush: 'post' })
  watch(theme, () => { if (container.value) void render(container.value) })

  return { enabled, failed, clientId }
}
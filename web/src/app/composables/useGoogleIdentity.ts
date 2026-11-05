import { ref, watch, type Ref } from 'vue'

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

  const render = async (el: HTMLElement) => {
    if (!enabled || el.childElementCount > 0) return
    try {
      const google = await loadGoogleIdentity()
      // A tela pode ter desmontado enquanto o script carregava.
      if (container.value !== el) return
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          if (response.credential) onCredential(response.credential)
        }
      })
      google.accounts.id.renderButton(el, {
        type: 'standard',
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        shape: 'rectangular',
        logo_alignment: 'left',
        width: Math.max(240, Math.floor(el.getBoundingClientRect().width) || 280)
      })
    } catch {
      failed.value = true
    }
  }

  // `flush: 'post'` garante que o elemento ja esteja no documento quando o Google tentar
  // medir a largura do botao.
  watch(container, (el) => { if (el) void render(el) }, { flush: 'post' })

  return { enabled, failed, clientId }
}
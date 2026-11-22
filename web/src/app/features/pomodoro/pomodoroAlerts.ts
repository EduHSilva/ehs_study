// Título da aba e notificações são o fallback quando o navegador não oferece a janela flutuante
// (Document Picture-in-Picture). O título base é capturado na primeira escrita para ser devolvido
// intacto quando o cronômetro não estiver ativo.
let baseTitle = ''
let ownsTitle = false

export function setTimerTitle(value: string | null): void {
  if (typeof document === 'undefined') return
  if (value === null) {
    if (!ownsTitle) return
    document.title = baseTitle
    ownsTitle = false
    return
  }
  if (!ownsTitle) {
    baseTitle = document.title
    ownsTitle = true
  }
  document.title = value
}

export function canNotify(): boolean {
  return typeof Notification !== 'undefined' && Notification.permission === 'granted'
}

// Precisa ser chamado dentro de um gesto do usuário (início do cronômetro ou clique no widget).
export async function requestNotificationPermission(): Promise<void> {
  if (typeof Notification === 'undefined' || Notification.permission !== 'default') return
  try {
    await Notification.requestPermission()
  } catch {
    // Navegador bloqueou o pedido: segue sem notificação.
  }
}

// Alguns navegadores móveis exigem ServiceWorker e recusam o construtor direto; nesse caso o
// alerta fica só no título da aba.
export function notifyFocusEnd(title: string, body: string): void {
  if (!canNotify()) return
  try {
    const notification = new Notification(title, {
      body,
      tag: 'studyflow-pomodoro',
      icon: `${import.meta.env.BASE_URL}favicon.svg`,
      requireInteraction: true
    })
    notification.onclick = () => {
      window.focus()
      notification.close()
    }
  } catch {
    // Sem notificação: o título da aba continua sinalizando o fim do ciclo.
  }
}

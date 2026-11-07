// O navegador só libera áudio depois de um gesto do usuário; `primeAudio` é chamado no clique de
// iniciar e mantém o contexto vivo para o chime tocar quando o ciclo terminar.
let context: AudioContext | null = null

function getContext(): AudioContext | null {
  if (context) return context
  try {
    context = new AudioContext()
  } catch {
    return null
  }
  return context
}

export function primeAudio(): void {
  const ctx = getContext()
  if (!ctx) return
  void ctx.resume().catch(() => undefined)
}

export function playChime(): void {
  const ctx = getContext()
  if (!ctx) return
  void ctx.resume().catch(() => undefined)
  const start = ctx.currentTime
  const notes = [880, 1046.5, 1318.5]
  notes.forEach((frequency, index) => {
    const at = start + index * 0.32
    const oscillator = ctx.createOscillator()
    const gain = ctx.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = frequency
    gain.gain.setValueAtTime(0.0001, at)
    gain.gain.exponentialRampToValueAtTime(0.22, at + 0.03)
    gain.gain.exponentialRampToValueAtTime(0.0001, at + 0.28)
    oscillator.connect(gain).connect(ctx.destination)
    oscillator.start(at)
    oscillator.stop(at + 0.3)
  })
}

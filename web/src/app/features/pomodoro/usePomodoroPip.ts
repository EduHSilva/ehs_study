import { computed, ref, watch, watchEffect } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { useTheme } from '../../composables/useTheme'
import { usePomodoro } from './usePomodoro'
import { usePomodoroTimer } from './usePomodoroTimer'
import { notifyFocusEnd, requestNotificationPermission, setTimerTitle } from './pomodoroAlerts'
import { PomodoroPipWindow, isPipSupported } from './pomodoroPip'
import type { PipView } from './pomodoroPip'

// Estado de módulo: a tela do Pomodoro e o widget compartilham a mesma janela flutuante, o mesmo
// pedido de notificação e o mesmo título da aba.
const { t } = useI18n()
const { theme } = useTheme()
const { saving, registerSession } = usePomodoro()
const { mode, running, started, completed, isActive, elapsedMinutes, timeLeft, toggle, advanceAfterFocus } =
  usePomodoroTimer()

const pip = new PomodoroPipWindow()
const pipSupported = isPipSupported()
const pipOpen = ref(false)

const canFinish = computed(() => mode.value === 'focus' && started.value)
const modeLabel = computed(() => t(mode.value === 'focus' ? 'focusModeShort' : mode.value === 'longBreak' ? 'longBreakModeShort' : 'breakModeShort'))
const completionLabel = computed(() => t(mode.value === 'focus' ? 'focusComplete' : mode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete'))

const finish = async (): Promise<void> => {
  if (!canFinish.value || saving.value) return
  if (await registerSession(t('focusSessionTitle'), elapsedMinutes.value)) advanceAfterFocus()
}

const view = computed<PipView>(() => ({
  label: completed.value ? completionLabel.value : modeLabel.value,
  time: timeLeft.value,
  status: completed.value ? '' : running.value ? t('timerRunning') : t('timerPaused'),
  completed: completed.value,
  canToggle: !completed.value,
  toggleLabel: running.value ? t('pause') : t('start'),
  canFinish: canFinish.value,
  finishLabel: saving.value ? t('saving') : t('finishSession'),
  finishDisabled: saving.value
}))

const toggleKeepVisible = async (): Promise<void> => {
  if (pipOpen.value) {
    pip.close()
    return
  }
  const opened = await pip.open(
    {
      toggle,
      finish: () => void finish(),
      closed: () => { pipOpen.value = false }
    },
    view.value
  )
  pipOpen.value = opened
}

watch(view, (next) => pip.update(next))
watch(theme, () => pip.refreshTheme(), { flush: 'post' })

// O pedido de notificação aproveita o gesto de iniciar o cronômetro; o navegador só pergunta uma vez.
watch(running, (value) => {
  if (value) void requestNotificationPermission()
})

// Com a aba oculta o widget não aparece: avisa pelo sistema, a menos que a janela flutuante esteja
// aberta (aí o usuário já vê o fim do ciclo).
watch(completed, (value, previous) => {
  if (!value || previous || pipOpen.value) return
  if (document.visibilityState === 'visible') return
  notifyFocusEnd(t('timerTitle'), completionLabel.value)
})

// Fallback universal: a aba passa a mostrar o tempo mesmo quando o PiP não existe.
watchEffect(() => {
  if (!isActive.value) {
    setTimerTitle(null)
    return
  }
  setTimerTitle(completed.value ? `✓ ${completionLabel.value}` : `${timeLeft.value} · ${modeLabel.value}`)
})

export function usePomodoroPip() {
  const dispose = (): void => {
    pip.close()
    setTimerTitle(null)
  }
  return { pipSupported, pipOpen, toggleKeepVisible, canFinish, saving, finish, dispose }
}

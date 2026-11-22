<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch, watchEffect } from 'vue'
import { currentPage, requestNavigate } from '../composables/useNavigation'
import { useI18n } from '../composables/useI18n'
import { useTheme } from '../composables/useTheme'
import { usePomodoro } from '../features/pomodoro/usePomodoro'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'
import { notifyFocusEnd, requestNotificationPermission, setTimerTitle } from '../features/pomodoro/pomodoroAlerts'
import { PomodoroPipWindow, isPipSupported } from '../features/pomodoro/pomodoroPip'
import type { PipView } from '../features/pomodoro/pomodoroPip'

const { t } = useI18n()
const { theme } = useTheme()
const { saving, registerSession } = usePomodoro()
const { mode, running, started, completed, isActive, elapsedMinutes, timeLeft, toggle, reset, advanceAfterFocus } = usePomodoroTimer()

const pip = new PomodoroPipWindow()
const pipSupported = isPipSupported()
const pipOpen = ref(false)

const visible = computed(() => currentPage.value !== 'pomodoro' && isActive.value)
const canFinish = computed(() => mode.value === 'focus' && started.value)
const modeLabel = computed(() => t(mode.value === 'focus' ? 'focusModeShort' : mode.value === 'longBreak' ? 'longBreakModeShort' : 'breakModeShort'))
const completionLabel = computed(() => t(mode.value === 'focus' ? 'focusComplete' : mode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete'))
const statusLabel = computed(() => (completed.value ? '' : running.value ? t('timerRunning') : t('timerPaused')))

const finish = async () => {
  if (!canFinish.value) return
  if (await registerSession(t('focusSessionTitle'), elapsedMinutes.value)) advanceAfterFocus()
}

const pipView = computed<PipView>(() => ({
  label: completed.value ? completionLabel.value : modeLabel.value,
  time: timeLeft.value,
  status: statusLabel.value,
  completed: completed.value,
  canToggle: !completed.value,
  toggleLabel: running.value ? t('pause') : t('start'),
  canFinish: canFinish.value,
  finishLabel: saving.value ? t('saving') : t('finishSession'),
  finishDisabled: saving.value
}))

const toggleKeepVisible = async () => {
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
    pipView.value
  )
  pipOpen.value = opened
}

watch(pipView, (view) => pip.update(view))
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

onBeforeUnmount(() => {
  pip.close()
  setTimerTitle(null)
})
</script>
<template>
  <aside v-if="visible" class="pomodoro-widget" :class="{ completed, break: mode === 'break' }">
    <p class="widget-announcement" role="status">{{ completed ? completionLabel : '' }}</p>
    <button class="widget-time" :aria-label="t('openTimer')" @click="requestNavigate('pomodoro')">
      <small>{{ completed ? completionLabel : modeLabel }}</small>
      <strong>{{ timeLeft }}</strong>
      <span v-if="!completed" class="widget-status">{{ running ? t('timerRunning') : t('timerPaused') }}</span>
    </button>
    <div class="widget-actions">
      <button v-if="!completed" class="widget-btn" @click="toggle">{{ running ? t('pause') : t('start') }}</button>
      <button v-if="canFinish" class="widget-btn primary" :disabled="saving" @click="finish">{{ saving ? t('saving') : t('finishSession') }}</button>
      <button
        v-if="pipSupported"
        class="widget-btn widget-pin"
        :class="{ active: pipOpen }"
        :aria-pressed="pipOpen"
        :aria-label="pipOpen ? t('keepVisibleOff') : t('keepVisible')"
        :title="pipOpen ? t('keepVisibleOff') : t('keepVisibleHint')"
        @click="toggleKeepVisible"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 17v5" /><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1Z" /></svg>
        <span class="widget-pin-label">{{ t('keepVisible') }}</span>
      </button>
      <button class="widget-btn ghost" :aria-label="t('dismissTimer')" :title="t('dismissTimer')" @click="reset">×</button>
    </div>
  </aside>
</template>

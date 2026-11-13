<script setup lang="ts">
import { computed } from 'vue'
import { currentPage, navigate } from '../composables/useNavigation'
import { useI18n } from '../composables/useI18n'
import { usePomodoro } from '../features/pomodoro/usePomodoro'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'

const { t } = useI18n()
const { saving, registerSession } = usePomodoro()
const { mode, running, started, completed, isActive, elapsedMinutes, timeLeft, toggle, reset, advanceAfterFocus } = usePomodoroTimer()

const visible = computed(() => currentPage.value !== 'pomodoro' && isActive.value)
const canFinish = computed(() => mode.value === 'focus' && started.value)
const modeLabel = computed(() => t(mode.value === 'focus' ? 'focusModeShort' : mode.value === 'longBreak' ? 'longBreakModeShort' : 'breakModeShort'))
const completionLabel = computed(() => t(mode.value === 'focus' ? 'focusComplete' : mode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete'))

const finish = async () => {
  if (!canFinish.value) return
  if (await registerSession(t('focusSessionTitle'), elapsedMinutes.value)) advanceAfterFocus()
}
</script>
<template>
  <aside v-if="visible" class="pomodoro-widget" :class="{ completed, break: mode === 'break' }">
    <p class="widget-announcement" role="status">{{ completed ? completionLabel : '' }}</p>
    <button class="widget-time" :aria-label="t('openTimer')" @click="navigate('pomodoro')">
      <small>{{ completed ? completionLabel : modeLabel }}</small>
      <strong>{{ timeLeft }}</strong>
      <span v-if="!completed" class="widget-status">{{ running ? t('timerRunning') : t('timerPaused') }}</span>
    </button>
    <div class="widget-actions">
      <button v-if="!completed" class="widget-btn" @click="toggle">{{ running ? t('pause') : t('start') }}</button>
      <button v-if="canFinish" class="widget-btn primary" :disabled="saving" @click="finish">{{ saving ? t('saving') : t('finishSession') }}</button>
      <button class="widget-btn ghost" :aria-label="t('dismissTimer')" :title="t('dismissTimer')" @click="reset">×</button>
    </div>
  </aside>
</template>

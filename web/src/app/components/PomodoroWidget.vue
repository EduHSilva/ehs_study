<script setup lang="ts">
import { computed, onBeforeUnmount } from 'vue'
import { currentPage, requestNavigate } from '../composables/useNavigation'
import { useI18n } from '../composables/useI18n'
import { usePomodoroTimer } from '../features/pomodoro/usePomodoroTimer'
import { usePomodoroPip } from '../features/pomodoro/usePomodoroPip'

const { t } = useI18n()
const { mode, running, completed, isActive, timeLeft, toggle, reset } = usePomodoroTimer()
const { pipSupported, pipOpen, toggleKeepVisible, canFinish, saving, finish, dispose } = usePomodoroPip()

const visible = computed(() => currentPage.value !== 'pomodoro' && isActive.value)
const modeLabel = computed(() => t(mode.value === 'focus' ? 'focusModeShort' : mode.value === 'longBreak' ? 'longBreakModeShort' : 'breakModeShort'))
const completionLabel = computed(() => t(mode.value === 'focus' ? 'focusComplete' : mode.value === 'longBreak' ? 'longBreakComplete' : 'breakComplete'))

onBeforeUnmount(dispose)
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

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { usePomodoro } from '../features/pomodoro/usePomodoro'

defineEmits<{ navigate: [page: AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { sessions, loading, error, load } = usePomodoro()
onMounted(load)

const running = ref(false); const breakMode = ref(false); const seconds = ref(25 * 60)
const time = () => `${String(Math.floor(seconds.value / 60)).padStart(2,'0')}:${String(seconds.value % 60).padStart(2,'0')}`
const setMode = (isBreak: boolean) => { breakMode.value=isBreak; seconds.value=isBreak?300:1500 }
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="pomodoro" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('timerTitle') }}</h1><p>{{ t('timerSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <div class="pomodoro-layout">
      <article class="card timer-card"><div class="mode-switch"><button :class="{selected:!breakMode}" @click="setMode(false)">{{ t('focusMode') }}</button><button :class="{selected:breakMode}" @click="setMode(true)">{{ t('breakMode') }}</button></div><div class="timer-ring"><div><strong>{{ time() }}</strong><small>{{ t('sessionCounter', { current: 3, total: 4 }) }}</small></div></div><div class="timer-actions"><button class="button secondary" @click="seconds=breakMode?300:1500">{{ t('reset') }}</button><button class="button" @click="running=!running">{{ running ? t('pause') : t('start') }}</button></div></article>
      <article class="card sessions"><h2>{{ t('sessions') }}</h2>
        <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
        <p v-else-if="!sessions.length" class="empty-note">{{ t('noSessions') }}</p>
        <div v-for="session in sessions" :key="session.id" class="session"><span>✓</span><div><strong>{{ session.title }}</strong><small>{{ session.completedAt }}</small></div><em>{{ session.minutes }} min</em></div>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('pomodoroLoading') }}</main>
</template>

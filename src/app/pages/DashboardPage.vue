<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'

defineEmits<{ navigate: [page: AppPage] }>()
import { useStudentProfile } from '../features/auth/useAuth'
import { useDashboard } from '../features/dashboard/useDashboard'

const { t } = useI18n()
const { student } = useStudentProfile()
const firstName = computed(() => student.value.name.split(' ')[0])
const { data, loading, error, progress, formattedTime, savingReminderId, load, toggleReminder, toggleFocus, resetFocus } = useDashboard()
onMounted(load)
</script>

<template>
  <main v-if="data" class="app-shell">
    <AppSidebar v-bind="student" active="dashboard" @navigate="$emit('navigate', $event)" />
    <section class="content"><header class="page-header"><div><h1>{{ t('greeting', { name: firstName }) }}</h1><p>{{ t('dashboardWelcome') }}</p></div><div class="header-actions"><LanguageToggle /><ThemeToggle /></div></header>
      <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
      <div class="dashboard-grid">
        <div class="left-column">
          <article class="card progress-card"><h2>{{ t('weeklyProgressTitle') }}</h2><div class="progress-content"><div class="progress-ring" :style="{ '--progress': `${progress * 3.6}deg` }"><strong>{{ progress }}%</strong></div><div><p>{{ t('weeklyProgressBefore') }} <b>{{ data.weeklyProgress.completed }} {{ t('of') }} {{ data.weeklyProgress.total }}</b> {{ t('weeklyProgressAfter') }}</p><small>{{ t('weeklyProgressCheer') }}</small></div></div></article>
          <article class="card focus-card"><div class="card-title"><h2>{{ t('focusTitle') }}</h2><span class="tag">{{ t('focusTag') }}</span></div><div class="focus-row"><strong>{{ formattedTime }}</strong><button class="round primary" @click="toggleFocus"><img src="/assets/pause.svg" :alt="t('pause')" /></button><button class="round" @click="resetFocus"><img src="/assets/redo.svg" :alt="t('reset')" /></button></div><p>{{ t('focusSubject') }} {{ data.focus.subject }}</p></article>
        </div>
        <div class="right-column">
          <article class="card"><div class="card-title"><h2>{{ t('todayReminders') }}</h2><button class="link">{{ t('seeAll') }}</button></div><div class="reminder-list"><button v-for="item in data.reminders" :key="item.id" class="reminder" :disabled="savingReminderId === item.id" @click="toggleReminder(item.id)"><span class="checkbox" :class="{ checked: item.completed }">✓</span><span class="reminder-info"><strong :class="{ done: item.completed }">{{ item.title }}</strong><small>{{ item.schedule }} <i /> <em>{{ item.subject }}</em></small></span><span class="priority" :class="item.priority.toLowerCase()">{{ item.priority }}</span></button></div></article>
          <article class="card notes"><h2>{{ t('recentNotes') }}</h2><div class="notes-grid"><article v-for="note in data.notes" :key="note.id" class="note"><span>{{ note.week }}</span><strong>{{ note.title }}</strong><small>{{ t('subjectLabel') }} {{ note.subject }}</small></article></div></article>
        </div>
      </div>
    </section>
  </main>
  <main v-else class="loading">{{ loading ? t('dashboardLoading') : t('dashboardError') }}</main>
</template>

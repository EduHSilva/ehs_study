<script setup lang="ts">
import { onMounted } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useReminders } from '../features/reminders/useReminders'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { items, loading, error, savingId, load, toggle, add } = useReminders()
onMounted(load)
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="reminders" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('reminders') }}</h1><p>{{ t('remindersSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <article class="card task-page">
      <p v-if="!items.length && !error" class="empty-note">{{ t('noReminders') }}</p>
      <button v-for="item in items" :key="item.id" class="reminder" :disabled="savingId === item.id" @click="toggle(item.id)"><span class="checkbox" :class="{checked:item.completed}">✓</span><span class="reminder-info"><strong :class="{done:item.completed}">{{item.title}}</strong><small>{{item.schedule}} <i/> <em>{{item.subject}}</em></small></span><span class="priority" :class="item.priority.toLowerCase()">{{item.priority}}</span></button>
      <button class="floating-add" @click="add({ title:t('newReminderTitle'), schedule:t('todayLabel'), subject:t('generalSubject') })">＋</button>
    </article>
  </section></main>
  <main v-else class="loading">{{ t('remindersLoading') }}</main>
</template>

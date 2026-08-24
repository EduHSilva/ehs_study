<script setup lang="ts">
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useAuth } from '../features/auth/useAuth'
defineProps<{ name: string; course: string; avatarUrl: string; active: AppPage }>()
defineEmits<{ navigate: [page: AppPage] }>()
const { t } = useI18n()
const { pending, logout } = useAuth()
const navItems: { key: AppPage; icon: string }[] = [
  { key:'dashboard', icon:'/assets/house.svg' }, { key:'pomodoro', icon:'/assets/clock.svg' }, { key:'notes', icon:'/assets/file-text.svg' }, { key:'reminders', icon:'/assets/list-check.svg' }, { key:'projects', icon:'/assets/graduation-cap.svg' }, { key:'review', icon:'/assets/file-search.svg' }
]
</script>

<template>
  <aside class="sidebar">
    <div><div class="brand"><span class="brand-icon"><img src="/assets/graduation-cap.svg" alt="" /></span><strong>StudyFlow</strong></div>
      <nav><button v-for="item in navItems" :key="item.key" class="nav-item" :class="{ active: active === item.key }" @click="$emit('navigate', item.key)"><img :src="item.icon" alt="" />{{ t(item.key) }}</button></nav></div>
    <div>
      <div class="profile"><img class="avatar" :src="avatarUrl" alt="" /><div><strong>{{ name }}</strong><span>{{ course }}</span></div></div>
      <button class="logout-btn" type="button" :disabled="pending" @click="logout">{{ t('logoutAction') }}</button>
    </div>
  </aside>
</template>

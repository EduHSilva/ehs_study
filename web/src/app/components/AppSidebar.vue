<script setup lang="ts">
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useAuth } from '../features/auth/useAuth'
defineProps<{ name: string; course: string; avatarUrl: string; active: AppPage }>()
defineEmits<{ navigate: [page: AppPage]; prefetch: [page: AppPage] }>()
const { t } = useI18n()
const { pending, logout } = useAuth()
const asset = (name: string) => `${import.meta.env.BASE_URL}assets/${name}`
const navItems: { key: AppPage; icon: string; disabled?: boolean }[] = [
    { key:'dashboard', icon:asset('house.svg') }, { key:'pomodoro', icon:asset('clock.svg') }, { key:'notes', icon:asset('file-text.svg') }, { key:'tasks', icon:asset('list-check.svg') }, { key:'kanban', icon:asset('kanban.svg') }, { key:'projects', icon:asset('layers.svg') }, { key:'review', icon:asset('file-search.svg'), disabled:true }, { key:'profile', icon:asset('user.svg') }
]
</script>

<template>
  <aside class="sidebar">
    <div><div class="brand"><span class="brand-icon"><img :src="asset('graduation-cap.svg')" alt="" /></span><strong>EHS Study</strong></div>
      <nav><button v-for="item in navItems" :key="item.key" class="nav-item" :class="{ active: active === item.key, disabled: item.disabled }" :disabled="item.disabled" :aria-label="item.disabled ? `${t(item.key)} [EM BREVE]` : undefined" @click="$emit('navigate', item.key)" @pointerenter="$emit('prefetch', item.key)" @focus="$emit('prefetch', item.key)"><img :src="item.icon" alt="" />{{ t(item.key) }}<span v-if="item.disabled" class="coming-soon">[EM BREVE]</span></button></nav></div>
    <div>
      <div class="profile"><img class="avatar" :src="avatarUrl" alt="" /><div><strong>{{ name }}</strong><span>{{ course }}</span></div></div>
      <button class="logout-btn" type="button" :disabled="pending" @click="logout">{{ t('logoutAction') }}</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import { useI18n } from '../composables/useI18n'
import type { AppPage } from '../composables/useNavigation'
import { useAuth, useStudentProfile } from '../features/auth/useAuth'
import { useProfileUpdate } from '../features/auth/useAuth'

defineEmits<{ navigate: [page: AppPage] }>()

const { t } = useI18n()
const { student } = useStudentProfile()
const { user } = useAuth()
const firstNameInitial = user.value?.name.charAt(0).toUpperCase() ?? '?'

const name = ref(user.value?.name ?? '')
const { pending, error, saved, updateProfile } = useProfileUpdate()
const submit = () => { void updateProfile(name.value) }
</script>

<template>
  <main class="app-shell">
    <AppSidebar v-bind="student" active="profile" @navigate="$emit('navigate', $event)" />
    <section class="content">
      <header class="page-header">
        <div><h1>{{ t('profileTitle') }}</h1><p>{{ t('profileSubtitle') }}</p></div>
        <div class="header-actions"><LanguageToggle /><ThemeToggle /></div>
      </header>
      <div class="profile-layout">
        <article class="card profile-card">
          <div class="profile-hero">
            <span class="avatar-lg">{{ firstNameInitial }}</span>
            <div><strong>{{ user?.name }}</strong><span>{{ user?.email }}</span></div>
          </div>
        </article>
        <form class="card profile-form" @submit.prevent="submit">
          <label class="field">
            <span>{{ t('profileNameLabel') }}</span>
            <input v-model="name" type="text" :placeholder="t('usernamePlaceholder')" minlength="2" required />
          </label>
          <label class="field">
            <span>{{ t('profileEmailLabel') }}</span>
            <input type="email" :value="user?.email" disabled />
          </label>
          <label class="field">
            <span>{{ t('profileIdLabel') }}</span>
            <input type="text" :value="user?.id" disabled />
          </label>
          <p v-if="error" class="form-error">{{ error }}</p>
          <p v-if="saved" class="form-success">{{ t('profileSaveSuccess') }}</p>
          <button class="button" type="submit" :disabled="pending">{{ pending ? t('profileSaving') : t('profileSave') }}</button>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.profile-layout{display:grid;grid-template-columns:minmax(0,1fr);gap:24px;max-width:640px}
.profile-card{padding:0;overflow:hidden}
.profile-hero{display:flex;align-items:center;gap:16px;padding:24px;background:linear-gradient(135deg,var(--color-primary-soft),transparent)}
.avatar-lg{width:64px;height:64px;border-radius:50%;background:var(--color-primary);color:#fff;display:grid;place-items:center;font:700 26px var(--font-heading)}
.profile-hero div{display:grid;gap:2px}
.profile-hero strong{font:600 18px var(--font-heading)}
.profile-hero span{color:var(--color-muted);font-size:13px}
.profile-form{display:grid;gap:14px;padding:24px}
.profile-form .button{width:100%;border-radius:8px;height:42px;font-weight:600}
.form-success{margin:0;padding:10px 12px;border-radius:8px;background:var(--color-success-soft);color:var(--color-success);font-size:13px}
.field input:disabled{background:var(--color-bg);color:var(--color-muted)}
</style>

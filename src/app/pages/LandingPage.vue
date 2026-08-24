<script setup lang="ts">
import { ref } from 'vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useI18n } from '../composables/useI18n'
import { useAuth } from '../features/auth/useAuth'

const { t } = useI18n()
const { pending, error, login } = useAuth()
const email = ref('')
const password = ref('')
const submit = () => { void login(email.value, password.value) }
</script>

<template>
  <div class="landing">
    <header class="landing-header">
      <div class="brand"><span class="brand-icon"><img src="/assets/graduation-cap.svg" alt="" /></span><strong>StudyFlow</strong></div>
      <div class="header-actions"><LanguageToggle /><ThemeToggle /></div>
    </header>
    <main class="landing-hero">
      <section class="landing-copy">
        <h1>{{ t('landingTitle') }}</h1>
        <p>{{ t('landingSubtitle') }}</p>
        <ul class="landing-points">
          <li><span>✓</span>{{ t('landingPointTimer') }}</li>
          <li><span>✓</span>{{ t('landingPointNotes') }}</li>
          <li><span>✓</span>{{ t('landingPointReminders') }}</li>
        </ul>
      </section>
      <form class="card login-card" @submit.prevent="submit">
        <h2>{{ t('loginTitle') }}</h2>
        <p class="login-sub">{{ t('loginSubtitle') }}</p>
        <label class="field">
          <span>{{ t('emailLabel') }}</span>
          <input v-model="email" type="email" autocomplete="email" :placeholder="t('emailPlaceholder')" required />
        </label>
        <label class="field">
          <span>{{ t('passwordLabel') }}</span>
          <input v-model="password" type="password" autocomplete="current-password" placeholder="••••••••" minlength="6" required />
        </label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button" type="submit" :disabled="pending">{{ pending ? t('loggingIn') : t('loginAction') }}</button>
      </form>
    </main>
  </div>
</template>

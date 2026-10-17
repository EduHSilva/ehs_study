<script setup lang="ts">
import { ref } from 'vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useGoogleIdentity } from '../composables/useGoogleIdentity'
import { useI18n } from '../composables/useI18n'
import { useAuth } from '../features/auth/useAuth'

const { t } = useI18n()
const { pending, error, login, loginWithGoogle, register, registerPending, registerError, registerSuccess } = useAuth()

const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const username = ref('')
const showPassword = ref(false)

const googleButton = ref<HTMLElement | null>(null)
const { enabled: googleEnabled, failed: googleFailed } = useGoogleIdentity(googleButton, (idToken) => {
  void loginWithGoogle(idToken)
})

const submit = () => {
  if (mode.value === 'login') {
    void login(email.value, password.value)
  } else {
    void register({ email: email.value, username: username.value, password: password.value })
  }
}

const switchMode = (next: 'login' | 'register') => {
  mode.value = next
  // A senha fica em claro ao trocar de aba, e o botão do Google vive no formulário de login.
  showPassword.value = false
}

const goLogin = () => {
  registerSuccess.value = false
  mode.value = 'login'
}
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
          <li><span>✓</span>{{ t('landingPointTasks') }}</li>
        </ul>
      </section>
      <form v-if="mode === 'login'" class="card login-card" @submit.prevent="submit">
        <h2>{{ t('loginTitle') }}</h2>
        <p class="login-sub">{{ t('loginSubtitle') }}</p>
        <label class="field">
          <span>{{ t('emailLabel') }}</span>
          <input v-model="email" type="email" autocomplete="email" :placeholder="t('emailPlaceholder')" required />
        </label>
        <label class="field">
          <span>{{ t('passwordLabel') }}</span>
          <div class="password-field">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="••••••••" minlength="6" required />
            <button class="password-peek" type="button" :aria-label="showPassword ? t('hidePassword') : t('showPassword')" :title="showPassword ? t('hidePassword') : t('showPassword')" @click="showPassword = !showPassword">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path v-if="showPassword" d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.4 5.2A9.5 9.5 0 0112 5c5 0 9 4.5 9 7 0 .9-.6 2.1-1.7 3.2M6.2 6.7C4.2 8 3 9.8 3 12c0 2.5 4 7 9 7 1.3 0 2.5-.3 3.5-.8"/><circle v-if="!showPassword" cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
        </label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button" type="submit" :disabled="pending">{{ pending ? t('loggingIn') : t('loginAction') }}</button>
        <template v-if="googleEnabled || googleFailed">
          <div class="or-divider"><span>{{ t('or') }}</span></div>
          <div ref="googleButton" class="google-slot" />
          <p v-if="googleFailed" class="form-hint">{{ t('googleUnavailable') }}</p>
        </template>
        <button class="link-btn" type="button" @click="switchMode('register')">{{ t('needAccount') }} <em>{{ t('goToRegister') }}</em></button>
      </form>
      <div v-else-if="registerSuccess" class="card login-card">
        <h2>{{ t('registerSuccessTitle') }}</h2>
        <p class="login-sub">{{ t('registerSuccessText') }}</p>
        <button class="button" type="button" @click="goLogin">{{ t('loginNow') }}</button>
      </div>
      <form v-else class="card login-card" @submit.prevent="submit">
        <h2>{{ t('registerTitle') }}</h2>
        <p class="login-sub">{{ t('registerSubtitle') }}</p>
        <label class="field">
          <span>{{ t('usernameLabel') }}</span>
          <input v-model="username" type="text" autocomplete="username" :placeholder="t('usernamePlaceholder')" minlength="2" required />
        </label>
        <label class="field">
          <span>{{ t('emailLabel') }}</span>
          <input v-model="email" type="email" autocomplete="email" :placeholder="t('emailPlaceholder')" required />
        </label>
        <label class="field">
          <span>{{ t('passwordLabel') }}</span>
          <div class="password-field">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="••••••••" minlength="6" required />
            <button class="password-peek" type="button" :aria-label="showPassword ? t('hidePassword') : t('showPassword')" :title="showPassword ? t('hidePassword') : t('showPassword')" @click="showPassword = !showPassword">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path v-if="showPassword" d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.4 5.2A9.5 9.5 0 0112 5c5 0 9 4.5 9 7 0 .9-.6 2.1-1.7 3.2M6.2 6.7C4.2 8 3 9.8 3 12c0 2.5 4 7 9 7 1.3 0 2.5-.3 3.5-.8"/><circle v-if="!showPassword" cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
        </label>
        <p v-if="registerError" class="form-error">{{ registerError }}</p>
        <button class="button" type="submit" :disabled="registerPending">{{ registerPending ? t('registering') : t('registerAction') }}</button>
        <button class="link-btn" type="button" @click="switchMode('login')">{{ t('alreadyHaveAccount') }} <em>{{ t('goToLogin') }}</em></button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.link-btn{border:0;background:transparent;color:var(--color-muted);font-size:13px;margin-top:4px;cursor:pointer}.link-btn em{font-style:normal;color:var(--color-primary);font-weight:600}
/* O botão do Google é um iframe: o slot reserva a altura para a página não pular quando ele
   carrega, e centraliza o iframe que o Google injeta. */
.google-slot{display:grid;place-items:center;min-height:44px}
.or-divider{display:flex;align-items:center;gap:12px;color:var(--color-subtle);font-size:12px}
.or-divider:before,.or-divider:after{content:'';flex:1;height:1px;background:var(--color-border)}
.form-hint{margin:0;font-size:12px;color:var(--color-subtle);text-align:center}
</style>

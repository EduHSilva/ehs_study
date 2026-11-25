<script setup lang="ts">
import { ref } from 'vue'
import LanguageToggle from '../components/LanguageToggle.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import { useGoogleIdentity } from '../composables/useGoogleIdentity'
import { useI18n } from '../composables/useI18n'
import { useAuth } from '../features/auth/useAuth'

const { t } = useI18n()
const { pending, error, emailSent, login, loginWithGoogle, register, registerPending, registerError, registerSuccess } = useAuth()

const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const username = ref('')
const showPassword = ref(false)

const asset = (name: string) => `${import.meta.env.BASE_URL}assets/${name}`

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
  emailSent.value = null
}

const goLogin = () => {
  registerSuccess.value = false
  mode.value = 'login'
}
</script>

<template>
  <div class="landing">
    <header class="landing-header">
      <div class="brand"><span class="brand-icon"><img :src="asset('graduation-cap.svg')" alt="" /></span><strong>Estudos</strong></div>
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
              <svg viewBox="0 0 24 24" aria-hidden="true"><template v-if="showPassword"><path d="M3 3l18 18M10.4 6.1A10.7 10.7 0 0 1 12 6c6 0 9.5 6 9.5 6a17.6 17.6 0 0 1-2.6 3.3M6.3 7.6A16.9 16.9 0 0 0 2.5 12S6 18 12 18c1.3 0 2.5-.2 3.6-.6"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></template><template v-else><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="3.2"/></template></svg>
            </button>
          </div>
        </label>
        <p class="forgot-link"><a href="/user/reset-password">{{ t('forgotPassword') }}</a></p>
        <p v-if="emailSent" class="form-success">{{ emailSent }}</p>
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
              <svg viewBox="0 0 24 24" aria-hidden="true"><template v-if="showPassword"><path d="M3 3l18 18M10.4 6.1A10.7 10.7 0 0 1 12 6c6 0 9.5 6 9.5 6a17.6 17.6 0 0 1-2.6 3.3M6.3 7.6A16.9 16.9 0 0 0 2.5 12S6 18 12 18c1.3 0 2.5-.2 3.6-.6"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></template><template v-else><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="3.2"/></template></svg>
            </button>
          </div>
        </label>
        <p v-if="registerError" class="form-error">{{ registerError }}</p>
        <button class="button" type="submit" :disabled="registerPending">{{ registerPending ? t('registering') : t('registerAction') }}</button>
        <button class="link-btn" type="button" @click="switchMode('login')">{{ t('alreadyHaveAccount') }} <em>{{ t('goToLogin') }}</em></button>
      </form>
    </main>
    <footer class="landing-footer">
      <span>Estudos © {{ new Date().getFullYear() }} · Desenvolvido por EHS Solutions</span>
      <a href="/app/privacidade/">{{ t('privacyPolicy') }}</a>
    </footer>
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
.forgot-link{margin:-4px 0 0;text-align:right;font-size:12px}
.forgot-link a{color:var(--color-primary);font-weight:600}
.landing-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:18px 24px 26px;font-size:12px;color:var(--color-muted);border-top:1px solid var(--color-border)}
.landing-footer a{color:var(--color-primary);font-weight:600}
@media(max-width:600px){.landing-footer{flex-wrap:wrap;justify-content:center;text-align:center;padding:16px 18px 24px}}
</style>

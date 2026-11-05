<script setup lang="ts">
import { useTheme } from '../composables/useTheme'
import { useI18n } from '../composables/useI18n'
const { theme, toggleTheme } = useTheme()
const { t } = useI18n()
</script>

<template>
  <button class="theme-toggle" type="button" :aria-label="theme === 'light' ? t('themeDarkAction') : t('themeLightAction')" @click="toggleTheme">
    <span aria-hidden="true">{{ theme === 'light' ? '☾' : '☀' }}</span>
    <span>{{ theme === 'light' ? t('themeDarkLabel') : t('themeLightLabel') }}</span>
  </button>
</template>

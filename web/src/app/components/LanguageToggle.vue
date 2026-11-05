<script setup lang="ts">
import { useI18n } from '../composables/useI18n'
const { t, toggleLocale } = useI18n()
</script>
<template><button class="theme-toggle" type="button" @click="toggleLocale">{{ t('language') }}</button></template>

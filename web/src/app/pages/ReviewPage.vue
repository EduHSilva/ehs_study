<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useReview } from '../features/review/useReview'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { report, loading, error, load } = useReview()
onMounted(load)

const tabs = computed(() => [
  { id: 'plagiarism', label: t('tabPlagiarism') },
  { id: 'abnt', label: t('tabAbnt') },
  { id: 'references', label: t('tabReferences') }
])
const tab = ref('plagiarism')
const referenceLabel = (status: 'Válida' | 'Atenção') => t(status === 'Válida' ? 'referenceValid' : 'referenceAttention')
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="review" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('reviewTitle') }}</h1><p>{{ t('reviewSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <div v-else-if="report" class="review-layout">
      <article class="card document-preview"><h2>{{ t('previewA4') }}</h2><div class="paper"><strong>{{report.documentTitle}}</strong><p>{{report.excerpt}}</p><mark>*{{report.flaggedExcerpt}}*</mark></div></article>
      <article class="card review-panel">
        <nav class="tabs"><button v-for="item in tabs" :key="item.id" :class="{selected:tab===item.id}" @click="tab=item.id">{{item.label}}</button></nav>
        <div class="similarity"><div class="progress-ring small" :style="{ '--progress': `${report.similarityPercent * 3.6}deg` }"><strong>{{report.similarityPercent}}%</strong></div><div><h2>{{ t('similarityTitle') }}</h2><p>{{ t('similarityText', { percent: report.similarityPercent }) }}</p></div></div>
        <h3>{{ t('tabAbnt') }}</h3>
        <p v-for="check in report.abntChecks" :key="check.label" class="checkline" :class="{error:!check.passed}">{{ check.passed ? '✓' : '×' }} {{ check.label }}</p>
        <h3>{{ t('referencesHeading') }}</h3>
        <p v-for="item in report.references" :key="item.citation" class="reference">{{item.citation}} <span>{{ referenceLabel(item.status) }}</span></p>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('reviewLoading') }}</main>
</template>

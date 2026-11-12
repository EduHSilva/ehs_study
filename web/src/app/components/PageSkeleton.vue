<script setup lang="ts">
// Placeholder animado para o primeiro carregamento de uma tela. O shell (sidebar + cabeçalho)
// continua visível: só o conteúdo é trocado, então a navegação não pisca a tela inteira.
withDefaults(defineProps<{
  label: string
  layout?: 'dashboard' | 'split' | 'list' | 'board'
}>(), { layout: 'list' })
</script>

<template>
  <div class="skeleton-page" :class="`skeleton-${layout}`" role="status" aria-live="polite" :aria-label="label">
    <span class="sr-only">{{ label }}</span>

    <template v-if="layout === 'dashboard'">
      <div class="skeleton-col">
        <div class="skeleton-card">
          <span class="skeleton skeleton-title" />
          <div class="skeleton-row">
            <span class="skeleton skeleton-ring" />
            <span class="skeleton-lines">
              <span class="skeleton skeleton-line wide" />
              <span class="skeleton skeleton-line medium" />
              <span class="skeleton skeleton-line tiny" />
            </span>
          </div>
        </div>
        <div class="skeleton-card">
          <span class="skeleton skeleton-title short" />
          <span class="skeleton skeleton-line wide" />
          <span class="skeleton skeleton-line medium" />
        </div>
      </div>
      <div class="skeleton-col">
        <div class="skeleton-card">
          <span class="skeleton skeleton-title short" />
          <span v-for="n in 3" :key="n" class="skeleton skeleton-row-line" />
        </div>
        <div class="skeleton-card">
          <span class="skeleton skeleton-title short" />
          <div class="skeleton-grid-two">
            <span v-for="n in 2" :key="n" class="skeleton skeleton-note" />
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="layout === 'split'">
      <div v-for="n in 2" :key="n" class="skeleton-card">
        <span class="skeleton skeleton-title" />
        <span v-for="line in 4" :key="line" class="skeleton skeleton-line" :class="{ short: line === 4 }" />
        <span class="skeleton skeleton-block" />
      </div>
    </template>

    <template v-else-if="layout === 'board'">
      <div v-for="n in 4" :key="n" class="skeleton-card skeleton-column">
        <span class="skeleton skeleton-title short" />
        <span class="skeleton skeleton-column-card" />
        <span class="skeleton skeleton-column-card" />
      </div>
    </template>

    <template v-else>
      <div class="skeleton-card">
        <span class="skeleton skeleton-title short" />
        <div class="skeleton-calendar">
          <span v-for="n in 28" :key="n" class="skeleton skeleton-day" />
        </div>
      </div>
      <div class="skeleton-card">
        <span class="skeleton skeleton-title short" />
        <span v-for="n in 4" :key="n" class="skeleton skeleton-row-line" />
      </div>
    </template>
  </div>
</template>

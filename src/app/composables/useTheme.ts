import { computed, ref } from 'vue'

export type Theme = 'light' | 'dark'
const storageKey = 'studyflow-theme'
const storedTheme = localStorage.getItem(storageKey)
const theme = ref<Theme>(storedTheme === 'dark' ? 'dark' : 'light')

function applyTheme(nextTheme: Theme) {
  theme.value = nextTheme
  document.documentElement.dataset.theme = nextTheme
  localStorage.setItem(storageKey, nextTheme)
}

applyTheme(theme.value)

export function useTheme() {
  return {
    theme: computed(() => theme.value),
    toggleTheme: () => applyTheme(theme.value === 'light' ? 'dark' : 'light')
  }
}

import { ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { onSessionCacheReset } from '../../composables/useSessionCache'
import { getReviewReport } from './review.api'
import type { ReviewReport } from './types'

// Cache de módulo: o relatório não muda a cada visita, então volta na hora e revalida depois.
const report = ref<ReviewReport | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
let loaded = false
let inFlight: Promise<void> | null = null
let cacheEpoch = 0

onSessionCacheReset(() => {
  cacheEpoch += 1
  loaded = false
  inFlight = null
  report.value = null
  loading.value = true
  error.value = null
})

export function useReview() {
  const { t } = useI18n()

  const load = (): Promise<void> => {
    if (inFlight) return inFlight
    const epoch = cacheEpoch
    loading.value = !loaded
    error.value = null
    inFlight = (async () => {
      try {
        const nextReport = await getReviewReport()
        if (epoch !== cacheEpoch) return
        report.value = nextReport
        loaded = true
      } catch {
        if (epoch !== cacheEpoch) return
        error.value = t('reviewLoadFailed')
      } finally {
        if (epoch === cacheEpoch) {
          loading.value = false
          inFlight = null
        }
      }
    })()
    return inFlight
  }

  return { report, loading, error, load }
}

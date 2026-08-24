import { ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { getReviewReport } from './review.api'
import type { ReviewReport } from './types'

export function useReview() {
  const { t } = useI18n()
  const report = ref<ReviewReport | null>(null)
  const loading = ref(true)
  const error = ref<string | null>(null)

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      report.value = await getReviewReport()
    } catch {
      error.value = t('reviewLoadFailed')
    } finally {
      loading.value = false
    }
  }

  return { report, loading, error, load }
}

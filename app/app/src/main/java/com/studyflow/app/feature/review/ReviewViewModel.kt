package com.studyflow.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.model.ReviewReport
import com.studyflow.app.data.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val report: ReviewReport? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorKey: String? = null,
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (silent) {
                _state.update { it.copy(refreshing = true) }
            } else {
                _state.update { it.copy(loading = true, errorKey = null) }
            }
            try {
                val report = reviewRepository.getReport()
                _state.update {
                    it.copy(report = report, loading = false, refreshing = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, errorKey = "reviewLoadFailed")
                }
            }
        }
    }
}

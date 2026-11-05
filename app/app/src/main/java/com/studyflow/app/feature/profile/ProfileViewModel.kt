package com.studyflow.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.domain.auth.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val pending: Boolean = false,
    val errorKey: String? = null,
    val saved: Boolean = false,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val session = authRepository.session

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun updateName(name: String) {
        val validation = AuthValidation.usernameError(name)
        if (validation != null) {
            _state.update { it.copy(errorKey = validation.messageKey, saved = false) }
            return
        }
        if (_state.value.pending) return
        viewModelScope.launch {
            _state.update { it.copy(pending = true, errorKey = null, saved = false) }
            try {
                authRepository.updateUserName(name)
                _state.update { it.copy(saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "profileSaveFailed") }
            } finally {
                _state.update { it.copy(pending = false) }
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}

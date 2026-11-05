package com.studyflow.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.domain.auth.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val pending: Boolean = false,
    val errorKey: String? = null,
    val registerPending: Boolean = false,
    val registerErrorKey: String? = null,
    val registerSuccess: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        val validation = AuthValidation.loginError(email, password)
        if (validation != null) {
            _state.update { it.copy(errorKey = validation.messageKey) }
            return
        }
        if (_state.value.pending) return
        viewModelScope.launch {
            _state.update { it.copy(pending = true, errorKey = null) }
            try {
                authRepository.login(email, password)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "loginFailed") }
            } finally {
                _state.update { it.copy(pending = false) }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (_state.value.pending) return
        viewModelScope.launch {
            _state.update { it.copy(pending = true, errorKey = null) }
            try {
                authRepository.loginWithGoogle(idToken)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "googleLoginFailed") }
            } finally {
                _state.update { it.copy(pending = false) }
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        val validation = AuthValidation.registerError(email, username, password)
        if (validation != null) {
            _state.update { it.copy(registerErrorKey = validation.messageKey) }
            return
        }
        if (_state.value.registerPending) return
        viewModelScope.launch {
            _state.update { it.copy(registerPending = true, registerErrorKey = null, registerSuccess = false) }
            try {
                authRepository.register(email, username, password)
                _state.update { it.copy(registerSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(registerErrorKey = "registerFailed") }
            } finally {
                _state.update { it.copy(registerPending = false) }
            }
        }
    }

    fun resetRegisterSuccess() {
        _state.update { it.copy(registerSuccess = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorKey = null) }
    }

    fun reportGoogleFailure() {
        _state.update { it.copy(errorKey = "googleLoginFailed") }
    }
}

class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val session = authRepository.session

    init {
        viewModelScope.launch { authRepository.initAuth() }
    }

    fun logout() = authRepository.logout()
}

package com.studyflow.app.data.repository

import com.studyflow.app.core.network.ApiException
import com.studyflow.app.core.network.unwrap
import com.studyflow.app.core.session.AuthSession
import com.studyflow.app.core.session.AuthUser
import com.studyflow.app.core.session.SessionManager
import com.studyflow.app.data.remote.AuthApi
import com.studyflow.app.data.remote.LoginRequest
import com.studyflow.app.data.remote.LoginDto
import com.studyflow.app.data.remote.RegisterRequest
import com.studyflow.app.data.remote.UpdateUserRequest
import kotlinx.coroutines.CancellationException

private const val APP_SCOPE = "STUDY"

class AuthRepository(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
) {
    val session = sessionManager.session

    suspend fun login(email: String, password: String): AuthSession =
        saveSession(
            api.login(
                LoginRequest(email = email.trim(), password = password, app = APP_SCOPE),
            ).unwrap(),
        )

    /** Mesmo endpoint; o back aceita `tokenGmail` no lugar de e-mail/senha. */
    suspend fun loginWithGoogle(idToken: String): AuthSession =
        saveSession(api.login(LoginRequest(tokenGmail = idToken, app = APP_SCOPE)).unwrap())

    suspend fun register(email: String, username: String, password: String) {
        api.register(
            RegisterRequest(
                email = email.trim(),
                username = username.trim(),
                password = password,
                role = "STUDY",
            ),
        ).unwrap()
    }

    /**
     * Revalida a sessão salva no início do app. `/users/auth/me` não tem schema documentado:
     * se não devolver `id`, preserva o do login (sem ele o `X-User-ID` some e as tasks vêm vazias).
     */
    suspend fun initAuth() {
        val stored = sessionManager.session.value ?: return
        try {
            val fresh = api.me().unwrap()
            sessionManager.save(
                AuthSession(
                    token = stored.token,
                    user = toUser(fresh).copy(id = fresh.id.ifBlank { stored.user.id }),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            sessionManager.expire()
        }
    }

    suspend fun updateUserName(name: String): AuthUser? {
        val current = sessionManager.session.value ?: return null
        val trimmed = name.trim()
        api.updateUser(current.user.id, UpdateUserRequest(trimmed)).unwrap()
        val updated = current.user.copy(name = trimmed)
        sessionManager.updateUser(updated)
        return updated
    }

    fun logout() = sessionManager.expire()

    private fun saveSession(dto: LoginDto): AuthSession {
        val token = dto.token ?: throw ApiException("Login sem token")
        val session = AuthSession(token = token, user = toUser(dto))
        sessionManager.save(session)
        return session
    }

    private fun toUser(dto: LoginDto): AuthUser = AuthUser(
        id = dto.id,
        name = dto.username,
        email = dto.email,
        course = dto.course,
        avatarUrl = dto.avatarUrl,
    )
}

package com.studyflow.app.core.session

import com.studyflow.app.core.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Sessão autenticada (token + usuário), espelhando a chave `studyflow-auth` do web.
 *
 * O [expire] é chamado pelo interceptor HTTP quando a API responde 401: limpa a sessão e o
 * gate de autenticação volta para a tela de login.
 */
class SessionManager(
    private val prefs: AppPreferences,
    private val json: Json,
) {
    private val _session = MutableStateFlow(readStored())
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val token: String? get() = _session.value?.token
    val userId: String? get() = _session.value?.user?.id?.takeIf { it.isNotBlank() }

    fun save(session: AuthSession) {
        _session.value = session
        prefs.writeString(KEY, json.encodeToString(AuthSession.serializer(), session))
    }

    fun updateUser(user: AuthUser) {
        val current = _session.value ?: return
        save(current.copy(user = user))
    }

    fun expire() {
        _session.value = null
        prefs.writeString(KEY, null)
    }

    private fun readStored(): AuthSession? {
        val raw = prefs.readString(KEY) ?: return null
        return runCatching { json.decodeFromString(AuthSession.serializer(), raw) }
            .getOrNull()
            ?.takeIf { it.token.isNotBlank() }
    }

    private companion object {
        const val KEY = "studyflow-auth"
    }
}

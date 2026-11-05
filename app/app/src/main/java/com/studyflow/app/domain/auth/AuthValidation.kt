package com.studyflow.app.domain.auth

/** Validações de formulário, equivalentes às de `useAuth.ts`. */
object AuthValidation {
    enum class Error(val messageKey: String) {
        INVALID_EMAIL("invalidEmail"),
        PASSWORD_TOO_SHORT("passwordTooShort"),
        USERNAME_TOO_SHORT("usernameTooShort"),
    }

    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun loginError(email: String, password: String): Error? = when {
        !emailPattern.matches(email.trim()) -> Error.INVALID_EMAIL
        password.length < 6 -> Error.PASSWORD_TOO_SHORT
        else -> null
    }

    fun registerError(email: String, username: String, password: String): Error? = when {
        !emailPattern.matches(email.trim()) -> Error.INVALID_EMAIL
        username.trim().length < 2 -> Error.USERNAME_TOO_SHORT
        password.length < 6 -> Error.PASSWORD_TOO_SHORT
        else -> null
    }

    fun usernameError(username: String): Error? =
        if (username.trim().length < 2) Error.USERNAME_TOO_SHORT else null
}

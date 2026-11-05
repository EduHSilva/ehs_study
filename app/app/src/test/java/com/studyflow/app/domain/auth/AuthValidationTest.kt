package com.studyflow.app.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `valid credentials pass`() {
        assertNull(AuthValidation.loginError("aluno@universidade.br", "segredo123"))
    }

    @Test
    fun `email must look like an email`() {
        assertEquals(
            AuthValidation.Error.INVALID_EMAIL,
            AuthValidation.loginError("aluno", "segredo123"),
        )
        assertEquals(
            AuthValidation.Error.INVALID_EMAIL,
            AuthValidation.loginError("aluno@universidade", "segredo123"),
        )
    }

    @Test
    fun `password needs six characters`() {
        assertEquals(
            AuthValidation.Error.PASSWORD_TOO_SHORT,
            AuthValidation.loginError("aluno@universidade.br", "12345"),
        )
    }

    @Test
    fun `register requires a username`() {
        assertEquals(
            AuthValidation.Error.USERNAME_TOO_SHORT,
            AuthValidation.registerError("aluno@universidade.br", " a ", "segredo123"),
        )
    }

    @Test
    fun `register validates email before password`() {
        assertEquals(
            AuthValidation.Error.INVALID_EMAIL,
            AuthValidation.registerError("invalido", "Aluno", "123"),
        )
    }

    @Test
    fun `validation errors expose their i18n key`() {
        assertEquals("invalidEmail", AuthValidation.Error.INVALID_EMAIL.messageKey)
        assertEquals("passwordTooShort", AuthValidation.Error.PASSWORD_TOO_SHORT.messageKey)
        assertEquals("usernameTooShort", AuthValidation.Error.USERNAME_TOO_SHORT.messageKey)
    }
}

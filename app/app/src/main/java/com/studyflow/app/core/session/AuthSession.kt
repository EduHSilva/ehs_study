package com.studyflow.app.core.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val course: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class AuthSession(
    val token: String,
    val user: AuthUser,
)

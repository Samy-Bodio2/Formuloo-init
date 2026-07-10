package com.formuloo.core.auth.domain.model

import kotlinx.serialization.Serializable

/** Etat global de l'authentification, observe par l'UI. */
sealed interface AuthState {
    data object LoggedOut : AuthState
    data object LoggedIn : AuthState
    data object Loading : AuthState
    data class Error(val message: String, val code: Int? = null) : AuthState
}

/** Profil utilisateur persiste localement (cf. TokenRepository). */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: List<String> = emptyList(),
    val tenantId: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val initials: String
        get() = buildString {
            firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
            lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
        }
}

/** Paire de jetons JWT issue par /auth/login/ ou /auth/refresh/. */
data class TokenSet(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long = 900L,
)

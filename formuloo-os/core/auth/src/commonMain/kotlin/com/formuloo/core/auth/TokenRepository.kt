package com.formuloo.core.auth

import com.formuloo.core.auth.domain.model.UserProfile
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Stockage cle/valeur multiplateforme des jetons Keycloak/JWT et du profil
 * utilisateur courant. `Settings()` (multiplatform-settings-no-arg) fournit
 * l'implementation expect/actual : EncryptedSharedPreferences-like sur Android,
 * NSUserDefaults sur iOS, Preferences sur Desktop (voir ADR-003).
 */
class TokenRepository(
    private val settings: Settings = Settings(),
) {
    fun saveTokens(accessToken: String, refreshToken: String) {
        settings[KEY_ACCESS_TOKEN] = accessToken
        settings[KEY_REFRESH_TOKEN] = refreshToken
    }

    fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)

    fun hasValidSession(): Boolean = getAccessToken() != null

    fun saveUserProfile(profile: UserProfile) {
        settings[KEY_USER_PROFILE] = Json.encodeToString(profile)
    }

    fun getUserProfile(): UserProfile? =
        settings.getStringOrNull(KEY_USER_PROFILE)?.let {
            try {
                Json.decodeFromString<UserProfile>(it)
            } catch (e: Exception) {
                null
            }
        }

    fun clearUserProfile() {
        settings.remove(KEY_USER_PROFILE)
    }

    fun clear() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        clearUserProfile()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_PROFILE = "user_profile"
    }
}

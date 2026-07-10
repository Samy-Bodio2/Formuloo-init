package com.formuloo.core.network.dto.auth

import com.formuloo.core.common.FieldErrorDetail
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.hr.PaginatedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Client pour le service d'authentification (`/api/v1/auth/...`).
 * Contrat source : docs/api contract/auth.yaml
 */
class AuthApiService(private val httpClient: HttpClient) {

    suspend fun login(email: String, password: String): NetworkResult<TokenResponse> =
        try {
            httpClient.post("$AUTH_BASE/login/") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun logout(refreshToken: String): NetworkResult<Unit> =
        try {
            val response = httpClient.post("$AUTH_BASE/logout/") {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequest(refresh = refreshToken))
            }
            if (response.status.isSuccess()) {
                NetworkResult.Success(Unit, response.status.value)
            } else {
                response.toErrorResult()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun refresh(refreshToken: String): NetworkResult<TokenResponse> =
        try {
            httpClient.post("$AUTH_BASE/token/refresh/") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refresh = refreshToken))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun getProfile(): NetworkResult<UserProfileDto> =
        try {
            httpClient.get("$AUTH_BASE/me/").toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun register(
        companyName: String,
        companySlug: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/register/") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterRequest(
                        companyName = companyName,
                        companySlug = companySlug,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                    ),
                )
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun verifyEmail(code: String): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/verify-email/") {
                contentType(ContentType.Application.Json)
                setBody(VerifyEmailRequest(code = code))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun resendVerification(email: String): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/resend-verification/") {
                contentType(ContentType.Application.Json)
                setBody(ResendVerificationRequest(email = email))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun forgotPassword(email: String): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/forgot-password/") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email = email))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun resetPassword(
        code: String,
        newPassword: String,
        confirmPassword: String,
    ): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/reset-password/") {
                contentType(ContentType.Application.Json)
                setBody(
                    ResetPasswordRequest(
                        code = code,
                        nouveauMotDePasse = newPassword,
                        confirmMotDePasse = confirmPassword,
                    ),
                )
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun getInvitePreview(code: String): NetworkResult<InvitePreviewDto> =
        try {
            httpClient.get("$AUTH_BASE/invite/preview/") {
                url { parameters.append("code", code) }
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun acceptInvitation(
        code: String,
        password: String,
        confirmPassword: String,
    ): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/invite/accept/") {
                contentType(ContentType.Application.Json)
                setBody(
                    AcceptInvitationRequest(
                        code = code,
                        password = password,
                        confirmPassword = confirmPassword,
                    ),
                )
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun searchUserByEmail(email: String): NetworkResult<PaginatedResponse<UserListItemDto>> =
        try {
            httpClient.get("$AUTH_BASE/utilisateurs/") {
                url { parameters.append("email", email) }
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun getUsersPage(page: Int): NetworkResult<PaginatedResponse<UserListItemDto>> =
        try {
            httpClient.get("$AUTH_BASE/utilisateurs/") {
                url { parameters.append("page", page.toString()) }
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun getRoles(): NetworkResult<List<RoleDto>> =
        try {
            httpClient.get("$AUTH_BASE/roles/").toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun inviteUser(
        firstName: String,
        lastName: String,
        email: String,
        roles: List<String>,
    ): NetworkResult<MessageResponse> =
        try {
            httpClient.post("$AUTH_BASE/invite/") {
                contentType(ContentType.Application.Json)
                setBody(InviteUserRequest(firstName = firstName, lastName = lastName, email = email, roles = roles))
            }.toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    suspend fun setUserActive(userId: String, active: Boolean): NetworkResult<MessageResponse> =
        try {
            val action = if (active) "activer" else "desactiver"
            httpClient.post("$AUTH_BASE/utilisateurs/$userId/$action/").toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NETWORK_ERROR
        }

    private suspend inline fun <reified T> HttpResponse.toResult(): NetworkResult<T> =
        if (status.isSuccess()) {
            NetworkResult.Success(body<T>(), status.value)
        } else {
            toErrorResult()
        }

    private suspend fun <T> HttpResponse.toErrorResult(): NetworkResult<T> {
        val apiError = try {
            body<ApiError>()
        } catch (e: Exception) {
            null
        }
        return NetworkResult.Error(
            message = apiError?.error?.message ?: "Erreur réseau (${status.value})",
            code = status.value,
            fieldErrors = apiError?.error?.details?.map {
                FieldErrorDetail(field = it.field, message = it.message)
            } ?: emptyList(),
        )
    }

    private companion object {
        const val AUTH_BASE = "/api/v1/auth"
        val NETWORK_ERROR = NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }
}

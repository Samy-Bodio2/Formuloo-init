package com.formuloo.core.network.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LogoutRequest(
    val refresh: String,
)

@Serializable
data class RefreshRequest(
    val refresh: String,
)

@Serializable
data class TokenResponse(
    val access: String,
    val refresh: String? = null,
    val user: UserProfileDto? = null,
)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val code: String,
    @SerialName("nouveau_mot_de_passe") val nouveauMotDePasse: String,
    @SerialName("confirm_mot_de_passe") val confirmMotDePasse: String,
)

@Serializable
data class RegisterRequest(
    @SerialName("company_name") val companyName: String,
    @SerialName("company_slug") val companySlug: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
)

@Serializable
data class VerifyEmailRequest(
    val code: String,
)

@Serializable
data class ResendVerificationRequest(
    val email: String,
)

@Serializable
data class AcceptInvitationRequest(
    val code: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
)

@Serializable
data class OrganisationBriefDto(
    val name: String,
    val initials: String,
)

@Serializable
data class InvitePreviewDto(
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val organisation: OrganisationBriefDto,
    val role: String? = null,
    val modules: List<String> = emptyList(),
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class UserListItemDto(
    val id: String,
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val roles: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class RoleDto(
    val id: String,
    val name: String,
    val code: String,
    @SerialName("is_system") val isSystem: Boolean = false,
    val permissions: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class InviteUserRequest(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val roles: List<String>,
)

@Serializable
data class ApiError(
    val error: ApiErrorDetail,
)

@Serializable
data class ApiErrorDetail(
    val code: String,
    val message: String,
    val details: List<FieldErrorDto> = emptyList(),
)

@Serializable
data class FieldErrorDto(
    val field: String,
    val message: String,
)

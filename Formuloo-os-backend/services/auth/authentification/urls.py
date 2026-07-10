"""
URLs du module Auth — Formuloo OS
Conforme ADR-003 : versionnement /api/v1/
"""

from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from authentification.views import (
    LoginView,
    LogoutView,
    MeView,
    ChangerMotDePasseView,
    RegisterView,
    VerifyEmailView,
    ResendVerificationView,
    InviteView,
    AcceptInvitationView,
    InvitePreviewView,
    ForgotPasswordView,
    ResetPasswordView,
    OrganisationListView,
    OrganisationDetailView,
    UtilisateursListView,
    UtilisateurDetailView,
    UtilisateurActiverView,
    UtilisateurDesactiverView,
    UtilisateurRolesView,
    RolesListView,
    RoleDetailView,
    PermissionsListView,
    PermissionDetailView,
    APIKeysListView,
    APIKeyDetailView,
    AuditLogsListView,
    AuditLogDetailView,
    RefreshTokensListView,
    RefreshTokenDetailView,
)

urlpatterns = [
    # ── INSCRIPTION & GESTION DE COMPTE ───────────
    path("register/", RegisterView.as_view(), name="auth-register"),
    path("verify-email/", VerifyEmailView.as_view(), name="auth-verify-email"),
    path(
        "resend-verification/",
        ResendVerificationView.as_view(),
        name="auth-resend-verification",
    ),
    path("invite/", InviteView.as_view(), name="auth-invite"),
    path("invite/accept/", AcceptInvitationView.as_view(), name="auth-invite-accept"),
    path(
        "invite/preview/",
        InvitePreviewView.as_view(),
        name="auth-invite-preview",
    ),
    path("forgot-password/", ForgotPasswordView.as_view(), name="auth-forgot-password"),
    path("reset-password/", ResetPasswordView.as_view(), name="auth-reset-password"),
    # ── AUTHENTIFICATION ──────────────────────────
    path("login/", LoginView.as_view(), name="auth-login"),
    path("logout/", LogoutView.as_view(), name="auth-logout"),
    path("token/refresh/", TokenRefreshView.as_view(), name="auth-token-refresh"),
    path("me/", MeView.as_view(), name="auth-me"),
    path(
        "me/changer-mot-de-passe/",
        ChangerMotDePasseView.as_view(),
        name="auth-changer-mdp",
    ),
    # ── SESSIONS ACTIVES ──────────────────────────
    path(
        "refresh-tokens/", RefreshTokensListView.as_view(), name="refresh-tokens-list"
    ),
    path(
        "refresh-tokens/<uuid:pk>/",
        RefreshTokenDetailView.as_view(),
        name="refresh-tokens-detail",
    ),
    # ── ORGANISATIONS ─────────────────────────────
    path("organisations/", OrganisationListView.as_view(), name="organisations-list"),
    path(
        "organisations/<uuid:pk>/",
        OrganisationDetailView.as_view(),
        name="organisations-detail",
    ),
    # ── UTILISATEURS ──────────────────────────────
    path("utilisateurs/", UtilisateursListView.as_view(), name="utilisateurs-list"),
    path(
        "utilisateurs/<uuid:pk>/",
        UtilisateurDetailView.as_view(),
        name="utilisateurs-detail",
    ),
    path(
        "utilisateurs/<uuid:pk>/activer/",
        UtilisateurActiverView.as_view(),
        name="utilisateurs-activer",
    ),
    path(
        "utilisateurs/<uuid:pk>/desactiver/",
        UtilisateurDesactiverView.as_view(),
        name="utilisateurs-desactiver",
    ),
    path(
        "utilisateurs/<uuid:pk>/roles/",
        UtilisateurRolesView.as_view(),
        name="utilisateurs-roles",
    ),
    # ── ROLES ─────────────────────────────────────
    path("roles/", RolesListView.as_view(), name="roles-list"),
    path("roles/<uuid:pk>/", RoleDetailView.as_view(), name="roles-detail"),
    # ── PERMISSIONS ───────────────────────────────
    path("permissions/", PermissionsListView.as_view(), name="permissions-list"),
    path(
        "permissions/<uuid:pk>/",
        PermissionDetailView.as_view(),
        name="permissions-detail",
    ),
    # ── CLÉS API ──────────────────────────────────
    path("api-keys/", APIKeysListView.as_view(), name="api-keys-list"),
    path("api-keys/<uuid:pk>/", APIKeyDetailView.as_view(), name="api-keys-detail"),
    # ── AUDIT LOGS ────────────────────────────────
    path("audit-logs/", AuditLogsListView.as_view(), name="audit-logs-list"),
    path(
        "audit-logs/<uuid:pk>/", AuditLogDetailView.as_view(), name="audit-logs-detail"
    ),
]

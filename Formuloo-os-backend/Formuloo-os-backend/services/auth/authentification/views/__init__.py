"""
Views du module Auth — Formuloo OS
Point d'entrée de toutes les views.
"""

from .auth import (
    LoginView,
    LogoutView,
    MeView,
    ChangerMotDePasseView,
)
from .registration import (
    RegisterView,
    VerifyEmailView,
    ResendVerificationView,
    InviteView,
    AcceptInvitationView,
    ForgotPasswordView,
    ResetPasswordView,
)
from .organisation import (
    OrganisationListView,
    OrganisationDetailView,
)
from .user import (
    UtilisateursListView,
    UtilisateurDetailView,
    UtilisateurActiverView,
    UtilisateurDesactiverView,
    UtilisateurRolesView,
)
from .role import (
    RolesListView,
    RoleDetailView,
    PermissionsListView,
    PermissionDetailView,
)
from .api_key import (
    APIKeysListView,
    APIKeyDetailView,
)
from .audit_log import (
    AuditLogsListView,
    AuditLogDetailView,
)
from .refresh_token import (
    RefreshTokensListView,
    RefreshTokenDetailView,
)

__all__ = [
    "LoginView",
    "LogoutView",
    "MeView",
    "ChangerMotDePasseView",
    "RegisterView",
    "VerifyEmailView",
    "ResendVerificationView",
    "InviteView",
    "AcceptInvitationView",
    "ForgotPasswordView",
    "ResetPasswordView",
    "OrganisationListView",
    "OrganisationDetailView",
    "UtilisateursListView",
    "UtilisateurDetailView",
    "UtilisateurActiverView",
    "UtilisateurDesactiverView",
    "UtilisateurRolesView",
    "RolesListView",
    "RoleDetailView",
    "PermissionsListView",
    "PermissionDetailView",
    "APIKeysListView",
    "APIKeyDetailView",
    "AuditLogsListView",
    "AuditLogDetailView",
    "RefreshTokensListView",
    "RefreshTokenDetailView",
]

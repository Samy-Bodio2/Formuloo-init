"""
Serializers du module Auth — Formuloo OS
Point d'entrée de tous les serializers.
"""

from .auth import (
    LoginSerializer,
    LogoutSerializer,
    MeSerializer,
    ChangerMotDePasseSerializer,
)
from .organisation import (
    OrganisationSerializer,
    OrganisationCreateSerializer,
)
from .user import (
    UserListSerializer,
    UserDetailSerializer,
    UserCreateSerializer,
    UserUpdateSerializer,
)
from .role import (
    PermissionSerializer,
    RoleListSerializer,
    RoleDetailSerializer,
    RoleCreateSerializer,
)
from .api_key import (
    APIKeySerializer,
    APIKeyCreateSerializer,
)
from .audit_log import (
    AuditLogSerializer,
)
from .refresh_token import (
    RefreshTokenSerializer,
)
from .registration import (
    RegisterSerializer,
    VerifyEmailSerializer,
    ResendVerificationSerializer,
    InviteSerializer,
    AcceptInvitationSerializer,
    ForgotPasswordSerializer,
    ResetPasswordSerializer,
)

__all__ = [
    "LoginSerializer",
    "LogoutSerializer",
    "MeSerializer",
    "ChangerMotDePasseSerializer",
    "OrganisationSerializer",
    "OrganisationCreateSerializer",
    "UserListSerializer",
    "UserDetailSerializer",
    "UserCreateSerializer",
    "UserUpdateSerializer",
    "PermissionSerializer",
    "RoleListSerializer",
    "RoleDetailSerializer",
    "RoleCreateSerializer",
    "APIKeySerializer",
    "APIKeyCreateSerializer",
    "AuditLogSerializer",
    "RefreshTokenSerializer",
    "RegisterSerializer",
    "VerifyEmailSerializer",
    "ResendVerificationSerializer",
    "InviteSerializer",
    "AcceptInvitationSerializer",
    "ForgotPasswordSerializer",
    "ResetPasswordSerializer",
]

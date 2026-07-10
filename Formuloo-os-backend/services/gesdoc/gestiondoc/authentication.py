"""
GesdocJWTAuthentication — Formuloo OS Service GesDoc
Authentification JWT sans lookup DB — service stateless.
"""

from drf_spectacular.extensions import OpenApiAuthenticationExtension
from rest_framework_simplejwt.authentication import JWTAuthentication


class GesdocJWTAuthenticationScheme(OpenApiAuthenticationExtension):
    target_class = "gestiondoc.authentication.GesdocJWTAuthentication"
    name = "jwtAuth"

    def get_security_definition(self, auto_schema):
        return {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": "Token JWT obtenu via POST /api/v1/auth/login/",
        }


class GesdocJWTUser:
    """Objet user léger extrait du JWT — pas de DB."""

    is_active = True
    is_anonymous = False
    is_authenticated = True

    def __init__(self, token):
        self.pk = token.get("user_id")
        self.id = self.pk
        self.tenant_id = token.get("tenant_id")
        self.roles = token.get("roles", [])
        self.permissions = token.get("permissions", [])
        self.auth_user_id = token.get("auth_user_id")

    def __str__(self):
        return f"GesdocJWTUser({self.pk})"


class GesdocJWTAuthentication(JWTAuthentication):
    """Surcharge get_user() — retourne GesdocJWTUser sans SQL."""

    def get_user(self, validated_token):
        return GesdocJWTUser(validated_token)

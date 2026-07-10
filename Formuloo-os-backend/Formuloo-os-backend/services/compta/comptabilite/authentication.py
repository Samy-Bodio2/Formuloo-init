"""
ComptaJWTAuthentication — Formuloo OS Service Compta
Authentification JWT sans lookup DB — service stateless.
"""

from drf_spectacular.extensions import OpenApiAuthenticationExtension
from rest_framework_simplejwt.authentication import JWTAuthentication


class ComptaJWTAuthenticationScheme(OpenApiAuthenticationExtension):
    target_class = "comptabilite.authentication.ComptaJWTAuthentication"
    name = "jwtAuth"

    def get_security_definition(self, auto_schema):
        return {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": "Token JWT obtenu via POST /api/v1/auth/login/",
        }


class ComptaJWTUser:
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
        return f"ComptaJWTUser({self.pk})"


class ComptaJWTAuthentication(JWTAuthentication):
    """Surcharge get_user() — retourne ComptaJWTUser sans SQL."""

    def get_user(self, validated_token):
        return ComptaJWTUser(validated_token)

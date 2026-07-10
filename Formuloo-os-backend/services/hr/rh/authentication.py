"""
HRJWTAuthentication — Formuloo OS Service RH

Authentification JWT sans lookup DB.

Le service RH ne gère pas ses propres utilisateurs —
ils sont gérés par le service Auth.

Cette classe décode et valide le token JWT,
puis retourne un objet user léger sans aucune
requête vers la base de données RH.
"""

from drf_spectacular.extensions import OpenApiAuthenticationExtension
from rest_framework_simplejwt.authentication import JWTAuthentication


class HRJWTAuthenticationScheme(OpenApiAuthenticationExtension):
    """
    Extension drf_spectacular pour HRJWTAuthentication.
    Permet au Swagger UI d'afficher le bouton "Authorize"
    avec un champ Bearer token.
    """

    target_class = "rh.authentication.HRJWTAuthentication"
    name = "jwtAuth"

    def get_security_definition(self, auto_schema):
        return {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": "Token JWT obtenu via POST /api/v1/auth/login/",
        }


class HRJWTUser:
    """
    Objet user léger issu du JWT.
    Pas de modèle Django, pas de DB.
    """

    is_active = True
    is_anonymous = False
    is_authenticated = True

    def __init__(self, token):
        self.pk = token.get("user_id")
        self.id = self.pk
        self.tenant_id = token.get("tenant_id")
        self.roles = token.get("roles", [])
        self.auth_user_id = token.get("auth_user_id")
        self.custom_role = token.get("custom_role")

    def __str__(self):
        return f"HRJWTUser({self.pk})"


class HRJWTAuthentication(JWTAuthentication):
    """
    Surcharge get_user() pour retourner HRJWTUser
    sans requête SQL — le service RH est stateless.
    """

    def get_user(self, validated_token):
        return HRJWTUser(validated_token)

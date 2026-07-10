"""
ServiceTokenAuthentication — Formuloo OS
Authentification des appels inter-services (HR → Auth, HR → Compta).

Mécanisme : header X-Service-Token = secret partagé configuré
dans INTERNAL_SERVICE_TOKEN (settings.py de chaque service).

Pas de JWT, pas de DB. Simple et sécurisé pour le réseau interne Docker.
"""

from django.conf import settings
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed


class ServiceUser:
    """Objet user synthétique représentant un service interne."""

    is_active = True
    is_anonymous = False
    is_authenticated = True
    pk = None
    id = None
    tenant = None
    tenant_id = None
    roles = []
    auth_user_id = None

    def __str__(self):
        return "ServiceUser(internal)"

    # Django admin compatibility
    def has_perm(self, perm, obj=None):
        return False

    def has_module_perms(self, app_label):
        return False


class ServiceTokenAuthentication(BaseAuthentication):
    """
    Authentifie une requête si elle porte X-Service-Token
    avec la valeur attendue dans settings.INTERNAL_SERVICE_TOKEN.

    Usage dans une view :
        authentication_classes = [ServiceTokenAuthentication, JWTAuthentication]
        permission_classes = [IsAuthenticated]
    """

    def authenticate(self, request):
        token = request.headers.get("X-Service-Token")
        if not token:
            return None

        expected = getattr(settings, "INTERNAL_SERVICE_TOKEN", None)
        if not expected:
            return None

        if token != expected:
            raise AuthenticationFailed("Token inter-service invalide.")

        return (ServiceUser(), None)

    def authenticate_header(self, request):
        return "X-Service-Token"

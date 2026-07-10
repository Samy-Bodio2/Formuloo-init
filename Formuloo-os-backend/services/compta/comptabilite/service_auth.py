"""
ServiceTokenAuthentication — Formuloo OS Service Compta
Même mécanisme que dans Auth : X-Service-Token pour les appels inter-services.
"""

from django.conf import settings
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed


class ServiceUser:
    is_active = True
    is_anonymous = False
    is_authenticated = True
    pk = None
    id = None
    tenant_id = None
    roles = []
    permissions = []
    auth_user_id = None

    def __str__(self):
        return "ServiceUser(internal)"


class ServiceTokenAuthentication(BaseAuthentication):

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

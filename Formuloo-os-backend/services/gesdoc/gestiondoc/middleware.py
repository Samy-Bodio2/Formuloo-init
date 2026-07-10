"""
Middlewares — Formuloo OS Service GesDoc

1. HttpMethodMiddleware — bloque les méthodes non supportées (→ 405)
2. TenantMiddleware — extrait tenant_id, roles, permissions du JWT
"""

from django.http import JsonResponse
from gestiondoc.authentication import GesdocJWTAuthentication

ALLOWED_HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"]


class HttpMethodMiddleware:

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.method not in ALLOWED_HTTP_METHODS:
            return JsonResponse(
                {
                    "error": {
                        "code": "METHOD_NOT_ALLOWED",
                        "message": f"Méthode {request.method} non autorisée.",
                    }
                },
                status=405,
            )
        return self.get_response(request)


class TenantMiddleware:
    """
    Enrichit request.user avec les données JWT :
        tenant_id    : UUID organisation
        roles        : rôles système ['COMPTABLE', ...]
        permissions  : permissions granulaires ['gesdoc.read.documents', ...]
        auth_user_id : UUID user dans Auth
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        try:
            jwt_auth = GesdocJWTAuthentication()
            result = jwt_auth.authenticate(request)

            if result is not None:
                user, token = result
                user.tenant_id = token.get("tenant_id", None)
                user.roles = token.get("roles", [])
                user.permissions = token.get("permissions", [])
                user.auth_user_id = token.get("auth_user_id", None)
                request.user = user

        except Exception:
            pass

        return self.get_response(request)

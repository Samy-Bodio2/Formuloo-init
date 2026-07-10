"""
URLs principales — Service RH
Formuloo OS
Conforme ADR-003 : versionnement /api/v1/
"""

from django.contrib import admin
from django.urls import include, path
from drf_spectacular.views import (
    SpectacularAPIView,
    SpectacularRedocView,
    SpectacularSwaggerView,
)
from rest_framework.permissions import AllowAny

# ── Handlers d'erreurs globaux ────────────────────────────
# Garantissent que les erreurs retournent
# toujours du JSON au lieu de HTML
# Même en DEBUG=True


def handler404(request, exception=None):
    """
    Handler 404 — Ressource introuvable.
    Retourne JSON au lieu de HTML Django.
    Corrige l'erreur Schemathesis :
    'Undocumented Content-Type: text/html'
    """
    from django.http import JsonResponse

    return JsonResponse(
        {
            "error": {
                "code": "NOT_FOUND",
                "message": "Ressource introuvable.",
            }
        },
        status=404,
    )


def handler405(request, exception=None):
    """
    Handler 405 — Méthode non autorisée.
    Retourne 405 pour les méthodes non documentées
    Ex: TRACE, CONNECT, etc.
    Corrige l'erreur Schemathesis :
    'Unsupported methods returned 401, expected 405'
    """
    from django.http import JsonResponse

    return JsonResponse(
        {
            "error": {
                "code": "METHOD_NOT_ALLOWED",
                "message": "Méthode non autorisée.",
            }
        },
        status=405,
    )


def handler500(request):
    """
    Handler 500 — Erreur serveur.
    Retourne JSON au lieu de HTML Django.
    """
    from django.http import JsonResponse

    return JsonResponse(
        {
            "error": {
                "code": "INTERNAL_ERROR",
                "message": "Une erreur interne est survenue.",
            }
        },
        status=500,
    )


urlpatterns = [
    # Admin Django
    path("admin/", admin.site.urls),
    # OpenAPI 3 — accessible sans auth (doc publique)
    path(
        "api/schema/",
        SpectacularAPIView.as_view(permission_classes=[AllowAny]),
        name="schema",
    ),
    path(
        "api/schema/swagger-ui/",
        SpectacularSwaggerView.as_view(url="/api/schema/hr/", permission_classes=[AllowAny]),
        name="swagger-ui",
    ),
    path(
        "api/schema/redoc/",
        SpectacularRedocView.as_view(url_name="schema", permission_classes=[AllowAny]),
        name="redoc",
    ),
    # RH endpoints — versionnement /api/v1/
    path("api/v1/hr/", include("rh.urls")),
]

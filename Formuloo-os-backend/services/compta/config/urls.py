"""URLs principales — Service Compta — Formuloo OS"""

from django.contrib import admin
from django.urls import include, path
from drf_spectacular.views import (
    SpectacularAPIView,
    SpectacularRedocView,
    SpectacularSwaggerView,
)
from rest_framework.permissions import AllowAny


def handler404(request, exception=None):
    from django.http import JsonResponse
    return JsonResponse(
        {"error": {"code": "NOT_FOUND", "message": "Ressource introuvable."}},
        status=404,
    )


def handler500(request):
    from django.http import JsonResponse
    return JsonResponse(
        {"error": {"code": "INTERNAL_ERROR", "message": "Erreur interne."}},
        status=500,
    )


urlpatterns = [
    path("admin/", admin.site.urls),
    path(
        "api/schema/",
        SpectacularAPIView.as_view(permission_classes=[AllowAny]),
        name="schema",
    ),
    path(
        "api/schema/swagger-ui/",
        SpectacularSwaggerView.as_view(url="/api/schema/compta/", permission_classes=[AllowAny]),
        name="swagger-ui",
    ),
    path(
        "api/schema/redoc/",
        SpectacularRedocView.as_view(url_name="schema", permission_classes=[AllowAny]),
        name="redoc",
    ),
    path("api/v1/compta/", include("comptabilite.urls")),
]

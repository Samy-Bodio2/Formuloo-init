"""
URLs principales — Service Auth
Formuloo OS

Conforme ADR-003 : versionnement /api/v1/
"""

from django.contrib import admin
from django.urls import path, include
from drf_spectacular.views import (
    SpectacularAPIView,
    SpectacularSwaggerView,
    SpectacularRedocView,
)
from rest_framework.permissions import AllowAny

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
        SpectacularSwaggerView.as_view(url="/api/schema/auth/", permission_classes=[AllowAny]),
        name="swagger-ui",
    ),
    path(
        "api/schema/redoc/",
        SpectacularRedocView.as_view(url_name="schema", permission_classes=[AllowAny]),
        name="redoc",
    ),
    # Auth endpoints — versionnement /api/v1/
    path("api/v1/auth/", include("authentification.urls")),
]

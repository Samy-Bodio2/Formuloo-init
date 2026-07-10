"""
Configuration Django — Service RH
Formuloo OS — ERP cloud pour PME africaines

Conforme à :
- ADR-001 : Architecture Microservices
- ADR-002 : Authentification SSO (JWT + Keycloak)
- ADR-003 : Versionnement /api/v1/
- ADR-004 : PostgreSQL + Redis
"""

from datetime import timedelta
from pathlib import Path

import environ

# ── CHEMINS ───────────────────────────────────────────────
BASE_DIR = Path(__file__).resolve().parent.parent

# ── VARIABLES D'ENVIRONNEMENT ─────────────────────────────
env = environ.Env(DEBUG=(bool, False))
environ.Env.read_env(BASE_DIR / ".env")

# ── SECURITE ──────────────────────────────────────────────
SECRET_KEY = env("SECRET_KEY")
DEBUG = env("DEBUG")
ALLOWED_HOSTS = env.list("ALLOWED_HOSTS")

# ── APPLICATIONS ──────────────────────────────────────────
INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    # Third-party
    "rest_framework",
    "rest_framework_simplejwt",
    "corsheaders",
    "drf_spectacular",
    # Local
    "rh",
]

# ── MIDDLEWARE ────────────────────────────────────────────
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "rh.middleware.HttpMethodMiddleware",
    # Middleware Formuloo OS :
    # Extrait tenant_id, roles, auth_user_id,
    # custom_role depuis le JWT
    # et les injecte dans request.user
    "rh.middleware.TenantMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# ── BASE DE DONNÉES ───────────────────────────────────────
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": env("DB_NAME", default="formuloo_hr"),
        "USER": env("DB_USER", default="postgres"),
        "PASSWORD": env("DB_PASSWORD", default=""),
        "HOST": env("DB_HOST", default="localhost"),
        "PORT": env("DB_PORT", default="5432"),
    }
}

# ── CACHE REDIS ───────────────────────────────────────────
CACHES = {
    "default": {
        "BACKEND": "django_redis.cache.RedisCache",
        "LOCATION": env("REDIS_URL", default="redis://localhost:6379/0"),
        "OPTIONS": {
            "CLIENT_CLASS": "django_redis.client.DefaultClient",
        },
    }
}

# ── DJANGO REST FRAMEWORK ─────────────────────────────────
REST_FRAMEWORK = {
    # Authentification JWT par défaut
    "DEFAULT_AUTHENTICATION_CLASSES": ("rh.authentication.HRJWTAuthentication",),
    # Authentification obligatoire par défaut
    "DEFAULT_PERMISSION_CLASSES": ("rest_framework.permissions.IsAuthenticated",),
    # Handler global des exceptions
    # Garantit un format JSON uniforme pour toutes les erreurs
    # Conforme au contrat hr.yaml v2.1.0
    "EXCEPTION_HANDLER": "rh.exceptions.custom_exception_handler",
    # Schéma OpenAPI 3.0
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
    # Pagination par défaut
    "DEFAULT_PAGINATION_CLASS": ("rest_framework.pagination.PageNumberPagination"),
    "PAGE_SIZE": 20,
}

# ── JWT ───────────────────────────────────────────────────
SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(
        minutes=env.int("ACCESS_TOKEN_LIFETIME", default=15)
    ),
    "REFRESH_TOKEN_LIFETIME": timedelta(
        days=env.int("REFRESH_TOKEN_LIFETIME", default=7)
    ),
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "ALGORITHM": "HS256",
    "SIGNING_KEY": env("AUTH_SECRET_KEY", default=SECRET_KEY),
    "AUTH_HEADER_TYPES": ("Bearer",),
    "USER_ID_FIELD": "id",
    "USER_ID_CLAIM": "user_id",
}

# ── DRF SPECTACULAR (OpenAPI 3) ───────────────────────────
SPECTACULAR_SETTINGS = {
    "TITLE": "Formuloo OS — HR Service API",
    "DESCRIPTION": (
        "Service de gestion des Ressources Humaines de Formuloo OS. "
        "Gère les employés, postes, départements, contrats, congés, "
        "présences et fiches de paie SYSCOHADA. "
        "Toutes les données sont isolées par tenant_id (JWT)."
    ),
    "VERSION": "2.1.0",
    "SERVE_INCLUDE_SCHEMA": False,
    "COMPONENT_SPLIT_REQUEST": True,
    "SWAGGER_UI_SETTINGS": {
        "persistAuthorization": True,
        "displayRequestDuration": True,
    },
    "ENUM_NAME_OVERRIDES": {
        "TypeCongeEnum": "rh.models.conge.Conge.TypeConge.choices",
        "StatutCongeEnum": "rh.models.conge.Conge.StatutConge.choices",
        "StatutEmployeEnum": "rh.models.employe.Employe.Status.choices",
        "StatutFichePaieEnum": "rh.models.fiche_paie.FichePaie.Statut.choices",
        "ModePaiementEnum": "rh.models.fiche_paie.FichePaie.ModePaiement.choices",
    },
}

# ── CORS ──────────────────────────────────────────────────
CORS_ALLOW_CREDENTIALS = True
CORS_ALLOW_HEADERS = [
    "accept",
    "accept-encoding",
    "authorization",
    "content-type",
    "dnt",
    "origin",
    "user-agent",
    "x-csrftoken",
    "x-requested-with",
    "x-tenant-id",
]

if DEBUG:
    CORS_ALLOW_ALL_ORIGINS = True
else:
    _extra_origins = env.list("CORS_ALLOWED_ORIGINS", default=[])
    CORS_ALLOWED_ORIGINS = list({
        "http://localhost:3000",
        "http://localhost:5173",
        env("FRONTEND_URL", default="http://localhost:3000"),
        *_extra_origins,
    })

# ── INTERNATIONALISATION ──────────────────────────────────
LANGUAGE_CODE = "fr-fr"
TIME_ZONE = "Africa/Douala"
USE_I18N = True
USE_TZ = True

# ── FICHIERS STATIQUES ────────────────────────────────────
STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# ── LOGGING ───────────────────────────────────────────────
# Configuration des logs pour le service RH
# Niveau INFO en production
# Niveau DEBUG en développement
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "verbose": {
            "format": "{levelname} {asctime} "
            "{module} {process:d} "
            "{thread:d} {message}",
            "style": "{",
        },
        "simple": {
            "format": "{levelname} {asctime} " "{module} — {message}",
            "style": "{",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "simple",
        },
    },
    "loggers": {
        # Logger principal du service RH
        "rh": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": True,
        },
        # Logger Django
        "django": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": True,
        },
        # Logger des requêtes Django
        "django.request": {
            "handlers": ["console"],
            "level": "ERROR",
            "propagate": False,
        },
    },
}

# ── EMAIL ────────────────────────────────────────────────
EMAIL_BACKEND = env("EMAIL_BACKEND", default="django.core.mail.backends.smtp.EmailBackend")
EMAIL_HOST = env("EMAIL_HOST", default="smtp.gmail.com")
EMAIL_PORT = env.int("EMAIL_PORT", default=587)
EMAIL_USE_TLS = env.bool("EMAIL_USE_TLS", default=True)
EMAIL_HOST_USER = env("EMAIL_HOST_USER", default="")
EMAIL_HOST_PASSWORD = env("EMAIL_HOST_PASSWORD", default="")
DEFAULT_FROM_EMAIL = env("DEFAULT_FROM_EMAIL", default="Formuloo OS <noreply@formuloo.cm>")

# ── COMMUNICATION INTER-SERVICES ─────────────────────────
# Partagé entre Auth, HR et Compta. Réseau Docker interne uniquement.
INTERNAL_SERVICE_TOKEN = env("INTERNAL_SERVICE_TOKEN", default="formuloo-internal-secret-change-in-prod")

# Auth
AUTH_SERVICE_URL = env("AUTH_SERVICE_URL", default="http://localhost:8000")
AUTH_SERVICE_TOKEN = INTERNAL_SERVICE_TOKEN  # alias rétrocompatible

# Compta
COMPTA_SERVICE_URL = env("COMPTA_SERVICE_URL", default="http://localhost:8002")

# ── Handlers d'erreurs personnalisés ─────────────────────
# Remplacent les pages HTML Django par du JSON
# Corrige l'erreur Schemathesis :
# 'Undocumented Content-Type: text/html'
handler404 = "config.urls.handler404"
handler500 = "config.urls.handler500"

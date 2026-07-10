"""
Configuration Django — Service Auth
Formuloo OS — ERP cloud pour PME africaines

Conforme à :
- ADR-001 : Architecture Microservices
- ADR-002 : Authentification SSO (JWT + Keycloak)
- ADR-003 : Versionnement /api/v1/
- ADR-004 : PostgreSQL + Redis
"""

import environ
from pathlib import Path
from datetime import timedelta

# ── CHEMINS ──────────────────────────────────────
BASE_DIR = Path(__file__).resolve().parent.parent

# ── VARIABLES D'ENVIRONNEMENT ────────────────────
env = environ.Env(DEBUG=(bool, False))
environ.Env.read_env(BASE_DIR / ".env")

# ── SECURITE ─────────────────────────────────────
SECRET_KEY = env("SECRET_KEY")
DEBUG = env("DEBUG")
ALLOWED_HOSTS = env.list("ALLOWED_HOSTS")

# ── APPLICATIONS ─────────────────────────────────
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
    "rest_framework_simplejwt.token_blacklist",
    "corsheaders",
    "drf_spectacular",
    # Local
    "authentification",
]

# ── MIDDLEWARE ────────────────────────────────────
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
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

# ── BASE DE DONNÉES ───────────────────────────────
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": env("DB_NAME", default="formuloo_auth"),
        "USER": env("DB_USER", default="postgres"),
        "PASSWORD": env("DB_PASSWORD", default=""),
        "HOST": env("DB_HOST", default="localhost"),
        "PORT": env("DB_PORT", default="5432"),
    }
}

# ── CACHE REDIS ───────────────────────────────────
CACHES = {
    "default": {
        "BACKEND": "django_redis.cache.RedisCache",
        "LOCATION": env("REDIS_URL", default="redis://localhost:6379/0"),
        "OPTIONS": {
            "CLIENT_CLASS": "django_redis.client.DefaultClient",
        },
    }
}

# ── DJANGO REST FRAMEWORK ─────────────────────────
REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ),
    "DEFAULT_PERMISSION_CLASSES": ("rest_framework.permissions.IsAuthenticated",),
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
    "EXCEPTION_HANDLER": "authentification.exceptions.custom_exception_handler",
}

# ── JWT ───────────────────────────────────────────
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
    "AUTH_HEADER_TYPES": ("Bearer",),
    "USER_ID_FIELD": "id",
    "USER_ID_CLAIM": "user_id",
}

# ── DRF SPECTACULAR (OpenAPI 3) ───────────────────
SPECTACULAR_SETTINGS = {
    "TITLE": "Formuloo OS — Auth Service API",
    "DESCRIPTION": "Service d'authentification SSO pour Formuloo OS ERP",
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,
    "COMPONENT_SPLIT_REQUEST": True,
}

# ── CORS ──────────────────────────────────────────
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

# ── INTERNATIONALISATION ──────────────────────────
LANGUAGE_CODE = "fr-fr"
TIME_ZONE = "Africa/Douala"
USE_I18N = True
USE_TZ = True

# ── FICHIERS STATIQUES ────────────────────────────
STATIC_URL = "static/"

# ── MODELE UTILISATEUR ────────────────────────────
AUTH_USER_MODEL = "authentification.User"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# ── EMAIL ─────────────────────────────────────────
# En dev  : les emails s'affichent dans la console Docker (stdout)
# En prod : configurer SMTP via les variables d'environnement EMAIL_*
if DEBUG:
    EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"
else:
    EMAIL_BACKEND = "django.core.mail.backends.smtp.EmailBackend"
    EMAIL_HOST = env("EMAIL_HOST", default="smtp.gmail.com")
    EMAIL_PORT = env.int("EMAIL_PORT", default=587)
    EMAIL_USE_TLS = env.bool("EMAIL_USE_TLS", default=True)
    EMAIL_HOST_USER = env("EMAIL_HOST_USER", default="")
    EMAIL_HOST_PASSWORD = env("EMAIL_HOST_PASSWORD", default="")

DEFAULT_FROM_EMAIL = env(
    "DEFAULT_FROM_EMAIL", default="Formuloo OS <noreply@formuloo.io>"
)

# URL du frontend — utilisée dans les liens d'email (reset, invitation, etc.)
FRONTEND_URL = env("FRONTEND_URL", default="http://localhost:3000")

# ── COMMUNICATION INTER-SERVICES ─────────────────
# Secret partagé entre Auth, HR et Compta pour les appels API internes.
# Ne jamais exposer à l'extérieur (réseau Docker interne uniquement).
INTERNAL_SERVICE_TOKEN = env("INTERNAL_SERVICE_TOKEN", default="formuloo-internal-secret-change-in-prod")

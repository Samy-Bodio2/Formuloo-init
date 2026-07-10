"""
Configuration Django — Service GesDoc
Formuloo OS — ERP cloud pour PME africaines

Module de gestion documentaire OCR + Blockchain (thèse).
Conforme à :
- ADR-001 : Architecture Microservices
- ADR-002 : Authentification SSO (JWT)
- ADR-003 : Versionnement /api/v1/
- ADR-004 : PostgreSQL + Redis
"""

from datetime import timedelta
from pathlib import Path

import environ

BASE_DIR = Path(__file__).resolve().parent.parent

env = environ.Env(DEBUG=(bool, False))
environ.Env.read_env(BASE_DIR / ".env")

SECRET_KEY = env("SECRET_KEY")
DEBUG = env("DEBUG")
ALLOWED_HOSTS = env.list("ALLOWED_HOSTS")

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "rest_framework_simplejwt",
    "corsheaders",
    "drf_spectacular",
    "gestiondoc",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "gestiondoc.middleware.HttpMethodMiddleware",
    "gestiondoc.middleware.TenantMiddleware",
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

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": env("DB_NAME", default="formuloo_gesdoc"),
        "USER": env("DB_USER", default="postgres"),
        "PASSWORD": env("DB_PASSWORD", default=""),
        "HOST": env("DB_HOST", default="localhost"),
        "PORT": env("DB_PORT", default="5432"),
    }
}

CACHES = {
    "default": {
        "BACKEND": "django_redis.cache.RedisCache",
        "LOCATION": env("REDIS_URL", default="redis://localhost:6379/3"),
        "OPTIONS": {
            "CLIENT_CLASS": "django_redis.client.DefaultClient",
        },
    }
}

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "gestiondoc.authentication.GesdocJWTAuthentication",
    ),
    "DEFAULT_PERMISSION_CLASSES": ("rest_framework.permissions.IsAuthenticated",),
    "EXCEPTION_HANDLER": "gestiondoc.exceptions.custom_exception_handler",
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 20,
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(
        minutes=env.int("ACCESS_TOKEN_LIFETIME", default=15)
    ),
    "REFRESH_TOKEN_LIFETIME": timedelta(
        days=env.int("REFRESH_TOKEN_LIFETIME", default=7)
    ),
    "ALGORITHM": "HS256",
    "SIGNING_KEY": env("AUTH_SECRET_KEY", default=SECRET_KEY),
    "AUTH_HEADER_TYPES": ("Bearer",),
    "USER_ID_FIELD": "id",
    "USER_ID_CLAIM": "user_id",
}

SPECTACULAR_SETTINGS = {
    "TITLE": "Formuloo OS — GesDoc Service API",
    "DESCRIPTION": (
        "Service de gestion documentaire intelligente de Formuloo OS. "
        "Pipeline OCR (OpenCV + Tesseract5 + EasyOCR fallback + CamemBERT/spaCy) "
        "et certification blockchain (ancrage du hash SHA-256 sur Ethereum). "
        "Toutes les données sont isolées par tenant_id (JWT)."
    ),
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,
    "COMPONENT_SPLIT_REQUEST": True,
    "SWAGGER_UI_SETTINGS": {
        "persistAuthorization": True,
        "displayRequestDuration": True,
    },
}

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

# ── Inter-services ───────────────────────────────────────────
INTERNAL_SERVICE_TOKEN = env("INTERNAL_SERVICE_TOKEN", default="")
COMPTA_SERVICE_URL = env("COMPTA_SERVICE_URL", default="http://localhost:8002")
# Optionnel — service Analytics pas encore déployé (pas d'entrée docker-compose).
# Si vide, les alertes d'intégrité restent uniquement dans le journal d'audit local.
ANALYTICS_SERVICE_URL = env("ANALYTICS_SERVICE_URL", default="")

# ── Celery ────────────────────────────────────────────────────
CELERY_BROKER_URL = env("CELERY_BROKER_URL", default="redis://localhost:6379/4")
CELERY_RESULT_BACKEND = env("CELERY_RESULT_BACKEND", default="redis://localhost:6379/4")
CELERY_ACCEPT_CONTENT = ["json"]
CELERY_TASK_SERIALIZER = "json"
CELERY_RESULT_SERIALIZER = "json"
CELERY_TIMEZONE = "Africa/Douala"
CELERY_TASK_ALWAYS_EAGER = env.bool("CELERY_TASK_ALWAYS_EAGER", default=False)

# ── Stockage fichiers (volume Docker local) ──────────────────
MEDIA_ROOT = env("MEDIA_ROOT", default=str(BASE_DIR / "media"))
MEDIA_URL = "/media/"
DOCUMENT_MAX_UPLOAD_SIZE = env.int("DOCUMENT_MAX_UPLOAD_SIZE", default=20 * 1024 * 1024)  # 20 Mo
SIGNED_URL_EXPIRY_SECONDS = env.int("SIGNED_URL_EXPIRY_SECONDS", default=15 * 60)  # 15 min

# ── Pipeline OCR ──────────────────────────────────────────────
OCR_FALLBACK_CONFIDENCE_THRESHOLD = env.int("OCR_FALLBACK_CONFIDENCE_THRESHOLD", default=80)
TESSERACT_CMD = env("TESSERACT_CMD", default="tesseract")
TESSERACT_LANG = env("TESSERACT_LANG", default="fra")

# ── Blockchain (Ethereum Sepolia via Infura) ─────────────────
BLOCKCHAIN_NETWORK = env("BLOCKCHAIN_NETWORK", default="sepolia")
INFURA_PROJECT_ID = env("INFURA_PROJECT_ID", default="")
BLOCKCHAIN_PRIVATE_KEY = env("SEPOLIA_PRIVATE_KEY", default="")
BLOCKCHAIN_CONTRACT_ADDRESS = env("CONTRACT_ADDRESS", default="")
ETHERSCAN_BASE_URL = env(
    "ETHERSCAN_BASE_URL", default="https://sepolia.etherscan.io"
)

LANGUAGE_CODE = "fr-fr"
TIME_ZONE = "Africa/Douala"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "simple": {
            "format": "{levelname} {asctime} {module} — {message}",
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
        "gestiondoc": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": True,
        },
        "django": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": True,
        },
        "django.request": {
            "handlers": ["console"],
            "level": "ERROR",
            "propagate": True,
        },
    },
}

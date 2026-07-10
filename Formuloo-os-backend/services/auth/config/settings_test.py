"""
Settings de test — utilise SQLite en mémoire
Pour exécuter les tests sans PostgreSQL ni Redis
"""

from .settings import *  # noqa: F401, F403

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": ":memory:",
    }
}

CACHES = {
    "default": {
        "BACKEND": "django.core.cache.backends.dummy.DummyCache",
    }
}

ALLOWED_HOSTS = ["*"]

# Les emails sont capturés en mémoire pendant les tests
# → pas d'envoi réel, accessible via django.core.mail.outbox
EMAIL_BACKEND = "django.core.mail.backends.locmem.EmailBackend"
DEFAULT_FROM_EMAIL = "test@formuloo.io"
FRONTEND_URL = "http://localhost:3000"

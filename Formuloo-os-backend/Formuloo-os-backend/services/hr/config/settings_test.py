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

# En test, la clé de signature JWT peut être la même que SECRET_KEY
# (aucun vrai token n'est généré — force_authenticate bypass l'auth)
SIMPLE_JWT = {
    **SIMPLE_JWT,  # noqa: F405
    "SIGNING_KEY": SECRET_KEY,  # noqa: F405
}

ALLOWED_HOSTS = ["*"]

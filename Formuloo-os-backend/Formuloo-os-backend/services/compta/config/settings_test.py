"""Settings de test — SQLite en mémoire, pas de Redis."""

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

SIMPLE_JWT = {
    **SIMPLE_JWT,  # noqa: F405
    "SIGNING_KEY": SECRET_KEY,  # noqa: F405
}

ALLOWED_HOSTS = ["*"]

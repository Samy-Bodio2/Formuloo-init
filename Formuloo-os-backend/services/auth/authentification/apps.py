"""
Configuration de l'application Auth — Formuloo OS
"""

from django.apps import AppConfig


class AuthentificationConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "authentification"
    verbose_name = "Authentification"

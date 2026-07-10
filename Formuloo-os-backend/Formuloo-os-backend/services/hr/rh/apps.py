"""
Configuration de l'application RH — Formuloo OS
"""

from django.apps import AppConfig


class RhConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "rh"
    verbose_name = "Ressources Humaines"

    def ready(self):
        """
        Appelé au démarrage de Django.
        Importe les signaux si nécessaire.
        """
        pass

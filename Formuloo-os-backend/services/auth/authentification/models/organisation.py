"""
Modèle Organisation (Tenant) — Formuloo OS
Représente une PME cliente de la plateforme.
Conforme ADR-001 : isolation multi-tenant

NOTE ADR-005 : Les champs plan et timezone sont omis
au Sprint 1 et seront ajoutés aux Sprints 4 et 6.
"""

import uuid
from django.db import models


class Organisation(models.Model):

    class Locale(models.TextChoices):
        FR = "fr", "Français"
        EN = "en", "English"
        PT = "pt", "Português"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    slug = models.SlugField(max_length=100, unique=True)
    name = models.CharField(max_length=255)
    currency = models.CharField(max_length=10, default="XAF")
    locale = models.CharField(max_length=5, choices=Locale.choices, default=Locale.FR)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "organisations"
        verbose_name = "Organisation"
        verbose_name_plural = "Organisations"
        ordering = ["name"]

    def __str__(self):
        return f"{self.name} ({self.slug})"

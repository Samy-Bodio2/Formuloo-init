"""
Modèle Permission — Formuloo OS
Permissions granulaires RBAC.
Conforme ADR-002 : contrôle d'accès basé sur les rôles

Format du code : module.action.ressource
Exemple : hr.read.employes
"""

import uuid
from django.db import models


class Permission(models.Model):

    class Module(models.TextChoices):
        AUTH = "auth", "Authentification"
        HR = "hr", "Ressources Humaines"
        COMPTA = "compta", "Comptabilité"
        CRM = "crm", "CRM"
        STOCK = "stock", "Stock"
        ANALYTICS = "analytics", "Analytics"

    class Action(models.TextChoices):
        READ = "read", "Lire"
        WRITE = "write", "Écrire"
        DELETE = "delete", "Supprimer"
        VALIDATE = "validate", "Valider"
        CLOSE = "close", "Clôturer"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    module = models.CharField(max_length=20, choices=Module.choices)
    action = models.CharField(max_length=20, choices=Action.choices)
    resource = models.CharField(max_length=100)
    code = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = "permissions"
        verbose_name = "Permission"
        verbose_name_plural = "Permissions"
        ordering = ["module", "action", "resource"]
        unique_together = [["module", "action", "resource"]]

    def save(self, *args, **kwargs):
        """
        Génère automatiquement le code
        à partir de module.action.resource
        """
        self.code = f"{self.module}.{self.action}.{self.resource}"
        super().save(*args, **kwargs)

    def __str__(self):
        return self.code

"""
Modèle Role — Formuloo OS
Rôles RBAC associés aux utilisateurs.
Conforme ADR-002 : contrôle d'accès basé sur les rôles

Rôles système (is_system=True) :
- SUPER_ADMIN  : accès total à la plateforme
- ADMIN_PME    : accès total à son organisation
- RH_MANAGER   : accès au module RH
- COMPTABLE    : accès au module Comptabilité
- COMMERCIAL   : accès au module CRM
- EMPLOYE      : accès à ses propres données
"""

import uuid
from django.db import models


class Role(models.Model):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant = models.ForeignKey(
        "authentification.Organisation",
        on_delete=models.CASCADE,
        related_name="roles",
        null=True,
        blank=True,
    )
    name = models.CharField(max_length=100)
    code = models.CharField(max_length=100)
    is_system = models.BooleanField(default=False)
    permissions = models.ManyToManyField(
        "authentification.Permission", related_name="roles", blank=True
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "roles"
        verbose_name = "Rôle"
        verbose_name_plural = "Rôles"
        unique_together = [["tenant", "code"]]
        ordering = ["name"]

    def __str__(self):
        return f"{self.name} ({self.code})"

    def save(self, *args, **kwargs):
        """
        Empêche la modification des rôles système.
        Les rôles système (is_system=True) sont
        créés par les migrations et ne peuvent
        pas être modifiés par les utilisateurs.
        """
        if self.pk:
            original = Role.objects.filter(pk=self.pk).first()
            if original and original.is_system:
                raise ValueError("Les rôles système ne peuvent pas être modifiés.")
        super().save(*args, **kwargs)

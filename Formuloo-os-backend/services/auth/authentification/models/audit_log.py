"""
Modèle AuditLog — Formuloo OS
Journal immuable des actions utilisateurs.
Conforme ADR-002 : traçabilité + sécurité

IMPORTANT : Ce modèle est IMMUABLE.
Aucune modification ou suppression n'est autorisée.
"""

import uuid
from django.db import models


class AuditLog(models.Model):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant = models.ForeignKey(
        "authentification.Organisation",
        on_delete=models.CASCADE,
        related_name="audit_logs",
        null=True,
        blank=True,
    )
    user = models.ForeignKey(
        "authentification.User",
        on_delete=models.SET_NULL,
        related_name="audit_logs",
        null=True,
        blank=True,
    )
    action = models.CharField(max_length=100)
    resource = models.CharField(max_length=100)
    resource_id = models.UUIDField(null=True, blank=True)
    payload = models.JSONField(default=dict)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "audit_logs"
        verbose_name = "Journal d'audit"
        verbose_name_plural = "Journaux d'audit"
        ordering = ["-timestamp"]

    def __str__(self):
        return f"{self.action} — {self.user} — {self.timestamp}"

    def save(self, *args, **kwargs):
        """
        IMMUABLE : interdit la modification d'un log existant.
        Vérifie si l'objet existe déjà en base de données
        avant d'autoriser la sauvegarde.
        """
        if self.pk and AuditLog.objects.filter(pk=self.pk).exists():
            raise ValueError("Un journal d'audit ne peut pas être modifié.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        """
        IMMUABLE : interdit la suppression d'un log.
        """
        raise ValueError("Un journal d'audit ne peut pas être supprimé.")

    @classmethod
    def log(
        cls,
        tenant,
        user,
        action,
        resource,
        resource_id=None,
        payload=None,
        ip_address=None,
    ):
        """
        Méthode utilitaire pour créer un log d'audit.

        Usage :
            AuditLog.log(
                tenant=request.tenant,
                user=request.user,
                action='LOGIN',
                resource='User',
                resource_id=user.id,
                payload={'email': user.email},
                ip_address=request.META.get('REMOTE_ADDR')
            )
        """
        return cls.objects.create(
            tenant=tenant,
            user=user,
            action=action,
            resource=resource,
            resource_id=resource_id,
            payload=payload or {},
            ip_address=ip_address,
        )

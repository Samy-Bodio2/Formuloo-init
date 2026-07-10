"""
Modèle RefreshToken — Formuloo OS
Gestion du blacklisting des tokens JWT.
Conforme ADR-002 : authentification SSO + sécurité
"""

import uuid
from django.db import models


class RefreshToken(models.Model):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        "authentification.User", on_delete=models.CASCADE, related_name="refresh_tokens"
    )
    token = models.TextField(unique=True)
    is_blacklisted = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()
    blacklisted_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = "refresh_tokens"
        verbose_name = "Token de rafraîchissement"
        verbose_name_plural = "Tokens de rafraîchissement"
        ordering = ["-created_at"]

    def __str__(self):
        status = "blacklisté" if self.is_blacklisted else "actif"
        return f"Token {self.user} — {status}"

    def blacklist(self):
        """
        Blackliste le token lors de la déconnexion.
        Le token ne pourra plus être utilisé pour
        obtenir un nouvel access token.
        """
        from django.utils import timezone

        self.is_blacklisted = True
        self.blacklisted_at = timezone.now()
        self.save(update_fields=["is_blacklisted", "blacklisted_at"])

    @property
    def is_expired(self):
        """
        Vérifie si le token est expiré.
        """
        from django.utils import timezone

        return timezone.now() > self.expires_at

    @property
    def is_valid(self):
        """
        Vérifie si le token est valide :
        - non blacklisté
        - non expiré
        """
        return not self.is_blacklisted and not self.is_expired

"""
Modèle APIKey — Formuloo OS
Clés API pour les intégrations tierces.
Conforme ADR-002 : sécurité + multi-tenant
"""

import uuid
import hashlib
import secrets
from django.db import models


class APIKey(models.Model):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant = models.ForeignKey(
        "authentification.Organisation",
        on_delete=models.CASCADE,
        related_name="api_keys",
    )
    owner = models.ForeignKey(
        "authentification.User", on_delete=models.CASCADE, related_name="api_keys"
    )
    name = models.CharField(max_length=100)
    key_hash = models.CharField(max_length=64, unique=True)
    scopes = models.JSONField(default=list)
    rate_limit = models.IntegerField(default=100)
    is_active = models.BooleanField(default=True)
    last_used = models.DateTimeField(null=True, blank=True)
    expires_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "api_keys"
        verbose_name = "Clé API"
        verbose_name_plural = "Clés API"
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.name} — {self.tenant}"

    @staticmethod
    def generate_key():
        """
        Génère une clé API sécurisée.
        Retourne (clé_brute, hash_sha256).
        La clé brute est affichée UNE SEULE FOIS.
        Seul le hash est stocké en base.
        """
        raw_key = secrets.token_hex(32)
        key_hash = hashlib.sha256(raw_key.encode()).hexdigest()
        return raw_key, key_hash

    @staticmethod
    def hash_key(raw_key):
        """
        Hash une clé brute en SHA-256.
        Utilisé pour vérifier une clé reçue.
        """
        return hashlib.sha256(raw_key.encode()).hexdigest()

"""
Modèle OneTimeToken — Formuloo OS

Token à usage unique pour les flux hors-session :
  - VERIFY_EMAIL   : lien d'activation envoyé à l'inscription
  - RESET_PASSWORD : lien de réinitialisation du mot de passe
  - INVITATION     : lien d'invitation d'un employé par le RH Manager

Chaque token expire après TOKEN_EXPIRY_HOURS heures et ne peut
être utilisé qu'une seule fois (used_at non null = déjà consommé).

Conforme ADR-002 : authentification SSO + JWT
"""

import secrets
import uuid
from datetime import timedelta

from django.db import models
from django.utils import timezone


# Durée de validité en heures selon le type de token
TOKEN_EXPIRY_HOURS = {
    "VERIFY_EMAIL": 48,
    "RESET_PASSWORD": 2,
    "INVITATION": 72,
}


class OneTimeToken(models.Model):
    """
    Token à usage unique lié à un utilisateur.

    Workflow :
      1. create()   — génère un token UUID aléatoire
      2. is_valid() — vérifie expiry + non utilisé
      3. use()      — consomme le token (used_at = now)

    Un token consommé ou expiré est rejeté lors du
    prochain appel à is_valid().
    """

    class Type(models.TextChoices):
        VERIFY_EMAIL = "VERIFY_EMAIL", "Vérification email"
        RESET_PASSWORD = "RESET_PASSWORD", "Réinitialisation mot de passe"
        INVITATION = "INVITATION", "Invitation employé"

    # ── Identifiant ───────────────────────────────────
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Utilisateur concerné ─────────────────────────
    user = models.ForeignKey(
        "authentification.User",
        on_delete=models.CASCADE,
        related_name="one_time_tokens",
        help_text="Utilisateur auquel ce token est lié",
    )

    # ── Token (valeur transmise par email, pour le lien web) ─
    token = models.UUIDField(
        default=uuid.uuid4,
        unique=True,
        help_text="Valeur du token — UUID aléatoire unique",
    )

    # ── Code (saisie manuelle côté client mobile) ────
    code = models.CharField(
        max_length=6,
        db_index=True,
        default="",
        help_text="Code à 6 chiffres généré côté serveur — saisie manuelle",
    )

    # ── Type de flux ─────────────────────────────────
    token_type = models.CharField(
        max_length=20,
        choices=Type.choices,
        help_text="Type de flux auquel ce token correspond",
    )

    # ── Expiration ────────────────────────────────────
    expires_at = models.DateTimeField(
        help_text="Date/heure d'expiration du token",
    )

    # ── Consommation ──────────────────────────────────
    # null = non utilisé, non-null = déjà consommé
    used_at = models.DateTimeField(
        null=True,
        blank=True,
        help_text="Date/heure d'utilisation — null si non encore utilisé",
    )

    # ── Timestamps ────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "one_time_tokens"
        verbose_name = "Token usage unique"
        verbose_name_plural = "Tokens usage unique"
        ordering = ["-created_at"]
        indexes = [
            # Lookup rapide par valeur de token (URL)
            models.Index(fields=["token"]),
            # Lookup par utilisateur + type (resend, invalidation)
            models.Index(fields=["user", "token_type"]),
        ]

    def __str__(self):
        status = "utilisé" if self.used_at else "actif"
        return f"{self.token_type} — {self.user.email} [{status}]"

    # ── Méthodes métier ───────────────────────────────

    def is_valid(self) -> bool:
        """
        Retourne True si le token est encore utilisable :
          - pas encore consommé (used_at est null)
          - pas encore expiré (expires_at > maintenant)
        """
        return self.used_at is None and timezone.now() < self.expires_at

    def use(self):
        """
        Consomme le token en une seule opération atomique.
        Lève ValueError si le token est déjà utilisé ou expiré.
        """
        if not self.is_valid():
            raise ValueError("Token invalide ou expiré.")
        self.used_at = timezone.now()
        self.save(update_fields=["used_at"])

    @classmethod
    def _generate_unique_code(cls, token_type: str) -> str:
        """
        Génère un code à 6 chiffres ne correspondant à aucun token
        encore actif (non utilisé, non expiré) du même type.

        Évite qu'un code en cours de validité soit attribué à deux
        utilisateurs différents en même temps.
        """
        for _ in range(10):
            code = f"{secrets.randbelow(1_000_000):06d}"
            collision = cls.objects.filter(
                token_type=token_type,
                code=code,
                used_at__isnull=True,
                expires_at__gt=timezone.now(),
            ).exists()
            if not collision:
                return code
        # Cas extrêmement improbable — on accepte le risque résiduel
        # plutôt que de bloquer l'inscription.
        return code

    @classmethod
    def create_for(cls, user, token_type: str) -> "OneTimeToken":
        """
        Crée un token neuf pour un utilisateur donné.

        Invalide d'abord tous les tokens du même type
        pour cet utilisateur (un seul token actif à la fois).

        Args:
            user        : instance User
            token_type  : OneTimeToken.Type.VERIFY_EMAIL | etc.

        Returns:
            OneTimeToken : l'instance nouvellement créée
        """
        # Invalider les tokens précédents du même type
        # (on met used_at = now pour les "griller" sans les supprimer)
        cls.objects.filter(
            user=user, token_type=token_type, used_at__isnull=True
        ).update(used_at=timezone.now())

        expiry_hours = TOKEN_EXPIRY_HOURS.get(token_type, 24)
        return cls.objects.create(
            user=user,
            token_type=token_type,
            code=cls._generate_unique_code(token_type),
            expires_at=timezone.now() + timedelta(hours=expiry_hours),
        )

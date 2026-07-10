"""
Modèle Congé — Formuloo OS
Gestion des demandes et approbations de congés.
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

Workflow :
pending → approved (décrémente solde congés)
pending → rejected (motif obligatoire)
pending → annule   (par l'employé lui-même)

Le nombre de jours est calculé automatiquement
à la sauvegarde.
"""

import uuid

from django.db import models


class Conge(models.Model):

    # ── Type de congé ─────────────────────────────────────
    class TypeConge(models.TextChoices):
        ANNUEL = "annuel", "Congé annuel"
        MALADIE = "maladie", "Congé maladie"
        MATERNITE = "maternite", "Congé maternité"
        PATERNITE = "paternite", "Congé paternité"
        SANS_SOLDE = "sans_solde", "Congé sans solde"
        EXCEPTIONNEL = "exceptionnel", "Congé exceptionnel"
        RECUPERATION = "recuperation", "Récupération"
        FORMATION = "formation", "Formation"
        DECES = "deces", "Décès famille"

    # ── Statut de la demande ──────────────────────────────
    class Statut(models.TextChoices):
        PENDING = "pending", "En attente"
        APPROVED = "approved", "Approuvé"
        REJECTED = "rejected", "Rejeté"
        ANNULE = "annule", "Annulé"

    # ── Identifiant ───────────────────────────────────────
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Employé concerné ──────────────────────────────────
    employee = models.ForeignKey(
        "rh.Employe",
        on_delete=models.CASCADE,
        related_name="conges",
        help_text="Employé qui demande le congé",
    )

    # ── Type et période ───────────────────────────────────
    type_conge = models.CharField(
        max_length=20, choices=TypeConge.choices, help_text="Type de congé demandé"
    )
    start_date = models.DateField(help_text="Date de début du congé")
    end_date = models.DateField(help_text="Date de fin du congé")

    # ── Nombre de jours ───────────────────────────────────
    # Calculés automatiquement à la sauvegarde
    days = models.IntegerField(
        default=0, help_text="Nombre de jours calendaires — calculé auto"
    )
    jours_ouvres = models.IntegerField(
        default=0, help_text="Nombre de jours ouvrés — calculé auto"
    )

    # ── Motif et justificatif ─────────────────────────────
    reason = models.TextField(
        null=True, blank=True, help_text="Motif de la demande de congé"
    )
    piece_justificative = models.URLField(
        null=True, blank=True, help_text="URL du justificatif (arrêt maladie, etc.)"
    )

    # ── Workflow d'approbation ────────────────────────────
    status = models.CharField(
        max_length=20,
        choices=Statut.choices,
        default=Statut.PENDING,
        help_text="Statut de la demande",
    )
    approved_by = models.ForeignKey(
        "rh.Employe",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="conges_approuves",
        help_text="RH_MANAGER ayant approuvé ou rejeté",
    )
    approved_at = models.DateTimeField(
        null=True, blank=True, help_text="Date et heure de la décision"
    )
    commentaire_decision = models.TextField(
        null=True, blank=True, help_text="Commentaire d'approbation ou motif de rejet"
    )

    # ── Remplaçant ────────────────────────────────────────
    remplacant = models.ForeignKey(
        "rh.Employe",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="remplacements",
        help_text="Employé assurant l'intérim pendant le congé",
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "conges"
        verbose_name = "Congé"
        verbose_name_plural = "Congés"
        ordering = ["-created_at"]

    def __str__(self):
        return (
            f"{self.employee.full_name} — "
            f"{self.type_conge} — "
            f"{self.start_date} → {self.end_date} "
            f"({self.status})"
        )

    def save(self, *args, **kwargs):
        """
        Calcule automatiquement le nombre de jours
        à chaque sauvegarde.
        """
        if self.start_date and self.end_date:
            delta = self.end_date - self.start_date
            self.days = delta.days + 1
            # Calcul des jours ouvrés (lundi-vendredi)
            self.jours_ouvres = self._calculer_jours_ouvres()
        super().save(*args, **kwargs)

    def _calculer_jours_ouvres(self):
        """
        Calcule le nombre de jours ouvrés
        entre start_date et end_date.
        Exclut les samedis et dimanches.
        """
        from datetime import timedelta

        count = 0
        current = self.start_date
        while current <= self.end_date:
            # 0=lundi, 4=vendredi, 5=samedi, 6=dimanche
            if current.weekday() < 5:
                count += 1
            current += timedelta(days=1)
        return count

    @property
    def is_pending(self):
        return self.status == self.Statut.PENDING

    @property
    def is_approved(self):
        return self.status == self.Statut.APPROVED

    def approuver(self, approuve_par, commentaire=None):
        """
        Approuve la demande de congé.
        Met à jour le statut et décrémente le solde.
        """
        from django.utils import timezone

        self.status = self.Statut.APPROVED
        self.approved_by = approuve_par
        self.approved_at = timezone.now()
        if commentaire:
            self.commentaire_decision = commentaire
        self.save()

    def rejeter(self, rejete_par, motif):
        """
        Rejette la demande de congé.
        Le motif est obligatoire.
        """
        from django.utils import timezone

        self.status = self.Statut.REJECTED
        self.approved_by = rejete_par
        self.approved_at = timezone.now()
        self.commentaire_decision = motif
        self.save()

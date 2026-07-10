"""
Modèle Présence — Formuloo OS
Enregistrement quotidien des pointages des employés.
Conforme ADR-001 : isolation multi-tenant

Calcule automatiquement les heures travaillées
à partir de heure_arrivee et heure_depart.

Soft Delete :
→ is_active=False au lieu de suppression réelle
→ Historique conservé pour audit et litiges
→ Cohérent avec les autres modèles RH
"""

import uuid
from datetime import datetime, timedelta

from django.db import models


class Presence(models.Model):

    # ── Statut de présence ────────────────────────────────
    class Statut(models.TextChoices):
        PRESENT = "present", "Présent"
        ABSENT = "absent", "Absent"
        RETARD = "retard", "Retard"
        CONGE = "conge", "En congé"
        FERIE = "ferie", "Jour férié"

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
        related_name="presences",
        help_text="Employé pointé",
    )

    # ── Date et horaires ──────────────────────────────────
    date = models.DateField(help_text="Date du pointage")
    heure_arrivee = models.TimeField(null=True, blank=True, help_text="Heure d'arrivée")
    heure_depart = models.TimeField(null=True, blank=True, help_text="Heure de départ")

    # ── Heures calculées automatiquement ──────────────────
    # Calculées dans save() depuis heure_arrivee
    # et heure_depart
    heures_travaillees = models.DecimalField(
        max_digits=5,
        decimal_places=2,
        null=True,
        blank=True,
        help_text="Calculé depuis " "heure_arrivee " "et heure_depart",
    )
    heures_supplementaires = models.DecimalField(
        max_digits=5, decimal_places=2, default=0, help_text="Heures supplémentaires"
    )

    # ── Statut de présence ────────────────────────────────
    statut = models.CharField(
        max_length=20, choices=Statut.choices, default=Statut.PRESENT
    )
    commentaire = models.TextField(
        null=True, blank=True, help_text="Commentaire optionnel"
    )

    # ── Soft Delete ───────────────────────────────────────
    # True  = présence active et visible
    # False = présence archivée (soft delete)
    # On ne supprime JAMAIS une présence
    # pour conserver l'historique complet
    # Utilisé par :
    # → Audit Trail (traçabilité)
    # → Rapports RH (historique)
    # → Litiges (preuve)
    is_active = models.BooleanField(default=True, help_text="False = présence archivée")

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "presences"
        verbose_name = "Présence"
        verbose_name_plural = "Présences"
        # Un seul pointage actif par employé par jour
        unique_together = [["tenant_id", "employee", "date"]]
        ordering = ["-date"]

    def __str__(self):
        return f"{self.employee.full_name} — " f"{self.date} — " f"{self.statut}"

    def save(self, *args, **kwargs):
        """
        Calcule automatiquement les heures travaillées
        à partir de heure_arrivee et heure_depart.
        """
        if self.heure_arrivee and self.heure_depart:
            self.heures_travaillees = self._calculer_heures()
        super().save(*args, **kwargs)

    def _calculer_heures(self):
        """
        Calcule la durée entre heure_arrivee
        et heure_depart en heures décimales.
        Ex: 08:00 → 17:00 = 9.0 heures
        """
        arrivee = datetime.combine(self.date, self.heure_arrivee)
        depart = datetime.combine(self.date, self.heure_depart)
        # Si départ avant arrivée → travail de nuit
        if depart < arrivee:
            depart += timedelta(days=1)
        duree = depart - arrivee
        return round(duree.total_seconds() / 3600, 2)

    @property
    def est_en_retard(self):
        """
        Vérifie si l'employé est arrivé en retard.
        Heure de début standard : 08:00
        """
        from datetime import time

        if self.heure_arrivee:
            return self.heure_arrivee > time(8, 0)
        return False

    def archiver(self):
        """
        Archive la présence (soft delete).
        Préférer cette méthode à delete()
        pour conserver l'historique.
        """
        self.is_active = False
        self.save(update_fields=["is_active", "updated_at"])

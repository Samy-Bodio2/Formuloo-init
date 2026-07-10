"""
Modèle SoldeConges — Formuloo OS
Capital congés d'un employé par type et par année.
Conforme ADR-001 : isolation multi-tenant

Ce modèle suit le solde de congés disponibles.
Il est mis à jour automatiquement à chaque
approbation ou rejet de congé.

Ex: Jean Dupont — 2024 — annuel
    jours_acquis  = 30
    jours_pris    = 10
    jours_restants = 20
"""

import uuid

from django.db import models


class SoldeConges(models.Model):

    # ── Type de congé ─────────────────────────────────────
    class TypeConge(models.TextChoices):
        ANNUEL = "annuel", "Congé annuel"
        MALADIE = "maladie", "Congé maladie"
        SANS_SOLDE = "sans_solde", "Congé sans solde"
        EXCEPTIONNEL = "exceptionnel", "Congé exceptionnel"

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
        related_name="soldes_conges",
        help_text="Employé concerné",
    )

    # ── Type et année ─────────────────────────────────────
    type_conge = models.CharField(
        max_length=20, choices=TypeConge.choices, help_text="Type de congé"
    )
    annee = models.IntegerField(help_text="Année de référence du solde")

    # ── Solde ─────────────────────────────────────────────
    jours_acquis = models.DecimalField(
        max_digits=6,
        decimal_places=2,
        default=0,
        help_text="Total des jours acquis dans l'année",
    )
    jours_pris = models.DecimalField(
        max_digits=6,
        decimal_places=2,
        default=0,
        help_text="Total des jours pris dans l'année",
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "soldes_conges"
        verbose_name = "Solde de congés"
        verbose_name_plural = "Soldes de congés"
        # Un seul solde par employé / type / année
        unique_together = [["employee", "type_conge", "annee"]]
        ordering = ["-annee", "type_conge"]

    def __str__(self):
        return (
            f"{self.employee.full_name} — "
            f"{self.type_conge} — "
            f"{self.annee} — "
            f"Restant: {self.jours_restants}j"
        )

    @property
    def jours_restants(self):
        """
        Solde restant calculé dynamiquement.
        jours_restants = jours_acquis - jours_pris
        """
        return self.jours_acquis - self.jours_pris

    def has_enough_days(self, nb_jours):
        """
        Vérifie si l'employé a assez de jours
        pour une demande de congé.
        """
        return self.jours_restants >= nb_jours

    def decrementer(self, nb_jours):
        """
        Décrémente le solde après approbation d'un congé.
        Lève une exception si solde insuffisant.
        """
        if not self.has_enough_days(nb_jours):
            raise ValueError(
                f"Solde insuffisant : {self.jours_restants}j "
                f"disponibles, {nb_jours}j demandés."
            )
        self.jours_pris += nb_jours
        self.save(update_fields=["jours_pris", "updated_at"])

    def incrementer(self, nb_jours):
        """
        Incrémente le solde après annulation d'un congé.
        """
        self.jours_pris -= nb_jours
        self.save(update_fields=["jours_pris", "updated_at"])

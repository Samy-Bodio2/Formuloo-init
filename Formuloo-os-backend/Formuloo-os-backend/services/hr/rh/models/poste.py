"""
Modèle Poste — Formuloo OS
Fonction ou rôle occupé dans l'entreprise.
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

Un poste appartient à un département et définit
la grille salariale et le niveau hiérarchique.
"""

import uuid

from django.db import models


class Poste(models.Model):

    class Niveau(models.TextChoices):
        JUNIOR = "junior", "Junior"
        MID = "mid", "Mid-level"
        SENIOR = "senior", "Senior"
        MANAGER = "manager", "Manager"
        DIRECTEUR = "directeur", "Directeur"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Département ───────────────────────────────────────
    departement = models.ForeignKey(
        "rh.Departement",
        on_delete=models.CASCADE,
        related_name="postes",
        help_text="Département auquel appartient ce poste",
    )

    # ── Informations du poste ─────────────────────────────
    titre = models.CharField(max_length=100, help_text="Intitulé du poste")
    code = models.CharField(
        max_length=50, help_text="Code unique du poste dans le tenant"
    )
    description = models.TextField(
        null=True, blank=True, help_text="Description des responsabilités du poste"
    )

    # ── Niveau hiérarchique ───────────────────────────────
    # Conforme CDC : junior, mid, senior, manager, directeur
    niveau = models.CharField(
        max_length=20,
        choices=Niveau.choices,
        default=Niveau.JUNIOR,
        help_text="Niveau hiérarchique du poste",
    )

    # ── Grille salariale ──────────────────────────────────
    salaire_min = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        null=True,
        blank=True,
        help_text="Salaire minimum pour ce poste",
    )
    salaire_max = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        null=True,
        blank=True,
        help_text="Salaire maximum pour ce poste",
    )
    devise = models.CharField(
        max_length=10, default="XAF", help_text="Devise de la grille salariale"
    )

    # ── Statut ────────────────────────────────────────────
    is_active = models.BooleanField(default=True, help_text="False = poste archivé")

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "postes"
        verbose_name = "Poste"
        verbose_name_plural = "Postes"
        # Code unique par tenant
        unique_together = [["tenant_id", "code"]]
        ordering = ["titre"]

    def __str__(self):
        return f"{self.titre} — {self.niveau} ({self.departement.nom})"

    @property
    def nb_employes(self):
        """
        Nombre d'employés actifs occupant ce poste.
        Calculé dynamiquement.
        """
        return self.employes.filter(status="active").count()

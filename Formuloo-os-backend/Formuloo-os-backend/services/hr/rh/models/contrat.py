"""
Modèle Contrat — Formuloo OS
Contrat de travail liant un employé à la PME.
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

Un employé peut avoir plusieurs contrats successifs.
Ex: Stage → CDD → CDI
Le numéro de contrat est auto-généré à la création.
"""

import uuid

from django.db import models


class Contrat(models.Model):

    # ── Type de contrat ───────────────────────────────────
    class TypeContrat(models.TextChoices):
        CDI = "CDI", "Contrat à Durée Indéterminée"
        CDD = "CDD", "Contrat à Durée Déterminée"
        INTERIM = "Interim", "Intérim"
        STAGE = "Stage", "Stage"
        FREELANCE = "Freelance", "Freelance"

    # ── Statut du contrat ─────────────────────────────────
    class StatutContrat(models.TextChoices):
        EN_ATTENTE = "en_attente", "En attente"
        ACTIF = "actif", "Actif"
        EXPIRE = "expire", "Expiré"
        RESILIE = "resilie", "Résilié"
        SUSPENDU = "suspendu", "Suspendu"

    # ── Périodicité de paie ───────────────────────────────
    class PeriodicitePaie(models.TextChoices):
        MENSUEL = "mensuel", "Mensuel"
        BIMENSUEL = "bimensuel", "Bimensuel"
        HEBDOMADAIRE = "hebdomadaire", "Hebdomadaire"

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
        related_name="contrats",
        help_text="Employé concerné par ce contrat",
    )

    # ── Numéro auto-généré ────────────────────────────────
    numero = models.CharField(
        max_length=50, help_text="Numéro unique auto-généré — ex: CTR-2024-001"
    )

    # ── Type et dates ─────────────────────────────────────
    type = models.CharField(
        max_length=20,
        choices=TypeContrat.choices,
        help_text="Type de contrat conforme CDC",
    )
    start_date = models.DateField(help_text="Date de début du contrat")
    # Null pour les CDI
    end_date = models.DateField(
        null=True, blank=True, help_text="Date de fin — null pour les CDI"
    )

    # ── Période d'essai ───────────────────────────────────
    trial_period = models.IntegerField(
        null=True, blank=True, help_text="Durée de la période d'essai en jours"
    )

    # ── Rémunération ──────────────────────────────────────
    gross_salary = models.DecimalField(
        max_digits=15, decimal_places=2, help_text="Salaire brut mensuel contractuel"
    )
    currency = models.CharField(
        max_length=10, default="XAF", help_text="Devise (XAF, EUR, USD...)"
    )

    # ── Conditions de travail ─────────────────────────────
    work_hours_per_week = models.DecimalField(
        max_digits=5,
        decimal_places=2,
        default=40,
        help_text="Heures de travail par semaine",
    )
    periodicite_paie = models.CharField(
        max_length=20,
        choices=PeriodicitePaie.choices,
        default=PeriodicitePaie.MENSUEL,
        help_text="Fréquence de paiement du salaire",
    )
    jours_conge_annuel = models.IntegerField(
        default=30, help_text="Nombre de jours de congé annuel"
    )
    lieu_travail = models.CharField(
        max_length=200, null=True, blank=True, help_text="Lieu de travail principal"
    )
    poste = models.CharField(
        max_length=100,
        null=True,
        blank=True,
        help_text="Intitulé du poste dans le contrat",
    )

    # ── Statut ────────────────────────────────────────────
    statut = models.CharField(
        max_length=20, choices=StatutContrat.choices, default=StatutContrat.EN_ATTENTE
    )
    is_active = models.BooleanField(default=True, help_text="Contrat actif ou archivé")

    # ── Résiliation ───────────────────────────────────────
    motif_resiliation = models.TextField(
        null=True, blank=True, help_text="Motif de résiliation du contrat"
    )
    date_resiliation = models.DateField(
        null=True, blank=True, help_text="Date de résiliation"
    )

    # ── Documents ─────────────────────────────────────────
    document_url = models.URLField(
        null=True, blank=True, help_text="URL du contrat signé numérisé"
    )
    # Date et heure de signature — conforme CDC
    signed_at = models.DateTimeField(
        null=True, blank=True, help_text="Date et heure de signature du contrat"
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "contrats"
        verbose_name = "Contrat"
        verbose_name_plural = "Contrats"
        unique_together = [["tenant_id", "numero"]]
        ordering = ["-start_date"]

    def __str__(self):
        return f"{self.numero} — {self.employee.full_name} ({self.type})"

    def save(self, *args, **kwargs):
        """
        Auto-génère le numéro de contrat à la création.
        Format : CTR-YYYY-NNN
        Ex: CTR-2024-001
        """
        if not self.numero:
            from django.utils import timezone

            year = timezone.now().year
            count = Contrat.objects.filter(tenant_id=self.tenant_id).count() + 1
            self.numero = f"CTR-{year}-{count:03d}"
        super().save(*args, **kwargs)

    @property
    def is_cdi(self):
        """Vérifie si le contrat est un CDI."""
        return self.type == self.TypeContrat.CDI

    @property
    def is_expired(self):
        """Vérifie si le contrat est expiré."""
        from django.utils import timezone

        if self.end_date:
            return self.end_date < timezone.now().date()
        return False

"""
Modèle FichePaie — Formuloo OS
Fiche de paie mensuelle conforme SYSCOHADA.
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

Intègre les spécificités fiscales africaines :
- CNPS : Caisse Nationale de Prévoyance Sociale
- IRPP : Impôt sur le Revenu des Personnes Physiques

Workflow :
brouillon → valide (approuvé par RH/Comptable)
valide    → paye   (paiement effectué)

Le salaire net est calculé automatiquement :
net = gross + total_bonuses - total_deductions
"""

import uuid

from django.db import models


class FichePaie(models.Model):

    # ── Statut de la fiche ────────────────────────────────
    class Statut(models.TextChoices):
        BROUILLON = "brouillon", "Brouillon"
        VALIDE = "valide", "Validé"
        PAYE = "paye", "Payé"

    # ── Mode de paiement ──────────────────────────────────
    class ModePaiement(models.TextChoices):
        VIREMENT = "virement", "Virement bancaire"
        ESPECES = "especes", "Espèces"
        MOBILE_MONEY = "mobile_money", "Mobile Money"
        CHEQUE = "cheque", "Chèque"

    # ── Identifiant ───────────────────────────────────────
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Employé et contrat ────────────────────────────────
    employee = models.ForeignKey(
        "rh.Employe",
        on_delete=models.CASCADE,
        related_name="fiches_paie",
        help_text="Employé concerné",
    )
    contrat = models.ForeignKey(
        "rh.Contrat",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="fiches_paie",
        help_text="Contrat de base pour le calcul",
    )

    # ── Période ───────────────────────────────────────────
    mois = models.IntegerField(help_text="Mois de la fiche (1-12)")
    annee = models.IntegerField(help_text="Année de la fiche")
    period = models.CharField(
        max_length=7, help_text="Période auto-générée ex: 2024-06"
    )

    # ── Salaire de base ───────────────────────────────────
    salaire_base = models.DecimalField(
        max_digits=15, decimal_places=2, help_text="Salaire de base mensuel"
    )
    heures_travaillees = models.DecimalField(
        max_digits=7,
        decimal_places=2,
        default=0,
        help_text="Heures travaillées dans le mois",
    )
    heures_supplementaires = models.DecimalField(
        max_digits=7,
        decimal_places=2,
        default=0,
        help_text="Heures supplémentaires effectuées",
    )
    taux_horaire_supp = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        default=0,
        help_text="Taux majoré des heures supplémentaires",
    )

    # ── Primes et avantages (JSON — conforme CDC) ─────────
    bonuses = models.JSONField(
        default=dict,
        help_text="""
                          Détail des primes en JSON.
                          Ex: {
                            'prime_transport': 30000,
                            'prime_logement': 50000,
                            'prime_rendement': 25000,
                            'autres': 0
                          }
                          """,
    )

    # ── Déductions SYSCOHADA (JSON — conforme CDC) ────────
    deductions = models.JSONField(
        default=dict,
        help_text="""
                          Détail des déductions en JSON.
                          Ex: {
                            'cotisation_cnps': 44000,
                            'impot_irpp': 35000,
                            'credit_logement': 0,
                            'autres': 0
                          }
                          """,
    )

    # ── Totaux calculés automatiquement ──────────────────
    gross = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        default=0,
        help_text="Salaire brut = base + primes + heures supp",
    )
    net_salary = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        default=0,
        help_text="Salaire net = gross - total_deductions",
    )
    currency = models.CharField(max_length=10, default="XAF")

    # ── Statut et workflow ────────────────────────────────
    statut = models.CharField(
        max_length=20, choices=Statut.choices, default=Statut.BROUILLON
    )
    date_validation = models.DateTimeField(
        null=True, blank=True, help_text="Date de validation par RH/Comptable"
    )
    paid_at = models.DateTimeField(
        null=True, blank=True, help_text="Date de paiement effectif"
    )
    mode_paiement = models.CharField(
        max_length=20, choices=ModePaiement.choices, null=True, blank=True
    )

    # ── Documents ─────────────────────────────────────────
    pdf_url = models.URLField(
        null=True, blank=True, help_text="URL du bulletin de paie PDF généré"
    )

    # ── Lien Comptabilité ─────────────────────────────────
    # UUID de l'écriture SYSCOHADA dans le service Compta
    journal_entry_id = models.UUIDField(
        null=True,
        blank=True,
        help_text="UUID de l'écriture SYSCOHADA dans le service Comptabilité",
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "fiches_paie"
        verbose_name = "Fiche de paie"
        verbose_name_plural = "Fiches de paie"
        # Une seule fiche par employé par mois
        unique_together = [["tenant_id", "employee", "mois", "annee"]]
        ordering = ["-annee", "-mois"]

    def __str__(self):
        return (
            f"{self.employee.full_name} — "
            f"{self.period} — "
            f"Net: {self.net_salary} {self.currency}"
        )

    def save(self, *args, **kwargs):
        """
        Calcule automatiquement :
        - La période (YYYY-MM)
        - Le salaire brut
        - Le salaire net
        """
        # Auto-générer la période
        self.period = f"{self.annee}-{self.mois:02d}"

        # Calculer le total des primes
        total_bonuses = sum(self.bonuses.values()) if self.bonuses else 0

        # Calculer les heures supplémentaires
        montant_heures_supp = self.heures_supplementaires * self.taux_horaire_supp

        # Calculer le salaire brut
        self.gross = self.salaire_base + total_bonuses + montant_heures_supp

        # Calculer le total des déductions
        total_deductions = sum(self.deductions.values()) if self.deductions else 0

        # Calculer le salaire net
        self.net_salary = self.gross - total_deductions

        super().save(*args, **kwargs)

    def valider(self):
        """
        Valide la fiche de paie.
        Passe de brouillon à valide.
        """
        from django.utils import timezone

        if self.statut != self.Statut.BROUILLON:
            raise ValueError("Seule une fiche en brouillon peut être validée.")
        self.statut = self.Statut.VALIDE
        self.date_validation = timezone.now()
        self.save(update_fields=["statut", "date_validation", "updated_at"])

    def payer(self, mode_paiement):
        """
        Marque la fiche comme payée.
        Passe de valide à paye.
        """
        from django.utils import timezone

        if self.statut != self.Statut.VALIDE:
            raise ValueError("Seule une fiche validée peut être marquée comme payée.")
        self.statut = self.Statut.PAYE
        self.paid_at = timezone.now()
        self.mode_paiement = mode_paiement
        self.save(update_fields=["statut", "paid_at", "mode_paiement", "updated_at"])

    @property
    def total_bonuses(self):
        """Total des primes du mois."""
        return sum(self.bonuses.values()) if self.bonuses else 0

    @property
    def total_deductions(self):
        """Total des déductions du mois."""
        return sum(self.deductions.values()) if self.deductions else 0

    @property
    def cotisation_cnps(self):
        """Cotisation CNPS extraite des déductions."""
        return self.deductions.get("cotisation_cnps", 0)

    @property
    def impot_irpp(self):
        """Impôt IRPP extrait des déductions."""
        return self.deductions.get("impot_irpp", 0)

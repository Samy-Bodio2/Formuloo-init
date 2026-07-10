from decimal import Decimal
from django.db import models
from django.db.models import Sum
from django.core.exceptions import ValidationError


class Ecriture(models.Model):

    class Statut(models.TextChoices):
        BROUILLON = "BROUILLON", "Brouillon"
        VALIDEE = "VALIDEE", "Validée"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    journal = models.ForeignKey(
        "comptabilite.Journal",
        on_delete=models.PROTECT,
        related_name="ecritures",
    )
    exercice = models.ForeignKey(
        "comptabilite.Exercice",
        on_delete=models.PROTECT,
        related_name="ecritures",
    )
    date_ecriture = models.DateField()
    libelle = models.CharField(max_length=255)
    statut = models.CharField(
        max_length=10, choices=Statut.choices, default=Statut.BROUILLON
    )
    created_by = models.UUIDField(null=True, blank=True, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "ecritures"
        ordering = ["-date_ecriture", "-id"]

    def __str__(self):
        return f"Écriture {self.id} — {self.libelle}"

    def _aggregate(self, field):
        """Utilise une annotation si dispo (évite N+1), sinon agrège en Python."""
        annotated = getattr(self, f"_total_{field}", None)
        if annotated is not None:
            return annotated or Decimal("0.00")
        agg = self.lignes.aggregate(total=Sum(field))
        return agg["total"] or Decimal("0.00")

    @property
    def total_debit(self):
        return self._aggregate("debit")

    @property
    def total_credit(self):
        return self._aggregate("credit")

    @property
    def est_equilibree(self):
        return self.total_debit == self.total_credit


class LigneEcriture(models.Model):

    id = models.AutoField(primary_key=True)
    ecriture = models.ForeignKey(
        Ecriture, on_delete=models.CASCADE, related_name="lignes"
    )
    compte = models.ForeignKey(
        "comptabilite.Compte",
        on_delete=models.PROTECT,
        related_name="lignes_ecriture",
    )
    libelle = models.CharField(max_length=255, blank=True)
    debit = models.DecimalField(max_digits=15, decimal_places=2, default=0)
    credit = models.DecimalField(max_digits=15, decimal_places=2, default=0)
    lettre = models.CharField(
        max_length=10, blank=True, db_index=True,
        help_text="Code de lettrage (rapprochement facture/paiement)",
    )

    class Meta:
        db_table = "lignes_ecriture"

    def clean(self):
        if self.debit < 0 or self.credit < 0:
            raise ValidationError("Débit et crédit doivent être positifs.")
        if self.debit > 0 and self.credit > 0:
            raise ValidationError("Une ligne ne peut avoir à la fois débit et crédit.")

    def __str__(self):
        return f"Ligne {self.id} — {self.compte.numero}"

from decimal import Decimal
from django.db import models


class Facture(models.Model):

    class TypeDocument(models.TextChoices):
        FACTURE = "FACTURE", "Facture"
        AVOIR = "AVOIR", "Avoir / Note de crédit"

    class Statut(models.TextChoices):
        BROUILLON = "BROUILLON", "Brouillon"
        EMISE = "EMISE", "Émise"
        PARTIELLEMENT_PAYEE = "PARTIELLEMENT_PAYEE", "Partiellement payée"
        PAYEE = "PAYEE", "Payée"
        ANNULEE = "ANNULEE", "Annulée"

    class Devise(models.TextChoices):
        XAF = "XAF", "Franc CFA (BEAC)"
        EUR = "EUR", "Euro"
        USD = "USD", "Dollar américain"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    type_document = models.CharField(
        max_length=10, choices=TypeDocument.choices, default=TypeDocument.FACTURE
    )
    facture_origine = models.ForeignKey(
        "self", on_delete=models.SET_NULL,
        null=True, blank=True, related_name="avoirs",
        help_text="Facture d'origine pour un avoir",
    )
    numero = models.CharField(max_length=30, blank=True)
    client_nom = models.CharField(max_length=200)
    client_email = models.EmailField(blank=True)
    devise = models.CharField(
        max_length=3, choices=Devise.choices, default=Devise.XAF
    )
    statut = models.CharField(
        max_length=20, choices=Statut.choices, default=Statut.BROUILLON
    )
    date_emission = models.DateField(null=True, blank=True)
    date_echeance = models.DateField()
    tva_taux = models.DecimalField(max_digits=5, decimal_places=2, default=0)
    ecriture = models.OneToOneField(
        "comptabilite.Ecriture",
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name="facture",
    )
    created_by = models.UUIDField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "factures"
        ordering = ["-created_at"]

    def __str__(self):
        prefix = "AVO" if self.type_document == self.TypeDocument.AVOIR else "FAC"
        return f"{self.numero or f'{prefix}-{self.id}'} — {self.client_nom}"

    @property
    def montant_ht(self):
        return sum(l.montant_total for l in self.lignes.all()) or Decimal("0")

    @property
    def tva(self):
        return self.montant_ht * self.tva_taux / 100

    @property
    def montant_ttc(self):
        return self.montant_ht + self.tva

    @property
    def montant_paye(self):
        return sum(p.montant for p in self.paiements.all()) or Decimal("0")

    @property
    def solde_restant_du(self):
        return max(self.montant_ttc - self.montant_paye, Decimal("0"))

    def generer_numero(self):
        from datetime import date
        from django.db import transaction
        annee = date.today().year
        prefix = "AVO" if self.type_document == self.TypeDocument.AVOIR else "FAC"
        with transaction.atomic():
            count = Facture.objects.select_for_update().filter(
                tenant_id=self.tenant_id,
                type_document=self.type_document,
                created_at__year=annee,
            ).count()
            self.numero = f"{prefix}-{annee}-{str(count + 1).zfill(4)}"

    def mettre_a_jour_statut_paiement(self):
        """Recalcule et sauvegarde le statut selon le montant payé."""
        if self.statut in (self.Statut.ANNULEE, self.Statut.BROUILLON):
            return
        restant = self.solde_restant_du
        if restant <= Decimal("0"):
            self.statut = self.Statut.PAYEE
        elif self.montant_paye > Decimal("0"):
            self.statut = self.Statut.PARTIELLEMENT_PAYEE
        else:
            self.statut = self.Statut.EMISE
        self.save(update_fields=["statut"])


class LigneFacture(models.Model):

    id = models.AutoField(primary_key=True)
    facture = models.ForeignKey(
        Facture, on_delete=models.CASCADE, related_name="lignes"
    )
    description = models.CharField(max_length=255)
    quantite = models.DecimalField(max_digits=10, decimal_places=2, default=1)
    prix_unitaire = models.DecimalField(max_digits=15, decimal_places=2)

    class Meta:
        db_table = "lignes_facture"

    @property
    def montant_total(self):
        return self.quantite * self.prix_unitaire

    def __str__(self):
        return f"{self.description} × {self.quantite}"

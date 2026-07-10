from decimal import Decimal
from django.db import models


class FactureFournisseur(models.Model):
    """
    Facture d'achat reçue d'un fournisseur.
    Cycle : BROUILLON → RECUE → VALIDEE → PAYEE | PARTIELLEMENT_PAYEE | ANNULEE

    Écriture OHADA générée à la validation :
        6xx Achats/Charges (débit) = montant HT
        4452 TVA déductible (débit) = TVA
        401 Fournisseurs (crédit) = montant TTC

    Écriture OHADA générée au paiement :
        401 Fournisseurs (débit) = montant payé
        521 Banque / 571 Caisse (crédit) = montant payé
    """

    class TypeDocument(models.TextChoices):
        FACTURE = "FACTURE", "Facture fournisseur"
        AVOIR = "AVOIR", "Avoir fournisseur"

    class Statut(models.TextChoices):
        BROUILLON = "BROUILLON", "Brouillon"
        RECUE = "RECUE", "Reçue"
        VALIDEE = "VALIDEE", "Validée (en attente de paiement)"
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
        help_text="Facture d'origine pour un avoir fournisseur",
    )
    numero_interne = models.CharField(max_length=30, blank=True)
    numero_fournisseur = models.CharField(
        max_length=100, blank=True,
        help_text="Numéro sur la facture du fournisseur",
    )
    fournisseur_nom = models.CharField(max_length=200)
    fournisseur_email = models.EmailField(blank=True)
    devise = models.CharField(max_length=3, choices=Devise.choices, default=Devise.XAF)
    statut = models.CharField(
        max_length=20, choices=Statut.choices, default=Statut.BROUILLON
    )
    date_reception = models.DateField(null=True, blank=True)
    date_facture = models.DateField(help_text="Date sur la facture fournisseur")
    date_echeance = models.DateField()
    tva_taux = models.DecimalField(max_digits=5, decimal_places=2, default=0)
    ecriture = models.OneToOneField(
        "comptabilite.Ecriture",
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name="facture_fournisseur",
    )
    created_by = models.UUIDField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "factures_fournisseurs"
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.numero_interne or f'ACH-{self.id}'} — {self.fournisseur_nom}"

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
        return sum(p.montant for p in self.paiements_fournisseur.all()) or Decimal("0")

    @property
    def solde_restant_du(self):
        return max(self.montant_ttc - self.montant_paye, Decimal("0"))

    def generer_numero_interne(self):
        from datetime import date
        from django.db import transaction
        annee = date.today().year
        prefix = "AVO-F" if self.type_document == self.TypeDocument.AVOIR else "ACH"
        with transaction.atomic():
            count = FactureFournisseur.objects.select_for_update().filter(
                tenant_id=self.tenant_id,
                type_document=self.type_document,
                created_at__year=annee,
            ).count()
            self.numero_interne = f"{prefix}-{annee}-{str(count + 1).zfill(4)}"

    def mettre_a_jour_statut_paiement(self):
        if self.statut in (self.Statut.ANNULEE, self.Statut.BROUILLON, self.Statut.RECUE):
            return
        restant = self.solde_restant_du
        if restant <= Decimal("0"):
            self.statut = self.Statut.PAYEE
        elif self.montant_paye > Decimal("0"):
            self.statut = self.Statut.PARTIELLEMENT_PAYEE
        else:
            self.statut = self.Statut.VALIDEE
        self.save(update_fields=["statut"])


class LigneFactureFournisseur(models.Model):

    id = models.AutoField(primary_key=True)
    facture = models.ForeignKey(
        FactureFournisseur, on_delete=models.CASCADE, related_name="lignes"
    )
    description = models.CharField(max_length=255)
    compte_charge = models.ForeignKey(
        "comptabilite.Compte",
        on_delete=models.PROTECT,
        null=True, blank=True,
        related_name="lignes_achat",
        help_text="Compte de charge OHADA (classe 6) à imputer",
    )
    quantite = models.DecimalField(max_digits=10, decimal_places=2, default=1)
    prix_unitaire = models.DecimalField(max_digits=15, decimal_places=2)

    class Meta:
        db_table = "lignes_factures_fournisseurs"

    @property
    def montant_total(self):
        return self.quantite * self.prix_unitaire

    def __str__(self):
        return f"{self.description} × {self.quantite}"

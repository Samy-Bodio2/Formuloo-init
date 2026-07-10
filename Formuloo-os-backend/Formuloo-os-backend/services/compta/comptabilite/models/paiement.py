from django.db import models


class Paiement(models.Model):

    class ModePaiement(models.TextChoices):
        VIREMENT = "VIREMENT", "Virement bancaire"
        CHEQUE = "CHEQUE", "Chèque"
        ESPECES = "ESPECES", "Espèces"
        MOBILE_MONEY = "MOBILE_MONEY", "Mobile Money"

    class Devise(models.TextChoices):
        XAF = "XAF", "Franc CFA (BEAC)"
        EUR = "EUR", "Euro"
        USD = "USD", "Dollar américain"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    facture = models.ForeignKey(
        "comptabilite.Facture",
        on_delete=models.PROTECT,
        related_name="paiements",
    )
    montant = models.DecimalField(max_digits=15, decimal_places=2)
    devise = models.CharField(
        max_length=3, choices=Devise.choices, default=Devise.XAF
    )
    mode_paiement = models.CharField(max_length=15, choices=ModePaiement.choices)
    date_paiement = models.DateField()
    reference = models.CharField(max_length=100, blank=True)
    ecriture = models.OneToOneField(
        "comptabilite.Ecriture",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="paiement",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "paiements"
        ordering = ["-date_paiement"]

    def __str__(self):
        return f"Paiement {self.id} — {self.montant} {self.devise}"

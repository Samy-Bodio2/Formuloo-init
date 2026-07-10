from django.db import models


class Compte(models.Model):

    class TypeCompte(models.TextChoices):
        ACTIF = "ACTIF", "Actif"
        PASSIF = "PASSIF", "Passif"
        CHARGE = "CHARGE", "Charge"
        PRODUIT = "PRODUIT", "Produit"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    numero = models.CharField(max_length=20)
    libelle = models.CharField(max_length=200)
    # Classe OHADA : 1-Ressources durables, 2-Immobilisations, 3-Stocks,
    # 4-Tiers, 5-Trésorerie, 6-Charges, 7-Produits, 8-HAO
    classe = models.IntegerField()
    type_compte = models.CharField(max_length=10, choices=TypeCompte.choices)
    parent = models.ForeignKey(
        "self", on_delete=models.SET_NULL,
        null=True, blank=True, related_name="sous_comptes",
        help_text="Compte parent pour la hiérarchie SYSCOHADA",
    )
    is_systeme = models.BooleanField(default=False)
    is_actif = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "comptes"
        unique_together = [["tenant_id", "numero"]]
        ordering = ["numero"]

    def __str__(self):
        return f"{self.numero} — {self.libelle}"

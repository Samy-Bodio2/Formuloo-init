from django.db import models


class Exercice(models.Model):

    class Statut(models.TextChoices):
        OUVERT = "OUVERT", "Ouvert"
        CLOTURE = "CLOTURE", "Clôturé"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    annee = models.IntegerField()
    date_debut = models.DateField()
    date_fin = models.DateField()
    statut = models.CharField(
        max_length=10, choices=Statut.choices, default=Statut.OUVERT
    )
    date_cloture = models.DateField(null=True, blank=True)
    resultat_net = models.DecimalField(
        max_digits=15, decimal_places=2, null=True, blank=True,
        help_text="Résultat calculé à la clôture (produits - charges)",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "exercices"
        unique_together = [["tenant_id", "annee"]]
        ordering = ["-annee"]

    def __str__(self):
        return f"Exercice {self.annee} ({self.statut})"

    @property
    def is_ouvert(self):
        return self.statut == self.Statut.OUVERT

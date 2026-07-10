from django.db import models


class Journal(models.Model):

    class TypeJournal(models.TextChoices):
        VENTES = "VENTES", "Journal des ventes"
        ACHATS = "ACHATS", "Journal des achats"
        BANQUE = "BANQUE", "Journal de banque"
        CAISSE = "CAISSE", "Journal de caisse"
        OD = "OD", "Opérations diverses"

    id = models.AutoField(primary_key=True)
    tenant_id = models.UUIDField(db_index=True)
    code = models.CharField(max_length=10)
    libelle = models.CharField(max_length=100)
    type = models.CharField(max_length=10, choices=TypeJournal.choices)
    compte_contrepartie = models.ForeignKey(
        "comptabilite.Compte",
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name="journaux_contrepartie",
        help_text="Compte de contrepartie par défaut (ex: 521 pour BNQ)",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "journaux"
        unique_together = [["tenant_id", "code"]]
        ordering = ["code"]

    def __str__(self):
        return f"{self.code} — {self.libelle}"

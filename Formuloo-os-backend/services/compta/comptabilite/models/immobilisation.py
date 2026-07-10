"""
Module Immobilisations — Formuloo OS
Conforme SYSCOHADA révisé, Classe 2 — Comptes d'actif immobilisé.

Types d'amortissement supportés :
  - Linéaire (constant) : annuité = (valeur_origine - valeur_résiduelle) / durée_vie
  - Dégressif : taux linéaire × coefficient (1.5 ou 2 selon durée Cameroun)

Le plan d'amortissement est calculé et stocké en JSON pour l'affichage,
mais chaque dotation annuelle génère une écriture comptable réelle :
  6811 Dotations aux amortissements (débit)
  28xx Amortissements (crédit)
"""

from decimal import Decimal, ROUND_HALF_UP
from django.db import models
from django.utils import timezone


class Immobilisation(models.Model):
    """
    Bien immobilisé de l'entreprise.
    Classe SYSCOHADA 2 : immobilisations incorporelles (20x), corporelles (21x–28x),
    financières (26x–27x).
    """

    class Categorie(models.TextChoices):
        INCORPORELLE = "INCORPORELLE", "Incorporelle (brevets, logiciels)"
        CORPORELLE_TERRAIN = "TERRAIN", "Terrain"
        CORPORELLE_CONSTRUCTION = "CONSTRUCTION", "Construction"
        CORPORELLE_MATERIEL = "MATERIEL", "Matériel et équipement"
        CORPORELLE_MOBILIER = "MOBILIER", "Mobilier et aménagement"
        CORPORELLE_VEHICULE = "VEHICULE", "Véhicule"
        FINANCIERE = "FINANCIERE", "Financière (titres, prêts)"

    class MethodeAmortissement(models.TextChoices):
        LINEAIRE = "LINEAIRE", "Linéaire"
        DEGRESSIF = "DEGRESSIF", "Dégressif"
        NON_AMORTISSABLE = "NON_AMORTISSABLE", "Non amortissable (terrain)"

    class Statut(models.TextChoices):
        ACTIVE = "ACTIVE", "Active"
        AMORTIE = "AMORTIE", "Totalement amortie"
        CEDEE = "CEDEE", "Cédée / Sortie"

    tenant_id = models.CharField(max_length=100, db_index=True)

    # ── Identification ──────────────────────────────────────
    code = models.CharField(max_length=50, help_text="Code interne de l'immobilisation")
    designation = models.CharField(max_length=255)
    categorie = models.CharField(max_length=30, choices=Categorie.choices)
    numero_compte = models.CharField(
        max_length=10,
        help_text="Compte OHADA (ex: 2183 pour Matériel de bureau)",
    )
    fournisseur = models.CharField(max_length=255, blank=True)
    reference_facture = models.CharField(max_length=100, blank=True)

    # ── Valeurs ─────────────────────────────────────────────
    valeur_origine = models.DecimalField(
        max_digits=15, decimal_places=2,
        help_text="Coût d'acquisition (HT)",
    )
    valeur_residuelle = models.DecimalField(
        max_digits=15, decimal_places=2, default=0,
        help_text="Valeur résiduelle estimée en fin de vie",
    )
    devise = models.CharField(max_length=3, default="XAF")

    # ── Amortissement ────────────────────────────────────────
    methode = models.CharField(
        max_length=20,
        choices=MethodeAmortissement.choices,
        default=MethodeAmortissement.LINEAIRE,
    )
    duree_vie = models.IntegerField(
        help_text="Durée d'amortissement en années",
        default=5,
    )
    date_mise_en_service = models.DateField()

    # Cumul amortissements déjà passés
    cumul_amortissements = models.DecimalField(
        max_digits=15, decimal_places=2, default=0
    )

    # ── Statut ───────────────────────────────────────────────
    statut = models.CharField(
        max_length=20, choices=Statut.choices, default=Statut.ACTIVE
    )
    date_cession = models.DateField(null=True, blank=True)
    valeur_nette_cession = models.DecimalField(
        max_digits=15, decimal_places=2, null=True, blank=True
    )

    # ── Liaison comptable ────────────────────────────────────
    exercice = models.ForeignKey(
        "comptabilite.Exercice",
        on_delete=models.PROTECT,
        related_name="immobilisations",
        null=True, blank=True,
        help_text="Exercice d'acquisition",
    )

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-date_mise_en_service"]
        unique_together = [("tenant_id", "code")]

    def __str__(self):
        return f"{self.code} — {self.designation}"

    @property
    def valeur_nette_comptable(self) -> Decimal:
        """VNC = Valeur d'origine - Cumul amortissements."""
        return self.valeur_origine - self.cumul_amortissements

    @property
    def taux_lineaire(self) -> Decimal:
        """Taux d'amortissement linéaire annuel."""
        if self.duree_vie <= 0:
            return Decimal("0")
        return (Decimal("1") / Decimal(str(self.duree_vie))).quantize(
            Decimal("0.0001"), rounding=ROUND_HALF_UP
        )

    def calculer_annuite(self, annee: int) -> Decimal:
        """
        Calcule la dotation d'amortissement pour une année donnée.
        Linéaire : annuité constante = (VO - VR) / n
        Dégressif : taux = taux_linéaire × coeff, base = VNC début période
        """
        if self.methode == self.MethodeAmortissement.NON_AMORTISSABLE:
            return Decimal("0")

        base_amortissable = self.valeur_origine - self.valeur_residuelle
        if base_amortissable <= 0:
            return Decimal("0")

        if self.methode == self.MethodeAmortissement.LINEAIRE:
            annuite = (base_amortissable / Decimal(str(self.duree_vie))).quantize(
                Decimal("1"), rounding=ROUND_HALF_UP
            )
            # Ne pas dépasser la valeur amortissable restante
            restant = base_amortissable - self.cumul_amortissements
            return min(annuite, max(restant, Decimal("0")))

        # Dégressif — coefficient selon durée (Cameroun/France)
        if self.duree_vie <= 4:
            coeff = Decimal("1.5")
        elif self.duree_vie <= 6:
            coeff = Decimal("2.0")
        else:
            coeff = Decimal("2.5")

        taux_degressif = self.taux_lineaire * coeff
        vnc = self.valeur_nette_comptable
        if vnc <= self.valeur_residuelle:
            return Decimal("0")
        annuite = (vnc * taux_degressif).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
        restant = vnc - self.valeur_residuelle
        return min(annuite, restant)


class DotationAmortissement(models.Model):
    """
    Dotation annuelle d'amortissement pour une immobilisation.
    Chaque dotation déclenche une écriture comptable OHADA.
    """

    tenant_id = models.CharField(max_length=100, db_index=True)
    immobilisation = models.ForeignKey(
        Immobilisation, on_delete=models.CASCADE, related_name="dotations"
    )
    exercice = models.ForeignKey(
        "comptabilite.Exercice",
        on_delete=models.PROTECT,
        related_name="dotations_amortissement",
    )
    annee = models.IntegerField()
    montant = models.DecimalField(max_digits=15, decimal_places=2)
    ecriture = models.ForeignKey(
        "comptabilite.Ecriture",
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name="dotations",
    )
    date_comptabilisation = models.DateTimeField(default=timezone.now)

    class Meta:
        unique_together = [("immobilisation", "annee")]
        ordering = ["annee"]

    def __str__(self):
        return f"Dotation {self.annee} — {self.immobilisation.code} : {self.montant}"

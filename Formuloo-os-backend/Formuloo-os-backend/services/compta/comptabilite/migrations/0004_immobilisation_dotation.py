"""Migration Immobilisations + DotationAmortissement — Formuloo OS"""

from decimal import Decimal
import django.db.models.deletion
import django.utils.timezone
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("comptabilite", "0003_compte_is_actif_compte_parent_compte_updated_at_and_more"),
    ]

    operations = [
        migrations.CreateModel(
            name="Immobilisation",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("tenant_id", models.CharField(db_index=True, max_length=100)),
                ("code", models.CharField(max_length=50, help_text="Code interne de l'immobilisation")),
                ("designation", models.CharField(max_length=255)),
                ("categorie", models.CharField(
                    choices=[
                        ("INCORPORELLE", "Incorporelle (brevets, logiciels)"),
                        ("TERRAIN", "Terrain"),
                        ("CONSTRUCTION", "Construction"),
                        ("MATERIEL", "Matériel et équipement"),
                        ("MOBILIER", "Mobilier et aménagement"),
                        ("VEHICULE", "Véhicule"),
                        ("FINANCIERE", "Financière (titres, prêts)"),
                    ],
                    max_length=30,
                )),
                ("numero_compte", models.CharField(max_length=10, help_text="Compte OHADA (ex: 2183 pour Matériel de bureau)")),
                ("fournisseur", models.CharField(blank=True, max_length=255)),
                ("reference_facture", models.CharField(blank=True, max_length=100)),
                ("valeur_origine", models.DecimalField(decimal_places=2, max_digits=15, help_text="Coût d'acquisition (HT)")),
                ("valeur_residuelle", models.DecimalField(decimal_places=2, default=0, max_digits=15, help_text="Valeur résiduelle estimée en fin de vie")),
                ("devise", models.CharField(default="XAF", max_length=3)),
                ("methode", models.CharField(
                    choices=[
                        ("LINEAIRE", "Linéaire"),
                        ("DEGRESSIF", "Dégressif"),
                        ("NON_AMORTISSABLE", "Non amortissable (terrain)"),
                    ],
                    default="LINEAIRE",
                    max_length=20,
                )),
                ("duree_vie", models.IntegerField(default=5, help_text="Durée d'amortissement en années")),
                ("date_mise_en_service", models.DateField()),
                ("cumul_amortissements", models.DecimalField(decimal_places=2, default=0, max_digits=15)),
                ("statut", models.CharField(
                    choices=[
                        ("ACTIVE", "Active"),
                        ("AMORTIE", "Totalement amortie"),
                        ("CEDEE", "Cédée / Sortie"),
                    ],
                    default="ACTIVE",
                    max_length=20,
                )),
                ("date_cession", models.DateField(blank=True, null=True)),
                ("valeur_nette_cession", models.DecimalField(blank=True, decimal_places=2, max_digits=15, null=True)),
                ("exercice", models.ForeignKey(
                    blank=True,
                    null=True,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="immobilisations",
                    to="comptabilite.exercice",
                    help_text="Exercice d'acquisition",
                )),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
            ],
            options={
                "ordering": ["-date_mise_en_service"],
            },
        ),
        migrations.AddConstraint(
            model_name="immobilisation",
            constraint=models.UniqueConstraint(
                fields=["tenant_id", "code"], name="unique_immo_code_tenant"
            ),
        ),
        migrations.CreateModel(
            name="DotationAmortissement",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("tenant_id", models.CharField(db_index=True, max_length=100)),
                ("annee", models.IntegerField()),
                ("montant", models.DecimalField(decimal_places=2, max_digits=15)),
                ("date_comptabilisation", models.DateTimeField(default=django.utils.timezone.now)),
                ("ecriture", models.ForeignKey(
                    blank=True,
                    null=True,
                    on_delete=django.db.models.deletion.SET_NULL,
                    related_name="dotations",
                    to="comptabilite.ecriture",
                )),
                ("exercice", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="dotations_amortissement",
                    to="comptabilite.exercice",
                )),
                ("immobilisation", models.ForeignKey(
                    on_delete=django.db.models.deletion.CASCADE,
                    related_name="dotations",
                    to="comptabilite.immobilisation",
                )),
            ],
            options={
                "ordering": ["annee"],
            },
        ),
        migrations.AddConstraint(
            model_name="dotationamortissement",
            constraint=models.UniqueConstraint(
                fields=["immobilisation", "annee"], name="unique_dotation_immo_annee"
            ),
        ),
    ]

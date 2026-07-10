from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    initial = True

    dependencies = []

    operations = [
        migrations.CreateModel(
            name="Exercice",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                ("annee", models.IntegerField()),
                ("date_debut", models.DateField()),
                ("date_fin", models.DateField()),
                (
                    "statut",
                    models.CharField(
                        choices=[("OUVERT", "Ouvert"), ("CLOTURE", "Clôturé")],
                        default="OUVERT",
                        max_length=10,
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "exercices",
                "ordering": ["-annee"],
            },
        ),
        migrations.CreateModel(
            name="Compte",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                ("numero", models.CharField(max_length=20)),
                ("libelle", models.CharField(max_length=200)),
                ("classe", models.IntegerField()),
                (
                    "type_compte",
                    models.CharField(
                        choices=[
                            ("ACTIF", "Actif"),
                            ("PASSIF", "Passif"),
                            ("CHARGE", "Charge"),
                            ("PRODUIT", "Produit"),
                        ],
                        max_length=10,
                    ),
                ),
                ("is_systeme", models.BooleanField(default=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "comptes",
                "ordering": ["numero"],
            },
        ),
        migrations.CreateModel(
            name="Journal",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                ("code", models.CharField(max_length=10)),
                ("libelle", models.CharField(max_length=100)),
                (
                    "type",
                    models.CharField(
                        choices=[
                            ("VENTES", "Journal des ventes"),
                            ("ACHATS", "Journal des achats"),
                            ("BANQUE", "Journal de banque"),
                            ("CAISSE", "Journal de caisse"),
                            ("OD", "Opérations diverses"),
                        ],
                        max_length=10,
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "journaux",
                "ordering": ["code"],
            },
        ),
        migrations.CreateModel(
            name="Ecriture",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                (
                    "journal",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="ecritures",
                        to="comptabilite.journal",
                    ),
                ),
                (
                    "exercice",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="ecritures",
                        to="comptabilite.exercice",
                    ),
                ),
                ("date_ecriture", models.DateField()),
                ("libelle", models.CharField(max_length=255)),
                (
                    "statut",
                    models.CharField(
                        choices=[
                            ("BROUILLON", "Brouillon"),
                            ("VALIDEE", "Validée"),
                        ],
                        default="BROUILLON",
                        max_length=10,
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "ecritures",
                "ordering": ["-date_ecriture", "-id"],
            },
        ),
        migrations.CreateModel(
            name="LigneEcriture",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                (
                    "ecriture",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="lignes",
                        to="comptabilite.ecriture",
                    ),
                ),
                (
                    "compte",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="lignes_ecriture",
                        to="comptabilite.compte",
                    ),
                ),
                ("libelle", models.CharField(blank=True, max_length=255)),
                (
                    "debit",
                    models.DecimalField(decimal_places=2, default=0, max_digits=15),
                ),
                (
                    "credit",
                    models.DecimalField(decimal_places=2, default=0, max_digits=15),
                ),
            ],
            options={
                "db_table": "lignes_ecriture",
            },
        ),
        migrations.CreateModel(
            name="Facture",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                ("numero", models.CharField(blank=True, max_length=30)),
                ("client_nom", models.CharField(max_length=200)),
                ("client_email", models.EmailField(blank=True)),
                (
                    "devise",
                    models.CharField(
                        choices=[
                            ("XAF", "Franc CFA (BEAC)"),
                            ("EUR", "Euro"),
                            ("USD", "Dollar américain"),
                        ],
                        default="XAF",
                        max_length=3,
                    ),
                ),
                (
                    "statut",
                    models.CharField(
                        choices=[
                            ("BROUILLON", "Brouillon"),
                            ("EMISE", "Émise"),
                            ("PAYEE", "Payée"),
                            ("ANNULEE", "Annulée"),
                        ],
                        default="BROUILLON",
                        max_length=10,
                    ),
                ),
                ("date_emission", models.DateField(blank=True, null=True)),
                ("date_echeance", models.DateField()),
                (
                    "tva_taux",
                    models.DecimalField(decimal_places=2, default=0, max_digits=5),
                ),
                (
                    "ecriture",
                    models.OneToOneField(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="facture",
                        to="comptabilite.ecriture",
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "factures",
                "ordering": ["-created_at"],
            },
        ),
        migrations.CreateModel(
            name="LigneFacture",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                (
                    "facture",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="lignes",
                        to="comptabilite.facture",
                    ),
                ),
                ("description", models.CharField(max_length=255)),
                (
                    "quantite",
                    models.DecimalField(decimal_places=2, default=1, max_digits=10),
                ),
                (
                    "prix_unitaire",
                    models.DecimalField(decimal_places=2, max_digits=15),
                ),
            ],
            options={
                "db_table": "lignes_facture",
            },
        ),
        migrations.CreateModel(
            name="Paiement",
            fields=[
                ("id", models.AutoField(primary_key=True, serialize=False)),
                ("tenant_id", models.UUIDField(db_index=True)),
                (
                    "facture",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="paiements",
                        to="comptabilite.facture",
                    ),
                ),
                (
                    "montant",
                    models.DecimalField(decimal_places=2, max_digits=15),
                ),
                (
                    "devise",
                    models.CharField(
                        choices=[
                            ("XAF", "Franc CFA (BEAC)"),
                            ("EUR", "Euro"),
                            ("USD", "Dollar américain"),
                        ],
                        default="XAF",
                        max_length=3,
                    ),
                ),
                (
                    "mode_paiement",
                    models.CharField(
                        choices=[
                            ("VIREMENT", "Virement bancaire"),
                            ("CHEQUE", "Chèque"),
                            ("ESPECES", "Espèces"),
                            ("MOBILE_MONEY", "Mobile Money"),
                        ],
                        max_length=15,
                    ),
                ),
                ("date_paiement", models.DateField()),
                ("reference", models.CharField(blank=True, max_length=100)),
                (
                    "ecriture",
                    models.OneToOneField(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="paiement",
                        to="comptabilite.ecriture",
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "db_table": "paiements",
                "ordering": ["-date_paiement"],
            },
        ),
        migrations.AddConstraint(
            model_name="exercice",
            constraint=models.UniqueConstraint(
                fields=["tenant_id", "annee"], name="unique_exercice_tenant_annee"
            ),
        ),
        migrations.AddConstraint(
            model_name="compte",
            constraint=models.UniqueConstraint(
                fields=["tenant_id", "numero"], name="unique_compte_tenant_numero"
            ),
        ),
        migrations.AddConstraint(
            model_name="journal",
            constraint=models.UniqueConstraint(
                fields=["tenant_id", "code"], name="unique_journal_tenant_code"
            ),
        ),
    ]

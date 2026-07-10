from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("comptabilite", "0004_immobilisation_dotation"),
    ]

    operations = [
        migrations.AddField(
            model_name="ecriture",
            name="reference_piece",
            field=models.CharField(
                blank=True,
                default="",
                help_text="Référence de la pièce justificative (N° facture, relevé, etc.)",
                max_length=100,
            ),
        ),
    ]

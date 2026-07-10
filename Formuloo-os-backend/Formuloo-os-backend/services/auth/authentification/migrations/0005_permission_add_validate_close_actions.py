from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("authentification", "0004_add_one_time_token"),
    ]

    operations = [
        migrations.AlterField(
            model_name="permission",
            name="action",
            field=models.CharField(
                choices=[
                    ("read", "Lire"),
                    ("write", "Écrire"),
                    ("delete", "Supprimer"),
                    ("validate", "Valider"),
                    ("close", "Clôturer"),
                ],
                max_length=20,
            ),
        ),
    ]

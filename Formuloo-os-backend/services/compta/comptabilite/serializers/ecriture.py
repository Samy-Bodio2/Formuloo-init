from decimal import Decimal
from rest_framework import serializers
from comptabilite.models import Ecriture, LigneEcriture, Compte


class LigneEcritureSerializer(serializers.ModelSerializer):
    compte_numero = serializers.CharField(source="compte.numero", read_only=True)
    compte_libelle = serializers.CharField(source="compte.libelle", read_only=True)

    class Meta:
        model = LigneEcriture
        fields = ["id", "compte_id", "compte_numero", "compte_libelle", "libelle", "debit", "credit"]
        read_only_fields = ["id", "compte_numero", "compte_libelle"]


class LigneEcritureCreateSerializer(serializers.Serializer):
    compte_id = serializers.IntegerField()
    libelle = serializers.CharField(required=False, allow_blank=True, default="")
    debit = serializers.DecimalField(max_digits=15, decimal_places=2)
    credit = serializers.DecimalField(max_digits=15, decimal_places=2)

    def validate(self, data):
        if data["debit"] < 0 or data["credit"] < 0:
            raise serializers.ValidationError("Débit et crédit doivent être positifs.")
        if data["debit"] > 0 and data["credit"] > 0:
            raise serializers.ValidationError(
                "Une ligne ne peut avoir à la fois un débit et un crédit."
            )
        return data


class EcritureSerializer(serializers.ModelSerializer):
    journal_code = serializers.CharField(source="journal.code", read_only=True)
    lignes = LigneEcritureSerializer(many=True, read_only=True)
    total_debit = serializers.DecimalField(
        max_digits=15, decimal_places=2, read_only=True
    )
    total_credit = serializers.DecimalField(
        max_digits=15, decimal_places=2, read_only=True
    )

    class Meta:
        model = Ecriture
        fields = [
            "id", "journal_id", "journal_code", "exercice_id",
            "date_ecriture", "libelle", "reference_piece", "statut",
            "total_debit", "total_credit", "lignes", "created_at",
        ]
        read_only_fields = ["id", "statut", "journal_code", "total_debit", "total_credit", "created_at"]


class EcritureCreateSerializer(serializers.Serializer):
    journal_id = serializers.IntegerField()
    exercice_id = serializers.IntegerField()
    date_ecriture = serializers.DateField()
    libelle = serializers.CharField(max_length=255)
    reference_piece = serializers.CharField(max_length=100, required=False, allow_blank=True, default="")
    lignes = LigneEcritureCreateSerializer(many=True)

    def validate_lignes(self, lignes):
        if len(lignes) < 2:
            raise serializers.ValidationError(
                "Une écriture doit comporter au moins 2 lignes."
            )
        total_debit = sum(l["debit"] for l in lignes)
        total_credit = sum(l["credit"] for l in lignes)
        if total_debit != total_credit:
            raise serializers.ValidationError(
                f"L'écriture est déséquilibrée : "
                f"débit={total_debit} ≠ crédit={total_credit}. "
                f"La somme des débits doit être égale à la somme des crédits."
            )
        if total_debit == Decimal("0"):
            raise serializers.ValidationError("L'écriture ne peut pas être à zéro.")
        return lignes

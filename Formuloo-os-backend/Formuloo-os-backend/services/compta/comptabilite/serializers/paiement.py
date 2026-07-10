from rest_framework import serializers
from comptabilite.models import Paiement


class PaiementSerializer(serializers.ModelSerializer):
    ecriture_id = serializers.IntegerField(
        source="ecriture.id", read_only=True, allow_null=True
    )

    class Meta:
        model = Paiement
        fields = [
            "id", "facture_id", "montant", "devise", "mode_paiement",
            "date_paiement", "reference", "ecriture_id", "created_at",
        ]
        read_only_fields = ["id", "ecriture_id", "created_at"]


class PaiementCreateSerializer(serializers.Serializer):
    facture_id = serializers.IntegerField()
    montant = serializers.DecimalField(max_digits=15, decimal_places=2)
    mode_paiement = serializers.ChoiceField(choices=Paiement.ModePaiement.choices)
    date_paiement = serializers.DateField()
    reference = serializers.CharField(max_length=100, required=False, allow_blank=True, default="")

    def validate_montant(self, value):
        if value <= 0:
            raise serializers.ValidationError("Le montant doit être positif.")
        return value

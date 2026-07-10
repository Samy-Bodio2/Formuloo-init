from decimal import Decimal

from rest_framework import serializers

from gestiondoc.models import AccountingDocument

# Tolérance d'arrondi acceptée entre amount_ttc et amount_ht * (1 + tva_rate/100)
AMOUNT_TOLERANCE = Decimal("1.00")


class CorrectionSerializer(serializers.Serializer):
    field = serializers.CharField()
    original = serializers.CharField(required=False, allow_null=True, allow_blank=True)
    corrected = serializers.CharField(required=False, allow_null=True, allow_blank=True)


class ValidateOCRSerializer(serializers.Serializer):
    document_number = serializers.CharField(max_length=50)
    date = serializers.DateField()
    supplier = serializers.CharField(max_length=200)
    amount_ht = serializers.DecimalField(max_digits=15, decimal_places=2)
    tva_rate = serializers.DecimalField(max_digits=5, decimal_places=2)
    amount_ttc = serializers.DecimalField(max_digits=15, decimal_places=2)
    currency = serializers.ChoiceField(choices=AccountingDocument.Devise.choices, default=AccountingDocument.Devise.XAF)
    corrections = CorrectionSerializer(many=True, required=False, default=list)

    def validate(self, attrs):
        amount_ht = attrs["amount_ht"]
        tva_rate = attrs["tva_rate"]
        amount_ttc = attrs["amount_ttc"]
        expected_ttc = amount_ht * (Decimal("1") + tva_rate / Decimal("100"))
        if abs(expected_ttc - amount_ttc) > AMOUNT_TOLERANCE:
            raise serializers.ValidationError(
                "Montants incohérents : amount_ttc doit être proche de "
                "amount_ht × (1 + tva_rate/100)."
            )
        return attrs

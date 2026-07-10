from decimal import Decimal
from rest_framework import serializers
from comptabilite.models import PaiementFournisseur


class PaiementFournisseurSerializer(serializers.ModelSerializer):
    facture_numero = serializers.CharField(
        source="facture_fournisseur.numero_interne", read_only=True
    )
    fournisseur_nom = serializers.CharField(
        source="facture_fournisseur.fournisseur_nom", read_only=True
    )

    class Meta:
        model = PaiementFournisseur
        fields = [
            "id", "facture_fournisseur", "facture_numero", "fournisseur_nom",
            "montant", "devise", "mode_paiement", "date_paiement",
            "reference", "ecriture", "created_at",
        ]
        read_only_fields = ["id", "ecriture", "created_at"]


class PaiementFournisseurCreateSerializer(serializers.Serializer):
    montant = serializers.DecimalField(max_digits=15, decimal_places=2, min_value=Decimal("0.01"))
    mode_paiement = serializers.ChoiceField(choices=PaiementFournisseur.ModePaiement.choices)
    date_paiement = serializers.DateField()
    reference = serializers.CharField(max_length=100, required=False, allow_blank=True)

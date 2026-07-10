from rest_framework import serializers
from comptabilite.models import FactureFournisseur, LigneFactureFournisseur


class LigneFactureFournisseurSerializer(serializers.ModelSerializer):
    montant_total = serializers.DecimalField(max_digits=15, decimal_places=2, read_only=True)

    class Meta:
        model = LigneFactureFournisseur
        fields = ["id", "description", "compte_charge", "quantite", "prix_unitaire", "montant_total"]


class LigneFactureFournisseurCreateSerializer(serializers.Serializer):
    description = serializers.CharField(max_length=255)
    compte_charge_id = serializers.IntegerField(required=False, allow_null=True)
    quantite = serializers.DecimalField(max_digits=10, decimal_places=2, default=1)
    prix_unitaire = serializers.DecimalField(max_digits=15, decimal_places=2)


class FactureFournisseurSerializer(serializers.ModelSerializer):
    lignes = LigneFactureFournisseurSerializer(many=True, read_only=True)
    montant_ht = serializers.DecimalField(max_digits=15, decimal_places=2, read_only=True)
    tva = serializers.DecimalField(max_digits=15, decimal_places=2, read_only=True)
    montant_ttc = serializers.DecimalField(max_digits=15, decimal_places=2, read_only=True)

    class Meta:
        model = FactureFournisseur
        fields = [
            "id", "type_document", "facture_origine", "numero_interne", "numero_fournisseur",
            "fournisseur_nom", "fournisseur_email",
            "devise", "statut", "date_reception",
            "date_facture", "date_echeance",
            "tva_taux", "montant_ht", "tva", "montant_ttc",
            "ecriture", "created_at", "lignes",
        ]
        read_only_fields = ["id", "type_document", "facture_origine", "numero_interne", "statut", "ecriture", "created_at"]


class FactureFournisseurCreateSerializer(serializers.Serializer):
    fournisseur_nom = serializers.CharField(max_length=200)
    fournisseur_email = serializers.EmailField(required=False, allow_blank=True)
    numero_fournisseur = serializers.CharField(max_length=100, required=False, allow_blank=True)
    devise = serializers.ChoiceField(choices=FactureFournisseur.Devise.choices, default="XAF")
    date_facture = serializers.DateField()
    date_echeance = serializers.DateField()
    tva_taux = serializers.DecimalField(max_digits=5, decimal_places=2, default=0)
    lignes = LigneFactureFournisseurCreateSerializer(many=True)

    def validate_lignes(self, lignes):
        if not lignes:
            raise serializers.ValidationError("Au moins une ligne est requise.")
        return lignes

    def validate(self, data):
        if data["date_echeance"] < data["date_facture"]:
            raise serializers.ValidationError(
                {"date_echeance": "La date d'échéance doit être après la date de facture."}
            )
        return data


class FactureFournisseurUpdateSerializer(serializers.ModelSerializer):
    class Meta:
        model = FactureFournisseur
        fields = ["fournisseur_nom", "fournisseur_email", "numero_fournisseur",
                  "date_echeance", "tva_taux"]

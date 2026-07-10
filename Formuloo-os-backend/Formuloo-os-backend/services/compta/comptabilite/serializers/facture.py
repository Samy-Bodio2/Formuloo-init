from rest_framework import serializers
from comptabilite.models import Facture, LigneFacture


class LigneFactureSerializer(serializers.ModelSerializer):
    montant_total = serializers.DecimalField(
        max_digits=15, decimal_places=2, read_only=True
    )

    class Meta:
        model = LigneFacture
        fields = ["id", "description", "quantite", "prix_unitaire", "montant_total"]
        read_only_fields = ["id", "montant_total"]


class LigneFactureCreateSerializer(serializers.Serializer):
    description = serializers.CharField(max_length=255)
    quantite = serializers.DecimalField(max_digits=10, decimal_places=2, default=1)
    prix_unitaire = serializers.DecimalField(max_digits=15, decimal_places=2)

    def validate_quantite(self, value):
        if value <= 0:
            raise serializers.ValidationError("La quantité doit être positive.")
        return value

    def validate_prix_unitaire(self, value):
        if value < 0:
            raise serializers.ValidationError("Le prix unitaire ne peut pas être négatif.")
        return value


class FactureSerializer(serializers.ModelSerializer):
    lignes = LigneFactureSerializer(many=True, read_only=True)
    montant_ht = serializers.DecimalField(
        max_digits=15, decimal_places=2, read_only=True
    )
    tva = serializers.DecimalField(max_digits=15, decimal_places=2, read_only=True)
    montant_ttc = serializers.DecimalField(
        max_digits=15, decimal_places=2, read_only=True
    )
    ecriture_id = serializers.IntegerField(source="ecriture.id", read_only=True, allow_null=True)

    class Meta:
        model = Facture
        fields = [
            "id", "type_document", "facture_origine", "numero", "client_nom", "client_email",
            "lignes", "montant_ht", "tva_taux", "tva", "montant_ttc", "devise",
            "statut", "date_emission", "date_echeance", "ecriture_id", "created_at",
        ]
        read_only_fields = [
            "id", "type_document", "facture_origine", "numero", "statut", "date_emission",
            "montant_ht", "tva", "montant_ttc", "ecriture_id", "created_at",
        ]


class FactureCreateSerializer(serializers.Serializer):
    client_nom = serializers.CharField(max_length=200)
    client_email = serializers.EmailField(required=False, allow_blank=True, default="")
    lignes = LigneFactureCreateSerializer(many=True)
    devise = serializers.ChoiceField(choices=Facture.Devise.choices, default="XAF")
    tva_taux = serializers.DecimalField(max_digits=5, decimal_places=2, default=0)
    date_echeance = serializers.DateField()

    def validate_lignes(self, lignes):
        if not lignes:
            raise serializers.ValidationError(
                "Une facture doit comporter au moins une ligne."
            )
        return lignes


class FactureUpdateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Facture
        fields = ["client_nom", "client_email", "date_echeance"]

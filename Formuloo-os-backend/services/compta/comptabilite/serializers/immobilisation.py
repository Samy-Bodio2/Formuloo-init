"""
Serializers Immobilisations — Formuloo OS
"""

from rest_framework import serializers
from comptabilite.models import Immobilisation, DotationAmortissement


class ImmobilisationSerializer(serializers.ModelSerializer):
    valeur_nette_comptable = serializers.SerializerMethodField()
    taux_lineaire = serializers.SerializerMethodField()

    class Meta:
        model = Immobilisation
        fields = [
            "id",
            "code",
            "designation",
            "categorie",
            "numero_compte",
            "fournisseur",
            "reference_facture",
            "valeur_origine",
            "valeur_residuelle",
            "devise",
            "methode",
            "duree_vie",
            "date_mise_en_service",
            "cumul_amortissements",
            "valeur_nette_comptable",
            "taux_lineaire",
            "statut",
            "date_cession",
            "valeur_nette_cession",
            "exercice",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "cumul_amortissements", "created_at", "updated_at"]

    def get_valeur_nette_comptable(self, obj):
        return str(obj.valeur_nette_comptable)

    def get_taux_lineaire(self, obj):
        return str(obj.taux_lineaire)


class ImmobilisationCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Immobilisation
        fields = [
            "code",
            "designation",
            "categorie",
            "numero_compte",
            "fournisseur",
            "reference_facture",
            "valeur_origine",
            "valeur_residuelle",
            "devise",
            "methode",
            "duree_vie",
            "date_mise_en_service",
            "exercice",
        ]

    def validate_valeur_origine(self, value):
        if value <= 0:
            raise serializers.ValidationError("La valeur d'origine doit être positive.")
        return value

    def validate_duree_vie(self, value):
        if value < 1 or value > 50:
            raise serializers.ValidationError("La durée de vie doit être entre 1 et 50 ans.")
        return value


class DotationAmortissementSerializer(serializers.ModelSerializer):
    immobilisation_code = serializers.CharField(source="immobilisation.code", read_only=True)
    immobilisation_designation = serializers.CharField(
        source="immobilisation.designation", read_only=True
    )

    class Meta:
        model = DotationAmortissement
        fields = [
            "id",
            "immobilisation",
            "immobilisation_code",
            "immobilisation_designation",
            "exercice",
            "annee",
            "montant",
            "ecriture",
            "date_comptabilisation",
        ]
        read_only_fields = ["id", "date_comptabilisation", "ecriture"]

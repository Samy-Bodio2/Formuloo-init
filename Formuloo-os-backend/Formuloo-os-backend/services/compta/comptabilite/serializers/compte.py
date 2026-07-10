from rest_framework import serializers
from comptabilite.models import Compte


class CompteSerializer(serializers.ModelSerializer):

    class Meta:
        model = Compte
        fields = [
            "id", "numero", "libelle", "classe",
            "type_compte", "is_systeme", "created_at",
        ]
        read_only_fields = ["id", "is_systeme", "created_at"]


class CompteCreateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Compte
        fields = ["numero", "libelle", "classe", "type_compte"]

    def validate_classe(self, value):
        if not (1 <= value <= 8):
            raise serializers.ValidationError(
                "La classe OHADA doit être comprise entre 1 et 8."
            )
        return value


class CompteUpdateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Compte
        fields = ["libelle"]

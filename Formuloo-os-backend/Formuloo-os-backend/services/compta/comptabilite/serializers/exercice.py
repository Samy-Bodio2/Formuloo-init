from rest_framework import serializers
from comptabilite.models import Exercice


class ExerciceSerializer(serializers.ModelSerializer):

    class Meta:
        model = Exercice
        fields = ["id", "annee", "date_debut", "date_fin", "statut", "created_at"]
        read_only_fields = ["id", "statut", "created_at"]


class ExerciceCreateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Exercice
        fields = ["annee", "date_debut", "date_fin"]

    def validate(self, data):
        if data["date_debut"] >= data["date_fin"]:
            raise serializers.ValidationError(
                {"date_fin": "La date de fin doit être postérieure à la date de début."}
            )
        return data

from rest_framework import serializers
from comptabilite.models import Journal


class JournalSerializer(serializers.ModelSerializer):
    nb_ecritures = serializers.SerializerMethodField()
    compte_contrepartie_numero = serializers.SerializerMethodField()

    class Meta:
        model = Journal
        fields = ["id", "code", "libelle", "type", "nb_ecritures", "compte_contrepartie_numero", "created_at"]
        read_only_fields = ["id", "nb_ecritures", "compte_contrepartie_numero", "created_at"]

    def get_nb_ecritures(self, obj):
        return obj.ecritures.count()

    def get_compte_contrepartie_numero(self, obj):
        if obj.compte_contrepartie_id:
            return obj.compte_contrepartie.numero
        return None


class JournalCreateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Journal
        fields = ["code", "libelle", "type"]

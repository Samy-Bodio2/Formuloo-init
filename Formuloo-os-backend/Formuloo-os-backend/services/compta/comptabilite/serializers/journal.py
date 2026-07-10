from rest_framework import serializers
from comptabilite.models import Journal


class JournalSerializer(serializers.ModelSerializer):

    class Meta:
        model = Journal
        fields = ["id", "code", "libelle", "type", "created_at"]
        read_only_fields = ["id", "created_at"]


class JournalCreateSerializer(serializers.ModelSerializer):

    class Meta:
        model = Journal
        fields = ["code", "libelle", "type"]

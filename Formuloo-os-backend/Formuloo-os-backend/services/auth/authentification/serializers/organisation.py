"""
Serializers Organisation — Formuloo OS
CRUD des organisations (tenants).
Réservé au SUPER_ADMIN.
Conforme ADR-001 : isolation multi-tenant
"""

from rest_framework import serializers
from authentification.models import Organisation


class OrganisationSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour les organisations.
    Utilisé pour la liste et le détail.
    """

    class Meta:
        model = Organisation
        fields = [
            "id",
            "slug",
            "name",
            "currency",
            "locale",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]


class OrganisationCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour la création d'une organisation.
    POST /api/v1/auth/organisations/
    """

    class Meta:
        model = Organisation
        fields = ["slug", "name", "currency", "locale"]

    def validate_slug(self, value):
        """
        Vérifie que le slug est unique.
        """
        if Organisation.objects.filter(slug=value).exists():
            raise serializers.ValidationError("Ce slug est déjà utilisé.")
        return value

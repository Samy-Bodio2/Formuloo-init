"""
Serializers AuditLog — Formuloo OS
Journal d'audit immuable des actions utilisateurs.
Conforme ADR-002 : traçabilité + sécurité
Lecture seule — aucune modification autorisée.
"""

from rest_framework import serializers
from authentification.models import AuditLog


class AuditLogSerializer(serializers.ModelSerializer):
    """
    Serializer pour la liste et le détail des journaux d'audit.
    Lecture seule — immuable.
    """

    user = serializers.SerializerMethodField()

    class Meta:
        model = AuditLog
        fields = [
            "id",
            "user",
            "action",
            "resource",
            "resource_id",
            "payload",
            "ip_address",
            "timestamp",
        ]
        read_only_fields = fields

    def get_user(self, obj):
        if not obj.user:
            return None
        return {
            "id": str(obj.user.id),
            "email": obj.user.email,
            "nom": obj.user.get_full_name(),
        }

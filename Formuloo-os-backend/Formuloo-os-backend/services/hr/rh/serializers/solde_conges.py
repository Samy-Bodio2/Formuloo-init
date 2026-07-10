"""
Serializers SoldeConges — Formuloo OS
Capital congés d'un employé par type et par année.
Conforme au contrat hr.yaml v2.1.0
"""

from rest_framework import serializers

from rh.models import SoldeConges


class SoldeCongesSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/soldes-conges/
    GET /hr/soldes-conges/{id}/
    """

    employee = serializers.SerializerMethodField()
    jours_restants = serializers.SerializerMethodField()

    class Meta:
        model = SoldeConges
        fields = [
            "id",
            "employee",
            "type_conge",
            "annee",
            "jours_acquis",
            "jours_pris",
            "jours_restants",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "jours_restants", "created_at", "updated_at"]

    def get_employee(self, obj):
        """Retourne l'employé sous forme simplifiée."""
        return {
            "id": str(obj.employee.id),
            "employee_number": obj.employee.employee_number,
            "first_name": obj.employee.first_name,
            "last_name": obj.employee.last_name,
            "email": obj.employee.email,
        }

    def get_jours_restants(self, obj):
        """Retourne le solde restant calculé dynamiquement."""
        return float(obj.jours_restants)

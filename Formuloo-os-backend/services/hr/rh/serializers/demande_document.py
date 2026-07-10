"""
Serializers DemandeDocument — Formuloo OS
Workflow de demande officielle de document RH.
"""

from rest_framework import serializers

from rh.models import DemandeDocument


class DemandeDocumentSerializer(serializers.ModelSerializer):
    """Lecture complète d'une demande de document."""

    type_document_display = serializers.CharField(
        source="get_type_document_display", read_only=True
    )
    statut_display = serializers.CharField(source="get_statut_display", read_only=True)

    class Meta:
        model = DemandeDocument
        fields = [
            "id",
            "type_document",
            "type_document_display",
            "statut",
            "statut_display",
            "motif_demande",
            "motif_rejet",
            "document_data",
            "traitee_le",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields


class DemandeDocumentCreateSerializer(serializers.Serializer):
    """Soumission d'une nouvelle demande de document par l'employé."""

    type_document = serializers.ChoiceField(
        choices=DemandeDocument.TypeDocument.choices,
        help_text="Type de document : attestation_travail, attestation_salaire, bulletin_paie_copie",
    )
    motif_demande = serializers.CharField(
        required=False,
        allow_blank=True,
        max_length=500,
        help_text="Raison de la demande (optionnel — ex: demande de visa, prêt bancaire)",
    )


class DemandeDocumentRHSerializer(DemandeDocumentSerializer):
    """Lecture complète d'une demande avec informations de l'employé — usage RH uniquement."""

    employee_id = serializers.CharField(source="employee.id", read_only=True)
    employee_nom = serializers.SerializerMethodField()

    class Meta(DemandeDocumentSerializer.Meta):
        fields = list(DemandeDocumentSerializer.Meta.fields) + ["employee_id", "employee_nom"]
        read_only_fields = fields

    def get_employee_nom(self, obj):
        if not hasattr(obj, "employee") or obj.employee is None:
            return ""
        return f"{obj.employee.first_name} {obj.employee.last_name}".strip()


class RejeterDemandeSerializer(serializers.Serializer):
    """Payload du rejet d'une demande par le RH."""

    motif_rejet = serializers.CharField(
        required=True,
        min_length=5,
        max_length=500,
        help_text="Motif du rejet — obligatoire pour informer l'employé",
    )

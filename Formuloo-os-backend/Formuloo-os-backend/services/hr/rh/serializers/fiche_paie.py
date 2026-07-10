"""
Serializers FichePaie — Formuloo OS
Fiches de paie mensuelles conformes SYSCOHADA.
Conforme au contrat hr.yaml v2.1.0
"""

from decimal import Decimal
from rest_framework import serializers

from rh.models import Contrat, Employe, FichePaie
from rh.services.cotisations import calculer_cotisations


def _calculer_nb_parts(employee) -> int:
    """
    Détermine le nombre de parts fiscales selon la situation familiale.
    Cameroun : 1 part de base, +0.5 si marié, +0.5 par enfant (max 5).
    """
    parts = 1.0
    situation = getattr(employee, "situation_familiale", None)
    nb_enfants = getattr(employee, "nombre_enfants", 0) or 0
    if situation == "marie":
        parts += 0.5
    parts += nb_enfants * 0.5
    return max(1, min(int(parts), 5))


class FichePaieSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/payroll/
    GET /hr/payroll/{id}/
    """

    employee = serializers.SerializerMethodField()
    contrat = serializers.SerializerMethodField()

    class Meta:
        model = FichePaie
        fields = [
            "id",
            "employee",
            "contrat",
            "mois",
            "annee",
            "period",
            # Salaire de base
            "salaire_base",
            "heures_travaillees",
            "heures_supplementaires",
            "taux_horaire_supp",
            # Primes et déductions (JSON)
            "bonuses",
            "deductions",
            # Totaux calculés
            "gross",
            "net_salary",
            "currency",
            # Statut
            "statut",
            "date_validation",
            "paid_at",
            "mode_paiement",
            # Documents
            "pdf_url",
            "journal_entry_id",
            "created_at",
            "updated_at",
        ]
        read_only_fields = [
            "id",
            "period",
            "gross",
            "net_salary",
            "date_validation",
            "paid_at",
            "journal_entry_id",
            "created_at",
            "updated_at",
        ]

    def get_employee(self, obj):
        """Retourne l'employé sous forme simplifiée."""
        return {
            "id": str(obj.employee.id),
            "employee_number": obj.employee.employee_number,
            "first_name": obj.employee.first_name,
            "last_name": obj.employee.last_name,
            "email": obj.employee.email,
        }

    def get_contrat(self, obj):
        """Retourne le contrat sous forme simplifiée."""
        if not obj.contrat:
            return None
        return {
            "id": str(obj.contrat.id),
            "numero": obj.contrat.numero,
            "type": obj.contrat.type,
            "is_active": obj.contrat.is_active,
        }


class FichePaieCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour générer une fiche de paie.
    POST /hr/payroll/
    PUT  /hr/payroll/{id}/
    """

    employe_id = serializers.UUIDField(help_text="UUID de l'employé")
    contrat_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID du contrat"
    )

    class Meta:
        model = FichePaie
        fields = [
            "employe_id",
            "contrat_id",
            "mois",
            "annee",
            "heures_travaillees",
            "heures_supplementaires",
            "taux_horaire_supp",
            "bonuses",
            "deductions",
            "currency",
        ]

    def validate_employe_id(self, value):
        """Vérifie que l'employé existe dans le tenant."""
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Employe.objects.filter(
            id=value, tenant_id=tenant_id, status="active"
        ).exists():
            raise serializers.ValidationError("Employé introuvable ou inactif.")
        return value

    def validate_contrat_id(self, value):
        """Vérifie que le contrat existe et est actif."""
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Contrat.objects.filter(
            id=value, tenant_id=tenant_id, is_active=True
        ).exists():
            raise serializers.ValidationError("Contrat introuvable ou inactif.")
        return value

    def validate(self, data):
        """
        Vérifie qu'une fiche n'existe pas déjà
        pour cet employé ce mois-ci.
        """
        employe_id = data.get("employe_id")
        mois = data.get("mois")
        annee = data.get("annee")

        if employe_id and mois and annee:
            qs = FichePaie.objects.filter(
                employee_id=employe_id, mois=mois, annee=annee
            )
            if self.instance:
                qs = qs.exclude(id=self.instance.id)
            if qs.exists():
                raise serializers.ValidationError(
                    f"Une fiche de paie existe déjà pour "
                    f"cet employé pour {mois:02d}/{annee}."
                )

        return data

    def create(self, validated_data):
        """
        Génère une fiche de paie avec calcul automatique CNPS/IRPP.
        """
        request = self.context.get("request")
        employe_id = validated_data.pop("employe_id")
        contrat_id = validated_data.pop("contrat_id", None)

        employee = Employe.objects.get(id=employe_id)

        if not contrat_id:
            contrat = Contrat.objects.filter(
                employee=employee, is_active=True, statut="actif"
            ).first()
            if contrat:
                contrat_id = contrat.id

        # ── Calcul automatique CNPS / IRPP ────────────────
        deductions = dict(validated_data.get("deductions") or {})
        bonuses = validated_data.get("bonuses") or {}
        heures_supp = validated_data.get("heures_supplementaires", 0) or 0
        taux_supp = validated_data.get("taux_horaire_supp", 0) or 0
        salaire_base = employee.salaire_base
        brut_estime = Decimal(str(salaire_base)) + Decimal(str(sum(bonuses.values()) if bonuses else 0)) + Decimal(str(heures_supp)) * Decimal(str(taux_supp))

        nb_parts = _calculer_nb_parts(employee)
        cotisations = calculer_cotisations(brut_estime, nb_parts)

        # Injecter seulement si non déjà saisis manuellement
        if not deductions.get("cotisation_cnps"):
            deductions["cotisation_cnps"] = cotisations["cotisation_cnps"]
        if not deductions.get("impot_irpp"):
            deductions["impot_irpp"] = cotisations["impot_irpp"]
        validated_data["deductions"] = deductions

        return FichePaie.objects.create(
            tenant_id=request.user.tenant_id,
            employee_id=employe_id,
            contrat_id=contrat_id,
            salaire_base=salaire_base,
            **validated_data,
        )

    def update(self, instance, validated_data):
        """
        Modifie une fiche en statut brouillon uniquement.
        """
        if instance.statut != FichePaie.Statut.BROUILLON:
            raise serializers.ValidationError(
                "Impossible de modifier une fiche validée ou payée."
            )
        employe_id = validated_data.pop("employe_id", None)
        contrat_id = validated_data.pop("contrat_id", None)
        if employe_id:
            instance.employee_id = employe_id
        if contrat_id:
            instance.contrat_id = contrat_id
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class FichePaieValiderSerializer(serializers.Serializer):
    """
    Serializer pour valider une fiche de paie.
    POST /hr/payroll/{id}/valider/
    """

    pass


class FichePaiePayerSerializer(serializers.Serializer):
    """
    Serializer pour marquer une fiche comme payée.
    POST /hr/payroll/{id}/payer/
    """

    mode_paiement = serializers.ChoiceField(
        choices=["virement", "especes", "mobile_money", "cheque"],
        help_text="Mode de paiement utilisé",
    )


class PayrollRunSerializer(serializers.Serializer):
    """
    Serializer pour lancer la génération de paie en masse.
    POST /hr/payroll/run/
    """

    mois = serializers.IntegerField(
        min_value=1, max_value=12, help_text="Mois de la paie (1-12)"
    )
    annee = serializers.IntegerField(help_text="Année de la paie")

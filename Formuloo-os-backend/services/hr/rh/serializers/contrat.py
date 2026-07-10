"""
Serializers Contrat — Formuloo OS
Gestion des contrats de travail.
Conforme au contrat hr.yaml v2.1.0

Validations métier :
→ CDD et Stage doivent avoir une date de fin
→ Date de fin après date de début
→ Un seul contrat actif par employé
→ Salaire minimum légal SMIG Cameroun (36 270 XAF)
"""

from rest_framework import serializers

from rh.models import Contrat, Employe

# ── Constante SMIG Cameroun ───────────────────────────────
# Décret n°2014/2217/PM du 24 juillet 2014
# Salaire Minimum Interprofessionnel Garanti
SMIG_CAMEROUN = 36270


class ContratBriefSerializer(serializers.ModelSerializer):
    """
    Version simplifiée du contrat.
    Utilisée dans les références d'autres serializers.
    Ex: dans FichePaieSerializer.
    """

    class Meta:
        model = Contrat
        fields = ["id", "numero", "type", "is_active"]


class ContratSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/contrats/
    GET /hr/contrats/{id}/
    """

    employee = serializers.SerializerMethodField()

    class Meta:
        model = Contrat
        fields = [
            "id",
            "numero",
            "employee",
            "type",
            "start_date",
            "end_date",
            "trial_period",
            "gross_salary",
            "currency",
            "work_hours_per_week",
            "periodicite_paie",
            "jours_conge_annuel",
            "lieu_travail",
            "poste",
            "statut",
            "is_active",
            "motif_resiliation",
            "date_resiliation",
            "document_url",
            "signed_at",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "numero", "created_at", "updated_at"]

    def get_employee(self, obj):
        """Retourne l'employé sous forme simplifiée."""
        return {
            "id": str(obj.employee.id),
            "employee_number": obj.employee.employee_number,
            "first_name": obj.employee.first_name,
            "last_name": obj.employee.last_name,
            "email": obj.employee.email,
        }


class ContratCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour créer/modifier un contrat.
    POST /hr/contrats/
    PUT  /hr/contrats/{id}/

    Validations :
    1. Employé existe dans le tenant
    2. CDD/Stage → date de fin obligatoire
    3. Date de fin après date de début
    4. Un seul contrat actif par employé
    5. Salaire >= SMIG Cameroun (36 270 XAF)
    """

    employe_id = serializers.UUIDField(help_text="UUID de l'employé")

    class Meta:
        model = Contrat
        fields = [
            "employe_id",
            "type",
            "start_date",
            "end_date",
            "trial_period",
            "gross_salary",
            "currency",
            "work_hours_per_week",
            "periodicite_paie",
            "jours_conge_annuel",
            "lieu_travail",
            "poste",
            "document_url",
            "signed_at",
        ]

    def validate_employe_id(self, value):
        """
        Vérifie que l'employé existe dans le tenant.
        """
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Employe.objects.filter(id=value, tenant_id=tenant_id).exists():
            raise serializers.ValidationError(
                "L'employé sélectionné est introuvable dans votre organisation."
            )
        return value

    def validate(self, data):
        """
        Validations métier complètes :

        1. CDD et Stage doivent avoir une date de fin
           → Contrat à durée déterminée
             doit avoir une échéance

        2. Date de fin après date de début
           → Cohérence temporelle

        3. Un seul contrat actif par employé
           → Éviter les conflits contractuels
           Ex: CDI actif + Stage actif = IMPOSSIBLE ❌

        4. Salaire minimum légal SMIG
           → Conformité légale Cameroun
           → SMIG = 36 270 XAF/mois
           → Décret n°2014/2217/PM du 24/07/2014
        """
        type_contrat = data.get("type")
        start_date = data.get("start_date")
        end_date = data.get("end_date")
        employe_id = data.get("employe_id")
        gross_salary = data.get("gross_salary")

        # ── 1. CDD et Stage → date de fin obligatoire ────
        if type_contrat in ["CDD", "Stage"] and not end_date:
            raise serializers.ValidationError(
                f"Un contrat {type_contrat} " f"doit avoir une date de fin."
            )

        # ── 2. Date de fin après date de début ────────────
        if start_date and end_date:
            if end_date <= start_date:
                raise serializers.ValidationError(
                    "La date de fin doit être " "après la date de début."
                )

        # ── 3. Un seul contrat actif par employé ──────────
        # Un employé ne peut avoir qu'un seul
        # contrat actif à la fois
        if employe_id:
            qs = Contrat.objects.filter(
                employee_id=employe_id, is_active=True, statut="actif"
            )
            # En modification on exclut l'instance courante
            if self.instance:
                qs = qs.exclude(id=self.instance.id)
            if qs.exists():
                contrat_existant = qs.first()
                raise serializers.ValidationError(
                    f"Cet employé a déjà un contrat "
                    f"actif ({contrat_existant.numero}). "
                    f"Résiliez le contrat existant "
                    f"avant d'en créer un nouveau."
                )

        # ── 4. Salaire minimum légal SMIG ─────────────────
        # Vérifier uniquement pour les contrats
        # en XAF (Franc CFA)
        currency = data.get("currency", "XAF")
        if gross_salary and currency == "XAF":
            if gross_salary < SMIG_CAMEROUN:
                raise serializers.ValidationError(
                    f"Le salaire brut "
                    f"({gross_salary:,.0f} XAF) "
                    f"est inférieur au SMIG "
                    f"camerounais "
                    f"({SMIG_CAMEROUN:,.0f} XAF/mois). "
                    f"Décret n°2014/2217/PM "
                    f"du 24 juillet 2014."
                )

        return data

    def create(self, validated_data):
        """
        Crée un contrat avec tenant_id du JWT.
        Le numéro est auto-généré dans le modèle.
        """
        request = self.context.get("request")
        employe_id = validated_data.pop("employe_id")

        return Contrat.objects.create(
            tenant_id=request.user.tenant_id, employee_id=employe_id, **validated_data
        )

    def update(self, instance, validated_data):
        """
        Met à jour un contrat.
        """
        employe_id = validated_data.pop("employe_id", None)
        if employe_id:
            instance.employee_id = employe_id
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance

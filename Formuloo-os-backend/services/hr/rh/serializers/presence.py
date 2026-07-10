"""
Serializers Présence — Formuloo OS
Enregistrement quotidien des pointages.
Conforme au contrat hr.yaml v2.1.0

Validations métier :
→ heure_depart après heure_arrivee
→ Un seul pointage par employé par jour
→ Impossible de pointer si employé en congé approuvé
"""

from rest_framework import serializers

from rh.models import Employe, Presence


class PresenceSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/presences/
    GET /hr/presences/{id}/
    """

    employee = serializers.SerializerMethodField()

    class Meta:
        model = Presence
        fields = [
            "id",
            "employee",
            "date",
            "heure_arrivee",
            "heure_depart",
            "heures_travaillees",
            "heures_supplementaires",
            "statut",
            "commentaire",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "heures_travaillees", "created_at", "updated_at"]

    def get_employee(self, obj):
        """Retourne l'employé sous forme simplifiée."""
        return {
            "id": str(obj.employee.id),
            "employee_number": obj.employee.employee_number,
            "first_name": obj.employee.first_name,
            "last_name": obj.employee.last_name,
            "email": obj.employee.email,
        }


class PresenceCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour créer/modifier une présence.
    POST /hr/presences/
    PUT  /hr/presences/{id}/

    Validations :
    1. Employé existe dans le tenant
    2. heure_depart après heure_arrivee
    3. Un seul pointage par employé par jour
    4. Employé non en congé approuvé ce jour
    """

    employe_id = serializers.UUIDField(help_text="UUID de l'employé")

    class Meta:
        model = Presence
        fields = [
            "employe_id",
            "date",
            "heure_arrivee",
            "heure_depart",
            "heures_supplementaires",
            "statut",
            "commentaire",
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

        1. heure_depart après heure_arrivee
           → Cohérence des horaires

        2. Unicité du pointage (employe + date)
           → Un seul pointage par jour

        3. Congé approuvé ce jour
           → Un employé en congé ne peut pas
             avoir une présence le même jour
           Ex: Congé 01/07 → 15/07
               Présence 05/07 → IMPOSSIBLE ❌
        """
        heure_arrivee = data.get("heure_arrivee")
        heure_depart = data.get("heure_depart")
        employe_id = data.get("employe_id")
        date = data.get("date")

        # ── 1. Vérifier l'ordre des heures ───────────────
        # heure_depart doit être après heure_arrivee
        if heure_arrivee and heure_depart:
            if heure_depart <= heure_arrivee:
                raise serializers.ValidationError(
                    "L'heure de départ doit être " "après l'heure d'arrivée."
                )

        # ── 2. Vérifier unicité du pointage ──────────────
        # Un seul pointage par employé par jour
        if employe_id and date:
            qs = Presence.objects.filter(employee_id=employe_id, date=date)
            # En modification on exclut l'instance courante
            if self.instance:
                qs = qs.exclude(id=self.instance.id)
            if qs.exists():
                raise serializers.ValidationError(
                    "Un pointage a déjà été enregistré pour cet employé à cette date. "
                    "Modifiez le pointage existant ou choisissez une autre date."
                )

        # ── 3. Vérifier congé approuvé ce jour ───────────
        # Un employé en congé approuvé ne peut pas
        # avoir une présence le même jour
        if employe_id and date:
            from rh.models import Conge

            conge_actif = Conge.objects.filter(
                employee_id=employe_id,
                status="approved",
                start_date__lte=date,
                end_date__gte=date,
            ).exists()
            if conge_actif:
                raise serializers.ValidationError(
                    "Impossible de créer une présence — "
                    "cet employé est en congé approuvé "
                    "à cette date."
                )

        return data

    def create(self, validated_data):
        """
        Crée un pointage avec tenant_id du JWT.
        Les heures travaillées sont calculées
        automatiquement dans le modèle Presence.save()
        """
        request = self.context.get("request")
        employe_id = validated_data.pop("employe_id")

        return Presence.objects.create(
            tenant_id=request.user.tenant_id, employee_id=employe_id, **validated_data
        )

    def update(self, instance, validated_data):
        """
        Met à jour un pointage.
        Les heures travaillées sont recalculées
        automatiquement dans le modèle Presence.save()
        """
        employe_id = validated_data.pop("employe_id", None)
        if employe_id:
            instance.employee_id = employe_id
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance

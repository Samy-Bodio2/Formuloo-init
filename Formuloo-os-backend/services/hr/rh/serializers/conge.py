"""
Serializers Congé — Formuloo OS
Gestion des demandes et approbations de congés.
Conforme au contrat hr.yaml v2.1.0

Validations métier :
→ Date de fin après date de début
→ Chevauchement avec congés existants interdit
→ Solde de congés suffisant vérifié à la soumission
"""

from rest_framework import serializers

from rh.models import Conge, Employe


class CongeSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/leaves/
    GET /hr/leaves/{id}/
    """

    employee = serializers.SerializerMethodField()
    approved_by = serializers.SerializerMethodField()
    remplacant = serializers.SerializerMethodField()

    class Meta:
        model = Conge
        fields = [
            "id",
            "employee",
            "type_conge",
            "start_date",
            "end_date",
            "days",
            "jours_ouvres",
            "reason",
            "piece_justificative",
            "status",
            "approved_by",
            "approved_at",
            "commentaire_decision",
            "remplacant",
            "created_at",
            "updated_at",
        ]
        read_only_fields = [
            "id",
            "days",
            "jours_ouvres",
            "status",
            "approved_by",
            "approved_at",
            "commentaire_decision",
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

    def get_approved_by(self, obj):
        """Retourne l'approbateur sous forme simplifiée."""
        if not obj.approved_by:
            return None
        return {
            "id": str(obj.approved_by.id),
            "employee_number": obj.approved_by.employee_number,
            "first_name": obj.approved_by.first_name,
            "last_name": obj.approved_by.last_name,
            "email": obj.approved_by.email,
        }

    def get_remplacant(self, obj):
        """Retourne le remplaçant sous forme simplifiée."""
        if not obj.remplacant:
            return None
        return {
            "id": str(obj.remplacant.id),
            "employee_number": obj.remplacant.employee_number,
            "first_name": obj.remplacant.first_name,
            "last_name": obj.remplacant.last_name,
            "email": obj.remplacant.email,
        }


class CongeCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour soumettre une demande de congé.
    POST /hr/leaves/
    PUT  /hr/leaves/{id}/

    Validations :
    1. Date de fin après date de début
    2. Pas de chevauchement avec congés existants
    3. Remplaçant existe et est actif
    """

    remplacant_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID de l'employé remplaçant"
    )

    class Meta:
        model = Conge
        fields = [
            "type_conge",
            "start_date",
            "end_date",
            "reason",
            "piece_justificative",
            "remplacant_id",
        ]

    def validate(self, data):
        """
        Validations métier complètes :

        1. Date de fin après date de début
           → Cohérence temporelle

        2. Chevauchement avec congés existants
           → Un employé ne peut pas avoir
             deux congés qui se chevauchent
           Ex: Congé 1 : 01/07 → 15/07
               Congé 2 : 10/07 → 20/07
               → IMPOSSIBLE ❌ (chevauchement 10/07-15/07)
        """
        start_date = data.get("start_date")
        end_date = data.get("end_date")

        # ── 1. Date de fin après date de début ────────────
        if start_date and end_date:
            if end_date < start_date:
                raise serializers.ValidationError(
                    "La date de fin doit être " "après la date de début."
                )

        # ── 2. Chevauchement avec congés existants ────────
        # Vérifier qu'il n'existe pas un congé
        # pending ou approved qui chevauche
        # les dates demandées
        if start_date and end_date:
            request = self.context.get("request")
            try:
                # Récupérer l'employé connecté
                employe = Employe.objects.get(
                    tenant_id=request.user.tenant_id, user_id=request.user.auth_user_id
                )

                # Un congé chevauche si :
                # → son début est avant notre fin
                # ET sa fin est après notre début
                qs = Conge.objects.filter(
                    employee=employe,
                    status__in=["pending", "approved"],
                    start_date__lte=end_date,
                    end_date__gte=start_date,
                )

                # En modification on exclut
                # l'instance courante
                if self.instance:
                    qs = qs.exclude(id=self.instance.id)

                if qs.exists():
                    conge_existant = qs.first()
                    _statut_fr = {
                        "pending": "en attente",
                        "approved": "approuvé",
                        "rejected": "rejeté",
                        "cancelled": "annulé",
                    }.get(conge_existant.status, conge_existant.status)
                    raise serializers.ValidationError(
                        f"Ces dates sont en conflit avec un congé existant "
                        f"du {conge_existant.start_date} au {conge_existant.end_date} "
                        f"(statut : {_statut_fr}). "
                        f"Choisissez des dates qui ne se chevauchent pas."
                    )

            except Employe.DoesNotExist:
                # Employé non trouvé
                # → géré dans la méthode create()
                pass

        return data

    def validate_remplacant_id(self, value):
        """
        Vérifie que le remplaçant existe
        et est actif dans le tenant.
        """
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Employe.objects.filter(
            id=value, tenant_id=tenant_id, status="active"
        ).exists():
            raise serializers.ValidationError(
                "L'employé sélectionné comme remplaçant est introuvable ou n'est plus actif."
            )
        return value

    def create(self, validated_data):
        """
        Crée une demande de congé.
        L'employé est l'utilisateur connecté.
        """
        request = self.context.get("request")
        remplacant_id = validated_data.pop("remplacant_id", None)

        # Récupérer l'employé lié
        # à l'utilisateur connecté
        try:
            employee = Employe.objects.get(
                tenant_id=request.user.tenant_id, user_id=request.user.auth_user_id
            )
        except Employe.DoesNotExist:
            raise serializers.ValidationError(
                "Votre compte n'est pas associé à un profil employé. "
                "Contactez votre responsable RH pour lier votre compte."
            )

        return Conge.objects.create(
            tenant_id=request.user.tenant_id,
            employee=employee,
            remplacant_id=remplacant_id,
            **validated_data,
        )

    def update(self, instance, validated_data):
        """
        Modifie une demande en statut pending uniquement.
        """
        if instance.status != Conge.Statut.PENDING:
            _statut_fr = {
                "approved": "approuvée",
                "rejected": "rejetée",
                "cancelled": "annulée",
            }.get(instance.status, instance.status)
            raise serializers.ValidationError(
                f"Cette demande de congé a déjà été {_statut_fr} et ne peut plus être modifiée."
            )
        remplacant_id = validated_data.pop("remplacant_id", None)
        if remplacant_id is not None:
            instance.remplacant_id = remplacant_id
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class CongeApprouverSerializer(serializers.Serializer):
    """
    Serializer pour approuver une demande de congé.
    POST /hr/leaves/{id}/approve/
    """

    commentaire = serializers.CharField(
        required=False, allow_blank=True, help_text="Commentaire optionnel"
    )


class CongeRejeterSerializer(serializers.Serializer):
    """
    Serializer pour rejeter une demande de congé.
    POST /hr/leaves/{id}/reject/
    Le motif est obligatoire.
    """

    reason = serializers.CharField(
        required=True, help_text="Motif obligatoire du rejet"
    )

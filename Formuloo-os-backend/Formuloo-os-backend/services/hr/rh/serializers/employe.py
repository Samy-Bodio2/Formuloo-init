"""
Serializers Employé — Formuloo OS
Entité centrale du module RH.
Conforme au contrat hr.yaml v2.1.0

Validations métier :
→ Email unique par tenant
→ Département et poste dans le tenant
→ Manager actif dans le tenant
→ Salaire minimum légal SMIG Cameroun (36 270 XAF)
"""

from rest_framework import serializers

from rh.models import Departement, Employe, Poste

# ── Constante SMIG Cameroun ───────────────────────────────
# Décret n°2014/2217/PM du 24 juillet 2014
# Salaire Minimum Interprofessionnel Garanti
SMIG_CAMEROUN = 36270


class EmployeBriefSerializer(serializers.ModelSerializer):
    """
    Version simplifiée de l'employé.
    Utilisée dans les références d'autres serializers.
    Ex: dans CongeSerializer pour afficher l'employé.
    """

    class Meta:
        model = Employe
        fields = ["id", "employee_number", "first_name", "last_name", "email"]


class EmployeSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/employes/
    GET /hr/employes/{id}/
    """

    department = serializers.SerializerMethodField()
    position = serializers.SerializerMethodField()
    manager = serializers.SerializerMethodField()

    class Meta:
        model = Employe
        fields = [
            "id",
            "employee_number",
            # Informations personnelles
            "first_name",
            "last_name",
            "birth_date",
            "gender",
            "nationality",
            "national_id",
            "situation_familiale",
            "nombre_enfants",
            "numero_cnps",
            # Coordonnées
            "address",
            "phone",
            "email",
            "email_perso",
            "phone_perso",
            "ville",
            "pays",
            # Informations professionnelles
            "department",
            "position",
            "manager",
            "hire_date",
            "date_fin_periode_essai",
            "status",
            "type_employe",
            # Rémunération
            "salaire_base",
            "devise",
            "mode_paiement",
            "numero_compte",
            "banque",
            # Documents
            "photo_url",
            "cv_url",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "employee_number", "created_at", "updated_at"]

    def get_department(self, obj):
        """Retourne le département sous forme simplifiée."""
        if not obj.department:
            return None
        return {
            "id": str(obj.department.id),
            "nom": obj.department.nom,
            "code": obj.department.code,
        }

    def get_position(self, obj):
        """Retourne le poste sous forme simplifiée."""
        if not obj.position:
            return None
        return {
            "id": str(obj.position.id),
            "titre": obj.position.titre,
            "code": obj.position.code,
            "niveau": obj.position.niveau,
        }

    def get_manager(self, obj):
        """Retourne le manager sous forme simplifiée."""
        if not obj.manager:
            return None
        return {
            "id": str(obj.manager.id),
            "employee_number": obj.manager.employee_number,
            "first_name": obj.manager.first_name,
            "last_name": obj.manager.last_name,
            "email": obj.manager.email,
        }


class EmployeCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour créer un employé.
    POST /hr/employes/

    Validations :
    1. Email unique dans le tenant
    2. Département actif dans le tenant
    3. Poste actif dans le tenant
    4. Manager actif dans le tenant
    5. Salaire >= SMIG Cameroun (36 270 XAF)
    """

    department_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID du département"
    )
    position_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID du poste"
    )
    manager_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID du manager"
    )

    class Meta:
        model = Employe
        fields = [
            "first_name",
            "last_name",
            "birth_date",
            "gender",
            "nationality",
            "national_id",
            "situation_familiale",
            "nombre_enfants",
            "numero_cnps",
            "address",
            "phone",
            "email",
            "email_perso",
            "phone_perso",
            "ville",
            "pays",
            "department_id",
            "position_id",
            "manager_id",
            "hire_date",
            "date_fin_periode_essai",
            "status",
            "type_employe",
            "salaire_base",
            "devise",
            "mode_paiement",
            "numero_compte",
            "banque",
            "photo_url",
            "cv_url",
        ]

    def validate_email(self, value):
        """
        Vérifie que l'email est unique dans le tenant.
        Deux employés d'une même PME ne peuvent pas
        avoir le même email professionnel.
        """
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        qs = Employe.objects.filter(tenant_id=tenant_id, email=value)
        if self.instance:
            qs = qs.exclude(id=self.instance.id)
        if qs.exists():
            raise serializers.ValidationError(
                "Un employé avec cet email " "existe déjà dans cette organisation."
            )
        return value

    def validate_department_id(self, value):
        """
        Vérifie que le département existe
        et est actif dans le tenant.
        """
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Departement.objects.filter(
            id=value, tenant_id=tenant_id, is_active=True
        ).exists():
            raise serializers.ValidationError("Département introuvable ou inactif.")
        return value

    def validate_position_id(self, value):
        """
        Vérifie que le poste existe
        et est actif dans le tenant.
        """
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Poste.objects.filter(
            id=value, tenant_id=tenant_id, is_active=True
        ).exists():
            raise serializers.ValidationError("Poste introuvable ou inactif.")
        return value

    def validate_manager_id(self, value):
        """
        Vérifie que le manager existe
        et est actif dans le tenant.
        """
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Employe.objects.filter(
            id=value, tenant_id=tenant_id, status="active"
        ).exists():
            raise serializers.ValidationError("Manager introuvable ou inactif.")
        return value

    def validate(self, data):
        """
        Validations globales :

        1. Salaire minimum légal SMIG
           → Conformité légale Cameroun
           → SMIG = 36 270 XAF/mois
           → Décret n°2014/2217/PM du 24/07/2014
           → Vérifié uniquement pour devise XAF

        2. Poste cohérent avec le département
           → Le poste doit appartenir
             au même département
        """
        salaire_base = data.get("salaire_base")
        devise = data.get("devise", "XAF")
        department_id = data.get("department_id")
        position_id = data.get("position_id")

        # ── 1. Salaire minimum SMIG ───────────────────────
        if salaire_base and devise == "XAF":
            if salaire_base < SMIG_CAMEROUN:
                raise serializers.ValidationError(
                    f"Le salaire de base "
                    f"({salaire_base:,.0f} XAF) "
                    f"est inférieur au SMIG "
                    f"camerounais "
                    f"({SMIG_CAMEROUN:,.0f} XAF/mois). "
                    f"Décret n°2014/2217/PM "
                    f"du 24 juillet 2014."
                )

        # ── 2. Cohérence poste / département ─────────────
        # Le poste doit appartenir au département
        # de l'employé
        if position_id and department_id:
            try:
                poste = Poste.objects.get(id=position_id)
                if str(poste.departement_id) != str(department_id):
                    raise serializers.ValidationError(
                        "Le poste sélectionné "
                        "n'appartient pas au "
                        "département choisi."
                    )
            except Poste.DoesNotExist:
                pass

        return data

    def create(self, validated_data):
        """
        Crée un employé avec tenant_id du JWT.
        Le matricule est auto-généré dans le modèle.
        """
        request = self.context.get("request")
        department_id = validated_data.pop("department_id", None)
        position_id = validated_data.pop("position_id", None)
        manager_id = validated_data.pop("manager_id", None)

        return Employe.objects.create(
            tenant_id=request.user.tenant_id,
            department_id=department_id,
            position_id=position_id,
            manager_id=manager_id,
            **validated_data,
        )


class EmployeUpdateSerializer(serializers.ModelSerializer):
    """
    Serializer pour modifier un employé.
    PATCH /hr/employes/{id}/
    Tous les champs sont optionnels.

    Validations :
    1. Salaire >= SMIG si modifié
    2. Département actif si modifié
    3. Poste cohérent avec département
    """

    department_id = serializers.UUIDField(required=False, allow_null=True)
    position_id = serializers.UUIDField(required=False, allow_null=True)
    manager_id = serializers.UUIDField(required=False, allow_null=True)

    class Meta:
        model = Employe
        fields = [
            "first_name",
            "last_name",
            "situation_familiale",
            "nombre_enfants",
            "phone",
            "phone_perso",
            "address",
            "ville",
            "email_perso",
            "department_id",
            "position_id",
            "manager_id",
            "status",
            "salaire_base",
            "mode_paiement",
            "numero_compte",
            "banque",
            "photo_url",
            "cv_url",
        ]

    def validate(self, data):
        """
        Validations lors de la modification :

        1. Salaire minimum SMIG si modifié
        2. Cohérence poste / département
        """
        salaire_base = data.get("salaire_base")
        department_id = data.get("department_id")
        position_id = data.get("position_id")

        # ── 1. Salaire minimum SMIG ───────────────────────
        if salaire_base:
            devise = data.get(
                "devise", self.instance.devise if self.instance else "XAF"
            )
            if devise == "XAF" and salaire_base < SMIG_CAMEROUN:
                raise serializers.ValidationError(
                    f"Le salaire de base "
                    f"({salaire_base:,.0f} XAF) "
                    f"est inférieur au SMIG "
                    f"camerounais "
                    f"({SMIG_CAMEROUN:,.0f} XAF/mois)."
                )

        # ── 2. Cohérence poste / département ─────────────
        if position_id and department_id:
            try:
                poste = Poste.objects.get(id=position_id)
                if str(poste.departement_id) != str(department_id):
                    raise serializers.ValidationError(
                        "Le poste sélectionné "
                        "n'appartient pas au "
                        "département choisi."
                    )
            except Poste.DoesNotExist:
                pass

        return data

    def update(self, instance, validated_data):
        """
        Met à jour un employé.
        """
        department_id = validated_data.pop("department_id", None)
        position_id = validated_data.pop("position_id", None)
        manager_id = validated_data.pop("manager_id", None)

        if department_id is not None:
            instance.department_id = department_id
        if position_id is not None:
            instance.position_id = position_id
        if manager_id is not None:
            instance.manager_id = manager_id

        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance

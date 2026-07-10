"""
Serializers Département — Formuloo OS
Conforme au contrat hr.yaml v2.1.0
"""

from rest_framework import serializers

from rh.models import Departement, Employe


class DepartementBriefSerializer(serializers.ModelSerializer):
    """
    Version simplifiée du département.
    Utilisée dans les références d'autres serializers.
    Ex: dans EmployeSerializer pour afficher le département.
    """

    class Meta:
        model = Departement
        fields = ["id", "nom", "code"]


class DepartementSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/departements/
    GET /hr/departements/{id}/
    """

    # Département parent simplifié
    parent = DepartementBriefSerializer(read_only=True)

    # Responsable simplifié — import circulaire évité
    responsable = serializers.SerializerMethodField()

    # Calculé dynamiquement — pas en base
    nb_employes = serializers.SerializerMethodField()

    class Meta:
        model = Departement
        fields = [
            "id",
            "nom",
            "code",
            "description",
            "parent",
            "responsable",
            "budget",
            "devise",
            "nb_employes",
            "is_active",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]

    def get_responsable(self, obj):
        """Retourne le responsable sous forme simplifiée."""
        if not obj.responsable:
            return None
        return {
            "id": str(obj.responsable.id),
            "employee_number": obj.responsable.employee_number,
            "first_name": obj.responsable.first_name,
            "last_name": obj.responsable.last_name,
            "email": obj.responsable.email,
        }

    def get_nb_employes(self, obj):
        """Compte les employés actifs du département."""
        return Employe.objects.filter(department=obj, status="active").count()


class DepartementCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour créer/modifier un département.
    POST /hr/departements/
    PUT  /hr/departements/{id}/
    """

    parent_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID du département parent"
    )
    responsable_id = serializers.UUIDField(
        required=False, allow_null=True, help_text="UUID de l'employé responsable"
    )

    class Meta:
        model = Departement
        fields = [
            "nom",
            "code",
            "description",
            "parent_id",
            "responsable_id",
            "budget",
            "devise",
        ]

    def validate_code(self, value):
        """
        Vérifie que le code est unique dans le tenant.
        """
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        qs = Departement.objects.filter(tenant_id=tenant_id, code=value)
        # En modification on exclut l'instance courante
        if self.instance:
            qs = qs.exclude(id=self.instance.id)
        if qs.exists():
            raise serializers.ValidationError(
                "Un département avec ce code existe déjà."
            )
        return value

    def validate_parent_id(self, value):
        """
        Vérifie que le département parent existe
        dans le même tenant.
        """
        if not value:
            return value
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Departement.objects.filter(id=value, tenant_id=tenant_id).exists():
            raise serializers.ValidationError("Département parent introuvable.")
        return value

    def create(self, validated_data):
        """
        Crée un département avec tenant_id du JWT.
        """
        request = self.context.get("request")
        parent_id = validated_data.pop("parent_id", None)
        responsable_id = validated_data.pop("responsable_id", None)

        departement = Departement.objects.create(
            tenant_id=request.user.tenant_id,
            parent_id=parent_id,
            responsable_id=responsable_id,
            **validated_data,
        )
        return departement

    def update(self, instance, validated_data):
        """
        Met à jour un département.
        """
        parent_id = validated_data.pop("parent_id", None)
        responsable_id = validated_data.pop("responsable_id", None)

        if parent_id is not None:
            instance.parent_id = parent_id
        if responsable_id is not None:
            instance.responsable_id = responsable_id

        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class DepartementTreeSerializer(serializers.ModelSerializer):
    """
    Serializer pour l'organigramme hiérarchique.
    GET /hr/departements/tree/
    Récursif — retourne les sous-départements imbriqués.
    """

    nb_employes = serializers.SerializerMethodField()
    sous_departements = serializers.SerializerMethodField()

    class Meta:
        model = Departement
        fields = ["id", "nom", "code", "nb_employes", "sous_departements"]

    def get_nb_employes(self, obj):
        return Employe.objects.filter(department=obj, status="active").count()

    def get_sous_departements(self, obj):
        """
        Retourne les sous-départements actifs de manière récursive.
        """
        enfants = obj.sous_departements.filter(is_active=True)
        return DepartementTreeSerializer(enfants, many=True, context=self.context).data

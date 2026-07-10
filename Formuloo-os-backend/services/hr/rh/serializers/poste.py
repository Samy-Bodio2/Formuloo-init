"""
Serializers Poste — Formuloo OS
Conforme au contrat hr.yaml v2.1.0
"""

from rest_framework import serializers

from rh.models import Departement, Poste


class PosteBriefSerializer(serializers.ModelSerializer):
    """
    Version simplifiée du poste.
    Utilisée dans les références d'autres serializers.
    Ex: dans EmployeSerializer pour afficher le poste.
    """

    class Meta:
        model = Poste
        fields = ["id", "titre", "code", "niveau"]


class PosteSerializer(serializers.ModelSerializer):
    """
    Serializer complet pour liste et détail.
    GET /hr/postes/
    GET /hr/postes/{id}/
    """

    departement = serializers.SerializerMethodField()
    nb_employes = serializers.SerializerMethodField()

    class Meta:
        model = Poste
        fields = [
            "id",
            "departement",
            "titre",
            "code",
            "description",
            "niveau",
            "salaire_min",
            "salaire_max",
            "devise",
            "nb_employes",
            "is_active",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]

    def get_departement(self, obj):
        """Retourne le département sous forme simplifiée."""
        if not obj.departement:
            return None
        return {
            "id": str(obj.departement.id),
            "nom": obj.departement.nom,
            "code": obj.departement.code,
        }

    def get_nb_employes(self, obj):
        """Compte les employés actifs occupant ce poste."""
        return obj.employes.filter(status="active").count()


class PosteCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour créer/modifier un poste.
    POST /hr/postes/
    PUT  /hr/postes/{id}/
    """

    departement_id = serializers.UUIDField(help_text="UUID du département")

    class Meta:
        model = Poste
        fields = [
            "titre",
            "code",
            "departement_id",
            "description",
            "niveau",
            "salaire_min",
            "salaire_max",
            "devise",
        ]

    def validate_code(self, value):
        """
        Vérifie que le code est unique dans le tenant.
        """
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        qs = Poste.objects.filter(tenant_id=tenant_id, code=value)
        if self.instance:
            qs = qs.exclude(id=self.instance.id)
        if qs.exists():
            raise serializers.ValidationError(
                "Ce code est déjà utilisé par un autre poste. Choisissez un code différent."
            )
        return value

    def validate_departement_id(self, value):
        """
        Vérifie que le département existe dans le tenant.
        """
        request = self.context.get("request")
        tenant_id = request.user.tenant_id
        if not Departement.objects.filter(
            id=value, tenant_id=tenant_id, is_active=True
        ).exists():
            raise serializers.ValidationError(
                "Le département sélectionné est introuvable ou désactivé. "
                "Vérifiez la liste des départements actifs."
            )
        return value

    def validate(self, data):
        """
        Vérifie que salaire_min <= salaire_max.
        """
        salaire_min = data.get("salaire_min")
        salaire_max = data.get("salaire_max")
        if salaire_min and salaire_max:
            if salaire_min > salaire_max:
                raise serializers.ValidationError(
                    "Le salaire minimum ne peut pas "
                    "être supérieur au salaire maximum."
                )
        return data

    def create(self, validated_data):
        """Crée un poste avec tenant_id du JWT."""
        request = self.context.get("request")
        departement_id = validated_data.pop("departement_id")

        return Poste.objects.create(
            tenant_id=request.user.tenant_id,
            departement_id=departement_id,
            **validated_data,
        )

    def update(self, instance, validated_data):
        """Met à jour un poste."""
        departement_id = validated_data.pop("departement_id", None)
        if departement_id:
            instance.departement_id = departement_id
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance

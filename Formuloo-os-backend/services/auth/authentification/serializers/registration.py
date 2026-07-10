"""
Serializers d'inscription et de gestion de compte — Formuloo OS

Couvre les flux hors-session (pas de JWT requis) :
  - RegisterSerializer          : création d'une PME + admin RH Manager
  - VerifyEmailSerializer       : vérification de l'adresse email par token
  - ResendVerificationSerializer: renvoi du lien de vérification
  - InviteSerializer            : invitation d'un employé par le RH Manager
  - AcceptInvitationSerializer  : activation du compte via le lien d'invitation
  - ForgotPasswordSerializer    : demande de réinitialisation de mot de passe
  - ResetPasswordSerializer     : réinitialisation effective du mot de passe
"""

from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework import serializers

from authentification.models import OneTimeToken, Organisation, Role, User


class RegisterSerializer(serializers.Serializer):
    """
    Inscription publique : crée une nouvelle PME (Organisation)
    et son premier utilisateur avec le rôle RH_MANAGER.

    C'est le seul endpoint Auth qui ne nécessite pas de JWT.
    Un email de vérification est envoyé après la création.
    """

    # ── Données de l'organisation ──────────────────
    company_name = serializers.CharField(
        max_length=255,
        help_text="Nom de la PME (ex: PME Cameroun SARL)",
    )
    company_slug = serializers.SlugField(
        max_length=100,
        help_text="Identifiant URL unique de la PME (ex: pme-cameroun)",
    )

    # ── Données du premier utilisateur (admin) ─────
    first_name = serializers.CharField(max_length=100)
    last_name = serializers.CharField(max_length=100)
    email = serializers.EmailField()
    password = serializers.CharField(
        write_only=True,
        min_length=8,
        style={"input_type": "password"},
    )
    confirm_password = serializers.CharField(
        write_only=True,
        style={"input_type": "password"},
    )

    def validate_company_slug(self, value: str) -> str:
        """Vérifie que le slug n'est pas déjà pris."""
        if Organisation.objects.filter(slug=value).exists():
            raise serializers.ValidationError(
                "Ce slug est déjà utilisé par une autre organisation."
            )
        return value

    def validate_email(self, value: str) -> str:
        """Vérifie que l'email n'est pas déjà enregistré."""
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("Cet email est déjà utilisé.")
        return value

    def validate(self, data: dict) -> dict:
        """Vérifie la cohérence des mots de passe et leur solidité."""
        if data["password"] != data["confirm_password"]:
            raise serializers.ValidationError(
                {"confirm_password": "Les mots de passe ne correspondent pas."}
            )
        # Appliquer les validateurs Django (longueur, complexité, etc.)
        try:
            validate_password(data["password"])
        except DjangoValidationError as e:
            raise serializers.ValidationError({"password": list(e.messages)})
        return data

    def create(self, validated_data: dict) -> dict:
        """
        Crée l'Organisation et son premier utilisateur RH_MANAGER.
        Retourne un dict contenant les deux instances créées.
        """
        validated_data.pop("confirm_password")

        # Créer l'organisation (tenant)
        organisation = Organisation.objects.create(
            name=validated_data["company_name"],
            slug=validated_data["company_slug"],
        )

        # Créer l'utilisateur admin — is_verified=False jusqu'au clic email
        user = User.objects.create_user(
            email=validated_data["email"],
            tenant=organisation,
            password=validated_data["password"],
            first_name=validated_data["first_name"],
            last_name=validated_data["last_name"],
            is_verified=False,
        )

        # Attribuer le rôle RH_MANAGER (rôle système)
        rh_manager_role = Role.objects.filter(code="RH_MANAGER").first()
        if rh_manager_role:
            user.roles.add(rh_manager_role)

        return {"organisation": organisation, "user": user}


class VerifyEmailSerializer(serializers.Serializer):
    """
    Vérifie l'adresse email d'un utilisateur via le code à 6 chiffres
    reçu par email après l'inscription ou après renvoi.
    """

    code = serializers.RegexField(
        regex=r"^\d{6}$",
        help_text="Code à 6 chiffres reçu par email",
    )

    def validate_code(self, value: str) -> OneTimeToken:
        """
        Cherche et valide le code VERIFY_EMAIL.
        Retourne l'instance OneTimeToken si valide.
        Lève une erreur explicite sinon.
        """
        try:
            ott = OneTimeToken.objects.select_related("user").get(
                code=value,
                token_type=OneTimeToken.Type.VERIFY_EMAIL,
            )
        except OneTimeToken.DoesNotExist:
            raise serializers.ValidationError("Code invalide.")

        if not ott.is_valid():
            raise serializers.ValidationError("Ce code a expiré ou a déjà été utilisé.")

        # On stocke l'objet pour y accéder dans la view sans second lookup
        return ott


class ResendVerificationSerializer(serializers.Serializer):
    """
    Renvoie un email de vérification à l'adresse fournie.
    Ne révèle pas si l'email existe (protection contre l'énumération).
    """

    email = serializers.EmailField()


class InviteSerializer(serializers.Serializer):
    """
    Invitation d'un employé par le RH Manager.

    Crée un compte User inactif (sans mot de passe)
    et envoie un lien d'activation par email.
    L'employé définit son mot de passe en acceptant l'invitation.
    """

    first_name = serializers.CharField(max_length=100)
    last_name = serializers.CharField(max_length=100)
    email = serializers.EmailField()
    # Liste des codes de rôle à assigner (ex: ["EMPLOYE"])
    roles = serializers.ListField(
        child=serializers.CharField(max_length=50),
        min_length=1,
        help_text='Liste des codes de rôle à assigner (ex: ["EMPLOYE", "MANAGER"])',
    )

    def validate_email(self, value: str) -> str:
        """L'email doit être libre dans toute la plateforme."""
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("Un compte existe déjà avec cet email.")
        return value

    def validate_roles(self, value: list) -> list:
        """
        Vérifie que tous les codes de rôle soumis existent.
        Les rôles inexistants provoquent une erreur explicite.
        """
        valid_codes = set(Role.objects.values_list("code", flat=True))
        invalid = [r for r in value if r not in valid_codes]
        if invalid:
            raise serializers.ValidationError(
                f"Rôles inconnus : {', '.join(invalid)}. "
                f"Rôles disponibles : {', '.join(sorted(valid_codes))}"
            )
        return value


class AcceptInvitationSerializer(serializers.Serializer):
    """
    Activation du compte employé via le token d'invitation.
    L'employé choisit son mot de passe lors de cette étape.
    """

    code = serializers.RegexField(
        regex=r"^\d{6}$",
        help_text="Code à 6 chiffres reçu par email",
    )
    password = serializers.CharField(
        write_only=True,
        min_length=8,
        style={"input_type": "password"},
    )
    confirm_password = serializers.CharField(
        write_only=True,
        style={"input_type": "password"},
    )

    def validate_code(self, value: str) -> OneTimeToken:
        """Valide le code INVITATION."""
        try:
            ott = OneTimeToken.objects.select_related("user").get(
                code=value,
                token_type=OneTimeToken.Type.INVITATION,
            )
        except OneTimeToken.DoesNotExist:
            raise serializers.ValidationError("Code d'invitation invalide.")

        if not ott.is_valid():
            raise serializers.ValidationError(
                "Ce lien d'invitation a expiré ou a déjà été utilisé."
            )
        return ott

    def validate(self, data: dict) -> dict:
        if data["password"] != data["confirm_password"]:
            raise serializers.ValidationError(
                {"confirm_password": "Les mots de passe ne correspondent pas."}
            )
        try:
            validate_password(data["password"])
        except DjangoValidationError as e:
            raise serializers.ValidationError({"password": list(e.messages)})
        return data


class ForgotPasswordSerializer(serializers.Serializer):
    """
    Demande de réinitialisation du mot de passe.
    Envoie un lien de reset par email si le compte existe.
    Ne révèle jamais si l'email est connu (protection anti-énumération).
    """

    email = serializers.EmailField()


class ResetPasswordSerializer(serializers.Serializer):
    """
    Réinitialisation effective du mot de passe via le token de reset.
    """

    code = serializers.RegexField(
        regex=r"^\d{6}$",
        help_text="Code à 6 chiffres reçu par email",
    )
    nouveau_mot_de_passe = serializers.CharField(
        write_only=True,
        min_length=8,
        style={"input_type": "password"},
    )
    confirm_mot_de_passe = serializers.CharField(
        write_only=True,
        style={"input_type": "password"},
    )

    def validate_code(self, value: str) -> OneTimeToken:
        """Valide le code RESET_PASSWORD (2h d'expiry)."""
        try:
            ott = OneTimeToken.objects.select_related("user").get(
                code=value,
                token_type=OneTimeToken.Type.RESET_PASSWORD,
            )
        except OneTimeToken.DoesNotExist:
            raise serializers.ValidationError("Code de réinitialisation invalide.")

        if not ott.is_valid():
            raise serializers.ValidationError(
                "Ce lien a expiré (2h) ou a déjà été utilisé."
            )
        return ott

    def validate(self, data: dict) -> dict:
        if data["nouveau_mot_de_passe"] != data["confirm_mot_de_passe"]:
            raise serializers.ValidationError(
                {"confirm_mot_de_passe": "Les mots de passe ne correspondent pas."}
            )
        try:
            validate_password(data["nouveau_mot_de_passe"])
        except DjangoValidationError as e:
            raise serializers.ValidationError(
                {"nouveau_mot_de_passe": list(e.messages)}
            )
        return data

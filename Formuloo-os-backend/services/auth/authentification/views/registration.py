"""
Views d'inscription et de gestion de compte — Formuloo OS

Flux hors-session (AllowAny) :
  POST /register/              → RegisterView
  POST /verify-email/          → VerifyEmailView
  POST /resend-verification/   → ResendVerificationView
  POST /forgot-password/       → ForgotPasswordView
  POST /reset-password/        → ResetPasswordView

Flux protégé (IsAuthenticated + RH_MANAGER) :
  POST /invite/                → InviteView
  POST /invite/accept/         → AcceptInvitationView (AllowAny — lien email)

Les emails sont envoyés via Django's email backend.
En dev : EMAIL_BACKEND = console → apparaît dans les logs Docker.
En prod : EMAIL_BACKEND = smtp  → configurer EMAIL_HOST_USER etc.

Conforme ADR-002 : authentification SSO + JWT
"""

import logging
import re

from django.conf import settings
from django.core.mail import send_mail
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from authentification.models import AuditLog, OneTimeToken, Permission, Role, User
from authentification.serializers.registration import (
    AcceptInvitationSerializer,
    ForgotPasswordSerializer,
    InviteSerializer,
    RegisterSerializer,
    ResendVerificationSerializer,
    ResetPasswordSerializer,
    VerifyEmailSerializer,
)

logger = logging.getLogger(__name__)


def _send_verification_email(user: User, request=None):
    """
    Génère un token VERIFY_EMAIL et envoie l'email de vérification.

    En dev le mail s'affiche dans stdout (console backend).
    Le lien pointe vers FRONTEND_URL/verify-email?token=<uuid>.

    Args:
        user    : instance User dont l'email doit être vérifié
        request : requête HTTP (non utilisée ici, gardée pour extensibilité)
    """
    ott = OneTimeToken.create_for(user, OneTimeToken.Type.VERIFY_EMAIL)
    verify_url = f"{settings.FRONTEND_URL}/verify-email?token={ott.token}"

    send_mail(
        subject="Formuloo OS — Vérification de votre adresse email",
        message=(
            f"Bonjour {user.first_name},\n\n"
            f"Votre code de vérification : {ott.code}\n\n"
            f"Ou cliquez sur le lien ci-dessous pour activer votre compte :\n"
            f"{verify_url}\n\n"
            f"Le code et le lien expirent dans 48h.\n\n"
            f"L'équipe Formuloo OS"
        ),
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[user.email],
        fail_silently=False,
    )
    return ott


def _send_invitation_email(user: User, invited_by: User):
    """
    Génère un token INVITATION et envoie le code d'activation
    à l'employé invité.

    Le code à 6 chiffres se saisit directement dans l'app (pas de deep
    linking natif pour l'instant) — le lien web reste fourni en secours.

    Args:
        user       : employé invité (compte sans mot de passe)
        invited_by : RH Manager qui a lancé l'invitation
    """
    ott = OneTimeToken.create_for(user, OneTimeToken.Type.INVITATION)
    invite_url = f"{settings.FRONTEND_URL}/invite/accept?code={ott.code}"

    send_mail(
        subject=f"Formuloo OS — Invitation de {invited_by.get_full_name()}",
        message=(
            f"Bonjour {user.first_name},\n\n"
            f"{invited_by.get_full_name()} vous invite à rejoindre "
            f"{invited_by.tenant.name} sur Formuloo OS.\n\n"
            f"Votre code d'invitation : {ott.code}\n\n"
            f"Saisissez ce code dans l'application pour créer votre mot de passe "
            f"et activer votre compte.\n\n"
            f"Ou cliquez sur le lien ci-dessous :\n"
            f"{invite_url}\n\n"
            f"Le code et le lien expirent dans 72h.\n\n"
            f"L'équipe Formuloo OS"
        ),
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[user.email],
        fail_silently=False,
    )
    return ott


def _send_reset_email(user: User):
    """
    Génère un token RESET_PASSWORD et envoie le code de réinitialisation.

    Le code à 6 chiffres se saisit directement dans l'app (pas de deep
    linking natif pour l'instant) — le lien web reste fourni en secours.
    Expire après 2h.

    Args:
        user : instance User qui a demandé le reset
    """
    ott = OneTimeToken.create_for(user, OneTimeToken.Type.RESET_PASSWORD)
    reset_url = f"{settings.FRONTEND_URL}/reset-password?code={ott.code}"

    send_mail(
        subject="Formuloo OS — Réinitialisation de votre mot de passe",
        message=(
            f"Bonjour {user.first_name},\n\n"
            f"Vous avez demandé la réinitialisation de votre mot de passe.\n\n"
            f"Votre code de réinitialisation : {ott.code}\n\n"
            f"Saisissez ce code dans l'application (valable 2h).\n\n"
            f"Ou cliquez sur le lien ci-dessous :\n"
            f"{reset_url}\n\n"
            f"Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n"
            f"L'équipe Formuloo OS"
        ),
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[user.email],
        fail_silently=False,
    )
    return ott


# ─────────────────────────────────────────────────────────────────────────────
# REGISTER
# ─────────────────────────────────────────────────────────────────────────────


class RegisterView(APIView):
    """
    POST /api/v1/auth/register/

    Inscription publique : crée une PME + son premier utilisateur RH Manager.
    Un email de vérification est envoyé immédiatement.

    Aucun JWT requis — endpoint public.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Inscription — créer une PME",
        description=(
            "Crée une nouvelle organisation (tenant) et son administrateur RH Manager. "
            "Un email de vérification est envoyé à l'adresse fournie. "
            "Le compte est actif mais non vérifié jusqu'au clic sur le lien."
        ),
        tags=["Inscription & Compte"],
        request=RegisterSerializer,
        responses={
            201: {"description": "Compte créé — email de vérification envoyé"},
            400: {
                "description": "Données invalides (slug déjà pris, email existant, etc.)"
            },
        },
    )
    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        result = serializer.save()

        user = result["user"]
        organisation = result["organisation"]

        # Envoyer le lien de vérification. Une panne du serveur SMTP ne doit pas
        # faire perdre le compte déjà créé ni renvoyer un 500 trompeur au client :
        # on log l'erreur et l'utilisateur pourra relancer via /resend-verification/.
        ott = None
        try:
            ott = _send_verification_email(user, request)
        except Exception:
            logger.exception(
                "Échec de l'envoi de l'email de vérification pour %s", user.email
            )

        # Logger l'inscription
        AuditLog.log(
            tenant=organisation,
            user=user,
            action="REGISTER",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email, "company": organisation.name},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        response_data = {
            "message": (
                "Compte créé avec succès. "
                "Un email de vérification a été envoyé à "
                f"{user.email}."
            ),
            "user": {
                "id": str(user.id),
                "email": user.email,
                "first_name": user.first_name,
                "last_name": user.last_name,
                "is_verified": user.is_verified,
                "tenant_id": str(organisation.id),
                "tenant_name": organisation.name,
            },
        }

        # En DEBUG, exposer le code de vérification dans la réponse : permet
        # de tester le flux d'inscription depuis un client (mobile, Postman...)
        # sans avoir accès aux logs du serveur (où le backend console écrit le mail).
        if settings.DEBUG and ott is not None:
            response_data["debug_verification_code"] = ott.code

        return Response(response_data, status=status.HTTP_201_CREATED)


# ─────────────────────────────────────────────────────────────────────────────
# VÉRIFICATION EMAIL
# ─────────────────────────────────────────────────────────────────────────────


class VerifyEmailView(APIView):
    """
    POST /api/v1/auth/verify-email/

    Valide l'adresse email via le code à 6 chiffres reçu par email.
    Passe is_verified=True sur le User.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Vérifier l'adresse email",
        description=(
            "Active le compte utilisateur via le code à 6 chiffres reçu par email. "
            "Le code est valide 48h et ne peut être utilisé qu'une seule fois."
        ),
        tags=["Inscription & Compte"],
        request=VerifyEmailSerializer,
        responses={
            200: {"description": "Email vérifié — compte activé"},
            400: {"description": "Code invalide ou expiré"},
        },
    )
    def post(self, request):
        serializer = VerifyEmailSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Le serializer a déjà résolu le code → OneTimeToken instance
        ott = serializer.validated_data["code"]
        user = ott.user

        # Consommer le token et activer l'email
        ott.use()
        user.is_verified = True
        user.save(update_fields=["is_verified", "updated_at"])

        AuditLog.log(
            tenant=user.tenant,
            user=user,
            action="VERIFY_EMAIL",
            resource="User",
            resource_id=user.id,
            payload={},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {
                "message": "Adresse email vérifiée avec succès. Vous pouvez maintenant vous connecter."
            },
            status=status.HTTP_200_OK,
        )


class ResendVerificationView(APIView):
    """
    POST /api/v1/auth/resend-verification/

    Renvoie un email de vérification.
    Si l'email n'existe pas ou est déjà vérifié, répond 200 quand même
    (protection contre l'énumération d'adresses).
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Renvoyer le lien de vérification email",
        description=(
            "Génère et envoie un nouveau lien de vérification. "
            "Répond toujours 200 même si l'email est inconnu (anti-énumération)."
        ),
        tags=["Inscription & Compte"],
        request=ResendVerificationSerializer,
        responses={
            200: {
                "description": "Email envoyé (si le compte existe et n'est pas vérifié)"
            },
        },
    )
    def post(self, request):
        serializer = ResendVerificationSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        email = serializer.validated_data["email"]
        user = User.objects.filter(email=email).first()

        ott = None
        # Envoyer uniquement si le compte existe et n'est pas encore vérifié
        if user and not user.is_verified:
            try:
                ott = _send_verification_email(user, request)
            except Exception:
                logger.exception(
                    "Échec du renvoi de l'email de vérification pour %s", email
                )

        response_data = {
            "message": (
                "Si un compte non vérifié existe pour cet email, "
                "un nouveau lien vient d'être envoyé."
            )
        }
        # En DEBUG, exposer le code pour faciliter les tests client (mobile/Postman).
        if settings.DEBUG and ott is not None:
            response_data["debug_verification_code"] = ott.code

        # Toujours retourner 200 — ne pas révéler si l'email existe
        return Response(
            response_data,
            status=status.HTTP_200_OK,
        )


# ─────────────────────────────────────────────────────────────────────────────
# INVITATION EMPLOYÉ
# ─────────────────────────────────────────────────────────────────────────────


class InviteView(APIView):
    """
    POST /api/v1/auth/invite/

    Le RH Manager invite un employé à créer son compte.
    Crée un User sans mot de passe dans le même tenant.
    Envoie un lien d'activation valable 72h.

    Requiert : JWT avec rôle RH_MANAGER.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Inviter un employé",
        description=(
            "Crée un compte employé inactif et envoie un lien d'invitation par email. "
            "L'employé définit son mot de passe en cliquant sur le lien (72h). "
            "Réservé au RH Manager."
        ),
        tags=["Inscription & Compte"],
        request=InviteSerializer,
        responses={
            201: {"description": "Invitation envoyée"},
            400: {"description": "Email déjà utilisé ou rôles invalides"},
            403: {"description": "Réservé au RH Manager"},
        },
    )
    def post(self, request):
        # Seul le RH Manager peut inviter des employés
        if "RH_MANAGER" not in list(request.user.roles.values_list("code", flat=True)):
            return Response(
                {"error": {"code": "FORBIDDEN", "message": "Réservé au RH Manager."}},
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = InviteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        # Créer l'utilisateur sans mot de passe — is_active=False jusqu'à l'activation
        invited_user = User.objects.create_user(
            email=data["email"],
            tenant=request.user.tenant,
            password=None,  # Pas de mot de passe — sera défini via le lien
            first_name=data["first_name"],
            last_name=data["last_name"],
            is_active=False,  # Actif uniquement après acceptance
            is_verified=False,
        )

        # Assigner les rôles demandés
        roles = Role.objects.filter(code__in=data["roles"])
        invited_user.roles.set(roles)

        # Envoyer l'email d'invitation
        ott = _send_invitation_email(invited_user, invited_by=request.user)

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="INVITE_USER",
            resource="User",
            resource_id=invited_user.id,
            payload={"invited_email": invited_user.email, "roles": data["roles"]},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        response_data = {
            "message": f"Invitation envoyée à {invited_user.email}.",
            "user": {
                "id": str(invited_user.id),
                "email": invited_user.email,
                "first_name": invited_user.first_name,
                "last_name": invited_user.last_name,
                "roles": data["roles"],
                "is_active": False,
            },
        }
        # En DEBUG, exposer le code pour faciliter les tests client (mobile/Postman).
        if settings.DEBUG:
            response_data["debug_invitation_code"] = ott.code

        return Response(response_data, status=status.HTTP_201_CREATED)


class AcceptInvitationView(APIView):
    """
    POST /api/v1/auth/invite/accept/

    L'employé invité définit son mot de passe via le token d'invitation.
    Passe is_active=True et is_verified=True sur son compte.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Accepter une invitation",
        description=(
            "Activates the invited employee account by setting a password. "
            "Le token d'invitation est valable 72h et à usage unique."
        ),
        tags=["Inscription & Compte"],
        request=AcceptInvitationSerializer,
        responses={
            200: {"description": "Compte activé — connexion possible"},
            400: {
                "description": "Token invalide/expiré ou mots de passe non conformes"
            },
        },
    )
    def post(self, request):
        serializer = AcceptInvitationSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        ott = serializer.validated_data["code"]
        user = ott.user

        # Consommer le token, définir le mot de passe, activer le compte
        ott.use()
        user.set_password(serializer.validated_data["password"])
        user.is_active = True
        user.is_verified = True
        user.save(update_fields=["password", "is_active", "is_verified", "updated_at"])

        AuditLog.log(
            tenant=user.tenant,
            user=user,
            action="ACCEPT_INVITATION",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {
                "message": "Compte activé avec succès. Vous pouvez maintenant vous connecter.",
                "email": user.email,
            },
            status=status.HTTP_200_OK,
        )


class InvitePreviewView(APIView):
    """
    GET /api/v1/auth/invite/preview/?code=123456

    Prévisualise une invitation avant acceptation : organisation, rôle,
    modules accessibles, nom de l'inviteur, expiration. Ne consomme pas
    le code (contrairement à AcceptInvitationView).
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @staticmethod
    def _initials(name: str) -> str:
        words = name.split()
        return "".join(w[0] for w in words[:2]).upper()

    @extend_schema(
        summary="Prévisualiser une invitation",
        description=(
            "Retourne les informations de l'invitation (organisation, rôle, "
            "modules, inviteur, expiration) à partir du code à 6 chiffres, sans le consommer."
        ),
        tags=["Inscription & Compte"],
        parameters=[
            OpenApiParameter(
                name="code",
                type=str,
                location=OpenApiParameter.QUERY,
                required=True,
                description="Code à 6 chiffres reçu par email",
            ),
        ],
        responses={
            200: {"description": "Invitation valide — détails retournés"},
            400: {"description": "Code manquant, invalide ou invitation expirée/déjà utilisée"},
            404: {"description": "Code introuvable"},
        },
    )
    def get(self, request):
        code = request.query_params.get("code", "")
        if not re.fullmatch(r"\d{6}", code):
            return Response(
                {"error": {"code": "VALIDATION_ERROR", "message": "Code à 6 chiffres requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            ott = OneTimeToken.objects.select_related("user", "user__tenant").get(
                code=code, token_type=OneTimeToken.Type.INVITATION
            )
        except OneTimeToken.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Invitation introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if not ott.is_valid():
            return Response(
                {
                    "error": {
                        "code": "INVALID_TOKEN",
                        "message": "Cette invitation a expiré ou a déjà été utilisée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        invited_user = ott.user
        organisation = invited_user.tenant
        role = invited_user.roles.first()
        modules = (
            Permission.objects.filter(roles__in=invited_user.roles.all())
            .values_list("module", flat=True)
            .distinct()
            .order_by("module")
        )

        invited_by_name = None
        invite_log = (
            AuditLog.objects.filter(
                tenant=organisation,
                action="INVITE_USER",
                resource="User",
                resource_id=invited_user.id,
            )
            .order_by("-timestamp")
            .first()
        )
        if invite_log is not None:
            invited_by_name = invite_log.user.get_full_name()

        return Response(
            {
                "email": invited_user.email,
                "first_name": invited_user.first_name,
                "last_name": invited_user.last_name,
                "organisation": {
                    "name": organisation.name,
                    "initials": self._initials(organisation.name),
                },
                "role": role.name if role else None,
                "modules": list(modules),
                "invited_by": invited_by_name,
                "expires_at": ott.expires_at.isoformat(),
            },
            status=status.HTTP_200_OK,
        )


# ─────────────────────────────────────────────────────────────────────────────
# MOT DE PASSE OUBLIÉ / RESET
# ─────────────────────────────────────────────────────────────────────────────


class ForgotPasswordView(APIView):
    """
    POST /api/v1/auth/forgot-password/

    Envoie un email de réinitialisation si le compte existe.
    Répond toujours 200 (protection anti-énumération).
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Mot de passe oublié",
        description=(
            "Envoie un lien de réinitialisation par email si le compte existe. "
            "Répond toujours 200 pour ne pas révéler si l'email est connu."
        ),
        tags=["Inscription & Compte"],
        request=ForgotPasswordSerializer,
        responses={
            200: {"description": "Email envoyé si le compte existe"},
        },
    )
    def post(self, request):
        serializer = ForgotPasswordSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        email = serializer.validated_data["email"]
        user = User.objects.filter(email=email, is_active=True).first()

        ott = None
        if user:
            try:
                ott = _send_reset_email(user)
                logger.info("Password reset requested for %s", email)
            except Exception:
                logger.exception(
                    "Échec de l'envoi de l'email de réinitialisation pour %s", email
                )

        response_data = {
            "message": (
                "Si un compte actif existe pour cet email, "
                "un lien de réinitialisation vient d'être envoyé (valable 2h)."
            )
        }
        # En DEBUG, exposer le code pour faciliter les tests client (mobile/Postman).
        if settings.DEBUG and ott is not None:
            response_data["debug_reset_code"] = ott.code

        # Toujours 200 — ne pas révéler si l'email existe
        return Response(response_data, status=status.HTTP_200_OK)


class ResetPasswordView(APIView):
    """
    POST /api/v1/auth/reset-password/

    Réinitialise le mot de passe via le token reçu par email.
    Le token est invalide après usage ou après 2h.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    @extend_schema(
        summary="Réinitialiser le mot de passe",
        description=(
            "Définit un nouveau mot de passe via le token reçu par email. "
            "Le token RESET_PASSWORD expire après 2h et est à usage unique."
        ),
        tags=["Inscription & Compte"],
        request=ResetPasswordSerializer,
        responses={
            200: {"description": "Mot de passe réinitialisé"},
            400: {"description": "Token invalide/expiré ou mot de passe non conforme"},
        },
    )
    def post(self, request):
        serializer = ResetPasswordSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        ott = serializer.validated_data["code"]
        user = ott.user

        # Consommer le token, définir le nouveau mot de passe
        ott.use()
        user.set_password(serializer.validated_data["nouveau_mot_de_passe"])
        user.save(update_fields=["password", "updated_at"])

        AuditLog.log(
            tenant=user.tenant,
            user=user,
            action="RESET_PASSWORD",
            resource="User",
            resource_id=user.id,
            payload={},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {
                "message": "Mot de passe réinitialisé avec succès. Vous pouvez vous connecter."
            },
            status=status.HTTP_200_OK,
        )

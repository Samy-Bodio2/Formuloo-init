"""
Views self-service employé — Formuloo OS

Permet à un employé (rôle EMPLOYE, MANAGER, etc.) d'accéder
et gérer ses propres données sans avoir le rôle RH_MANAGER.

Tous les endpoints utilisent auth_user_id extrait du JWT
pour identifier l'employé correspondant dans la table rh_employes.

Endpoints :
    GET  /api/v1/hr/me/              → Profil de l'employé connecté
    GET  /api/v1/hr/me/conges/       → Mes demandes de congé
    POST /api/v1/hr/me/conges/       → Soumettre une demande de congé
    GET  /api/v1/hr/me/fiches-paie/  → Mes fiches de paie
    GET  /api/v1/hr/me/presences/    → Mes présences du mois courant

Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/
"""

from django.utils import timezone
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Conge, Employe, FichePaie, Presence
from rh.permissions import IsEmployeOrRH
from rh.serializers import (
    CongeCreateSerializer,
    CongeSerializer,
    EmployeSerializer,
    FichePaieSerializer,
    PresenceSerializer,
)


def _get_my_employee(request):
    """
    Résout l'employé correspondant à l'utilisateur connecté.

    Utilise auth_user_id injecté dans le JWT par le service Auth.
    Filtre par tenant_id pour garantir l'isolation multi-tenant.

    Args:
        request : requête HTTP Django (user = HRJWTUser)

    Returns:
        Employe | None : l'instance Employe ou None si introuvable

    Note:
        Un employé peut ne pas avoir de compte Auth (user_id=None).
        Cette view ne concerne que les employés avec un compte lié.
    """
    auth_user_id = getattr(request.user, "auth_user_id", None)
    tenant_id = getattr(request.user, "tenant_id", None)

    if not auth_user_id or not tenant_id:
        return None

    return (
        Employe.objects.filter(
            user_id=auth_user_id,
            tenant_id=tenant_id,
        )
        .select_related("department", "position", "manager")
        .first()
    )


class MeView(APIView):
    """
    GET /api/v1/hr/me/

    Retourne le profil RH de l'employé connecté.
    Utilise auth_user_id du JWT pour trouver la fiche employé.
    Retourne 404 si aucune fiche employé n'est associée au compte.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Mon profil employé",
        description=(
            "Retourne la fiche RH de l'employé connecté. "
            "L'employé est identifié par l'auth_user_id contenu dans son JWT. "
            "Retourne 404 si aucune fiche n'est liée au compte utilisateur."
        ),
        tags=["Self-Service Employé"],
        responses={
            200: EmployeSerializer,
            404: {"description": "Aucune fiche employé liée à ce compte"},
        },
    )
    def get(self, request):
        employe = _get_my_employee(request)
        if not employe:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": (
                            "Aucune fiche employé n'est associée à votre compte. "
                            "Contactez votre RH Manager."
                        ),
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = EmployeSerializer(employe)
        return Response(serializer.data, status=status.HTTP_200_OK)


class MeCongesView(APIView):
    """
    GET  /api/v1/hr/me/conges/ — Mes demandes de congé
    POST /api/v1/hr/me/conges/ — Soumettre une demande de congé

    Un employé ne voit et ne soumet que SES propres congés.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Mes demandes de congé",
        description=(
            "Liste toutes les demandes de congé de l'employé connecté. "
            "Filtres optionnels : statut (pending/approved/rejected), annee."
        ),
        tags=["Self-Service Employé"],
        parameters=[
            OpenApiParameter(
                "statut",
                OpenApiTypes.STR,
                description="Filtrer par statut : pending, approved, rejected",
            ),
            OpenApiParameter(
                "annee",
                OpenApiTypes.INT,
                description="Filtrer par année (ex: 2024)",
            ),
        ],
        responses={
            200: CongeSerializer(many=True),
            404: {"description": "Aucune fiche employé liée"},
        },
    )
    def get(self, request):
        employe = _get_my_employee(request)
        if not employe:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Aucune fiche employé liée à votre compte.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        qs = (
            Conge.objects.filter(employee=employe, tenant_id=request.user.tenant_id)
            .select_related("employee")
            .order_by("-start_date")
        )

        # Filtres optionnels
        statut = request.query_params.get("statut")
        if statut:
            qs = qs.filter(status=statut)

        annee = request.query_params.get("annee")
        if annee and annee.isdigit():
            qs = qs.filter(start_date__year=int(annee))

        serializer = CongeSerializer(qs, many=True)
        return Response(
            {"count": qs.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Soumettre une demande de congé",
        description=(
            "Crée une demande de congé pour l'employé connecté. "
            "Le statut initial est toujours 'pending' (en attente d'approbation)."
        ),
        tags=["Self-Service Employé"],
        request=CongeCreateSerializer,
        responses={
            201: CongeSerializer,
            400: {"description": "Données invalides ou chevauchement de dates"},
            404: {"description": "Aucune fiche employé liée"},
        },
    )
    def post(self, request):
        employe = _get_my_employee(request)
        if not employe:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Aucune fiche employé liée à votre compte.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # CongeCreateSerializer.create() trouve l'employé via request.user.auth_user_id
        # On lui fournit uniquement request dans le context — pas besoin de passer employee
        serializer = CongeCreateSerializer(
            data=request.data,
            context={"request": request},
        )
        serializer.is_valid(raise_exception=True)
        conge = serializer.save()
        return Response(
            CongeSerializer(conge).data,
            status=status.HTTP_201_CREATED,
        )


class MeFichesPaieView(APIView):
    """
    GET /api/v1/hr/me/fiches-paie/

    Retourne les fiches de paie de l'employé connecté.
    Seules les fiches validées ou payées sont visibles.
    Un employé ne peut pas voir ses fiches en brouillon.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Mes fiches de paie",
        description=(
            "Liste les fiches de paie validées ou payées de l'employé connecté. "
            "Les brouillons ne sont pas visibles pour l'employé. "
            "Filtre optionnel : annee."
        ),
        tags=["Self-Service Employé"],
        parameters=[
            OpenApiParameter(
                "annee",
                OpenApiTypes.INT,
                description="Filtrer par année (ex: 2024)",
            ),
        ],
        responses={
            200: FichePaieSerializer(many=True),
            404: {"description": "Aucune fiche employé liée"},
        },
    )
    def get(self, request):
        employe = _get_my_employee(request)
        if not employe:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Aucune fiche employé liée à votre compte.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # L'employé ne voit que ses fiches validées ou payées
        qs = (
            FichePaie.objects.filter(
                employee=employe,
                tenant_id=request.user.tenant_id,
                statut__in=[FichePaie.Statut.VALIDE, FichePaie.Statut.PAYE],
            )
            .select_related("employee", "contrat")
            .order_by("-annee", "-mois")
        )

        annee = request.query_params.get("annee")
        if annee and annee.isdigit():
            qs = qs.filter(annee=int(annee))

        serializer = FichePaieSerializer(qs, many=True)
        return Response(
            {"count": qs.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )


class MePresencesView(APIView):
    """
    GET /api/v1/hr/me/presences/

    Retourne les présences de l'employé connecté.
    Par défaut : mois courant. Filtre optionnel : mois, annee.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Mes présences",
        description=(
            "Liste les présences de l'employé connecté. "
            "Par défaut : mois courant. "
            "Filtres optionnels : mois (1-12), annee."
        ),
        tags=["Self-Service Employé"],
        parameters=[
            OpenApiParameter(
                "mois",
                OpenApiTypes.INT,
                description="Mois à filtrer (1-12). Défaut : mois courant.",
            ),
            OpenApiParameter(
                "annee",
                OpenApiTypes.INT,
                description="Année à filtrer. Défaut : année courante.",
            ),
        ],
        responses={
            200: PresenceSerializer(many=True),
            404: {"description": "Aucune fiche employé liée"},
        },
    )
    def get(self, request):
        employe = _get_my_employee(request)
        if not employe:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Aucune fiche employé liée à votre compte.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        now = timezone.now()
        mois = request.query_params.get("mois")
        annee = request.query_params.get("annee")

        # Défaut : mois et année courants
        try:
            mois_int = int(mois) if mois else now.month
            annee_int = int(annee) if annee else now.year
        except ValueError:
            mois_int, annee_int = now.month, now.year

        qs = Presence.objects.filter(
            employee=employe,
            tenant_id=request.user.tenant_id,
            date__month=mois_int,
            date__year=annee_int,
        ).order_by("date")

        serializer = PresenceSerializer(qs, many=True)
        return Response(
            {
                "count": qs.count(),
                "periode": f"{annee_int}-{mois_int:02d}",
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

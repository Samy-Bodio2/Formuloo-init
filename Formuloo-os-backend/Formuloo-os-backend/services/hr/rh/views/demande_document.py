"""
Views DemandeDocument — Formuloo OS
Workflow de demande officielle de document RH.

Un employé ne peut pas se délivrer lui-même une attestation de travail
(document légal signé par l'employeur). Il soumet une demande, le RH
la traite, et l'employé récupère le document approuvé.

Endpoints employé (self-service) :
    GET  /api/v1/hr/me/demandes-document/         → mes demandes
    POST /api/v1/hr/me/demandes-document/         → soumettre une demande
    GET  /api/v1/hr/me/demandes-document/{pk}/    → détail + données si approuvée

Endpoints RH :
    GET  /api/v1/hr/demandes-document/                   → toutes les demandes du tenant
    GET  /api/v1/hr/demandes-document/{pk}/              → détail
    POST /api/v1/hr/demandes-document/{pk}/approuver/    → approuver + générer
    POST /api/v1/hr/demandes-document/{pk}/rejeter/      → rejeter avec motif

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

from rh.models import DemandeDocument, Employe
from rh.permissions import IsEmployeOrRH, IsRHManager
from rh.serializers import (
    DemandeDocumentCreateSerializer,
    DemandeDocumentSerializer,
    RejeterDemandeSerializer,
)
from rh.views.declaration import generer_donnees_attestation_travail

# ── HELPERS ───────────────────────────────────────────────────────────────────


def _get_my_employee(request):
    """Résout l'employé correspondant à l'utilisateur connecté via auth_user_id."""
    auth_user_id = getattr(request.user, "auth_user_id", None)
    tenant_id = getattr(request.user, "tenant_id", None)
    if not auth_user_id or not tenant_id:
        return None
    return (
        Employe.objects.filter(user_id=auth_user_id, tenant_id=tenant_id)
        .select_related("department", "position")
        .first()
    )


def _generer_document_data(demande: DemandeDocument) -> dict:
    """
    Génère les données structurées du document selon son type.
    Appelé à l'approbation — résultat stocké dans demande.document_data.
    """
    employe = Employe.objects.select_related("department", "position").get(
        pk=demande.employee_id
    )

    if demande.type_document == DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL:
        return generer_donnees_attestation_travail(employe)

    if demande.type_document == DemandeDocument.TypeDocument.ATTESTATION_SALAIRE:
        return {
            "meta": {
                "date_generation": timezone.now().isoformat(),
                "type_document": "Attestation de salaire",
            },
            "employe": {
                "matricule": employe.employee_number,
                "nom_complet": employe.full_name,
                "email": employe.email,
            },
            "remuneration": {
                "salaire_base": float(employe.salaire_base),
                "devise": employe.devise,
                "mode_paiement": employe.mode_paiement,
            },
        }

    # BULLETIN_PAIE_COPIE — retourne la dernière fiche de paie validée
    from rh.models import FichePaie

    derniere_fiche = (
        FichePaie.objects.filter(
            employee=employe,
            statut__in=[FichePaie.Statut.VALIDE, FichePaie.Statut.PAYE],
        )
        .order_by("-annee", "-mois")
        .first()
    )
    return {
        "meta": {
            "date_generation": timezone.now().isoformat(),
            "type_document": "Copie bulletin de paie",
        },
        "employe": {
            "matricule": employe.employee_number,
            "nom_complet": employe.full_name,
        },
        "fiche_paie": (
            {
                "mois": derniere_fiche.mois if derniere_fiche else None,
                "annee": derniere_fiche.annee if derniere_fiche else None,
                "salaire_base": (
                    float(derniere_fiche.salaire_base) if derniere_fiche else None
                ),
                "statut": derniere_fiche.statut if derniere_fiche else None,
            }
            if derniere_fiche
            else None
        ),
    }


# ── ENDPOINTS EMPLOYÉ (SELF-SERVICE) ─────────────────────────────────────────


class MeDemandesDocumentView(APIView):
    """
    GET  /api/v1/hr/me/demandes-document/ → mes demandes
    POST /api/v1/hr/me/demandes-document/ → soumettre une nouvelle demande

    Un employé ne peut soumettre et voir que SES propres demandes.
    Il ne reçoit les données du document que si le statut est APPROUVEE.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Mes demandes de document",
        description=(
            "Liste toutes les demandes de document soumises par l'employé connecté. "
            "Les données du document (document_data) ne sont visibles qu'après approbation par le RH."
        ),
        tags=["Demandes Documents"],
        parameters=[
            OpenApiParameter(
                "statut",
                OpenApiTypes.STR,
                description="Filtrer par statut : en_attente, approuvee, rejetee, annulee",
            ),
        ],
        responses={200: DemandeDocumentSerializer(many=True)},
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

        qs = DemandeDocument.objects.filter(
            employee=employe, tenant_id=request.user.tenant_id
        )

        statut = request.query_params.get("statut")
        if statut:
            qs = qs.filter(statut=statut)

        serializer = DemandeDocumentSerializer(qs, many=True)
        return Response(
            {"count": qs.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Soumettre une demande de document",
        description=(
            "Soumet une demande officielle de document au RH Manager. "
            "Le document ne sera accessible qu'après approbation du RH. "
            "Types disponibles : attestation_travail, attestation_salaire, bulletin_paie_copie."
        ),
        tags=["Demandes Documents"],
        request=DemandeDocumentCreateSerializer,
        responses={
            201: DemandeDocumentSerializer,
            400: {"description": "Données invalides"},
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

        serializer = DemandeDocumentCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        demande = DemandeDocument.objects.create(
            tenant_id=request.user.tenant_id,
            employee=employe,
            type_document=serializer.validated_data["type_document"],
            motif_demande=serializer.validated_data.get("motif_demande", ""),
        )

        return Response(
            DemandeDocumentSerializer(demande).data,
            status=status.HTTP_201_CREATED,
        )


class MeDemandeDocumentDetailView(APIView):
    """
    GET /api/v1/hr/me/demandes-document/{pk}/

    Détail d'une demande de l'employé connecté.
    Le champ document_data est renseigné uniquement si statut=APPROUVEE.
    L'employé peut annuler sa demande si elle est encore EN_ATTENTE.
    """

    permission_classes = [IsAuthenticated, IsEmployeOrRH]

    @extend_schema(
        summary="Détail d'une demande de document",
        description=(
            "Retourne le détail d'une demande et, si elle est approuvée, "
            "les données structurées du document pour génération PDF par le frontend."
        ),
        tags=["Demandes Documents"],
        responses={
            200: DemandeDocumentSerializer,
            403: {"description": "Cette demande ne vous appartient pas"},
            404: {"description": "Demande introuvable"},
        },
    )
    def get(self, request, pk):
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

        try:
            demande = DemandeDocument.objects.get(
                pk=pk,
                employee=employe,
                tenant_id=request.user.tenant_id,
            )
        except DemandeDocument.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Demande introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        return Response(
            DemandeDocumentSerializer(demande).data, status=status.HTTP_200_OK
        )

    @extend_schema(
        summary="Annuler une demande de document",
        description="Annule une demande encore en attente. Impossible si déjà traitée.",
        tags=["Demandes Documents"],
        responses={
            200: DemandeDocumentSerializer,
            400: {"description": "La demande a déjà été traitée"},
            404: {"description": "Demande introuvable"},
        },
    )
    def delete(self, request, pk):
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

        try:
            demande = DemandeDocument.objects.get(
                pk=pk,
                employee=employe,
                tenant_id=request.user.tenant_id,
            )
        except DemandeDocument.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Demande introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if not demande.est_traitable:
            return Response(
                {
                    "error": {
                        "code": "ALREADY_PROCESSED",
                        "message": "Cette demande a déjà été traitée et ne peut pas être annulée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        demande.statut = DemandeDocument.Statut.ANNULEE
        demande.save(update_fields=["statut", "updated_at"])

        return Response(
            DemandeDocumentSerializer(demande).data, status=status.HTTP_200_OK
        )


# ── ENDPOINTS RH ─────────────────────────────────────────────────────────────


class DemandesDocumentRHView(APIView):
    """
    GET /api/v1/hr/demandes-document/

    Liste toutes les demandes de documents du tenant.
    Filtrable par statut, type_document, employee_id.
    Accès : RH_MANAGER uniquement.
    """

    permission_classes = [IsAuthenticated, IsRHManager]

    @extend_schema(
        summary="Toutes les demandes de documents (RH)",
        description=(
            "Liste toutes les demandes de document du tenant. "
            "Par défaut : demandes en attente de traitement."
        ),
        tags=["Demandes Documents"],
        parameters=[
            OpenApiParameter(
                "statut",
                OpenApiTypes.STR,
                description="Filtrer par statut — défaut : en_attente",
            ),
            OpenApiParameter(
                "type_document",
                OpenApiTypes.STR,
                description="Filtrer par type : attestation_travail, attestation_salaire, bulletin_paie_copie",
            ),
        ],
        responses={200: DemandeDocumentSerializer(many=True)},
    )
    def get(self, request):
        qs = DemandeDocument.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("employee")

        # Par défaut : afficher les demandes en attente
        statut = request.query_params.get("statut", DemandeDocument.Statut.EN_ATTENTE)
        if statut:
            qs = qs.filter(statut=statut)

        type_document = request.query_params.get("type_document")
        if type_document:
            qs = qs.filter(type_document=type_document)

        serializer = DemandeDocumentSerializer(qs, many=True)
        return Response(
            {"count": qs.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )


class DemandeDocumentApprouverView(APIView):
    """
    POST /api/v1/hr/demandes-document/{pk}/approuver/

    Approuve une demande et génère les données du document.
    Le document_data est renseigné selon le type de document.
    Accès : RH_MANAGER uniquement.
    """

    permission_classes = [IsAuthenticated, IsRHManager]

    @extend_schema(
        summary="Approuver une demande de document",
        description=(
            "Approuve la demande et génère les données structurées du document. "
            "L'employé peut ensuite télécharger son document via GET /me/demandes-document/{pk}/."
        ),
        tags=["Demandes Documents"],
        responses={
            200: DemandeDocumentSerializer,
            400: {"description": "Demande déjà traitée"},
            404: {"description": "Demande introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            demande = DemandeDocument.objects.select_related("employee").get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except DemandeDocument.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Demande introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if not demande.est_traitable:
            return Response(
                {
                    "error": {
                        "code": "ALREADY_PROCESSED",
                        "message": f"Cette demande est déjà '{demande.get_statut_display()}'.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Générer les données du document selon son type
        document_data = _generer_document_data(demande)

        demande.statut = DemandeDocument.Statut.APPROUVEE
        demande.document_data = document_data
        demande.traitee_par = getattr(request.user, "auth_user_id", None)
        demande.traitee_le = timezone.now()
        demande.save(
            update_fields=[
                "statut",
                "document_data",
                "traitee_par",
                "traitee_le",
                "updated_at",
            ]
        )

        return Response(
            DemandeDocumentSerializer(demande).data, status=status.HTTP_200_OK
        )


class DemandeDocumentRejeterView(APIView):
    """
    POST /api/v1/hr/demandes-document/{pk}/rejeter/

    Rejette une demande avec un motif obligatoire.
    L'employé verra le motif dans son espace self-service.
    Accès : RH_MANAGER uniquement.
    """

    permission_classes = [IsAuthenticated, IsRHManager]

    @extend_schema(
        summary="Rejeter une demande de document",
        description=(
            "Rejette la demande avec un motif expliqué à l'employé. "
            "Le motif est visible dans l'espace self-service de l'employé."
        ),
        tags=["Demandes Documents"],
        request=RejeterDemandeSerializer,
        responses={
            200: DemandeDocumentSerializer,
            400: {"description": "Demande déjà traitée ou motif manquant"},
            404: {"description": "Demande introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            demande = DemandeDocument.objects.get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except DemandeDocument.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Demande introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if not demande.est_traitable:
            return Response(
                {
                    "error": {
                        "code": "ALREADY_PROCESSED",
                        "message": f"Cette demande est déjà '{demande.get_statut_display()}'.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = RejeterDemandeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        demande.statut = DemandeDocument.Statut.REJETEE
        demande.motif_rejet = serializer.validated_data["motif_rejet"]
        demande.traitee_par = getattr(request.user, "auth_user_id", None)
        demande.traitee_le = timezone.now()
        demande.save(
            update_fields=[
                "statut",
                "motif_rejet",
                "traitee_par",
                "traitee_le",
                "updated_at",
            ]
        )

        return Response(
            DemandeDocumentSerializer(demande).data, status=status.HTTP_200_OK
        )

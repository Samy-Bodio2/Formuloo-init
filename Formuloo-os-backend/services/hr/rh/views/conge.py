"""
Views Congé — Formuloo OS
Gestion des demandes et approbations de congés.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET    /leaves/              → IsManagerOrRH
    POST   /leaves/              → IsEmployeOrRH
    GET    /leaves/{id}/         → IsOwnerOrRH
    PUT    /leaves/{id}/         → IsOwnerOrRH
    DELETE /leaves/{id}/         → IsOwnerOrRH
    POST   /leaves/{id}/approve/ → IsManagerOrRH
    POST   /leaves/{id}/reject/  → IsManagerOrRH

Audit :
    POST   → CREATE_CONGE
    POST approve/ → APPROUVER_CONGE
    POST reject/  → REJETER_CONGE
    DELETE → ANNULER_CONGE
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Conge, SoldeConges
from rh.permissions import (
    IsEmployeOrRH,
    IsManagerOrRH,
    IsOwnerOrRH,
)
from rh.serializers import (
    CongeApprouverSerializer,
    CongeCreateSerializer,
    CongeRejeterSerializer,
    CongeSerializer,
)
from rh.services.audit import audit_conge
from rh.services.email import notifier_conge_approuve, notifier_conge_rejete


class CongesListView(APIView):
    """
    GET  /api/v1/hr/leaves/ — Liste des congés
    POST /api/v1/hr/leaves/ — Soumettre une demande
    """

    def get_permissions(self):
        """
        GET  → IsManagerOrRH
        POST → IsEmployeOrRH
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsManagerOrRH()]
        return [IsAuthenticated(), IsEmployeOrRH()]

    @extend_schema(
        summary="Lister les demandes de congés",
        description="Retourne la liste paginée " "des congés du tenant.",
        tags=["Conges"],
        responses={
            200: CongeSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        conges = Conge.objects.filter(tenant_id=request.user.tenant_id).select_related(
            "employee", "approved_by", "remplacant"
        )

        # Filtre employé
        employe_id = request.query_params.get("employe_id")
        if employe_id:
            conges = conges.filter(employee_id=employe_id)

        # Filtre statut
        statut = request.query_params.get("statut")
        if statut:
            conges = conges.filter(status=statut)

        # Filtre type_conge
        type_conge = request.query_params.get("type_conge")
        if type_conge:
            conges = conges.filter(type_conge=type_conge)

        # Filtre période
        date_debut = request.query_params.get("date_debut")
        if date_debut:
            conges = conges.filter(start_date__gte=date_debut)

        date_fin = request.query_params.get("date_fin")
        if date_fin:
            conges = conges.filter(end_date__lte=date_fin)

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = conges.count()
        page_data = conges[start:end]

        serializer = CongeSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/leaves/" f"?page={page + 1}" if end < total else None
                ),
                "previous": (
                    f"/api/v1/hr/leaves/" f"?page={page - 1}" if page > 1 else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Soumettre une demande de congé",
        tags=["Conges"],
        request=CongeCreateSerializer,
        responses={
            201: CongeSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = CongeCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        conge = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_conge("CREATE_CONGE", conge, request)

        return Response(CongeSerializer(conge).data, status=status.HTTP_201_CREATED)


class CongeDetailView(APIView):
    """
    GET    /api/v1/hr/leaves/{id}/ — Détail
    PUT    /api/v1/hr/leaves/{id}/ — Modifier
    DELETE /api/v1/hr/leaves/{id}/ — Annuler
    """

    permission_classes = [IsAuthenticated, IsOwnerOrRH]

    def get_object(self, pk, tenant_id):
        try:
            return Conge.objects.get(id=pk, tenant_id=tenant_id)
        except Conge.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'une demande de congé",
        tags=["Conges"],
        responses={
            200: CongeSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Congé introuvable"},
        },
    )
    def get(self, request, pk):
        conge = self.get_object(pk, request.user.tenant_id)
        if not conge:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Demande de congé introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, conge)

        serializer = CongeSerializer(conge)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier une demande de congé",
        tags=["Conges"],
        request=CongeCreateSerializer,
        responses={
            200: CongeSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Congé introuvable"},
        },
    )
    def put(self, request, pk):
        conge = self.get_object(pk, request.user.tenant_id)
        if not conge:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Demande de congé introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, conge)

        if not conge.is_pending:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Impossible de modifier "
                        "une demande déjà traitée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = CongeCreateSerializer(
            conge, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        conge = serializer.save()

        return Response(CongeSerializer(conge).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Annuler une demande de congé",
        tags=["Conges"],
        responses={
            204: {"description": "Demande annulée"},
            400: {"description": "Impossible d'annuler"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Congé introuvable"},
        },
    )
    def delete(self, request, pk):
        conge = self.get_object(pk, request.user.tenant_id)
        if not conge:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Demande de congé introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, conge)

        if not conge.is_pending:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Impossible d'annuler " "une demande déjà traitée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── Audit avant annulation ─────────────────────────
        audit_conge("ANNULER_CONGE", conge, request)

        conge.status = Conge.Statut.ANNULE
        conge.save(update_fields=["status", "updated_at"])

        return Response(status=status.HTTP_204_NO_CONTENT)


class CongeApprouverView(APIView):
    """
    POST /api/v1/hr/leaves/{id}/approve/
    Approuver une demande de congé.
    """

    permission_classes = [IsAuthenticated, IsManagerOrRH]

    @extend_schema(
        summary="Approuver une demande de congé",
        tags=["Conges"],
        request=CongeApprouverSerializer,
        responses={
            200: CongeSerializer,
            400: {"description": "Solde insuffisant"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Congé introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            conge = Conge.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except Conge.DoesNotExist:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Demande de congé introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        if not conge.is_pending:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Cette demande a déjà " "été traitée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = CongeApprouverSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Vérifier le solde de congés
        try:
            solde = SoldeConges.objects.get(
                employee=conge.employee,
                type_conge=conge.type_conge,
                annee=conge.start_date.year,
            )
            if not solde.has_enough_days(conge.days):
                return Response(
                    {
                        "error": {
                            "code": "INSUFFICIENT_BALANCE",
                            "message": f"Solde insuffisant — "
                            f"{solde.jours_restants}j "
                            f"disponibles, "
                            f"{conge.days}j demandés.",
                        }
                    },
                    status=status.HTTP_400_BAD_REQUEST,
                )
            # Décrémente le solde
            solde.decrementer(conge.days)
        except SoldeConges.DoesNotExist:
            pass

        # Récupérer l'approbateur
        from rh.models import Employe

        try:
            approbateur = Employe.objects.get(
                tenant_id=request.user.tenant_id, user_id=request.user.auth_user_id
            )
        except Employe.DoesNotExist:
            approbateur = None

        # Approuver
        commentaire = serializer.validated_data.get("commentaire", "")
        conge.approuver(approbateur, commentaire)

        # ── Audit ─────────────────────────────────────────
        audit_conge(
            "APPROUVER_CONGE", conge, request, extra={"commentaire": commentaire}
        )

        # ── Notification email employé (non bloquante) ─────
        notifier_conge_approuve(conge.employee, conge)

        return Response(CongeSerializer(conge).data, status=status.HTTP_200_OK)


class CongeRejeterView(APIView):
    """
    POST /api/v1/hr/leaves/{id}/reject/
    Rejeter une demande de congé.
    """

    permission_classes = [IsAuthenticated, IsManagerOrRH]

    @extend_schema(
        summary="Rejeter une demande de congé",
        tags=["Conges"],
        request=CongeRejeterSerializer,
        responses={
            200: CongeSerializer,
            400: {"description": "Motif obligatoire"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Congé introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            conge = Conge.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except Conge.DoesNotExist:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Demande de congé introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        if not conge.is_pending:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Cette demande a déjà " "été traitée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = CongeRejeterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Récupérer le rejecteur
        from rh.models import Employe

        try:
            rejecteur = Employe.objects.get(
                tenant_id=request.user.tenant_id, user_id=request.user.auth_user_id
            )
        except Employe.DoesNotExist:
            rejecteur = None

        # Rejeter
        motif = serializer.validated_data.get("reason")
        conge.rejeter(rejecteur, motif)

        # ── Audit ─────────────────────────────────────────
        audit_conge("REJETER_CONGE", conge, request, extra={"motif": motif})

        # ── Notification email employé (non bloquante) ─────
        notifier_conge_rejete(conge.employee, conge)

        return Response(CongeSerializer(conge).data, status=status.HTTP_200_OK)

"""
Views Employé — Formuloo OS
CRUD des employés + sous-ressources.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET    /employes/          → IsManagerOrRH
    POST   /employes/          → IsRHManager
    GET    /employes/{id}/     → IsOwnerOrRH
    PATCH  /employes/{id}/     → IsRHManager
    DELETE /employes/{id}/     → IsRHManager
    GET    /employes/{id}/contrats/  → IsOwnerOrRH
    GET    /employes/{id}/conges/    → IsOwnerOrRH
    GET    /employes/{id}/payslips/  → IsComptableOrRH

Audit :
    POST   → CREATE_EMPLOYE
    PATCH  → UPDATE_EMPLOYE
    DELETE → ARCHIVE_EMPLOYE
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Conge, Contrat, Employe, FichePaie
from rh.permissions import (
    IsComptableOrRH,
    IsManagerOrRH,
    IsOwnerOrRH,
    IsRHManager,
)
from rh.serializers import (
    CongeSerializer,
    ContratSerializer,
    EmployeCreateSerializer,
    EmployeSerializer,
    EmployeUpdateSerializer,
    FichePaieSerializer,
)
from rh.services.audit import audit_employe


class EmployesListView(APIView):
    """
    GET  /api/v1/hr/employes/ — Liste des employés
    POST /api/v1/hr/employes/ — Créer un employé
    """

    def get_permissions(self):
        """
        GET  → IsManagerOrRH
        POST → IsRHManager
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsManagerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    @extend_schema(
        summary="Lister les employés",
        description="Retourne la liste paginée " "des employés du tenant.",
        tags=["Employes"],
        responses={
            200: EmployeSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        employes = Employe.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("department", "position", "manager")

        # Filtre département
        departement_id = request.query_params.get("departement_id")
        if departement_id:
            employes = employes.filter(department_id=departement_id)

        # Filtre poste
        poste_id = request.query_params.get("poste_id")
        if poste_id:
            employes = employes.filter(position_id=poste_id)

        # Filtre statut
        statut = request.query_params.get("statut")
        if statut:
            employes = employes.filter(status=statut)

        # Filtre type_employe
        type_employe = request.query_params.get("type_employe")
        if type_employe:
            employes = employes.filter(type_employe=type_employe)

        # Filtre manager
        manager_id = request.query_params.get("manager_id")
        if manager_id:
            employes = employes.filter(manager_id=manager_id)

        # Filtre date_embauche
        date_embauche_min = request.query_params.get("date_embauche_min")
        if date_embauche_min:
            employes = employes.filter(hire_date__gte=date_embauche_min)
        date_embauche_max = request.query_params.get("date_embauche_max")
        if date_embauche_max:
            employes = employes.filter(hire_date__lte=date_embauche_max)

        # Recherche full-text
        search = request.query_params.get("search")
        if search:
            from django.db.models import Q

            employes = employes.filter(
                Q(first_name__icontains=search)
                | Q(last_name__icontains=search)
                | Q(email__icontains=search)
                | Q(employee_number__icontains=search)
            )

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = employes.count()
        page_data = employes[start:end]

        serializer = EmployeSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/employes/" f"?page={page + 1}" if end < total else None
                ),
                "previous": (
                    f"/api/v1/hr/employes/" f"?page={page - 1}" if page > 1 else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Créer un employé",
        tags=["Employes"],
        request=EmployeCreateSerializer,
        responses={
            201: EmployeSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = EmployeCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        employe = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_employe("CREATE_EMPLOYE", employe, request)

        return Response(EmployeSerializer(employe).data, status=status.HTTP_201_CREATED)


class EmployeDetailView(APIView):
    """
    GET    /api/v1/hr/employes/{id}/ — Détail
    PATCH  /api/v1/hr/employes/{id}/ — Modifier
    DELETE /api/v1/hr/employes/{id}/ — Archiver
    """

    def get_permissions(self):
        """
        GET    → IsOwnerOrRH
        PATCH  → IsRHManager
        DELETE → IsRHManager
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsOwnerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    def get_object(self, pk, tenant_id):
        try:
            return Employe.objects.get(id=pk, tenant_id=tenant_id)
        except Employe.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un employé",
        tags=["Employes"],
        responses={
            200: EmployeSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def get(self, request, pk):
        employe = self.get_object(pk, request.user.tenant_id)
        if not employe:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, employe)

        serializer = EmployeSerializer(employe)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un employé",
        tags=["Employes"],
        request=EmployeUpdateSerializer,
        responses={
            200: EmployeSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def patch(self, request, pk):
        employe = self.get_object(pk, request.user.tenant_id)
        if not employe:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = EmployeUpdateSerializer(
            employe, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        employe = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_employe("UPDATE_EMPLOYE", employe, request)

        return Response(EmployeSerializer(employe).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Archiver un employé",
        tags=["Employes"],
        responses={
            204: {"description": "Employé archivé"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def delete(self, request, pk):
        employe = self.get_object(pk, request.user.tenant_id)
        if not employe:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # ── Audit avant archivage ──────────────────────────
        audit_employe("ARCHIVE_EMPLOYE", employe, request)

        # Soft delete
        employe.status = Employe.Status.INACTIVE
        employe.save(update_fields=["status", "updated_at"])

        return Response(status=status.HTTP_204_NO_CONTENT)


class EmployeContratsView(APIView):
    """
    GET /api/v1/hr/employes/{id}/contrats/
    Contrats d'un employé spécifique.
    """

    permission_classes = [IsAuthenticated, IsOwnerOrRH]

    @extend_schema(
        summary="Contrats d'un employé",
        tags=["Employes"],
        responses={
            200: ContratSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            employe = Employe.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except Employe.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, employe)

        contrats = employe.contrats.all().order_by("-start_date")
        serializer = ContratSerializer(contrats, many=True)
        return Response(
            {"count": contrats.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )


class EmployeCongesView(APIView):
    """
    GET /api/v1/hr/employes/{id}/conges/
    Congés d'un employé spécifique.
    """

    permission_classes = [IsAuthenticated, IsOwnerOrRH]

    @extend_schema(
        summary="Congés d'un employé",
        tags=["Employes"],
        responses={
            200: CongeSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            employe = Employe.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except Employe.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, employe)

        conges = employe.conges.all().order_by("-created_at")
        serializer = CongeSerializer(conges, many=True)
        return Response(
            {"count": conges.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )


class EmployePayslipsView(APIView):
    """
    GET /api/v1/hr/employes/{id}/payslips/
    Fiches de paie d'un employé spécifique.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Fiches de paie d'un employé",
        tags=["Employes"],
        responses={
            200: FichePaieSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Employé introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            employe = Employe.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except Employe.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        fiches = employe.fiches_paie.all().order_by("-annee", "-mois")
        serializer = FichePaieSerializer(fiches, many=True)
        return Response(
            {"count": fiches.count(), "results": serializer.data},
            status=status.HTTP_200_OK,
        )

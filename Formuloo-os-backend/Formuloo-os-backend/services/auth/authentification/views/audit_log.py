"""
Views AuditLog — Formuloo OS
Journal d'audit immuable des actions utilisateurs.
Conforme ADR-002 : traçabilité + sécurité

GET  (utilisateurs JWT) : lecture paginée du tenant courant
POST (services internes) : enregistrement d'un événement depuis HR/Compta
                           authentifié par X-Service-Token
"""

import uuid
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from drf_spectacular.utils import extend_schema

from authentification.models import AuditLog, Organisation, User
from authentification.serializers import AuditLogSerializer
from authentification.service_auth import ServiceTokenAuthentication


class AuditLogsListView(APIView):
    """
    GET  /api/v1/auth/audit-logs/  — lecture tenant courant (JWT)
    POST /api/v1/auth/audit-logs/  — écriture inter-services (X-Service-Token)
    """

    def get_authenticators(self):
        from rest_framework_simplejwt.authentication import JWTAuthentication
        return [ServiceTokenAuthentication(), JWTAuthentication()]

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les journaux d'audit",
        description="Retourne l'historique immuable et paginé des actions du tenant. Lecture seule.",
        tags=["Audit"],
        responses={
            200: {"description": "Liste paginée des journaux d'audit"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        if not hasattr(request.user, "tenant") or request.user.tenant is None:
            return Response(
                {"error": {"code": "FORBIDDEN", "message": "Accès réservé aux utilisateurs."}},
                status=status.HTTP_403_FORBIDDEN,
            )

        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size

        audit_logs = AuditLog.objects.filter(tenant=request.user.tenant).order_by("-timestamp")

        total = audit_logs.count()
        page_logs = audit_logs[start:end]

        serializer = AuditLogSerializer(page_logs, many=True)
        return Response(
            {
                "count": total,
                "next": (f"/api/v1/auth/audit-logs/?page={page + 1}" if end < total else None),
                "previous": (f"/api/v1/auth/audit-logs/?page={page - 1}" if page > 1 else None),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Enregistrer un événement d'audit (inter-services)",
        description="Endpoint interne pour les services HR et Compta. Authentifié par X-Service-Token.",
        tags=["Audit"],
        responses={
            201: {"description": "Événement enregistré"},
            400: {"description": "Données invalides"},
            401: {"description": "Token inter-service invalide"},
        },
    )
    def post(self, request):
        from authentification.service_auth import ServiceUser
        if not isinstance(request.user, ServiceUser):
            return Response(
                {"error": {"code": "FORBIDDEN", "message": "Endpoint réservé aux services internes."}},
                status=status.HTTP_403_FORBIDDEN,
            )

        data = request.data
        action = data.get("action")
        resource = data.get("resource")
        if not action or not resource:
            return Response(
                {"error": {"code": "INVALID", "message": "action et resource sont requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Résoudre le tenant
        tenant = None
        tenant_id_raw = data.get("tenant_id")
        if tenant_id_raw:
            try:
                tenant = Organisation.objects.get(id=tenant_id_raw)
            except (Organisation.DoesNotExist, Exception):
                pass

        # Résoudre l'utilisateur
        user = None
        user_id_raw = data.get("user_id")
        if user_id_raw:
            try:
                user = User.objects.get(id=user_id_raw)
            except (User.DoesNotExist, Exception):
                pass

        # Résoudre resource_id
        resource_id = None
        resource_id_raw = data.get("resource_id")
        if resource_id_raw:
            try:
                resource_id = uuid.UUID(str(resource_id_raw))
            except (ValueError, AttributeError):
                pass

        AuditLog.objects.create(
            tenant=tenant,
            user=user,
            action=action,
            resource=resource,
            resource_id=resource_id,
            payload=data.get("payload", {}),
            ip_address=data.get("ip_address"),
        )

        return Response({"status": "logged"}, status=status.HTTP_201_CREATED)


class AuditLogDetailView(APIView):
    """
    GET /api/v1/auth/audit-logs/{id}/
    Détail d'un journal d'audit. Lecture seule — immuable.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Détail d'un journal d'audit",
        description="Retourne les informations d'une entrée du journal d'audit. Lecture seule.",
        tags=["Audit"],
        responses={
            200: {"description": "Journal d'audit trouvé"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Journal introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            audit_log = AuditLog.objects.get(id=pk, tenant=request.user.tenant)
        except AuditLog.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Journal d'audit introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = AuditLogSerializer(audit_log)
        return Response(serializer.data, status=status.HTTP_200_OK)

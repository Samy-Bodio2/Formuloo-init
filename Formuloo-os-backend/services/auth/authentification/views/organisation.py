"""
Views Organisation — Formuloo OS
CRUD des organisations (tenants).
Réservé au SUPER_ADMIN.
Conforme ADR-001 : isolation multi-tenant
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from drf_spectacular.utils import extend_schema

from authentification.models import Organisation, AuditLog
from authentification.serializers import (
    OrganisationSerializer,
    OrganisationCreateSerializer,
)


class OrganisationListView(APIView):
    """
    GET  /api/v1/auth/organisations/ — Liste des organisations
    POST /api/v1/auth/organisations/ — Créer une organisation
    Réservé au SUPER_ADMIN.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les organisations",
        description="Retourne la liste de toutes les organisations. Réservé au SUPER_ADMIN.",
        tags=["Organisations"],
        responses={
            200: OrganisationSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Permissions insuffisantes"},
        },
    )
    def get(self, request):
        organisations = Organisation.objects.all().order_by("name")
        serializer = OrganisationSerializer(organisations, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Créer une organisation",
        description="Crée une nouvelle organisation (PME cliente). Réservé au SUPER_ADMIN.",
        tags=["Organisations"],
        request=OrganisationCreateSerializer,
        responses={
            201: OrganisationSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Permissions insuffisantes"},
        },
    )
    def post(self, request):
        serializer = OrganisationCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        organisation = serializer.save()

        # Logger la création
        AuditLog.log(
            tenant=organisation,
            user=request.user,
            action="CREATE_ORGANISATION",
            resource="Organisation",
            resource_id=organisation.id,
            payload={"name": organisation.name, "slug": organisation.slug},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            OrganisationSerializer(organisation).data, status=status.HTTP_201_CREATED
        )


class OrganisationDetailView(APIView):
    """
    GET    /api/v1/auth/organisations/{id}/ — Détail
    PUT    /api/v1/auth/organisations/{id}/ — Modifier
    DELETE /api/v1/auth/organisations/{id}/ — Supprimer
    Réservé au SUPER_ADMIN.
    """

    permission_classes = [IsAuthenticated]

    def get_organisation(self, pk):
        try:
            return Organisation.objects.get(id=pk)
        except Organisation.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'une organisation",
        tags=["Organisations"],
        responses={
            200: OrganisationSerializer,
            404: {"description": "Organisation introuvable"},
        },
    )
    def get(self, request, pk):
        organisation = self.get_organisation(pk)
        if not organisation:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Organisation introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = OrganisationSerializer(organisation)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier une organisation",
        tags=["Organisations"],
        request=OrganisationCreateSerializer,
        responses={
            200: OrganisationSerializer,
            400: {"description": "Données invalides"},
            404: {"description": "Organisation introuvable"},
        },
    )
    def put(self, request, pk):
        organisation = self.get_organisation(pk)
        if not organisation:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Organisation introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = OrganisationSerializer(
            organisation, data=request.data, partial=True
        )
        serializer.is_valid(raise_exception=True)
        serializer.save()

        # Logger la modification
        AuditLog.log(
            tenant=organisation,
            user=request.user,
            action="UPDATE_ORGANISATION",
            resource="Organisation",
            resource_id=organisation.id,
            payload=request.data,
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Supprimer une organisation",
        tags=["Organisations"],
        responses={
            204: {"description": "Organisation supprimée"},
            404: {"description": "Organisation introuvable"},
        },
    )
    def delete(self, request, pk):
        organisation = self.get_organisation(pk)
        if not organisation:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Organisation introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Logger avant suppression
        AuditLog.log(
            tenant=organisation,
            user=request.user,
            action="DELETE_ORGANISATION",
            resource="Organisation",
            resource_id=organisation.id,
            payload={"name": organisation.name},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        organisation.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)

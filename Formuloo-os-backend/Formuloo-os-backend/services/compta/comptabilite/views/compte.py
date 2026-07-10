from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Compte
from comptabilite.serializers import CompteSerializer, CompteCreateSerializer, CompteUpdateSerializer
from comptabilite.permissions import CanReadComptes, CanWriteComptes, CanDeleteComptes


class ComptesListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadComptes()]
        return [IsAuthenticated(), CanWriteComptes()]

    def get(self, request):
        qs = Compte.objects.filter(tenant_id=request.user.tenant_id)
        classe = request.query_params.get("classe")
        type_compte = request.query_params.get("type_compte")
        if classe:
            qs = qs.filter(classe=classe)
        if type_compte:
            qs = qs.filter(type_compte=type_compte)
        paginator = PageNumberPagination()
        paginator.page_size = 50
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            CompteSerializer(page, many=True).data
        )

    def post(self, request):
        ser = CompteCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        compte = Compte.objects.create(
            tenant_id=request.user.tenant_id,
            **ser.validated_data,
        )
        return Response(CompteSerializer(compte).data, status=status.HTTP_201_CREATED)


class CompteDetailView(APIView):

    def _get(self, pk, tenant_id):
        try:
            return Compte.objects.get(pk=pk, tenant_id=tenant_id)
        except Compte.DoesNotExist:
            raise NotFound()

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadComptes()]
        if self.request.method == "DELETE":
            return [IsAuthenticated(), CanDeleteComptes()]
        return [IsAuthenticated(), CanWriteComptes()]

    def get(self, request, pk):
        return Response(CompteSerializer(self._get(pk, request.user.tenant_id)).data)

    def put(self, request, pk):
        compte = self._get(pk, request.user.tenant_id)
        if compte.is_systeme:
            return Response(
                {"error": {"code": "SYSTEME", "message": "Les comptes système ne peuvent pas être modifiés."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ser = CompteUpdateSerializer(compte, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return Response(CompteSerializer(compte).data)

    def delete(self, request, pk):
        compte = self._get(pk, request.user.tenant_id)
        if compte.is_systeme:
            return Response(
                {"error": {"code": "SYSTEME", "message": "Les comptes système ne peuvent pas être supprimés."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if compte.lignes_ecriture.exists():
            return Response(
                {"error": {"code": "COMPTE_UTILISE", "message": "Ce compte a des écritures — suppression impossible."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        compte.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Journal
from comptabilite.serializers import JournalSerializer, JournalCreateSerializer
from comptabilite.permissions import CanReadJournaux, CanWriteJournaux


class JournauxListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadJournaux()]
        return [IsAuthenticated(), CanWriteJournaux()]

    def get(self, request):
        qs = Journal.objects.filter(tenant_id=request.user.tenant_id)
        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            JournalSerializer(page, many=True).data
        )

    def post(self, request):
        ser = JournalCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        journal = Journal.objects.create(
            tenant_id=request.user.tenant_id,
            **ser.validated_data,
        )
        return Response(JournalSerializer(journal).data, status=status.HTTP_201_CREATED)


class JournalDetailView(APIView):

    def get_permissions(self):
        return [IsAuthenticated(), CanReadJournaux()]

    def get(self, request, pk):
        try:
            journal = Journal.objects.get(pk=pk, tenant_id=request.user.tenant_id)
        except Journal.DoesNotExist:
            raise NotFound()
        return Response(JournalSerializer(journal).data)

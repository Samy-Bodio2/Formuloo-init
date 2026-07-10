from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from django.db.models import Q

from gestiondoc.models import AccountingDocument
from gestiondoc.pagination import paginate_by_cursor
from gestiondoc.permissions import CanReadDocuments

IN_PROCESSING_STATUSES = (
    AccountingDocument.Status.PENDING_OCR,
    AccountingDocument.Status.PREPROCESSING,
    AccountingDocument.Status.EXTRACTING,
    AccountingDocument.Status.ANALYZING,
    AccountingDocument.Status.EXTRACTED,
    AccountingDocument.Status.VALIDATED,
    AccountingDocument.Status.PENDING_CHAIN,
)


class DocumentListView(APIView):
    """GET /documents/ — liste paginée + statistiques du dashboard."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request):
        tenant_id = request.user.tenant_id
        qs = AccountingDocument.objects.filter(tenant_id=tenant_id)

        status_param = request.query_params.get("status")
        if status_param:
            qs = qs.filter(status=status_param)

        document_type = request.query_params.get("document_type")
        if document_type:
            qs = qs.filter(document_type=document_type)

        supplier = request.query_params.get("supplier")
        if supplier:
            qs = qs.filter(supplier__icontains=supplier)

        q = request.query_params.get("q")
        if q:
            qs = qs.filter(
                Q(document_number__icontains=q) | Q(supplier__icontains=q)
            )

        date_from = request.query_params.get("date_from")
        if date_from:
            qs = qs.filter(created_at__date__gte=date_from)

        date_to = request.query_params.get("date_to")
        if date_to:
            qs = qs.filter(created_at__date__lte=date_to)

        try:
            page_size = min(int(request.query_params.get("page_size", 20)), 100)
        except ValueError:
            page_size = 20

        cursor = request.query_params.get("cursor")
        count = qs.count()
        results, next_cursor = paginate_by_cursor(qs, cursor, page_size, time_field="created_at")

        all_tenant_docs = AccountingDocument.objects.filter(tenant_id=tenant_id)
        stats = {
            "total_archived": all_tenant_docs.count(),
            "certified_on_chain": all_tenant_docs.filter(status=AccountingDocument.Status.CERTIFIED).count(),
            "in_processing": all_tenant_docs.filter(status__in=IN_PROCESSING_STATUSES).count(),
            "integrity_alerts": all_tenant_docs.filter(status=AccountingDocument.Status.TAMPERED).count(),
        }

        return Response({
            "count": count,
            "next_cursor": next_cursor,
            "stats": stats,
            "results": [
                {
                    "id": str(d.id),
                    "number": d.document_number,
                    "supplier": d.supplier,
                    "amount_ttc": d.amount_ttc,
                    "currency": d.currency,
                    "status": d.status,
                    "document_type": d.document_type,
                    "certified_at": d.anchored_at,
                }
                for d in results
            ],
        })

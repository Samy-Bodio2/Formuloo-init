from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.permissions import CanReadDocuments
from gestiondoc.services import storage
from gestiondoc.utils import get_document_or_404

DEFAULT_PROGRESS = {"preprocessing": "pending", "extraction": "pending", "fallback_ai": "pending"}


class OCRStatusView(APIView):
    """GET /documents/{id}/ocr-status/ — polling du statut de traitement OCR."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)
        return Response({
            "id": str(document.id),
            "status": document.status,
            "progress": document.progress or DEFAULT_PROGRESS,
            "ocr_engine": document.ocr_engine or None,
            "confidence": document.confidence,
            "error": document.error or None,
        })


class OCRResultView(APIView):
    """GET /documents/{id}/ocr-result/ — résultat brut de l'extraction OCR."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)
        return Response({
            "id": str(document.id),
            "raw_text": document.raw_text,
            "fields": document.ocr_fields,
            "preview_url": storage.generate_signed_url(document.preview_path, request) or None,
            "ocr_engine": document.ocr_engine or None,
            "confidence": document.confidence,
        })

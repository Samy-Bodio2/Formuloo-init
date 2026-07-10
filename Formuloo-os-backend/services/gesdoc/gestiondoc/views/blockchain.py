from django.conf import settings
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.models import AccountingDocument
from gestiondoc.permissions import CanReadDocuments
from gestiondoc.services import blockchain_service, storage
from gestiondoc.utils import get_document_or_404

NOT_YET_VALIDATED_STATUSES = (
    AccountingDocument.Status.PENDING_OCR,
    AccountingDocument.Status.PREPROCESSING,
    AccountingDocument.Status.EXTRACTING,
    AccountingDocument.Status.ANALYZING,
    AccountingDocument.Status.EXTRACTED,
)

CHAIN_STATUS_MAP = {
    AccountingDocument.Status.VALIDATED: "pending_chain",
    AccountingDocument.Status.PENDING_CHAIN: "anchoring",
    AccountingDocument.Status.CERTIFIED: "certified",
    AccountingDocument.Status.FAILED: "chain_error",
    AccountingDocument.Status.TAMPERED: "chain_error",
}


class BlockchainStatusView(APIView):
    """GET /documents/{id}/blockchain-status/ — statut de l'ancrage blockchain."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        if document.status in NOT_YET_VALIDATED_STATUSES:
            return Response(
                {"error": {"code": "NOT_VALIDATED", "message": "Document non encore validé — étape 3 manquante."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response({
            "id": str(document.id),
            "status": CHAIN_STATUS_MAP.get(document.status, "pending_chain"),
            "hash_sha256": document.hash_sha256,
            "tx_hash": document.tx_hash or None,
            "block_number": document.block_number,
            "blockchain": document.blockchain,
            "anchored_at": document.anchored_at,
            "certified_pdf_url": (
                storage.generate_signed_url(document.certified_pdf_path, request)
                if document.certified_pdf_path else None
            ),
        })


class BlockchainProofView(APIView):
    """GET /documents/{id}/blockchain-proof/ — preuve complète de certification blockchain."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)
        return Response({
            "document_id": str(document.id),
            "document_number": document.document_number,
            "hash_sha256": document.hash_sha256,
            "tx_hash": document.tx_hash,
            "block_number": document.block_number,
            "blockchain": document.blockchain,
            "network": settings.BLOCKCHAIN_NETWORK,
            "anchored_at": document.anchored_at,
            "explorer_url": (
                blockchain_service.build_explorer_url(document.tx_hash) if document.tx_hash else None
            ),
            "certified_by": str(document.certified_by) if document.certified_by else None,
            "tenant_id": str(document.tenant_id),
        })

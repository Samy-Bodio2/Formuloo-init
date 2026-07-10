from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.permissions import CanReadDocuments
from gestiondoc.services import storage
from gestiondoc.utils import get_document_or_404


class DocumentDetailView(APIView):
    """GET /documents/{id}/ — fiche complète d'un document (toutes données agrégées)."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        return Response({
            # ── Identité ──────────────────────────────────────
            "id": str(document.id),
            "document_type": document.document_type,
            "fiscal_year": document.fiscal_year or None,
            "notes": document.notes or None,
            "original_filename": document.original_filename or None,

            # ── Cycle de vie ──────────────────────────────────
            "status": document.status,
            "progress": document.progress or None,
            "error": document.error or None,

            # ── Fichiers (URLs signées) ────────────────────────
            "file_url": storage.generate_signed_url(document.file_path, request) or None,
            "preview_url": storage.generate_signed_url(document.preview_path, request) or None,

            # ── Résultat OCR ──────────────────────────────────
            "ocr_engine": document.ocr_engine or None,
            "confidence": document.confidence,
            "raw_text": document.raw_text or None,
            "ocr_fields": document.ocr_fields or None,

            # ── Champs validés (dénormalisés) ─────────────────
            "document_number": document.document_number or None,
            "supplier": document.supplier or None,
            "invoice_date": document.invoice_date,
            "amount_ht": document.amount_ht,
            "tva_rate": document.tva_rate,
            "amount_ttc": document.amount_ttc,
            "currency": document.currency,
            "corrections": document.corrections or [],
            "validated_by": str(document.validated_by) if document.validated_by else None,
            "validated_at": document.validated_at,

            # ── Blockchain ────────────────────────────────────
            "hash_sha256": document.hash_sha256 or None,
            "tx_hash": document.tx_hash or None,
            "block_number": document.block_number,
            "blockchain": document.blockchain or None,
            "anchored_at": document.anchored_at,
            "certified_pdf_url": (
                storage.generate_signed_url(document.certified_pdf_path, request)
                if document.certified_pdf_path else None
            ),
            "certified_by": str(document.certified_by) if document.certified_by else None,

            # ── Liaison comptable ─────────────────────────────
            "journal_entry_id": document.journal_entry_id,
            "linked_at": document.linked_at,

            # ── Horodatages ───────────────────────────────────
            "created_at": document.created_at,
            "updated_at": document.updated_at,
        })

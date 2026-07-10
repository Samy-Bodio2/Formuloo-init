from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.models import AccountingDocument, DocumentAuditLog
from gestiondoc.permissions import CanWriteDocuments
from gestiondoc.serializers import ValidateOCRSerializer
from gestiondoc.tasks import task_blockchain_anchor
from gestiondoc.utils import get_document_or_404

ALREADY_VALIDATED_STATUSES = (
    AccountingDocument.Status.VALIDATED,
    AccountingDocument.Status.PENDING_CHAIN,
    AccountingDocument.Status.CERTIFIED,
    # TAMPERED : document certifié puis falsifié — re-certification via workflow
    # dédié (verify-integrity + action manuelle), pas par re-validation silencieuse.
    AccountingDocument.Status.TAMPERED,
)


class ValidateOCRView(APIView):
    """POST /documents/{id}/validate-ocr/ — soumission des données OCR validées et corrigées."""

    permission_classes = [IsAuthenticated, CanWriteDocuments]

    def post(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        if document.status in ALREADY_VALIDATED_STATUSES:
            return Response(
                {"error": {"code": "ALREADY_VALIDATED", "message": "Document déjà validé — double soumission."}},
                status=status.HTTP_409_CONFLICT,
            )

        serializer = ValidateOCRSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        validated = serializer.validated_data
        corrections = validated.get("corrections", [])

        document.document_number = validated["document_number"]
        document.invoice_date = validated["date"]
        document.supplier = validated["supplier"]
        document.amount_ht = validated["amount_ht"]
        document.tva_rate = validated["tva_rate"]
        document.amount_ttc = validated["amount_ttc"]
        document.currency = validated["currency"]
        document.corrections = corrections
        document.validated_by = request.user.auth_user_id
        document.validated_at = timezone.now()
        document.status = AccountingDocument.Status.VALIDATED
        document.save()

        DocumentAuditLog.objects.create(
            tenant_id=document.tenant_id, document=document,
            action=DocumentAuditLog.Action.OCR_VALIDATED, label="Validation des données OCR",
            detail=f"{len(corrections)} correction(s)",
            actor_type=DocumentAuditLog.ActorType.USER, user_id=request.user.auth_user_id,
        )
        if corrections:
            DocumentAuditLog.objects.create(
                tenant_id=document.tenant_id, document=document,
                action=DocumentAuditLog.Action.OCR_CORRECTION, label="Corrections OCR",
                detail=str(corrections),
                actor_type=DocumentAuditLog.ActorType.USER, user_id=request.user.auth_user_id,
            )

        task_blockchain_anchor.delay(str(document.id))

        return Response({
            "id": str(document.id),
            "status": document.status,
            "validated_by": str(document.validated_by) if document.validated_by else None,
            "validated_at": document.validated_at,
            "corrections_count": len(corrections),
        })

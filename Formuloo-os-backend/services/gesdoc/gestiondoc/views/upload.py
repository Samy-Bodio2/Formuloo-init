from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.models import AccountingDocument, DocumentAuditLog
from gestiondoc.permissions import CanWriteDocuments
from gestiondoc.serializers import UploadSerializer
from gestiondoc.services import storage
from gestiondoc.tasks import task_ocr_pipeline


class UploadView(APIView):
    """POST /documents/upload/ — dépôt d'une pièce comptable, déclenche le pipeline OCR."""

    permission_classes = [IsAuthenticated, CanWriteDocuments]

    def post(self, request):
        serializer = UploadSerializer(data=request.data)
        if not serializer.is_valid():
            errors = serializer.errors
            if "document_type" in errors:
                return Response(
                    {"error": {"code": "INVALID_DOCUMENT_TYPE", "message": "document_type invalide ou hors énumération."}},
                    status=status.HTTP_422_UNPROCESSABLE_ENTITY,
                )
            return Response(
                {
                    "error": {
                        "code": "VALIDATION_ERROR",
                        "message": "Fichier manquant, type MIME invalide ou taille dépassée.",
                        "details": errors,
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        data = serializer.validated_data
        tenant_id = request.user.tenant_id
        document_id = storage.new_document_id()
        file_path, _ = storage.save_raw_upload(tenant_id, document_id, data["file"])

        document = AccountingDocument.objects.create(
            id=document_id,
            tenant_id=tenant_id,
            document_type=data["document_type"],
            fiscal_year=data.get("fiscal_year", ""),
            notes=data.get("notes", ""),
            file_path=file_path,
            original_filename=data["file"].name,
            status=AccountingDocument.Status.PENDING_OCR,
        )

        DocumentAuditLog.objects.create(
            tenant_id=document.tenant_id, document=document,
            action=DocumentAuditLog.Action.UPLOAD, label="Upload du document",
            detail=document.original_filename,
            actor_type=DocumentAuditLog.ActorType.USER, user_id=request.user.auth_user_id,
        )

        task = task_ocr_pipeline.delay(str(document.id))

        return Response(
            {
                "id": str(document.id),
                "status": document.status,
                "document_type": document.document_type,
                "file_url": storage.generate_signed_url(document.file_path, request),
                "task_id": task.id,
                "created_at": document.created_at,
            },
            status=status.HTTP_201_CREATED,
        )

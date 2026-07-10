from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.permissions import CanWriteDocuments
from gestiondoc.services import integrity as integrity_service
from gestiondoc.utils import get_document_or_404


class VerifyIntegrityView(APIView):
    """POST /documents/{id}/verify-integrity/ — recalcule et compare le hash au hash on-chain."""

    permission_classes = [IsAuthenticated, CanWriteDocuments]

    def post(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        if not document.hash_sha256:
            return Response(
                {"error": {"code": "NOT_CERTIFIED", "message": "Document non certifié (pas de hash on-chain)."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        uploaded_file = request.FILES.get("file")
        result = integrity_service.verify_integrity(
            document, uploaded_file=uploaded_file, user_id=request.user.auth_user_id,
        )
        return Response(result)

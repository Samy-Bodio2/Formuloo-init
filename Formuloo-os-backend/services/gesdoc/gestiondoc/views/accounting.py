from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.models import AccountingDocument, DocumentAuditLog
from gestiondoc.permissions import CanReadDocuments, CanWriteDocuments
from gestiondoc.serializers import LinkJournalEntrySerializer
from gestiondoc.services import syscohada
from gestiondoc.utils import get_document_or_404


class AccountingPrefillView(APIView):
    """GET /documents/{id}/accounting-prefill/ — suggestion d'écriture SYSCOHADA."""

    permission_classes = [IsAuthenticated, CanReadDocuments]

    def get(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        if document.status != AccountingDocument.Status.CERTIFIED:
            return Response(
                {"error": {"code": "NOT_CERTIFIED", "message": "Document pas encore au statut certified."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response({
            "source_document_id": str(document.id),
            "suggested_journal_entry": syscohada.suggest_journal_entry(document),
            "confidence": syscohada.suggestion_confidence(document),
        })


class LinkJournalEntryView(APIView):
    """
    POST /documents/{id}/link-journal-entry/ — lie une écriture Compta au document source.

    Rattachement manuel (Acteur "Comptable") — la liaison automatique
    (Acteur "Système (auto)") se fait normalement dès la certification
    via task_blockchain_anchor. Ce endpoint sert de filet de rattrapage
    si l'appel automatique a échoué (Compta indisponible, comptes
    manquants) ou pour corriger un rattachement.
    """

    permission_classes = [IsAuthenticated, CanWriteDocuments]

    def post(self, request, id):
        document = get_document_or_404(request.user.tenant_id, id)

        serializer = LinkJournalEntrySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        document.journal_entry_id = serializer.validated_data["journal_entry_id"]
        document.linked_at = timezone.now()
        document.save(update_fields=["journal_entry_id", "linked_at", "updated_at"])

        DocumentAuditLog.objects.create(
            tenant_id=document.tenant_id, document=document,
            action=DocumentAuditLog.Action.JOURNAL_LINKED,
            label="Écriture comptable liée manuellement",
            detail=f"Ecriture Compta #{document.journal_entry_id}",
            actor_type=DocumentAuditLog.ActorType.USER, user_id=request.user.auth_user_id,
        )

        return Response({
            "document_id": str(document.id),
            "journal_entry_id": document.journal_entry_id,
            "linked_at": document.linked_at,
        })

from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework.exceptions import NotFound

from gestiondoc.models import AccountingDocument


def get_document_or_404(tenant_id, document_id) -> AccountingDocument:
    try:
        return AccountingDocument.objects.get(id=document_id, tenant_id=tenant_id)
    except (AccountingDocument.DoesNotExist, DjangoValidationError, ValueError):
        raise NotFound("Document introuvable.")

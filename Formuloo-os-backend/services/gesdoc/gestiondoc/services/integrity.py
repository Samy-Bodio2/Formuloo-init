"""
Vérification d'intégrité — Formuloo OS Service GesDoc

Recalcule le hash SHA-256 du fichier stocké (ou d'un re-upload) et le
compare au hash ancré on-chain. Exécutée en synchrone dans la vue car
le contrat attend le résultat dans la réponse HTTP (pas de polling).
"""

import logging

from django.conf import settings
from django.utils import timezone

from gestiondoc.models import AccountingDocument, DocumentAuditLog
from gestiondoc.services import storage

logger = logging.getLogger(__name__)


def _notify_analytics_best_effort(document, recalculated_hash):
    """Best-effort — le module Analytics n'est pas encore déployé (pas d'entrée docker-compose)."""
    if not settings.ANALYTICS_SERVICE_URL:
        return
    try:
        import requests

        requests.post(
            f"{settings.ANALYTICS_SERVICE_URL}/api/v1/analytics/_internal/alerts/",
            json={
                "tenant_id": str(document.tenant_id),
                "type": "document_integrity_alert",
                "document_id": str(document.id),
                "hash_on_chain": document.hash_sha256,
                "hash_recalculated": recalculated_hash,
            },
            headers={"X-Service-Token": settings.INTERNAL_SERVICE_TOKEN},
            timeout=3,
        )
    except Exception:
        logger.warning("Notification Analytics impossible (best-effort) pour %s", document.id, exc_info=True)


def verify_integrity(document: AccountingDocument, uploaded_file=None, user_id=None) -> dict:
    if uploaded_file is not None:
        content = uploaded_file.read()
    else:
        content = storage.read_file(document.file_path)

    recalculated = storage.compute_sha256(content)
    match = recalculated == document.hash_sha256
    verified_at = timezone.now()

    if match:
        DocumentAuditLog.objects.create(
            tenant_id=document.tenant_id, document=document,
            action=DocumentAuditLog.Action.INTEGRITY_CHECK,
            label="Vérification d'intégrité", detail="Hash conforme au hash ancré on-chain.",
            actor_type=DocumentAuditLog.ActorType.USER, user_id=user_id,
        )
        alert_id = None
    else:
        document.status = AccountingDocument.Status.TAMPERED
        document.save(update_fields=["status", "updated_at"])
        alert = DocumentAuditLog.objects.create(
            tenant_id=document.tenant_id, document=document,
            action=DocumentAuditLog.Action.INTEGRITY_ALERT,
            label="Alerte d'intégrité",
            detail="Le hash recalculé ne correspond pas au hash ancré on-chain — document potentiellement falsifié.",
            actor_type=DocumentAuditLog.ActorType.SYSTEM,
        )
        alert_id = alert.id
        _notify_analytics_best_effort(document, recalculated)

    return {
        "document_id": str(document.id),
        "integrity": "valid" if match else "tampered",
        "hash_on_chain": document.hash_sha256,
        "hash_recalculated": recalculated,
        "match": match,
        "verified_at": verified_at,
        "alert_triggered": not match,
        "alert_id": str(alert_id) if alert_id else None,
    }

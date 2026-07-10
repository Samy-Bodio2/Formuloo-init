"""
Tâches asynchrones Celery — Formuloo OS Service GesDoc

task_ocr_pipeline    : déclenchée par le Signal `post_save` à l'upload
task_blockchain_anchor : déclenchée par le Signal `post_save` à la validation OCR

Retry exponentiel (`max_retries=3`) — surtout utile pour
task_blockchain_anchor, sensible aux latences/erreurs réseau Sepolia.
"""

import io
import logging
import os

from celery import shared_task
from django.conf import settings
from django.utils import timezone

from gestiondoc.models import AccountingDocument, DocumentAuditLog
from gestiondoc.services import blockchain_service, compta_client, ocr_engine, storage
from gestiondoc.services import pdf as pdf_service

logger = logging.getLogger(__name__)


def _log(document, action, label, detail="", actor_type=DocumentAuditLog.ActorType.SYSTEM, user_id=None):
    DocumentAuditLog.objects.create(
        tenant_id=document.tenant_id, document=document, action=action,
        label=label, detail=detail, actor_type=actor_type, user_id=user_id,
    )


def _generate_thumbnail(file_path: str, max_width: int = 800) -> bytes | None:
    """Thumbnail JPEG de la première page (PDF) ou de l'image brute."""
    from PIL import Image

    absolute_path = os.path.join(settings.MEDIA_ROOT, file_path)

    if absolute_path.lower().endswith(".pdf"):
        from pdf2image import convert_from_path

        pages = convert_from_path(absolute_path, dpi=96, first_page=1, last_page=1)
        if not pages:
            return None
        img = pages[0]
    else:
        img = Image.open(absolute_path).convert("RGB")

    w, h = img.size
    if w > max_width:
        img = img.resize((max_width, int(h * max_width / w)), Image.LANCZOS)

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=75, optimize=True)
    return buf.getvalue()


@shared_task(bind=True, max_retries=3, default_retry_delay=10)
def task_ocr_pipeline(self, document_id):
    try:
        document = AccountingDocument.objects.get(id=document_id)
    except AccountingDocument.DoesNotExist:
        logger.warning("task_ocr_pipeline: document %s introuvable", document_id)
        return

    document.status = AccountingDocument.Status.PREPROCESSING
    document.progress = {
        "preprocessing": "in_progress", "extraction": "pending", "fallback_ai": "pending",
    }
    document.save(update_fields=["status", "progress", "updated_at"])

    try:
        document.status = AccountingDocument.Status.EXTRACTING
        document.progress["preprocessing"] = "done"
        document.progress["extraction"] = "in_progress"
        document.save(update_fields=["status", "progress", "updated_at"])

        result = ocr_engine.run_ocr_pipeline(document.file_path)

        document.status = AccountingDocument.Status.ANALYZING
        document.progress["extraction"] = "done"
        document.progress["fallback_ai"] = "in_progress"
        document.save(update_fields=["status", "progress", "updated_at"])

        document.raw_text = result.raw_text
        document.ocr_fields = result.fields
        document.ocr_engine = result.engine
        document.confidence = result.confidence
        document.progress["fallback_ai"] = "done"
        document.status = AccountingDocument.Status.EXTRACTED
        document.save()

        _log(
            document, DocumentAuditLog.Action.OCR_EXTRACTED, "Extraction OCR",
            detail=f"Moteur {result.engine}, confiance {result.confidence}%",
        )
    except Exception as exc:
        document.status = AccountingDocument.Status.FAILED
        document.error = str(exc)
        document.save(update_fields=["status", "error", "updated_at"])
        logger.exception("task_ocr_pipeline a échoué pour %s", document_id)
        raise self.retry(exc=exc)

    # Preview thumbnail — best-effort : l'échec n'invalide pas l'extraction OCR
    try:
        preview_bytes = _generate_thumbnail(document.file_path)
        if preview_bytes:
            preview_path = storage.save_derived_file(
                document.tenant_id, document.id,
                "previews", f"{document.id}.jpg", preview_bytes,
            )
            document.preview_path = preview_path
            document.save(update_fields=["preview_path", "updated_at"])
    except Exception:
        logger.warning("Génération du preview échouée pour %s (non bloquant)", document_id)


@shared_task(bind=True, max_retries=3, default_retry_delay=30)
def task_blockchain_anchor(self, document_id):
    try:
        document = AccountingDocument.objects.get(id=document_id)
    except AccountingDocument.DoesNotExist:
        logger.warning("task_blockchain_anchor: document %s introuvable", document_id)
        return

    document.status = AccountingDocument.Status.PENDING_CHAIN
    document.save(update_fields=["status", "updated_at"])

    try:
        content = storage.read_file(document.file_path)
        document.hash_sha256 = storage.compute_sha256(content)

        anchor = blockchain_service.anchor_hash(document.hash_sha256)
        document.tx_hash = anchor["tx_hash"]
        document.block_number = anchor["block_number"]
        document.blockchain = "ethereum"
        document.anchored_at = timezone.now()
        document.certified_by = document.validated_by
        document.status = AccountingDocument.Status.CERTIFIED
        document.save()

        certificate = pdf_service.generer_certificat_pdf(document)
        document.certified_pdf_path = storage.save_derived_file(
            document.tenant_id, document.id, "certified", f"{document.id}.pdf", certificate,
        )
        document.save(update_fields=["certified_pdf_path", "updated_at"])

        _log(
            document, DocumentAuditLog.Action.CERTIFIED, "Certification blockchain",
            detail=f"Hash ancré sur {document.blockchain} ({document.tx_hash})",
        )
    except Exception as exc:
        document.status = AccountingDocument.Status.FAILED
        document.error = str(exc)
        document.save(update_fields=["status", "error", "updated_at"])
        logger.exception("task_blockchain_anchor a échoué pour %s", document_id)
        raise self.retry(exc=exc)

    # Liaison comptable automatique (Acteur "Système (auto)" du contrat) —
    # best-effort : si Compta est indisponible ou si des comptes manquent,
    # le document reste certifié quand même ; POST /link-journal-entry/
    # permet un rattachement manuel ultérieur (Acteur "Comptable").
    try:
        ecriture_id = compta_client.creer_ecriture_achat(document)
        if ecriture_id:
            document.journal_entry_id = int(ecriture_id)
            document.linked_at = timezone.now()
            document.save(update_fields=["journal_entry_id", "linked_at", "updated_at"])
            _log(
                document, DocumentAuditLog.Action.JOURNAL_LINKED,
                "Écriture comptable liée automatiquement",
                detail=f"Ecriture Compta #{ecriture_id}",
            )
    except Exception:
        logger.exception("Liaison comptable automatique échouée pour %s", document_id)

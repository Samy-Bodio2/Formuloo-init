import uuid

from django.db import models


class DocumentAuditLog(models.Model):
    """
    Journal d'audit immuable — aucun UPDATE ni DELETE prévu côté API.
    Une entrée par événement du cycle de vie d'un document.
    """

    class Action(models.TextChoices):
        UPLOAD = "upload", "Upload"
        OCR_EXTRACTED = "ocr_extracted", "Extraction OCR"
        OCR_VALIDATED = "ocr_validated", "Validation des données OCR"
        OCR_CORRECTION = "ocr_correction", "Correction OCR"
        CERTIFIED = "certified", "Certification blockchain"
        JOURNAL_LINKED = "journal_linked", "Écriture comptable liée"
        INTEGRITY_CHECK = "integrity_check", "Vérification d'intégrité"
        INTEGRITY_ALERT = "integrity_alert", "Alerte d'intégrité"

    class ActorType(models.TextChoices):
        SYSTEM = "system", "Système"
        USER = "user", "Utilisateur"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant_id = models.UUIDField(db_index=True)
    document = models.ForeignKey(
        "gestiondoc.AccountingDocument",
        on_delete=models.CASCADE,
        related_name="audit_entries",
    )
    action = models.CharField(max_length=30, choices=Action.choices, db_index=True)
    label = models.CharField(max_length=255)
    detail = models.TextField(blank=True)
    actor_type = models.CharField(max_length=10, choices=ActorType.choices, default=ActorType.SYSTEM)
    user_id = models.UUIDField(null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "document_audit_log"
        ordering = ["-timestamp"]
        indexes = [
            models.Index(fields=["tenant_id", "action"], name="audit_tenant_action_idx"),
            models.Index(fields=["tenant_id", "timestamp"], name="audit_tenant_time_idx"),
        ]

    def __str__(self):
        return f"AuditLog {self.action} — {self.document_id}"

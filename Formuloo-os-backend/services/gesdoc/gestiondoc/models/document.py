import uuid

from django.db import models


class AccountingDocument(models.Model):
    """
    Pièce comptable (facture, bon de commande, reçu) suivant le cycle
    upload → OCR → validation → certification blockchain → liaison
    comptable → vérification d'intégrité.

    Machine à états (`status`) :
        pending_ocr → preprocessing → extracting → analyzing → extracted
        → validated → pending_chain → certified
    `tampered` et `failed` peuvent survenir depuis n'importe quel état.
    """

    class DocumentType(models.TextChoices):
        INVOICE = "invoice", "Facture"
        PURCHASE_ORDER = "purchase_order", "Bon de commande"
        RECEIPT = "receipt", "Reçu"

    class Status(models.TextChoices):
        PENDING_OCR = "pending_ocr", "En attente OCR"
        PREPROCESSING = "preprocessing", "Prétraitement"
        EXTRACTING = "extracting", "Extraction"
        ANALYZING = "analyzing", "Analyse"
        EXTRACTED = "extracted", "Extrait"
        VALIDATED = "validated", "Validé"
        PENDING_CHAIN = "pending_chain", "Ancrage en cours"
        CERTIFIED = "certified", "Certifié"
        TAMPERED = "tampered", "Falsifié"
        FAILED = "failed", "Échec"

    class OCREngine(models.TextChoices):
        TESSERACT5 = "tesseract5", "Tesseract 5"
        EASYOCR = "easyocr", "EasyOCR"
        PADDLEOCR = "paddleocr", "PaddleOCR"

    class Devise(models.TextChoices):
        XAF = "XAF", "Franc CFA (BEAC)"
        EUR = "EUR", "Euro"
        USD = "USD", "Dollar américain"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant_id = models.UUIDField(db_index=True)

    document_type = models.CharField(max_length=20, choices=DocumentType.choices, db_index=True)
    fiscal_year = models.CharField(max_length=4, blank=True)
    notes = models.TextField(blank=True)

    # ── Stockage (volume Docker local) ────────────────────
    file_path = models.CharField(max_length=500)
    original_filename = models.CharField(max_length=255, blank=True)
    preview_path = models.CharField(max_length=500, blank=True)

    status = models.CharField(
        max_length=20, choices=Status.choices, default=Status.PENDING_OCR, db_index=True,
    )
    progress = models.JSONField(default=dict, blank=True)
    error = models.TextField(blank=True)

    # ── Résultat OCR ───────────────────────────────────────
    ocr_engine = models.CharField(max_length=20, choices=OCREngine.choices, blank=True)
    confidence = models.IntegerField(null=True, blank=True)
    raw_text = models.TextField(blank=True)
    ocr_fields = models.JSONField(default=dict, blank=True)

    # ── Champs validés (dénormalisés pour le dashboard) ────
    document_number = models.CharField(max_length=50, blank=True, db_index=True)
    supplier = models.CharField(max_length=200, blank=True, db_index=True)
    invoice_date = models.DateField(null=True, blank=True)
    amount_ht = models.DecimalField(max_digits=15, decimal_places=2, null=True, blank=True)
    tva_rate = models.DecimalField(max_digits=5, decimal_places=2, null=True, blank=True)
    amount_ttc = models.DecimalField(max_digits=15, decimal_places=2, null=True, blank=True)
    currency = models.CharField(max_length=3, choices=Devise.choices, default=Devise.XAF)
    corrections = models.JSONField(default=list, blank=True)
    validated_by = models.UUIDField(null=True, blank=True)
    validated_at = models.DateTimeField(null=True, blank=True)

    # ── Blockchain ─────────────────────────────────────────
    hash_sha256 = models.CharField(max_length=64, blank=True, db_index=True)
    tx_hash = models.CharField(max_length=100, blank=True)
    block_number = models.BigIntegerField(null=True, blank=True)
    blockchain = models.CharField(max_length=20, default="ethereum", blank=True)
    anchored_at = models.DateTimeField(null=True, blank=True)
    certified_pdf_path = models.CharField(max_length=500, blank=True)
    certified_by = models.UUIDField(null=True, blank=True)

    # ── Liaison Comptabilité ───────────────────────────────
    journal_entry_id = models.IntegerField(
        null=True, blank=True,
        help_text="id de l'Ecriture créée côté module Compta (pas de FK cross-service)",
    )
    linked_at = models.DateTimeField(null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "accounting_documents"
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["tenant_id", "status"], name="doc_tenant_status_idx"),
            models.Index(fields=["tenant_id", "created_at"], name="doc_tenant_created_idx"),
        ]

    def __str__(self):
        return f"Document {self.id} — {self.document_number or self.original_filename}"

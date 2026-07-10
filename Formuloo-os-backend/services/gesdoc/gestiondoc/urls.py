from django.urls import path

from gestiondoc.views.accounting import AccountingPrefillView, LinkJournalEntryView
from gestiondoc.views.audit import AuditLogView
from gestiondoc.views.blockchain import BlockchainProofView, BlockchainStatusView
from gestiondoc.views.detail import DocumentDetailView
from gestiondoc.views.files import SignedFileView
from gestiondoc.views.integrity import VerifyIntegrityView
from gestiondoc.views.list import DocumentListView
from gestiondoc.views.ocr import OCRResultView, OCRStatusView
from gestiondoc.views.upload import UploadView
from gestiondoc.views.validate import ValidateOCRView

urlpatterns = [
    # ── Dashboard & Audit (routes fixes — avant les routes {id}) ──
    path("", DocumentListView.as_view(), name="documents-list"),
    path("audit-log/", AuditLogView.as_view(), name="documents-audit-log"),
    path("files/<str:token>/", SignedFileView.as_view(), name="documents-signed-file"),

    # ── Upload ─────────────────────────────────────────────────
    path("upload/", UploadView.as_view(), name="documents-upload"),

    # ── Détail complet ─────────────────────────────────────────
    path("<uuid:id>/", DocumentDetailView.as_view(), name="documents-detail"),

    # ── OCR ────────────────────────────────────────────────────
    path("<uuid:id>/ocr-status/", OCRStatusView.as_view(), name="documents-ocr-status"),
    path("<uuid:id>/ocr-result/", OCRResultView.as_view(), name="documents-ocr-result"),

    # ── Validation ─────────────────────────────────────────────
    path("<uuid:id>/validate-ocr/", ValidateOCRView.as_view(), name="documents-validate-ocr"),

    # ── Blockchain ─────────────────────────────────────────────
    path("<uuid:id>/blockchain-status/", BlockchainStatusView.as_view(), name="documents-blockchain-status"),
    path("<uuid:id>/blockchain-proof/", BlockchainProofView.as_view(), name="documents-blockchain-proof"),

    # ── Comptabilité ───────────────────────────────────────────
    path("<uuid:id>/accounting-prefill/", AccountingPrefillView.as_view(), name="documents-accounting-prefill"),
    path("<uuid:id>/link-journal-entry/", LinkJournalEntryView.as_view(), name="documents-link-journal-entry"),

    # ── Intégrité ──────────────────────────────────────────────
    path("<uuid:id>/verify-integrity/", VerifyIntegrityView.as_view(), name="documents-verify-integrity"),
]

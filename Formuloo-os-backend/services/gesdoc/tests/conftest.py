"""
Configuration pytest — Service GesDoc
Fixtures partagées. Isolation multi-tenant.
"""

import uuid
from decimal import Decimal
from unittest.mock import patch

import pytest
from django.contrib.auth.models import User
from django.core.cache import cache
from rest_framework.test import APIClient

from gestiondoc.models import AccountingDocument


@pytest.fixture(autouse=True)
def flush_cache():
    """Vide le cache avant et après chaque test pour éviter la contamination."""
    cache.clear()
    yield
    cache.clear()


@pytest.fixture(autouse=True)
def use_tmp_media(settings, tmp_path):
    """Isole le stockage local des fichiers dans un répertoire temporaire par test."""
    settings.MEDIA_ROOT = str(tmp_path)


@pytest.fixture(autouse=True)
def mock_heavy_pipelines():
    """
    CELERY_TASK_ALWAYS_EAGER=True fait exécuter les tâches en synchrone
    dans le process de test. Les moteurs OCR (Tesseract/EasyOCR),
    l'ancrage blockchain (web3/Infura/Sepolia) et l'appel HTTP interne
    vers Compta ne sont pas disponibles/appelables dans l'environnement
    de test : on les mocke par défaut. Un test spécifique peut
    surcharger ces mocks localement.
    """
    from gestiondoc.services.ocr_engine import OCRExtraction

    fake_ocr_result = OCRExtraction(
        raw_text="CAMTEL BUSINESS SA Facture N INV-2025-0217 TTC 2206125.00 HT 1850000.00 TVA 19.25%",
        fields={
            "document_number": {"value": "INV-2025-0217", "confidence": 98},
            "supplier": {"value": "CAMTEL BUSINESS SA", "confidence": 90},
            "amount_ht": {"value": 1850000.0, "confidence": 91},
            "amount_ttc": {"value": 2206125.0, "confidence": 93},
            "tva_rate": {"value": 19.25, "confidence": 99},
        },
        engine="tesseract5",
        confidence=88,
    )

    with patch("gestiondoc.tasks.ocr_engine.run_ocr_pipeline", return_value=fake_ocr_result) as ocr_mock, \
         patch(
             "gestiondoc.tasks.blockchain_service.anchor_hash",
             return_value={"tx_hash": "0x" + "ab" * 32, "block_number": 19204553},
         ) as anchor_mock, \
         patch(
             "gestiondoc.tasks.compta_client.creer_ecriture_achat", return_value="42",
         ) as compta_mock:
        yield {"ocr": ocr_mock, "anchor": anchor_mock, "compta": compta_mock}


# ── Tenants ───────────────────────────────────────────────

TENANT_ALPHA = uuid.uuid4()
TENANT_BETA = uuid.uuid4()

PERMS_COMPTABLE = ["gesdoc.read.documents", "gesdoc.write.documents"]
PERMS_ADMIN = PERMS_COMPTABLE + ["gesdoc.read.audit"]


def make_user(tenant_id, permissions):
    user = User.objects.create_user(
        username=f"user_{uuid.uuid4().hex[:8]}",
        email=f"user_{uuid.uuid4().hex[:8]}@test.cm",
        password="pw",
    )
    user.tenant_id = tenant_id
    user.permissions = permissions
    user.roles = ["COMPTABLE"]
    user.auth_user_id = uuid.uuid4()
    return user


def make_client(user):
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def comptable_alpha(db):
    return make_user(TENANT_ALPHA, PERMS_COMPTABLE)


@pytest.fixture
def admin_alpha(db):
    return make_user(TENANT_ALPHA, PERMS_ADMIN)


@pytest.fixture
def comptable_beta(db):
    return make_user(TENANT_BETA, PERMS_COMPTABLE)


@pytest.fixture
def client_comptable(comptable_alpha):
    return make_client(comptable_alpha)


@pytest.fixture
def client_admin(admin_alpha):
    return make_client(admin_alpha)


@pytest.fixture
def client_beta(comptable_beta):
    return make_client(comptable_beta)


@pytest.fixture
def client_anonyme():
    return APIClient()


# ── FIXTURES DONNÉES ──────────────────────────────────────

@pytest.fixture
def uploaded_pdf():
    from django.core.files.uploadedfile import SimpleUploadedFile

    return SimpleUploadedFile(
        "facture.pdf", b"%PDF-1.4 contenu factice", content_type="application/pdf",
    )


def _write_dummy_file(tmp_path, relative_path, content=b"%PDF-1.4 contenu factice"):
    import os

    absolute = os.path.join(str(tmp_path), relative_path)
    os.makedirs(os.path.dirname(absolute), exist_ok=True)
    with open(absolute, "wb") as fh:
        fh.write(content)


@pytest.fixture
def document_pending(db, tmp_path):
    document = AccountingDocument.objects.create(
        tenant_id=TENANT_ALPHA,
        document_type=AccountingDocument.DocumentType.INVOICE,
        file_path="raw/tenant/doc.pdf",
        original_filename="facture.pdf",
        status=AccountingDocument.Status.PENDING_OCR,
    )
    _write_dummy_file(tmp_path, document.file_path)
    return document


@pytest.fixture
def document_extracted(db, tmp_path):
    document = AccountingDocument.objects.create(
        tenant_id=TENANT_ALPHA,
        document_type=AccountingDocument.DocumentType.INVOICE,
        file_path="raw/tenant/doc.pdf",
        original_filename="facture.pdf",
        status=AccountingDocument.Status.EXTRACTED,
        raw_text="CAMTEL BUSINESS SA Facture N INV-2025-0217",
        ocr_engine=AccountingDocument.OCREngine.TESSERACT5,
        confidence=88,
        ocr_fields={
            "document_number": {"value": "INV-2025-0217", "confidence": 98},
            "supplier": {"value": "CAMTEL BUSINESS SA", "confidence": 95},
        },
    )
    _write_dummy_file(tmp_path, document.file_path)
    return document


@pytest.fixture
def document_certified(db, tmp_path):
    document = AccountingDocument.objects.create(
        tenant_id=TENANT_ALPHA,
        document_type=AccountingDocument.DocumentType.INVOICE,
        file_path="raw/tenant/doc.pdf",
        original_filename="facture.pdf",
        status=AccountingDocument.Status.CERTIFIED,
        document_number="INV-2025-0217",
        supplier="CAMTEL BUSINESS SA",
        amount_ht=1850000,
        tva_rate=Decimal("19.25"),
        amount_ttc=2206125,
        currency="XAF",
        validated_by=uuid.uuid4(),
        # Doit correspondre au sha256 du contenu factice écrit ci-dessous
        # (garantit un scénario "integrity: valid" par défaut sur ce fixture).
        hash_sha256="98a614600b03797010e8250a6ecab61784816dfb2f4d6b5de9b9ba354459ee21",
        tx_hash="0x7f4ec2a91",
        block_number=19204553,
        blockchain="ethereum",
    )
    _write_dummy_file(tmp_path, document.file_path)
    return document

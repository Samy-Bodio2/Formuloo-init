import pytest
from django.urls import reverse

from gestiondoc.models import AccountingDocument, DocumentAuditLog

pytestmark = pytest.mark.django_db


def test_upload_succeeds_and_triggers_ocr_pipeline(client_comptable, uploaded_pdf, mock_heavy_pipelines):
    url = reverse("documents-upload")
    response = client_comptable.post(
        url, {"file": uploaded_pdf, "document_type": "invoice", "fiscal_year": "2025"},
        format="multipart",
    )

    assert response.status_code == 201
    body = response.json()
    assert body["status"] == "pending_ocr"
    assert body["document_type"] == "invoice"
    assert body["task_id"]
    assert body["file_url"]

    document = AccountingDocument.objects.get(id=body["id"])
    # Le pipeline OCR (mocké) tourne en synchrone (CELERY_TASK_ALWAYS_EAGER) :
    # l'état en base a donc déjà avancé au-delà de la réponse HTTP.
    assert document.status == AccountingDocument.Status.EXTRACTED
    assert document.ocr_engine == "tesseract5"
    mock_heavy_pipelines["ocr"].assert_called_once()

    assert DocumentAuditLog.objects.filter(
        document=document, action=DocumentAuditLog.Action.UPLOAD,
    ).exists()


def test_upload_invalid_document_type_returns_422(client_comptable, uploaded_pdf):
    url = reverse("documents-upload")
    response = client_comptable.post(
        url, {"file": uploaded_pdf, "document_type": "not_a_type"}, format="multipart",
    )
    assert response.status_code == 422


def test_upload_missing_file_returns_400(client_comptable):
    url = reverse("documents-upload")
    response = client_comptable.post(url, {"document_type": "invoice"}, format="multipart")
    assert response.status_code == 400


def test_upload_wrong_content_type_returns_400(client_comptable):
    from django.core.files.uploadedfile import SimpleUploadedFile

    url = reverse("documents-upload")
    bad_file = SimpleUploadedFile("virus.exe", b"MZ", content_type="application/x-msdownload")
    response = client_comptable.post(
        url, {"file": bad_file, "document_type": "invoice"}, format="multipart",
    )
    assert response.status_code == 400


def test_upload_requires_authentication(client_anonyme, uploaded_pdf):
    url = reverse("documents-upload")
    response = client_anonyme.post(
        url, {"file": uploaded_pdf, "document_type": "invoice"}, format="multipart",
    )
    assert response.status_code == 401

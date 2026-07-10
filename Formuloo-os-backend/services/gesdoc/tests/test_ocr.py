import pytest
from django.urls import reverse

pytestmark = pytest.mark.django_db


def test_ocr_status_returns_progress_and_confidence(client_comptable, document_extracted):
    url = reverse("documents-ocr-status", kwargs={"id": document_extracted.id})
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "extracted"
    assert body["ocr_engine"] == "tesseract5"
    assert body["confidence"] == 88


def test_ocr_status_not_found_for_other_tenant(client_beta, document_extracted):
    url = reverse("documents-ocr-status", kwargs={"id": document_extracted.id})
    response = client_beta.get(url)
    assert response.status_code == 404


def test_ocr_result_returns_extracted_fields(client_comptable, document_extracted):
    url = reverse("documents-ocr-result", kwargs={"id": document_extracted.id})
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["fields"]["document_number"]["value"] == "INV-2025-0217"
    assert body["fields"]["document_number"]["confidence"] == 98
    assert body["ocr_engine"] == "tesseract5"


def test_ocr_result_unknown_document_returns_404(client_comptable):
    import uuid

    url = reverse("documents-ocr-result", kwargs={"id": uuid.uuid4()})
    response = client_comptable.get(url)
    assert response.status_code == 404

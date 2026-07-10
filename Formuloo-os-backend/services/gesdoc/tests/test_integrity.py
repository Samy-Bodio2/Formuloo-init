import pytest
from django.urls import reverse

from gestiondoc.models import AccountingDocument, DocumentAuditLog

pytestmark = pytest.mark.django_db


def test_verify_integrity_valid_document(client_comptable, document_certified):
    url = reverse("documents-verify-integrity", kwargs={"id": document_certified.id})
    response = client_comptable.post(url, {}, format="multipart")

    assert response.status_code == 200
    body = response.json()
    assert body["integrity"] == "valid"
    assert body["match"] is True
    assert body["alert_triggered"] is False

    document_certified.refresh_from_db()
    assert document_certified.status == AccountingDocument.Status.CERTIFIED


def test_verify_integrity_tampered_document(client_comptable, document_certified):
    document_certified.hash_sha256 = "0" * 64  # ne correspond plus au contenu réel
    document_certified.save(update_fields=["hash_sha256"])

    url = reverse("documents-verify-integrity", kwargs={"id": document_certified.id})
    response = client_comptable.post(url, {}, format="multipart")

    assert response.status_code == 200
    body = response.json()
    assert body["integrity"] == "tampered"
    assert body["match"] is False
    assert body["alert_triggered"] is True
    assert body["alert_id"]

    document_certified.refresh_from_db()
    assert document_certified.status == AccountingDocument.Status.TAMPERED
    assert DocumentAuditLog.objects.filter(
        document=document_certified, action=DocumentAuditLog.Action.INTEGRITY_ALERT,
    ).exists()


def test_verify_integrity_not_certified_returns_400(client_comptable, document_extracted):
    url = reverse("documents-verify-integrity", kwargs={"id": document_extracted.id})
    response = client_comptable.post(url, {}, format="multipart")
    assert response.status_code == 400

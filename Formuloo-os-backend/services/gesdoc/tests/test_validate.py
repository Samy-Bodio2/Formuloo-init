import pytest
from django.urls import reverse

from gestiondoc.models import AccountingDocument, DocumentAuditLog

pytestmark = pytest.mark.django_db

VALID_PAYLOAD = {
    "document_number": "INV-2025-0217",
    "date": "2025-06-02",
    "supplier": "CAMTEL BUSINESS SA",
    "amount_ht": "1850000.00",
    "tva_rate": "19.25",
    "amount_ttc": "2206125.00",
    "currency": "XAF",
    "corrections": [{"field": "amount_ht", "original": "1800000", "corrected": "1850000"}],
}


def test_validate_ocr_succeeds_and_triggers_blockchain_anchor(
    client_comptable, document_extracted, mock_heavy_pipelines,
):
    url = reverse("documents-validate-ocr", kwargs={"id": document_extracted.id})
    response = client_comptable.post(url, VALID_PAYLOAD, format="json")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "validated"
    assert body["corrections_count"] == 1

    document = AccountingDocument.objects.get(id=document_extracted.id)
    # task_blockchain_anchor tourne en synchrone (eager) et mène jusqu'à CERTIFIED
    assert document.status == AccountingDocument.Status.CERTIFIED
    assert document.hash_sha256
    assert document.tx_hash == mock_heavy_pipelines["anchor"].return_value["tx_hash"]
    mock_heavy_pipelines["anchor"].assert_called_once()

    # Liaison comptable automatique (Acteur "Système (auto)") — voir tasks.py
    assert document.journal_entry_id == 42
    assert document.linked_at is not None
    mock_heavy_pipelines["compta"].assert_called_once()
    assert DocumentAuditLog.objects.filter(
        document=document, action=DocumentAuditLog.Action.JOURNAL_LINKED,
        actor_type=DocumentAuditLog.ActorType.SYSTEM,
    ).exists()


def test_validate_ocr_auto_link_failure_does_not_block_certification(
    client_comptable, document_extracted, mock_heavy_pipelines,
):
    """Compta indisponible/erreur → document reste certifié, journal_entry_id reste None."""
    mock_heavy_pipelines["compta"].return_value = None

    url = reverse("documents-validate-ocr", kwargs={"id": document_extracted.id})
    response = client_comptable.post(url, VALID_PAYLOAD, format="json")
    assert response.status_code == 200

    document = AccountingDocument.objects.get(id=document_extracted.id)
    assert document.status == AccountingDocument.Status.CERTIFIED
    assert document.journal_entry_id is None


def test_validate_ocr_incoherent_amounts_returns_400(client_comptable, document_extracted):
    payload = {**VALID_PAYLOAD, "amount_ttc": "999999.00"}
    url = reverse("documents-validate-ocr", kwargs={"id": document_extracted.id})
    response = client_comptable.post(url, payload, format="json")
    assert response.status_code == 400


def test_validate_ocr_missing_fields_returns_400(client_comptable, document_extracted):
    url = reverse("documents-validate-ocr", kwargs={"id": document_extracted.id})
    response = client_comptable.post(url, {"document_number": "INV-1"}, format="json")
    assert response.status_code == 400


def test_validate_ocr_double_submission_returns_409(client_comptable, document_certified):
    url = reverse("documents-validate-ocr", kwargs={"id": document_certified.id})
    response = client_comptable.post(url, VALID_PAYLOAD, format="json")
    assert response.status_code == 409


def test_validate_ocr_cross_tenant_returns_404(client_beta, document_extracted):
    url = reverse("documents-validate-ocr", kwargs={"id": document_extracted.id})
    response = client_beta.post(url, VALID_PAYLOAD, format="json")
    assert response.status_code == 404

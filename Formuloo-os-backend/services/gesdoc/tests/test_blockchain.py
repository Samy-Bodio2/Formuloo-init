import pytest
from django.urls import reverse

pytestmark = pytest.mark.django_db


def test_blockchain_status_not_validated_returns_400(client_comptable, document_extracted):
    url = reverse("documents-blockchain-status", kwargs={"id": document_extracted.id})
    response = client_comptable.get(url)
    assert response.status_code == 400


def test_blockchain_status_certified(client_comptable, document_certified):
    url = reverse("documents-blockchain-status", kwargs={"id": document_certified.id})
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "certified"
    assert body["hash_sha256"] == document_certified.hash_sha256
    assert body["tx_hash"] == document_certified.tx_hash


def test_blockchain_proof_returns_explorer_url(client_comptable, document_certified):
    url = reverse("documents-blockchain-proof", kwargs={"id": document_certified.id})
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["hash_sha256"] == document_certified.hash_sha256
    assert body["explorer_url"].endswith(document_certified.tx_hash)
    assert body["network"] == "sepolia"

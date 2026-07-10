import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_accounting_prefill_not_certified_returns_400(client_comptable, document_extracted):
    url = reverse("documents-accounting-prefill", kwargs={"id": document_extracted.id})
    response = client_comptable.get(url)
    assert response.status_code == 400


def test_accounting_prefill_certified_returns_suggested_entry(client_comptable, document_certified):
    url = reverse("documents-accounting-prefill", kwargs={"id": document_certified.id})
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    lines = body["suggested_journal_entry"]["lines"]
    accounts = [line["account_code"] for line in lines]
    assert "6011" in accounts  # achats
    assert "4011" in accounts  # fournisseurs

    total_debit = sum(float(line["debit"]) for line in lines)
    total_credit = sum(float(line["credit"]) for line in lines)
    assert round(total_debit, 2) == round(total_credit, 2)


def test_link_journal_entry_stores_reference(client_comptable, document_certified):
    url = reverse("documents-link-journal-entry", kwargs={"id": document_certified.id})
    response = client_comptable.post(url, {"journal_entry_id": 42}, format="json")

    assert response.status_code == 200
    body = response.json()
    assert body["journal_entry_id"] == 42

    document_certified.refresh_from_db()
    assert document_certified.journal_entry_id == 42
    assert document_certified.linked_at is not None


def test_link_journal_entry_requires_journal_entry_id(client_comptable, document_certified):
    url = reverse("documents-link-journal-entry", kwargs={"id": document_certified.id})
    response = client_comptable.post(url, {}, format="json")
    assert response.status_code == 400

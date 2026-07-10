import pytest
from django.urls import reverse


pytestmark = pytest.mark.django_db


def test_dashboard_lists_only_own_tenant(client_comptable, document_extracted, document_certified):
    url = reverse("documents-list")
    response = client_comptable.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 2
    assert body["stats"]["total_archived"] == 2
    assert body["stats"]["certified_on_chain"] == 1
    assert body["stats"]["in_processing"] == 1


def test_dashboard_isolates_other_tenant(client_beta, document_extracted, document_certified):
    url = reverse("documents-list")
    response = client_beta.get(url)

    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 0
    assert body["stats"]["total_archived"] == 0


def test_dashboard_filters_by_status(client_comptable, document_extracted, document_certified):
    url = reverse("documents-list")
    response = client_comptable.get(url, {"status": "certified"})

    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 1
    assert body["results"][0]["id"] == str(document_certified.id)


def test_dashboard_filters_by_supplier(client_comptable, document_certified):
    url = reverse("documents-list")
    response = client_comptable.get(url, {"supplier": "camtel"})

    assert response.status_code == 200
    body = response.json()
    assert body["count"] == 1

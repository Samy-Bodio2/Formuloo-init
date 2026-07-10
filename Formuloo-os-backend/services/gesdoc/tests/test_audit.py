import pytest
from django.urls import reverse

from gestiondoc.models import DocumentAuditLog

pytestmark = pytest.mark.django_db


def test_audit_log_requires_admin_permission(client_comptable, document_certified):
    url = reverse("documents-audit-log")
    response = client_comptable.get(url)
    assert response.status_code == 403


def test_audit_log_lists_entries_for_admin(client_admin, document_certified):
    DocumentAuditLog.objects.create(
        tenant_id=document_certified.tenant_id, document=document_certified,
        action=DocumentAuditLog.Action.CERTIFIED, label="Certification blockchain",
        actor_type=DocumentAuditLog.ActorType.SYSTEM,
    )

    url = reverse("documents-audit-log")
    response = client_admin.get(url)

    assert response.status_code == 200
    body = response.json()
    assert len(body["results"]) == 1
    assert body["results"][0]["action"] == "certified"
    assert body["results"][0]["document_number"] == document_certified.document_number


def test_audit_log_filters_by_category_alerts(client_admin, document_certified):
    DocumentAuditLog.objects.create(
        tenant_id=document_certified.tenant_id, document=document_certified,
        action=DocumentAuditLog.Action.CERTIFIED, label="Certification blockchain",
        actor_type=DocumentAuditLog.ActorType.SYSTEM,
    )
    DocumentAuditLog.objects.create(
        tenant_id=document_certified.tenant_id, document=document_certified,
        action=DocumentAuditLog.Action.INTEGRITY_ALERT, label="Alerte d'intégrité",
        actor_type=DocumentAuditLog.ActorType.SYSTEM,
    )

    url = reverse("documents-audit-log")
    response = client_admin.get(url, {"category": "alerts"})

    assert response.status_code == 200
    body = response.json()
    assert len(body["results"]) == 1
    assert body["results"][0]["action"] == "integrity_alert"


def test_audit_log_export_csv_returns_file(client_admin, document_certified):
    DocumentAuditLog.objects.create(
        tenant_id=document_certified.tenant_id, document=document_certified,
        action=DocumentAuditLog.Action.CERTIFIED, label="Certification blockchain",
        actor_type=DocumentAuditLog.ActorType.SYSTEM,
    )
    url = reverse("documents-audit-log")
    response = client_admin.get(url, {"export": "csv"})
    assert response.status_code == 200
    assert "text/csv" in response["Content-Type"]
    assert response["Content-Disposition"] == 'attachment; filename="audit_log.csv"'
    content = response.content.decode("utf-8-sig")
    assert "Horodatage" in content
    assert "certified" in content

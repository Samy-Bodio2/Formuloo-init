"""
Tests AuditLog — Formuloo OS
Teste les endpoints des journaux d'audit.
Conforme ADR-002 : traçabilité + immuabilité
"""

import pytest
from authentification.models import AuditLog


@pytest.mark.django_db
class TestAuditLogs:

    # ── LIST ──────────────────────────────────────

    def test_lister_audit_logs_succes(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : des logs existent dans le tenant
        When  : l'admin liste les logs
        Then  : liste retournée — 200
        """
        # Créer quelques logs
        AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="LOGIN",
            resource="User",
            resource_id=admin_alpha.id,
            payload={"email": admin_alpha.email},
            ip_address="127.0.0.1",
        )
        AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="UPDATE_PROFILE",
            resource="User",
            resource_id=admin_alpha.id,
            payload={},
            ip_address="127.0.0.1",
        )

        response = client_alpha.get("/api/v1/auth/audit-logs/")

        assert response.status_code == 200
        assert response.data["count"] == 2

    def test_lister_audit_logs_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les logs
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/audit-logs/")
        assert response.status_code == 401

    def test_isolation_audit_logs_multi_tenant(
        self, client_alpha, admin_alpha, org_alpha, admin_beta, org_beta
    ):
        """
        Given : logs dans PME Alpha et PME Beta
        When  : PME Alpha liste ses logs
        Then  : elle ne voit pas les logs de PME Beta
        """
        AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="LOGIN",
            resource="User",
            resource_id=admin_alpha.id,
            payload={},
            ip_address="127.0.0.1",
        )
        AuditLog.log(
            tenant=org_beta,
            user=admin_beta,
            action="LOGIN",
            resource="User",
            resource_id=admin_beta.id,
            payload={},
            ip_address="127.0.0.1",
        )

        response = client_alpha.get("/api/v1/auth/audit-logs/")

        assert response.data["count"] == 1
        assert response.data["results"][0]["action"] == "LOGIN"

    def test_pagination_audit_logs(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : 25 logs existent dans le tenant
        When  : on liste avec page_size=20
        Then  : 20 logs retournés + count=25
        """
        for i in range(25):
            AuditLog.log(
                tenant=org_alpha,
                user=admin_alpha,
                action=f"ACTION_{i}",
                resource="User",
                resource_id=admin_alpha.id,
                payload={},
                ip_address="127.0.0.1",
            )

        response = client_alpha.get("/api/v1/auth/audit-logs/?page=1&page_size=20")

        assert response.status_code == 200
        assert response.data["count"] == 25
        assert len(response.data["results"]) == 20
        assert response.data["next"] is not None

    # ── DETAIL ────────────────────────────────────

    def test_detail_audit_log_succes(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : un log existe dans le tenant
        When  : on demande son détail
        Then  : log retourné — 200
        """
        log = AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="LOGIN",
            resource="User",
            resource_id=admin_alpha.id,
            payload={"email": admin_alpha.email},
            ip_address="127.0.0.1",
        )

        response = client_alpha.get(f"/api/v1/auth/audit-logs/{log.id}/")

        assert response.status_code == 200
        assert response.data["action"] == "LOGIN"
        assert response.data["resource"] == "User"

    def test_detail_audit_log_autre_tenant(self, client_alpha, admin_beta, org_beta):
        """
        Given : un log appartient à PME Beta
        When  : PME Alpha essaie d'y accéder
        Then  : erreur 404
        """
        log = AuditLog.log(
            tenant=org_beta,
            user=admin_beta,
            action="LOGIN",
            resource="User",
            resource_id=admin_beta.id,
            payload={},
            ip_address="127.0.0.1",
        )

        response = client_alpha.get(f"/api/v1/auth/audit-logs/{log.id}/")

        assert response.status_code == 404

    # ── IMMUABILITÉ ───────────────────────────────

    def test_audit_log_immuable(self, admin_alpha, org_alpha):
        """
        Given : un log existe
        When  : on tente de le modifier directement
        Then  : ValueError levée — immuable
        """
        log = AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="LOGIN",
            resource="User",
            resource_id=admin_alpha.id,
            payload={},
            ip_address="127.0.0.1",
        )

        with pytest.raises(ValueError):
            log.action = "MODIFIE"
            log.save()

    def test_audit_log_non_supprimable(self, admin_alpha, org_alpha):
        """
        Given : un log existe
        When  : on tente de le supprimer directement
        Then  : ValueError levée — immuable
        """
        log = AuditLog.log(
            tenant=org_alpha,
            user=admin_alpha,
            action="LOGIN",
            resource="User",
            resource_id=admin_alpha.id,
            payload={},
            ip_address="127.0.0.1",
        )

        with pytest.raises(ValueError):
            log.delete()

    def test_login_cree_audit_log(self, admin_alpha, org_alpha):
        """
        Given : un utilisateur se connecte
        When  : login réussi
        Then  : un AuditLog LOGIN est créé automatiquement
        """
        from rest_framework.test import APIClient

        client = APIClient()

        initial_count = AuditLog.objects.filter(
            tenant=org_alpha, action="LOGIN"
        ).count()

        client.post(
            "/api/v1/auth/login/",
            {"email": "admin@pme-alpha.com", "password": "password123"},
            format="json",
        )

        final_count = AuditLog.objects.filter(tenant=org_alpha, action="LOGIN").count()

        assert final_count == initial_count + 1

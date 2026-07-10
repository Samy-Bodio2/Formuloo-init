"""
Tests Rôles et Permissions — Formuloo OS
Teste les endpoints CRUD des rôles et permissions.
Conforme ADR-002 : RBAC
"""

import pytest


@pytest.mark.django_db
class TestRoles:

    # ── LIST ──────────────────────────────────────

    def test_lister_roles_succes(self, client_alpha, role_admin_alpha, role_rh_alpha):
        """
        Given : deux rôles dans PME Alpha
        When  : l'admin liste les rôles
        Then  : il voit ses 2 rôles — 200
        """
        response = client_alpha.get("/api/v1/auth/roles/")

        assert response.status_code == 200
        assert len(response.data) == 2

    def test_lister_roles_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les rôles
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/roles/")
        assert response.status_code == 401

    def test_isolation_roles_multi_tenant(
        self, client_alpha, role_admin_alpha, client_beta, org_beta
    ):
        """
        Given : PME Alpha et PME Beta ont chacune des rôles
        When  : PME Alpha liste ses rôles
        Then  : elle ne voit pas les rôles de PME Beta
        """
        from authentification.models import Role

        Role.objects.create(
            tenant=org_beta, name="Admin Beta", code="ADMIN_BETA", is_system=False
        )

        response_alpha = client_alpha.get("/api/v1/auth/roles/")
        codes_alpha = [r["code"] for r in response_alpha.data]

        assert "ADMIN_BETA" not in codes_alpha

    # ── CREATE ────────────────────────────────────

    def test_creer_role_succes(self, client_alpha, db, org_alpha):
        """
        Given : admin connecté
        When  : il crée un nouveau rôle
        Then  : rôle créé — 201
        """
        response = client_alpha.post(
            "/api/v1/auth/roles/",
            {
                "name": "Chef Comptable",
                "code": "CHEF_COMPTABLE",
            },
            format="json",
        )

        assert response.status_code == 201
        assert response.data["code"] == "CHEF_COMPTABLE"
        assert response.data["name"] == "Chef Comptable"

    def test_creer_role_code_existant(self, client_alpha, role_rh_alpha):
        """
        Given : un rôle avec ce code existe dans le tenant
        When  : on tente de créer avec le même code
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/roles/",
            {
                "name": "Autre RH",
                "code": "RH_MANAGER",
            },
            format="json",
        )

        assert response.status_code == 400

    def test_creer_role_avec_permissions(self, client_alpha, permissions_hr):
        """
        Given : des permissions existent
        When  : on crée un rôle avec permissions
        Then  : rôle créé avec permissions — 201
        """
        codes = [p.code for p in permissions_hr]

        response = client_alpha.post(
            "/api/v1/auth/roles/",
            {
                "name": "Gestionnaire RH",
                "code": "GESTIONNAIRE_RH",
                "permissions": codes,
            },
            format="json",
        )

        assert response.status_code == 201
        assert len(response.data["permissions"]) == len(codes)

    # ── DETAIL ────────────────────────────────────

    def test_detail_role_succes(self, client_alpha, role_rh_alpha):
        """
        Given : un rôle existe dans le tenant
        When  : on demande son détail
        Then  : rôle retourné — 200
        """
        response = client_alpha.get(f"/api/v1/auth/roles/{role_rh_alpha.id}/")

        assert response.status_code == 200
        assert response.data["code"] == "RH_MANAGER"

    def test_detail_role_inexistant(self, client_alpha, db):
        """
        Given : aucun rôle avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        import uuid

        fake_id = uuid.uuid4()
        response = client_alpha.get(f"/api/v1/auth/roles/{fake_id}/")

        assert response.status_code == 404

    # ── UPDATE ────────────────────────────────────

    def test_modifier_role_succes(self, client_alpha, role_rh_alpha):
        """
        Given : un rôle non système existe
        When  : l'admin le modifie
        Then  : rôle modifié — 200
        """
        response = client_alpha.put(
            f"/api/v1/auth/roles/{role_rh_alpha.id}/",
            {
                "name": "Responsable RH Senior",
                "code": "RH_MANAGER",
            },
            format="json",
        )

        assert response.status_code == 200
        assert response.data["name"] == "Responsable RH Senior"

    def test_modifier_role_systeme_interdit(self, client_alpha, role_admin_alpha):
        """
        Given : un rôle système existe (is_system=True)
        When  : on tente de le modifier
        Then  : erreur 403
        """
        response = client_alpha.put(
            f"/api/v1/auth/roles/{role_admin_alpha.id}/",
            {"name": "Modifié"},
            format="json",
        )

        assert response.status_code == 403

    # ── DELETE ────────────────────────────────────

    def test_supprimer_role_succes(self, client_alpha, role_rh_alpha):
        """
        Given : un rôle non système existe
        When  : l'admin le supprime
        Then  : rôle supprimé — 204
        """
        response = client_alpha.delete(f"/api/v1/auth/roles/{role_rh_alpha.id}/")

        assert response.status_code == 204

    def test_supprimer_role_systeme_interdit(self, client_alpha, role_admin_alpha):
        """
        Given : un rôle système (is_system=True)
        When  : on tente de le supprimer
        Then  : erreur 403
        """
        response = client_alpha.delete(f"/api/v1/auth/roles/{role_admin_alpha.id}/")

        assert response.status_code == 403


@pytest.mark.django_db
class TestPermissions:

    def test_lister_permissions_succes(self, client_alpha, permissions_hr):
        """
        Given : des permissions existent
        When  : l'admin liste les permissions
        Then  : liste retournée — 200
        """
        response = client_alpha.get("/api/v1/auth/permissions/")

        assert response.status_code == 200
        # La liste contient au moins les 3 permissions HR de la fixture
        # (+ les permissions compta créées par la migration 0006)
        assert len(response.data) >= 3
        codes = [p["code"] for p in response.data]
        for perm in permissions_hr:
            assert perm.code in codes

    def test_lister_permissions_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les permissions
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/permissions/")
        assert response.status_code == 401

    def test_detail_permission_succes(self, client_alpha, permissions_hr):
        """
        Given : une permission existe
        When  : on demande son détail
        Then  : permission retournée — 200
        """
        perm = permissions_hr[0]
        response = client_alpha.get(f"/api/v1/auth/permissions/{perm.id}/")

        assert response.status_code == 200
        assert response.data["module"] == "hr"

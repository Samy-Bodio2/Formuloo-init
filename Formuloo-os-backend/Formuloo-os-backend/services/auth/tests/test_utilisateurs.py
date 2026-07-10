"""
Tests Utilisateurs — Formuloo OS
Teste les endpoints CRUD des utilisateurs.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-002 : RBAC
"""

import pytest


@pytest.mark.django_db
class TestUtilisateurs:

    # ── LIST ──────────────────────────────────────

    def test_lister_utilisateurs_succes(self, client_alpha, admin_alpha, employe_alpha):
        """
        Given : deux utilisateurs dans PME Alpha
        When  : l'admin liste les utilisateurs
        Then  : il voit ses 2 utilisateurs
        """
        response = client_alpha.get("/api/v1/auth/utilisateurs/")

        assert response.status_code == 200
        assert response.data["count"] == 2
        assert len(response.data["results"]) == 2

    def test_lister_utilisateurs_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les utilisateurs
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/utilisateurs/")
        assert response.status_code == 401

    def test_isolation_multi_tenant_liste(
        self, client_alpha, admin_alpha, client_beta, admin_beta
    ):
        """
        Given : deux PME avec chacune un utilisateur
        When  : PME Alpha liste ses utilisateurs
        Then  : elle ne voit PAS les utilisateurs de PME Beta
        """
        response_alpha = client_alpha.get("/api/v1/auth/utilisateurs/")
        response_beta = client_beta.get("/api/v1/auth/utilisateurs/")

        emails_alpha = [u["email"] for u in response_alpha.data["results"]]
        emails_beta = [u["email"] for u in response_beta.data["results"]]

        assert "admin@pme-beta.com" not in emails_alpha
        assert "admin@pme-alpha.com" not in emails_beta

    # ── CREATE ────────────────────────────────────

    def test_creer_utilisateur_succes(self, client_super_admin, org_alpha):
        """
        Given : super admin connecté
        When  : il crée un utilisateur avec tenant_id
        Then  : utilisateur créé — 201
        """
        response = client_super_admin.post(
            "/api/v1/auth/utilisateurs/",
            {
                "email": "nouveau@pme-alpha.com",
                "first_name": "Nouveau",
                "last_name": "User",
                "password": "password123",
                "tenant_id": str(org_alpha.id),
            },
            format="json",
        )

        assert response.status_code == 201
        assert response.data["email"] == "nouveau@pme-alpha.com"

    def test_creer_utilisateur_email_existant(self, client_alpha, admin_alpha):
        """
        Given : un utilisateur avec cet email existe dans le tenant
        When  : on tente de créer avec le même email
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/utilisateurs/",
            {
                "email": "admin@pme-alpha.com",
                "first_name": "Doublon",
                "last_name": "User",
                "password": "password123",
            },
            format="json",
        )

        assert response.status_code == 400

    def test_creer_utilisateur_password_trop_court(self, client_alpha, org_alpha):
        """
        Given : admin connecté
        When  : il crée un utilisateur avec password < 8 chars
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/utilisateurs/",
            {
                "email": "short@pme-alpha.com",
                "first_name": "Short",
                "last_name": "Pass",
                "password": "123",
            },
            format="json",
        )

        assert response.status_code == 400

    # ── DETAIL ────────────────────────────────────

    def test_detail_utilisateur_succes(self, client_alpha, employe_alpha):
        """
        Given : un utilisateur existe dans le tenant
        When  : l'admin demande son détail
        Then  : utilisateur retourné — 200
        """
        response = client_alpha.get(f"/api/v1/auth/utilisateurs/{employe_alpha.id}/")

        assert response.status_code == 200
        assert response.data["email"] == employe_alpha.email

    def test_isolation_multi_tenant_detail(self, client_alpha, admin_beta):
        """
        Given : admin_beta appartient à PME Beta
        When  : PME Alpha essaie d'accéder à admin_beta
        Then  : erreur 404 — isolation multi-tenant
        """
        response = client_alpha.get(f"/api/v1/auth/utilisateurs/{admin_beta.id}/")

        assert response.status_code == 404

    # ── UPDATE ────────────────────────────────────

    def test_modifier_utilisateur_succes(self, client_alpha, employe_alpha):
        """
        Given : un utilisateur existe dans le tenant
        When  : l'admin le modifie
        Then  : utilisateur modifié — 200
        """
        response = client_alpha.put(
            f"/api/v1/auth/utilisateurs/{employe_alpha.id}/",
            {"first_name": "Marie-Claire"},
            format="json",
        )

        assert response.status_code == 200
        assert response.data["first_name"] == "Marie-Claire"

    # ── DELETE (soft) ─────────────────────────────

    def test_supprimer_utilisateur_succes(self, client_alpha, employe_alpha):
        """
        Given : un utilisateur actif dans le tenant
        When  : l'admin le supprime (soft delete)
        Then  : utilisateur désactivé — 204
        """
        response = client_alpha.delete(f"/api/v1/auth/utilisateurs/{employe_alpha.id}/")

        assert response.status_code == 204

        # Vérifier que l'utilisateur est désactivé
        employe_alpha.refresh_from_db()
        assert employe_alpha.is_active is False

    # ── ACTIVER / DÉSACTIVER ──────────────────────

    def test_activer_utilisateur_succes(self, client_alpha, employe_alpha):
        """
        Given : un utilisateur désactivé
        When  : l'admin l'active
        Then  : utilisateur activé — 200
        """
        employe_alpha.is_active = False
        employe_alpha.save()

        response = client_alpha.post(
            f"/api/v1/auth/utilisateurs/{employe_alpha.id}/activer/"
        )

        assert response.status_code == 200
        employe_alpha.refresh_from_db()
        assert employe_alpha.is_active is True

    def test_desactiver_utilisateur_succes(self, client_alpha, employe_alpha):
        """
        Given : un utilisateur actif
        When  : l'admin le désactive
        Then  : utilisateur désactivé — 200
        """
        response = client_alpha.post(
            f"/api/v1/auth/utilisateurs/{employe_alpha.id}/desactiver/"
        )

        assert response.status_code == 200
        employe_alpha.refresh_from_db()
        assert employe_alpha.is_active is False

    # ── ROLES ─────────────────────────────────────

    def test_assigner_role_succes(self, client_alpha, employe_alpha, role_rh_alpha):
        """
        Given : un utilisateur et un rôle dans le même tenant
        When  : l'admin assigne le rôle
        Then  : rôle assigné — 200
        """
        response = client_alpha.post(
            f"/api/v1/auth/utilisateurs/{employe_alpha.id}/roles/",
            {"role": "RH_MANAGER"},
            format="json",
        )

        assert response.status_code == 200
        assert employe_alpha.roles.filter(code="RH_MANAGER").exists()

    def test_lister_roles_utilisateur(self, client_alpha, employe_alpha, role_rh_alpha):
        """
        Given : un utilisateur avec un rôle assigné
        When  : on liste ses rôles
        Then  : liste des rôles retournée — 200
        """
        employe_alpha.roles.add(role_rh_alpha)

        response = client_alpha.get(
            f"/api/v1/auth/utilisateurs/{employe_alpha.id}/roles/"
        )

        assert response.status_code == 200
        assert len(response.data) == 1
        assert response.data[0]["code"] == "RH_MANAGER"

"""
Tests Organisations — Formuloo OS
Teste les endpoints CRUD des organisations.
Conforme ADR-001 : isolation multi-tenant
"""

import pytest


@pytest.mark.django_db
class TestOrganisations:

    # ── LIST ──────────────────────────────────────

    def test_lister_organisations_super_admin(
        self, client_super_admin, org_alpha, org_beta
    ):
        """
        Given : deux organisations existent
        When  : le super admin liste les organisations
        Then  : il voit toutes les organisations
        """
        response = client_super_admin.get("/api/v1/auth/organisations/")

        assert response.status_code == 200
        assert len(response.data) == 2

    def test_lister_organisations_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les organisations
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/organisations/")
        assert response.status_code == 401

    # ── CREATE ────────────────────────────────────

    def test_creer_organisation_succes(self, client_super_admin, db):
        """
        Given : super admin connecté
        When  : il crée une organisation
        Then  : organisation créée — 201
        """
        response = client_super_admin.post(
            "/api/v1/auth/organisations/",
            {
                "slug": "pme-gamma",
                "name": "PME Gamma Cameroun",
                "currency": "XAF",
                "locale": "fr",
            },
            format="json",
        )

        assert response.status_code == 201
        assert response.data["slug"] == "pme-gamma"
        assert response.data["name"] == "PME Gamma Cameroun"

    def test_creer_organisation_slug_existant(self, client_super_admin, org_alpha):
        """
        Given : une organisation avec ce slug existe
        When  : on tente de créer avec le même slug
        Then  : erreur 400
        """
        response = client_super_admin.post(
            "/api/v1/auth/organisations/",
            {
                "slug": "pme-alpha",
                "name": "Autre PME Alpha",
            },
            format="json",
        )

        assert response.status_code == 400

    def test_creer_organisation_sans_slug(self, client_super_admin, db):
        """
        Given : super admin connecté
        When  : il crée une organisation sans slug
        Then  : erreur 400
        """
        response = client_super_admin.post(
            "/api/v1/auth/organisations/", {"name": "PME Sans Slug"}, format="json"
        )

        assert response.status_code == 400

    # ── DETAIL ────────────────────────────────────

    def test_detail_organisation_succes(self, client_super_admin, org_alpha):
        """
        Given : une organisation existe
        When  : le super admin demande son détail
        Then  : organisation retournée — 200
        """
        response = client_super_admin.get(f"/api/v1/auth/organisations/{org_alpha.id}/")

        assert response.status_code == 200
        assert response.data["slug"] == "pme-alpha"
        assert response.data["name"] == "PME Alpha Cameroun"

    def test_detail_organisation_inexistante(self, client_super_admin, db):
        """
        Given : aucune organisation avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        import uuid

        fake_id = uuid.uuid4()
        response = client_super_admin.get(f"/api/v1/auth/organisations/{fake_id}/")

        assert response.status_code == 404

    # ── UPDATE ────────────────────────────────────

    def test_modifier_organisation_succes(self, client_super_admin, org_alpha):
        """
        Given : une organisation existe
        When  : le super admin la modifie
        Then  : organisation modifiée — 200
        """
        response = client_super_admin.put(
            f"/api/v1/auth/organisations/{org_alpha.id}/",
            {"name": "PME Alpha Updated"},
            format="json",
        )

        assert response.status_code == 200
        assert response.data["name"] == "PME Alpha Updated"

    # ── DELETE ────────────────────────────────────

    def test_supprimer_organisation_succes(self, client_super_admin, org_alpha):
        """
        Given : une organisation existe
        When  : le super admin la supprime
        Then  : organisation supprimée — 204
        """
        response = client_super_admin.delete(
            f"/api/v1/auth/organisations/{org_alpha.id}/"
        )

        assert response.status_code == 204

    def test_supprimer_organisation_inexistante(self, client_super_admin, db):
        """
        Given : aucune organisation avec cet UUID
        When  : on tente de la supprimer
        Then  : erreur 404
        """
        import uuid

        fake_id = uuid.uuid4()
        response = client_super_admin.delete(f"/api/v1/auth/organisations/{fake_id}/")

        assert response.status_code == 404

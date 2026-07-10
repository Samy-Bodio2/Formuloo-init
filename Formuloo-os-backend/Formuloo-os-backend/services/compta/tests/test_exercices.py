"""Tests Exercices — isolation multi-tenant, RBAC, clôture."""

import pytest
from datetime import date
from comptabilite.models import Exercice


@pytest.mark.django_db
class TestExercices:

    def test_lister_exercices_succes(self, client_comptable, exercice_alpha):
        resp = client_comptable.get("/api/v1/compta/exercices/")
        assert resp.status_code == 200
        assert resp.data["count"] == 1

    def test_lister_exercices_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/exercices/")
        assert resp.status_code == 401

    def test_lister_exercices_sans_permission(self, client_anonyme, comptable_alpha):
        from rest_framework.test import APIClient
        from tests.conftest import make_user, TENANT_ALPHA
        user = make_user(TENANT_ALPHA, [])  # aucune permission
        client = APIClient()
        client.force_authenticate(user=user)
        resp = client.get("/api/v1/compta/exercices/")
        assert resp.status_code == 403

    def test_isolation_multi_tenant(self, client_beta, exercice_alpha):
        resp = client_beta.get("/api/v1/compta/exercices/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_exercice_succes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/exercices/",
            {"annee": 2025, "date_debut": "2025-01-01", "date_fin": "2025-12-31"},
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["annee"] == 2025
        assert resp.data["statut"] == "OUVERT"

    def test_creer_exercice_dates_invalides(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/exercices/",
            {"annee": 2025, "date_debut": "2025-12-31", "date_fin": "2025-01-01"},
            format="json",
        )
        assert resp.status_code == 400

    def test_detail_exercice(self, client_comptable, exercice_alpha):
        resp = client_comptable.get(f"/api/v1/compta/exercices/{exercice_alpha.id}/")
        assert resp.status_code == 200
        assert resp.data["annee"] == 2024

    def test_detail_exercice_autre_tenant(self, client_beta, exercice_alpha):
        resp = client_beta.get(f"/api/v1/compta/exercices/{exercice_alpha.id}/")
        assert resp.status_code == 404

    def test_cloturer_exercice_succes(self, client_daf, exercice_alpha):
        resp = client_daf.post(f"/api/v1/compta/exercices/{exercice_alpha.id}/cloturer/")
        assert resp.status_code == 200
        assert resp.data["exercice"]["statut"] == "CLOTURE"
        assert "resultat_net" in resp.data
        assert "type_resultat" in resp.data
        assert "exercice_suivant" in resp.data

    def test_cloturer_exercice_sans_permission(self, client_comptable, exercice_alpha):
        # COMPTABLE n'a pas compta.close.exercices
        resp = client_comptable.post(f"/api/v1/compta/exercices/{exercice_alpha.id}/cloturer/")
        assert resp.status_code == 403

    def test_cloturer_exercice_deja_cloture(self, client_daf, exercice_cloture):
        resp = client_daf.post(f"/api/v1/compta/exercices/{exercice_cloture.id}/cloturer/")
        assert resp.status_code == 400

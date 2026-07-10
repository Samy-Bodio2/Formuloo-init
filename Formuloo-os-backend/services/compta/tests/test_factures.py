"""Tests Factures — cycle de vie, isolation, états."""

import pytest
from comptabilite.models import Facture


@pytest.mark.django_db
class TestFactures:

    def test_lister_factures(self, client_comptable, facture_brouillon):
        resp = client_comptable.get("/api/v1/compta/factures/")
        assert resp.status_code == 200
        assert resp.data["count"] == 1

    def test_isolation_multi_tenant(self, client_beta, facture_brouillon):
        resp = client_beta.get("/api/v1/compta/factures/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_facture_succes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/factures/",
            {
                "client_nom": "PME Test SARL",
                "client_email": "pme@test.cm",
                "lignes": [
                    {"description": "Développement", "quantite": "2", "prix_unitaire": "150000.00"}
                ],
                "devise": "XAF",
                "date_echeance": "2024-08-31",
            },
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["statut"] == "BROUILLON"
        assert resp.data["client_nom"] == "PME Test SARL"

    def test_creer_facture_sans_lignes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/factures/",
            {"client_nom": "Test", "lignes": [], "devise": "XAF", "date_echeance": "2024-08-31"},
            format="json",
        )
        assert resp.status_code == 400

    def test_emettre_facture(self, client_comptable, facture_brouillon):
        resp = client_comptable.post(
            f"/api/v1/compta/factures/{facture_brouillon.id}/emettre/"
        )
        assert resp.status_code == 200
        assert resp.data["statut"] == "EMISE"
        assert resp.data["date_emission"] is not None

    def test_emettre_facture_deja_emise(self, client_comptable, facture_emise):
        resp = client_comptable.post(
            f"/api/v1/compta/factures/{facture_emise.id}/emettre/"
        )
        assert resp.status_code == 400

    def test_modifier_facture_brouillon(self, client_comptable, facture_brouillon):
        resp = client_comptable.put(
            f"/api/v1/compta/factures/{facture_brouillon.id}/",
            {"client_nom": "Nouveau Nom SARL"},
            format="json",
        )
        assert resp.status_code == 200
        assert resp.data["client_nom"] == "Nouveau Nom SARL"

    def test_modifier_facture_emise(self, client_comptable, facture_emise):
        resp = client_comptable.put(
            f"/api/v1/compta/factures/{facture_emise.id}/",
            {"client_nom": "Modif"},
            format="json",
        )
        assert resp.status_code == 400

    def test_annuler_facture(self, client_comptable, facture_brouillon):
        resp = client_comptable.delete(f"/api/v1/compta/factures/{facture_brouillon.id}/")
        assert resp.status_code == 204
        facture_brouillon.refresh_from_db()
        assert facture_brouillon.statut == "ANNULEE"

    def test_detail_facture(self, client_comptable, facture_brouillon):
        resp = client_comptable.get(f"/api/v1/compta/factures/{facture_brouillon.id}/")
        assert resp.status_code == 200
        assert len(resp.data["lignes"]) == 1

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/factures/")
        assert resp.status_code == 401

"""Tests Journaux comptables — CRUD, isolation."""

import pytest
from comptabilite.models import Journal


@pytest.mark.django_db
class TestJournaux:

    def test_lister_journaux(self, client_comptable, journal_ventes, journal_banque):
        resp = client_comptable.get("/api/v1/compta/journaux/")
        assert resp.status_code == 200
        assert resp.data["count"] == 2

    def test_isolation_multi_tenant(self, client_beta, journal_ventes):
        resp = client_beta.get("/api/v1/compta/journaux/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_journal_succes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/journaux/",
            {"code": "ACH", "libelle": "Journal des achats", "type": "ACHATS"},
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["code"] == "ACH"

    def test_detail_journal(self, client_comptable, journal_ventes):
        resp = client_comptable.get(f"/api/v1/compta/journaux/{journal_ventes.id}/")
        assert resp.status_code == 200
        assert resp.data["type"] == "VENTES"

    def test_detail_journal_autre_tenant(self, client_beta, journal_ventes):
        resp = client_beta.get(f"/api/v1/compta/journaux/{journal_ventes.id}/")
        assert resp.status_code == 404

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/journaux/")
        assert resp.status_code == 401

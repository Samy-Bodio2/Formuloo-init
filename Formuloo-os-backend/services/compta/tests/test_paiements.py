"""Tests Paiements — encaissement, isolation, statut facture."""

import pytest
from comptabilite.models import Paiement, Facture


@pytest.mark.django_db
class TestPaiements:

    def test_lister_paiements(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/paiements/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_enregistrer_paiement_succes(self, client_comptable, facture_emise):
        resp = client_comptable.post(
            "/api/v1/compta/paiements/",
            {
                "facture_id": facture_emise.id,
                "montant": "200000.00",
                "mode_paiement": "VIREMENT",
                "date_paiement": "2024-06-20",
                "reference": "VIR-2024-001",
            },
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["montant"] == "200000.00"

    def test_paiement_facture_brouillon(self, client_comptable, facture_brouillon):
        resp = client_comptable.post(
            "/api/v1/compta/paiements/",
            {
                "facture_id": facture_brouillon.id,
                "montant": "500000.00",
                "mode_paiement": "ESPECES",
                "date_paiement": "2024-06-20",
            },
            format="json",
        )
        assert resp.status_code == 400

    def test_paiement_marque_facture_payee(self, client_comptable, facture_emise):
        # facture_emise a une ligne à 200 000 XAF HT, TVA 0%
        resp = client_comptable.post(
            "/api/v1/compta/paiements/",
            {
                "facture_id": facture_emise.id,
                "montant": "200000.00",
                "mode_paiement": "VIREMENT",
                "date_paiement": "2024-06-20",
            },
            format="json",
        )
        assert resp.status_code == 201
        facture_emise.refresh_from_db()
        assert facture_emise.statut == "PAYEE"

    def test_detail_paiement(self, client_comptable, comptable_alpha, facture_emise):
        from datetime import date
        paiement = Paiement.objects.create(
            tenant_id=comptable_alpha.tenant_id,
            facture=facture_emise,
            montant=50000,
            devise="XAF",
            mode_paiement="ESPECES",
            date_paiement=date(2024, 6, 20),
        )
        resp = client_comptable.get(f"/api/v1/compta/paiements/{paiement.id}/")
        assert resp.status_code == 200

    def test_isolation_multi_tenant(self, client_beta, facture_emise):
        resp = client_beta.get("/api/v1/compta/paiements/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/paiements/")
        assert resp.status_code == 401

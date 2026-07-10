"""Tests États financiers — Grand livre, Balance, Bilan, Compte de résultat."""

import pytest


@pytest.mark.django_db
class TestEtats:

    def test_grand_livre_sans_exercice_id(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/grand-livre/")
        assert resp.status_code == 400

    def test_grand_livre_succes(self, client_comptable, exercice_alpha, ecriture_brouillon):
        resp = client_comptable.get(
            f"/api/v1/compta/grand-livre/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 200

    def test_balance_sans_exercice_id(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/balance/")
        assert resp.status_code == 400

    def test_balance_succes(self, client_comptable, exercice_alpha, ecriture_brouillon):
        resp = client_comptable.get(
            f"/api/v1/compta/balance/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 200
        assert "total_debit" in resp.data
        assert "total_credit" in resp.data
        assert "lignes" in resp.data

    def test_bilan_sans_exercice_id(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/bilan/")
        assert resp.status_code == 400

    def test_bilan_succes(self, client_comptable, exercice_alpha):
        resp = client_comptable.get(
            f"/api/v1/compta/bilan/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 200
        assert "actif" in resp.data
        assert "passif" in resp.data
        assert "equilibre" in resp.data

    def test_compte_resultat_sans_exercice_id(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/compte-resultat/")
        assert resp.status_code == 400

    def test_compte_resultat_succes(self, client_comptable, exercice_alpha):
        resp = client_comptable.get(
            f"/api/v1/compta/compte-resultat/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 200
        assert "produits" in resp.data
        assert "charges" in resp.data
        assert "resultat_net" in resp.data

    def test_etats_auditeur_peut_lire(self, client_auditeur, exercice_alpha):
        resp = client_auditeur.get(
            f"/api/v1/compta/bilan/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 200

    def test_etats_sans_auth(self, client_anonyme, exercice_alpha):
        resp = client_anonyme.get(
            f"/api/v1/compta/bilan/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 401

    def test_isolation_multi_tenant_bilan(self, client_beta, exercice_alpha):
        resp = client_beta.get(
            f"/api/v1/compta/bilan/?exercice_id={exercice_alpha.id}"
        )
        assert resp.status_code == 400

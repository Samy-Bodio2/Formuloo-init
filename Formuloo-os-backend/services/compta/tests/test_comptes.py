"""Tests Plan de comptes — CRUD, isolation, contraintes OHADA."""

import pytest
from comptabilite.models import Compte


@pytest.mark.django_db
class TestComptes:

    def test_lister_comptes(self, client_comptable, compte_client, compte_ventes):
        resp = client_comptable.get("/api/v1/compta/comptes/")
        assert resp.status_code == 200
        assert resp.data["count"] == 2

    def test_lister_comptes_filtre_classe(self, client_comptable, compte_client, compte_ventes):
        resp = client_comptable.get("/api/v1/compta/comptes/?classe=4")
        assert resp.status_code == 200
        assert resp.data["count"] == 1
        assert resp.data["results"][0]["numero"] == "411000"

    def test_isolation_multi_tenant(self, client_beta, compte_client):
        resp = client_beta.get("/api/v1/compta/comptes/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_compte_succes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/comptes/",
            {"numero": "601000", "libelle": "Achats", "classe": 6, "type_compte": "CHARGE"},
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["numero"] == "601000"

    def test_creer_compte_classe_invalide(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/comptes/",
            {"numero": "999000", "libelle": "Invalide", "classe": 9, "type_compte": "ACTIF"},
            format="json",
        )
        assert resp.status_code == 400

    def test_modifier_compte(self, client_comptable, compte_client):
        resp = client_comptable.put(
            f"/api/v1/compta/comptes/{compte_client.id}/",
            {"libelle": "Clients — modifié"},
            format="json",
        )
        assert resp.status_code == 200
        assert resp.data["libelle"] == "Clients — modifié"

    def test_supprimer_compte_sans_ecritures(self, client_comptable, comptable_alpha):
        compte = Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id,
            numero="999001", libelle="Compte test", classe=5, type_compte="ACTIF",
        )
        resp = client_comptable.delete(f"/api/v1/compta/comptes/{compte.id}/")
        assert resp.status_code == 204

    def test_supprimer_compte_avec_ecritures(self, client_comptable, compte_client, ecriture_brouillon):
        resp = client_comptable.delete(f"/api/v1/compta/comptes/{compte_client.id}/")
        assert resp.status_code == 400

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/comptes/")
        assert resp.status_code == 401

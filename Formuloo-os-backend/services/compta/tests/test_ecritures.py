"""
Tests Écritures — validation débit=crédit, multi-tenant, statuts.
Règle OHADA fondamentale : somme(débit) == somme(crédit).
"""

import pytest
from comptabilite.models import Ecriture


@pytest.mark.django_db
class TestEcritures:

    def test_lister_ecritures(self, client_comptable, ecriture_brouillon):
        resp = client_comptable.get("/api/v1/compta/ecritures/")
        assert resp.status_code == 200
        assert resp.data["count"] == 1

    def test_isolation_multi_tenant(self, client_beta, ecriture_brouillon):
        resp = client_beta.get("/api/v1/compta/ecritures/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_ecriture_equilibree(
        self, client_comptable, exercice_alpha, journal_ventes, compte_client, compte_ventes
    ):
        resp = client_comptable.post(
            "/api/v1/compta/ecritures/",
            {
                "journal_id": journal_ventes.id,
                "exercice_id": exercice_alpha.id,
                "date_ecriture": "2024-06-15",
                "libelle": "Vente test",
                "lignes": [
                    {"compte_id": compte_client.id, "libelle": "Client", "debit": "300000.00", "credit": "0.00"},
                    {"compte_id": compte_ventes.id, "libelle": "Ventes", "debit": "0.00", "credit": "300000.00"},
                ],
            },
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["statut"] == "BROUILLON"
        assert resp.data["total_debit"] == "300000.00"
        assert resp.data["total_credit"] == "300000.00"

    def test_creer_ecriture_desequilibree(
        self, client_comptable, exercice_alpha, journal_ventes, compte_client, compte_ventes
    ):
        """Règle OHADA : débit ≠ crédit → 400."""
        resp = client_comptable.post(
            "/api/v1/compta/ecritures/",
            {
                "journal_id": journal_ventes.id,
                "exercice_id": exercice_alpha.id,
                "date_ecriture": "2024-06-15",
                "libelle": "Écriture invalide",
                "lignes": [
                    {"compte_id": compte_client.id, "debit": "500000.00", "credit": "0.00"},
                    {"compte_id": compte_ventes.id, "debit": "0.00", "credit": "300000.00"},
                ],
            },
            format="json",
        )
        assert resp.status_code == 400

    def test_creer_ecriture_exercice_cloture(
        self, client_comptable, exercice_cloture, journal_ventes, compte_client, compte_ventes
    ):
        resp = client_comptable.post(
            "/api/v1/compta/ecritures/",
            {
                "journal_id": journal_ventes.id,
                "exercice_id": exercice_cloture.id,
                "date_ecriture": "2023-06-15",
                "libelle": "Test clôturé",
                "lignes": [
                    {"compte_id": compte_client.id, "debit": "100000.00", "credit": "0.00"},
                    {"compte_id": compte_ventes.id, "debit": "0.00", "credit": "100000.00"},
                ],
            },
            format="json",
        )
        assert resp.status_code == 400

    def test_valider_ecriture(self, client_comptable, ecriture_brouillon):
        resp = client_comptable.post(
            f"/api/v1/compta/ecritures/{ecriture_brouillon.id}/valider/"
        )
        assert resp.status_code == 200
        assert resp.data["statut"] == "VALIDEE"

    def test_supprimer_ecriture_brouillon(self, client_comptable, ecriture_brouillon):
        resp = client_comptable.delete(
            f"/api/v1/compta/ecritures/{ecriture_brouillon.id}/"
        )
        assert resp.status_code == 204

    def test_supprimer_ecriture_validee(self, client_comptable, ecriture_brouillon):
        ecriture_brouillon.statut = "VALIDEE"
        ecriture_brouillon.save()
        resp = client_comptable.delete(
            f"/api/v1/compta/ecritures/{ecriture_brouillon.id}/"
        )
        assert resp.status_code == 400

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/ecritures/")
        assert resp.status_code == 401

    def test_detail_ecriture(self, client_comptable, ecriture_brouillon):
        resp = client_comptable.get(f"/api/v1/compta/ecritures/{ecriture_brouillon.id}/")
        assert resp.status_code == 200
        assert len(resp.data["lignes"]) == 2

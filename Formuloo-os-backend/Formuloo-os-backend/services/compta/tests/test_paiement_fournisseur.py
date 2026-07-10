"""Tests paiements fournisseurs — cycle complet OHADA."""

import pytest
from datetime import date
from decimal import Decimal
from comptabilite.models import (
    FactureFournisseur, LigneFactureFournisseur,
    Compte, Journal, Exercice,
)
from tests.conftest import TENANT_ALPHA, TENANT_BETA


def _setup_comptes_paiement(tenant_id):
    """Crée exercice + journal banque + comptes 401/521 pour les écritures."""
    exercice = Exercice.objects.create(
        tenant_id=tenant_id, annee=2024,
        date_debut=date(2024, 1, 1), date_fin=date(2024, 12, 31),
        statut="OUVERT",
    )
    Journal.objects.create(tenant_id=tenant_id, code="BNQ", libelle="Banque", type="BANQUE")
    Journal.objects.create(tenant_id=tenant_id, code="CAI", libelle="Caisse", type="CAISSE")
    Compte.objects.create(tenant_id=tenant_id, numero="401000", libelle="Fournisseurs", classe=4, type_compte="PASSIF")
    Compte.objects.create(tenant_id=tenant_id, numero="521000", libelle="Banque", classe=5, type_compte="ACTIF")
    Compte.objects.create(tenant_id=tenant_id, numero="571000", libelle="Caisse", classe=5, type_compte="ACTIF")
    return exercice


def _make_achat_valide(tenant_id, montant=100000):
    f = FactureFournisseur.objects.create(
        tenant_id=tenant_id,
        numero_interne="ACH-2024-0001",
        fournisseur_nom="Fournisseur Test SARL",
        devise="XAF",
        statut="VALIDEE",
        date_facture=date(2024, 6, 1),
        date_echeance=date(2024, 7, 31),
    )
    LigneFactureFournisseur.objects.create(
        facture=f, description="Fournitures", quantite=1, prix_unitaire=montant
    )
    return f


@pytest.mark.django_db
class TestPaiementFournisseur:

    def test_paiement_total_virement(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)

        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "100000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 201
        assert Decimal(resp.data["montant"]) == Decimal("100000.00")
        assert resp.data["mode_paiement"] == "VIREMENT"

        achat.refresh_from_db()
        assert achat.statut == "PAYEE"

    def test_paiement_partiel_change_statut(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)

        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "50000.00", "mode_paiement": "CHEQUE", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 201
        achat.refresh_from_db()
        assert achat.statut == "PARTIELLEMENT_PAYEE"

    def test_paiement_complement_apres_partiel(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)

        # premier paiement partiel
        client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "60000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        # second paiement complément
        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "40000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-15"},
            format="json",
        )
        assert resp.status_code == 201
        achat.refresh_from_db()
        assert achat.statut == "PAYEE"

    def test_paiement_excessif_refuse(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)

        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "200000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "MONTANT_EXCESSIF"

    def test_paiement_especes_caisse(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 50000)

        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "50000.00", "mode_paiement": "ESPECES", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["mode_paiement"] == "ESPECES"

    def test_paiement_brouillon_refuse(self, client_comptable):
        achat = FactureFournisseur.objects.create(
            tenant_id=TENANT_ALPHA, fournisseur_nom="Test",
            statut="BROUILLON",
            date_facture=date(2024, 6, 1), date_echeance=date(2024, 7, 31),
        )
        resp = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "1000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "STATUT_INVALIDE"

    def test_paiement_autre_tenant_refuse(self, client_beta):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)
        resp = client_beta.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "100000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        assert resp.status_code == 404

    def test_lister_paiements_fournisseurs(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)
        client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "100000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        resp = client_comptable.get("/api/v1/compta/paiements-fournisseurs/")
        assert resp.status_code == 200
        assert resp.data["count"] == 1

    def test_detail_paiement_fournisseur(self, client_comptable):
        _setup_comptes_paiement(TENANT_ALPHA)
        achat = _make_achat_valide(TENANT_ALPHA, 100000)
        resp_pay = client_comptable.post(
            f"/api/v1/compta/achats/{achat.id}/payer/",
            {"montant": "100000.00", "mode_paiement": "VIREMENT", "date_paiement": "2024-07-01"},
            format="json",
        )
        pk = resp_pay.data["id"]
        resp = client_comptable.get(f"/api/v1/compta/paiements-fournisseurs/{pk}/")
        assert resp.status_code == 200
        assert resp.data["fournisseur_nom"] == "Fournisseur Test SARL"

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/paiements-fournisseurs/")
        assert resp.status_code == 401

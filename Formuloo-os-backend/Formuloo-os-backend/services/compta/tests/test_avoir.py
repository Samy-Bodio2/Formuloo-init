"""Tests avoirs (notes de crédit) clients et fournisseurs."""

import pytest
from datetime import date
from comptabilite.models import (
    Facture, LigneFacture,
    FactureFournisseur, LigneFactureFournisseur,
    Compte, Journal, Exercice,
)
from tests.conftest import TENANT_ALPHA


def _setup_comptes_vente(tenant_id):
    """Crée comptes et journal nécessaires pour les écritures de ventes."""
    exercice = Exercice.objects.create(
        tenant_id=tenant_id, annee=2024,
        date_debut=date(2024, 1, 1), date_fin=date(2024, 12, 31),
        statut="OUVERT",
    )
    Journal.objects.create(tenant_id=tenant_id, code="VTE", libelle="Ventes", type="VENTES")
    Compte.objects.create(tenant_id=tenant_id, numero="411000", libelle="Clients", classe=4, type_compte="ACTIF")
    Compte.objects.create(tenant_id=tenant_id, numero="701000", libelle="Ventes", classe=7, type_compte="PRODUIT")
    return exercice


def _make_facture_emise(tenant_id):
    f = Facture.objects.create(
        tenant_id=tenant_id,
        numero="FAC-2024-0001",
        client_nom="Client Test SARL",
        client_email="client@test.cm",
        devise="XAF",
        statut="EMISE",
        date_emission=date(2024, 6, 1),
        date_echeance=date(2024, 7, 31),
    )
    LigneFacture.objects.create(facture=f, description="Prestation", quantite=1, prix_unitaire=500000)
    return f


def _make_achat_valide(tenant_id):
    f = FactureFournisseur.objects.create(
        tenant_id=tenant_id,
        numero_interne="ACH-2024-0001",
        fournisseur_nom="Fournisseur SARL",
        fournisseur_email="fournisseur@test.cm",
        devise="XAF",
        statut="VALIDEE",
        date_facture=date(2024, 6, 1),
        date_echeance=date(2024, 7, 31),
    )
    LigneFactureFournisseur.objects.create(facture=f, description="Fournitures", quantite=1, prix_unitaire=200000)
    return f


@pytest.mark.django_db
class TestAvoirClient:

    def test_avoir_sur_facture_emise(self, client_comptable):
        _setup_comptes_vente(TENANT_ALPHA)
        facture = _make_facture_emise(TENANT_ALPHA)

        resp = client_comptable.post(f"/api/v1/compta/factures/{facture.id}/avoir/")
        assert resp.status_code == 201
        assert resp.data["type_document"] == "AVOIR"
        assert resp.data["client_nom"] == facture.client_nom
        assert resp.data["numero"].startswith("AVO-")

    def test_avoir_copie_les_lignes(self, client_comptable):
        _setup_comptes_vente(TENANT_ALPHA)
        facture = _make_facture_emise(TENANT_ALPHA)
        resp = client_comptable.post(f"/api/v1/compta/factures/{facture.id}/avoir/")
        assert resp.status_code == 201
        assert len(resp.data["lignes"]) == 1
        assert resp.data["lignes"][0]["description"].startswith("[AVOIR]")

    def test_avoir_sur_facture_brouillon_refuse(self, client_comptable, facture_brouillon):
        resp = client_comptable.post(f"/api/v1/compta/factures/{facture_brouillon.id}/avoir/")
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "STATUT_INVALIDE"

    def test_avoir_sur_avoir_refuse(self, client_comptable):
        _setup_comptes_vente(TENANT_ALPHA)
        facture = _make_facture_emise(TENANT_ALPHA)
        # create avoir
        resp = client_comptable.post(f"/api/v1/compta/factures/{facture.id}/avoir/")
        avoir_id = resp.data["id"]
        # try avoir on avoir
        resp2 = client_comptable.post(f"/api/v1/compta/factures/{avoir_id}/avoir/")
        assert resp2.status_code == 400
        assert resp2.data["error"]["code"] == "DEJA_AVOIR"

    def test_avoir_autre_tenant_refuse(self, client_beta):
        _setup_comptes_vente(TENANT_ALPHA)
        facture = _make_facture_emise(TENANT_ALPHA)
        resp = client_beta.post(f"/api/v1/compta/factures/{facture.id}/avoir/")
        assert resp.status_code == 404

    def test_avoir_sans_auth(self, client_anonyme):
        resp = client_anonyme.post("/api/v1/compta/factures/999/avoir/")
        assert resp.status_code == 401

    def test_avoir_inexistant(self, client_comptable):
        resp = client_comptable.post("/api/v1/compta/factures/99999/avoir/")
        assert resp.status_code == 404


@pytest.mark.django_db
class TestAvoirFournisseur:

    def test_avoir_sur_achat_valide(self, client_comptable):
        achat = _make_achat_valide(TENANT_ALPHA)
        resp = client_comptable.post(f"/api/v1/compta/achats/{achat.id}/avoir/")
        assert resp.status_code == 201
        assert resp.data["type_document"] == "AVOIR"
        assert resp.data["fournisseur_nom"] == achat.fournisseur_nom
        assert resp.data["numero_interne"].startswith("AVO-F")

    def test_avoir_fournisseur_copie_les_lignes(self, client_comptable):
        achat = _make_achat_valide(TENANT_ALPHA)
        resp = client_comptable.post(f"/api/v1/compta/achats/{achat.id}/avoir/")
        assert resp.status_code == 201
        assert len(resp.data["lignes"]) == 1
        assert resp.data["lignes"][0]["description"].startswith("[AVOIR]")

    def test_avoir_fournisseur_sur_brouillon_refuse(self, client_comptable):
        achat = FactureFournisseur.objects.create(
            tenant_id=TENANT_ALPHA, fournisseur_nom="Test",
            statut="BROUILLON", date_facture=date(2024, 6, 1),
            date_echeance=date(2024, 7, 31),
        )
        resp = client_comptable.post(f"/api/v1/compta/achats/{achat.id}/avoir/")
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "STATUT_INVALIDE"

    def test_avoir_fournisseur_autre_tenant_refuse(self, client_beta):
        achat = _make_achat_valide(TENANT_ALPHA)
        resp = client_beta.post(f"/api/v1/compta/achats/{achat.id}/avoir/")
        assert resp.status_code == 404

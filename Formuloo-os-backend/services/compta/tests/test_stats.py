"""Tests dashboard stats — indicateurs clés comptables."""

import pytest
from datetime import date, timedelta
from decimal import Decimal
from comptabilite.models import (
    Facture, LigneFacture,
    FactureFournisseur, LigneFactureFournisseur,
    Compte, Journal, Exercice, Ecriture, LigneEcriture,
)
from tests.conftest import TENANT_ALPHA


def _setup_exercice(tenant_id):
    return Exercice.objects.create(
        tenant_id=tenant_id, annee=date.today().year,
        date_debut=date(date.today().year, 1, 1),
        date_fin=date(date.today().year, 12, 31),
        statut="OUVERT",
    )


def _make_facture(tenant_id, montant, statut="EMISE", jours_echeance=30):
    f = Facture.objects.create(
        tenant_id=tenant_id,
        client_nom="Client Test",
        devise="XAF",
        statut=statut,
        date_emission=date.today(),
        date_echeance=date.today() + timedelta(days=jours_echeance),
    )
    f.generer_numero()
    f.save()
    LigneFacture.objects.create(facture=f, description="Prestation", quantite=1, prix_unitaire=montant)
    return f


@pytest.mark.django_db
class TestStats:

    def test_stats_retourne_structure_complete(self, client_auditeur):
        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.status_code == 200
        assert "date" in resp.data
        assert "devise" in resp.data
        assert "exercice_courant" in resp.data
        assert "ventes" in resp.data
        assert "achats" in resp.data
        assert "charges_mois" in resp.data
        assert "resultat_previsionnel_exercice" in resp.data
        assert "solde_tresorerie" in resp.data

    def test_stats_sans_donnees(self, client_auditeur):
        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.status_code == 200
        assert resp.data["exercice_courant"]["id"] is None
        assert Decimal(resp.data["ventes"]["ca_mois"]) == Decimal("0")
        assert resp.data["ventes"]["nb_factures_mois"] == 0

    def test_stats_ca_mois(self, client_auditeur):
        _setup_exercice(TENANT_ALPHA)
        _make_facture(TENANT_ALPHA, 500000, statut="EMISE")
        _make_facture(TENANT_ALPHA, 300000, statut="PAYEE")

        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.status_code == 200
        # 500000 + 300000 = 800000 XAF
        assert Decimal(resp.data["ventes"]["ca_mois"]) == Decimal("800000")
        assert resp.data["ventes"]["nb_factures_mois"] == 2

    def test_stats_factures_impayees(self, client_auditeur):
        _setup_exercice(TENANT_ALPHA)
        _make_facture(TENANT_ALPHA, 200000, statut="EMISE")
        _make_facture(TENANT_ALPHA, 150000, statut="EMISE")
        _make_facture(TENANT_ALPHA, 100000, statut="PAYEE")  # ne compte pas

        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.data["ventes"]["impayees"]["nb"] == 2
        assert Decimal(resp.data["ventes"]["impayees"]["montant"]) == Decimal("350000")

    def test_stats_factures_en_retard(self, client_auditeur):
        _setup_exercice(TENANT_ALPHA)
        # facture échue (en retard)
        f_retard = Facture.objects.create(
            tenant_id=TENANT_ALPHA, client_nom="Retard",
            devise="XAF", statut="EMISE",
            date_emission=date.today() - timedelta(days=60),
            date_echeance=date.today() - timedelta(days=30),
        )
        f_retard.generer_numero()
        f_retard.save()
        LigneFacture.objects.create(facture=f_retard, description="Service", quantite=1, prix_unitaire=75000)

        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.data["ventes"]["impayees"]["en_retard"]["nb"] >= 1
        assert Decimal(resp.data["ventes"]["impayees"]["en_retard"]["montant"]) >= Decimal("75000")

    def test_stats_achats_en_attente(self, client_auditeur):
        _setup_exercice(TENANT_ALPHA)
        achat = FactureFournisseur.objects.create(
            tenant_id=TENANT_ALPHA, fournisseur_nom="Fournisseur",
            statut="VALIDEE", date_facture=date.today(),
            date_echeance=date.today() + timedelta(days=30),
        )
        LigneFactureFournisseur.objects.create(facture=achat, description="Item", quantite=1, prix_unitaire=80000)

        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.data["achats"]["en_attente_paiement"]["nb"] == 1
        assert Decimal(resp.data["achats"]["en_attente_paiement"]["montant"]) == Decimal("80000")

    def test_stats_avec_exercice_ouvert(self, client_auditeur):
        exercice = _setup_exercice(TENANT_ALPHA)
        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.data["exercice_courant"]["id"] == exercice.id
        assert resp.data["exercice_courant"]["annee"] == date.today().year

    def test_stats_sans_permission(self, db):
        from rest_framework.test import APIClient
        from tests.conftest import make_user
        user = make_user(TENANT_ALPHA, ["compta.read.ecritures"])  # pas de compta.read.etats
        client = APIClient()
        client.force_authenticate(user=user)
        resp = client.get("/api/v1/compta/stats/")
        assert resp.status_code == 403

    def test_stats_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/stats/")
        assert resp.status_code == 401

    def test_stats_devise_xaf(self, client_auditeur):
        resp = client_auditeur.get("/api/v1/compta/stats/")
        assert resp.data["devise"] == "XAF"

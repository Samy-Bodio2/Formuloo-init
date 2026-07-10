"""Tests déclaration TVA — comptes 4431/4452 OHADA."""

import pytest
from datetime import date
from decimal import Decimal
from comptabilite.models import (
    Compte, Journal, Exercice, Ecriture, LigneEcriture,
)
from tests.conftest import TENANT_ALPHA


def _setup_tva(tenant_id):
    """Crée comptes TVA + exercice + journal OD."""
    exercice = Exercice.objects.create(
        tenant_id=tenant_id, annee=2024,
        date_debut=date(2024, 1, 1), date_fin=date(2024, 12, 31),
        statut="OUVERT",
    )
    journal = Journal.objects.create(tenant_id=tenant_id, code="OD", libelle="OD", type="OD")

    c_client = Compte.objects.create(tenant_id=tenant_id, numero="411000", libelle="Clients", classe=4, type_compte="ACTIF")
    c_ventes = Compte.objects.create(tenant_id=tenant_id, numero="701000", libelle="Ventes", classe=7, type_compte="PRODUIT")
    c_tva_col = Compte.objects.create(tenant_id=tenant_id, numero="4431000", libelle="TVA collectée", classe=4, type_compte="PASSIF")
    c_four = Compte.objects.create(tenant_id=tenant_id, numero="401000", libelle="Fournisseurs", classe=4, type_compte="PASSIF")
    c_achats = Compte.objects.create(tenant_id=tenant_id, numero="601000", libelle="Achats", classe=6, type_compte="CHARGE")
    c_tva_ded = Compte.objects.create(tenant_id=tenant_id, numero="4452000", libelle="TVA déductible", classe=4, type_compte="ACTIF")

    # Écriture de vente validée — TVA collectée 19 250 XAF
    ecriture_vente = Ecriture.objects.create(
        tenant_id=tenant_id, journal=journal, exercice=exercice,
        date_ecriture=date(2024, 6, 15), libelle="Vente client A", statut="VALIDEE",
    )
    LigneEcriture.objects.create(ecriture=ecriture_vente, compte=c_client, debit=119250, credit=0)
    LigneEcriture.objects.create(ecriture=ecriture_vente, compte=c_ventes, debit=0, credit=100000)
    LigneEcriture.objects.create(ecriture=ecriture_vente, compte=c_tva_col, debit=0, credit=19250)

    # Écriture d'achat validée — TVA déductible 9 500 XAF
    ecriture_achat = Ecriture.objects.create(
        tenant_id=tenant_id, journal=journal, exercice=exercice,
        date_ecriture=date(2024, 6, 20), libelle="Achat fournisseur B", statut="VALIDEE",
    )
    LigneEcriture.objects.create(ecriture=ecriture_achat, compte=c_achats, debit=50000, credit=0)
    LigneEcriture.objects.create(ecriture=ecriture_achat, compte=c_tva_ded, debit=9500, credit=0)
    LigneEcriture.objects.create(ecriture=ecriture_achat, compte=c_four, debit=0, credit=59500)

    return exercice


@pytest.mark.django_db
class TestDeclarationTVA:

    def test_declaration_tva_solde_a_payer(self, client_auditeur):
        _setup_tva(TENANT_ALPHA)
        resp = client_auditeur.get(
            "/api/v1/compta/declarations/tva/",
            {"date_debut": "2024-06-01", "date_fin": "2024-06-30"},
        )
        assert resp.status_code == 200
        assert Decimal(resp.data["tva_collectee"]) == Decimal("19250")
        assert Decimal(resp.data["tva_deductible"]["sur_achats"]) == Decimal("9500")
        assert Decimal(resp.data["tva_deductible"]["total"]) == Decimal("9500")
        assert Decimal(resp.data["solde"]) == Decimal("9750")
        assert resp.data["resultat"] == "TVA_A_PAYER"
        assert Decimal(resp.data["montant_a_payer"]) == Decimal("9750")
        assert Decimal(resp.data["credit_reporte"]) == Decimal("0")

    def test_declaration_tva_credit(self, client_auditeur):
        """Quand TVA déductible > TVA collectée → crédit de TVA."""
        tenant_id = TENANT_ALPHA
        exercice = Exercice.objects.create(
            tenant_id=tenant_id, annee=2024,
            date_debut=date(2024, 1, 1), date_fin=date(2024, 12, 31), statut="OUVERT",
        )
        journal = Journal.objects.create(tenant_id=tenant_id, code="OD2", libelle="OD2", type="OD")
        c_tva_col = Compte.objects.create(tenant_id=tenant_id, numero="4431001", libelle="TVA col", classe=4, type_compte="PASSIF")
        c_tva_ded = Compte.objects.create(tenant_id=tenant_id, numero="4452001", libelle="TVA ded", classe=4, type_compte="ACTIF")
        c_client = Compte.objects.create(tenant_id=tenant_id, numero="411001", libelle="Client", classe=4, type_compte="ACTIF")

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id, journal=journal, exercice=exercice,
            date_ecriture=date(2024, 7, 10), libelle="test", statut="VALIDEE",
        )
        LigneEcriture.objects.create(ecriture=ecriture, compte=c_client, debit=1000, credit=0)
        LigneEcriture.objects.create(ecriture=ecriture, compte=c_tva_col, debit=0, credit=5000)
        LigneEcriture.objects.create(ecriture=ecriture, compte=c_tva_ded, debit=8000, credit=0)

        resp = client_auditeur.get(
            "/api/v1/compta/declarations/tva/",
            {"date_debut": "2024-07-01", "date_fin": "2024-07-31"},
        )
        assert resp.status_code == 200
        assert resp.data["resultat"] == "CREDIT_TVA"
        assert Decimal(resp.data["credit_reporte"]) == Decimal("3000")
        assert Decimal(resp.data["montant_a_payer"]) == Decimal("0")

    def test_declaration_tva_periode_sans_transactions(self, client_auditeur):
        resp = client_auditeur.get(
            "/api/v1/compta/declarations/tva/",
            {"date_debut": "2024-01-01", "date_fin": "2024-01-31"},
        )
        assert resp.status_code == 200
        assert Decimal(resp.data["tva_collectee"]) == Decimal("0")
        assert Decimal(resp.data["solde"]) == Decimal("0")

    def test_declaration_tva_params_manquants(self, client_auditeur):
        resp = client_auditeur.get("/api/v1/compta/declarations/tva/")
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "MISSING_PARAM"

    def test_declaration_tva_sans_permission(self, db):
        from rest_framework.test import APIClient
        from tests.conftest import make_user
        user = make_user(TENANT_ALPHA, ["compta.write.ecritures"])  # pas de compta.read.etats
        client = APIClient()
        client.force_authenticate(user=user)
        resp = client.get("/api/v1/compta/declarations/tva/?date_debut=2024-01-01&date_fin=2024-01-31")
        assert resp.status_code == 403

    def test_declaration_tva_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/declarations/tva/?date_debut=2024-01-01&date_fin=2024-01-31")
        assert resp.status_code == 401

    def test_declaration_tva_periode_response_format(self, client_auditeur):
        _setup_tva(TENANT_ALPHA)
        resp = client_auditeur.get(
            "/api/v1/compta/declarations/tva/",
            {"date_debut": "2024-06-01", "date_fin": "2024-06-30"},
        )
        assert resp.status_code == 200
        assert resp.data["periode"]["date_debut"] == "2024-06-01"
        assert resp.data["periode"]["date_fin"] == "2024-06-30"
        assert resp.data["devise"] == "XAF"
        assert "sur_immobilisations" in resp.data["tva_deductible"]
        assert "sur_services" in resp.data["tva_deductible"]

"""Tests Factures Fournisseurs — cycle achats OHADA."""

import pytest
from comptabilite.models import FactureFournisseur


@pytest.mark.django_db
class TestAchats:

    def test_lister_achats_vide(self, client_comptable):
        resp = client_comptable.get("/api/v1/compta/achats/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_isolation_multi_tenant(self, client_beta):
        resp = client_beta.get("/api/v1/compta/achats/")
        assert resp.status_code == 200
        assert resp.data["count"] == 0

    def test_creer_achat_succes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Fournisseur Alpha SARL",
                "fournisseur_email": "fournisseur@alpha.cm",
                "numero_fournisseur": "FOUR-2024-001",
                "lignes": [
                    {"description": "Fournitures bureau", "quantite": "5", "prix_unitaire": "10000.00"}
                ],
                "devise": "XAF",
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        assert resp.status_code == 201
        assert resp.data["statut"] == "BROUILLON"
        assert resp.data["fournisseur_nom"] == "Fournisseur Alpha SARL"
        assert resp.data["numero_interne"].startswith("ACH-")

    def test_creer_achat_sans_lignes(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Test",
                "lignes": [],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        assert resp.status_code == 400

    def test_creer_achat_dates_invalides(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Test",
                "lignes": [{"description": "Item", "quantite": "1", "prix_unitaire": "1000.00"}],
                "date_facture": "2024-07-01",
                "date_echeance": "2024-06-01",
            },
            format="json",
        )
        assert resp.status_code == 400

    def test_recevoir_achat(self, client_comptable):
        # Créer d'abord
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Fournisseur Test",
                "lignes": [{"description": "Item", "quantite": "1", "prix_unitaire": "50000.00"}],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        assert resp.status_code == 201
        pk = resp.data["id"]

        # Recevoir
        resp2 = client_comptable.post(f"/api/v1/compta/achats/{pk}/recevoir/")
        assert resp2.status_code == 200
        assert resp2.data["statut"] == "RECUE"
        assert resp2.data["date_reception"] is not None

    def test_recevoir_achat_deja_recu(self, client_comptable):
        # Créer + recevoir
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Test",
                "lignes": [{"description": "Item", "quantite": "1", "prix_unitaire": "50000.00"}],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        pk = resp.data["id"]
        client_comptable.post(f"/api/v1/compta/achats/{pk}/recevoir/")
        resp3 = client_comptable.post(f"/api/v1/compta/achats/{pk}/recevoir/")
        assert resp3.status_code == 400

    def test_modifier_achat_brouillon(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Initial",
                "lignes": [{"description": "Item", "quantite": "1", "prix_unitaire": "1000.00"}],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        pk = resp.data["id"]
        resp2 = client_comptable.put(
            f"/api/v1/compta/achats/{pk}/",
            {"fournisseur_nom": "Modifié"},
            format="json",
        )
        assert resp2.status_code == 200
        assert resp2.data["fournisseur_nom"] == "Modifié"

    def test_annuler_achat(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Annuler",
                "lignes": [{"description": "Item", "quantite": "1", "prix_unitaire": "1000.00"}],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        pk = resp.data["id"]
        resp2 = client_comptable.delete(f"/api/v1/compta/achats/{pk}/")
        assert resp2.status_code == 204

        from comptabilite.models import FactureFournisseur as FF
        facture = FF.objects.get(pk=pk)
        assert facture.statut == "ANNULEE"

    def test_detail_achat(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/achats/",
            {
                "fournisseur_nom": "Detail Test",
                "lignes": [{"description": "Produit", "quantite": "2", "prix_unitaire": "25000.00"}],
                "date_facture": "2024-06-01",
                "date_echeance": "2024-07-01",
            },
            format="json",
        )
        pk = resp.data["id"]
        resp2 = client_comptable.get(f"/api/v1/compta/achats/{pk}/")
        assert resp2.status_code == 200
        assert len(resp2.data["lignes"]) == 1

    def test_sans_auth(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/compta/achats/")
        assert resp.status_code == 401


@pytest.mark.django_db
class TestEndpointInternePaie:
    """Tests de l'endpoint interne HR → Compta."""

    def test_sans_service_token(self, client_comptable):
        # JWT utilisateur = refus
        resp = client_comptable.post(
            "/api/v1/compta/_internal/ecritures-paie/",
            {"tenant_id": "test"},
            format="json",
        )
        assert resp.status_code == 403

    def test_avec_service_token_valide(
        self, client_comptable, exercice_alpha, journal_ventes,
        compte_client, compte_ventes, comptable_alpha
    ):
        from rest_framework.test import APIClient
        from comptabilite.models import Compte, Journal
        import uuid

        tenant_id = str(comptable_alpha.tenant_id)

        # Créer les comptes de paie nécessaires
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="641100",
            libelle="Charges de personnel", classe=6, type_compte="CHARGE",
        )
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="421100",
            libelle="Personnel rémunérations dues", classe=4, type_compte="PASSIF",
        )
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="431100",
            libelle="CNPS à reverser", classe=4, type_compte="PASSIF",
        )
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="447100",
            libelle="IRPP à reverser", classe=4, type_compte="PASSIF",
        )
        Journal.objects.create(
            tenant_id=comptable_alpha.tenant_id, code="OD", libelle="Opérations diverses", type="OD"
        )

        client = APIClient()
        client.credentials(HTTP_X_SERVICE_TOKEN="formuloo-internal-secret-change-in-prod")

        resp = client.post(
            "/api/v1/compta/_internal/ecritures-paie/",
            {
                "tenant_id": tenant_id,
                "fiche_paie_id": str(uuid.uuid4()),
                "employe_nom": "Jean Dupont",
                "periode": "2024-06",
                "date_paiement": "2024-06-30",
                "salaire_brut": "500000.00",
                "salaire_net": "400000.00",
                "cotisation_cnps": "55000.00",
                "impot_irpp": "45000.00",
                "autres_deductions": "0.00",
                "currency": "XAF",
            },
            format="json",
        )
        assert resp.status_code == 201
        assert "ecriture_id" in resp.data
        assert resp.data["total_debit"] == resp.data["total_credit"]

    def test_sans_exercice_ouvert(self, client_comptable, comptable_alpha):
        from rest_framework.test import APIClient
        import uuid

        client = APIClient()
        client.credentials(HTTP_X_SERVICE_TOKEN="formuloo-internal-secret-change-in-prod")

        resp = client.post(
            "/api/v1/compta/_internal/ecritures-paie/",
            {
                "tenant_id": str(comptable_alpha.tenant_id),
                "employe_nom": "Test",
                "periode": "2020-01",
                "date_paiement": "2020-01-31",
                "salaire_brut": "100000.00",
                "salaire_net": "80000.00",
                "cotisation_cnps": "10000.00",
                "impot_irpp": "10000.00",
                "autres_deductions": "0.00",
            },
            format="json",
        )
        assert resp.status_code == 400
        assert resp.data["error"]["code"] in ["NO_EXERCICE", "NO_JOURNAL"]


@pytest.mark.django_db
class TestEndpointInterneAchat:
    """Tests de l'endpoint interne GesDoc → Compta (pièces d'achat certifiées)."""

    def test_sans_service_token(self, client_comptable):
        resp = client_comptable.post(
            "/api/v1/compta/_internal/ecritures-achat/",
            {"tenant_id": "test"},
            format="json",
        )
        assert resp.status_code == 403

    def test_avec_service_token_valide(self, exercice_alpha, comptable_alpha):
        from rest_framework.test import APIClient
        from comptabilite.models import Compte, Journal
        import uuid

        tenant_id = str(comptable_alpha.tenant_id)

        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="601100",
            libelle="Achats de matières et fournitures", classe=6, type_compte="CHARGE",
        )
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="445100",
            libelle="TVA déductible", classe=4, type_compte="ACTIF",
        )
        Compte.objects.create(
            tenant_id=comptable_alpha.tenant_id, numero="401100",
            libelle="Fournisseurs", classe=4, type_compte="PASSIF",
        )
        Journal.objects.create(
            tenant_id=comptable_alpha.tenant_id, code="ACH", libelle="Journal des achats", type="ACHATS",
        )

        client = APIClient()
        client.credentials(HTTP_X_SERVICE_TOKEN="formuloo-internal-secret-change-in-prod")

        resp = client.post(
            "/api/v1/compta/_internal/ecritures-achat/",
            {
                "tenant_id": tenant_id,
                "document_id": str(uuid.uuid4()),
                "document_number": "INV-2025-0217",
                "document_type": "invoice",
                "supplier": "CAMTEL BUSINESS SA",
                "date": "2024-06-02",
                "amount_ht": "1850000.00",
                "tva_rate": "19.25",
                "amount_ttc": "2206125.00",
                "currency": "XAF",
            },
            format="json",
        )
        assert resp.status_code == 201
        assert "ecriture_id" in resp.data
        assert resp.data["total_debit"] == resp.data["total_credit"]

    def test_sans_exercice_ouvert(self, comptable_alpha):
        from rest_framework.test import APIClient

        client = APIClient()
        client.credentials(HTTP_X_SERVICE_TOKEN="formuloo-internal-secret-change-in-prod")

        resp = client.post(
            "/api/v1/compta/_internal/ecritures-achat/",
            {
                "tenant_id": str(comptable_alpha.tenant_id),
                "document_number": "INV-2020-0001",
                "document_type": "invoice",
                "supplier": "Test SARL",
                "date": "2020-01-31",
                "amount_ht": "100000.00",
                "tva_rate": "19.25",
                "amount_ttc": "119250.00",
                "currency": "XAF",
            },
            format="json",
        )
        assert resp.status_code == 400
        assert resp.data["error"]["code"] in ["NO_EXERCICE", "NO_JOURNAL"]

    def test_comptes_manquants(self, exercice_alpha, comptable_alpha):
        from rest_framework.test import APIClient
        from comptabilite.models import Journal

        Journal.objects.create(
            tenant_id=comptable_alpha.tenant_id, code="ACH", libelle="Journal des achats", type="ACHATS",
        )

        client = APIClient()
        client.credentials(HTTP_X_SERVICE_TOKEN="formuloo-internal-secret-change-in-prod")

        resp = client.post(
            "/api/v1/compta/_internal/ecritures-achat/",
            {
                "tenant_id": str(comptable_alpha.tenant_id),
                "document_number": "INV-2024-0099",
                "document_type": "invoice",
                "supplier": "Fournisseur Sans Comptes SARL",
                "date": "2024-06-02",
                "amount_ht": "100000.00",
                "tva_rate": "19.25",
                "amount_ttc": "119250.00",
                "currency": "XAF",
            },
            format="json",
        )
        assert resp.status_code == 400
        assert resp.data["error"]["code"] == "COMPTES_MANQUANTS"

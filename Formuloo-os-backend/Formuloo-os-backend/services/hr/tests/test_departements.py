"""
Tests Départements — Formuloo OS
Teste tous les endpoints des départements.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, organigramme, filtres, isolation
"""

import uuid

import pytest


@pytest.mark.django_db
class TestDepartementsListe:

    # ── GET LIST ──────────────────────────────────────────

    def test_lister_departements_succes(
        self, client_alpha, departement_alpha, departement_rh_alpha
    ):
        """
        Given : deux départements dans tenant Alpha
        When  : l'admin liste les départements
        Then  : 2 départements retournés — 200
        """
        response = client_alpha.get("/api/v1/hr/departements/")
        assert response.status_code == 200
        assert response.data["count"] == 2

    def test_lister_departements_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les départements
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/departements/")
        assert response.status_code == 401

    def test_isolation_multi_tenant_liste(
        self, client_alpha, departement_alpha, client_beta, departement_beta
    ):
        """
        Given : chaque tenant a un département
        When  : Alpha liste ses départements
        Then  : Alpha ne voit pas les départements de Beta
        """
        response_alpha = client_alpha.get("/api/v1/hr/departements/")
        response_beta = client_beta.get("/api/v1/hr/departements/")

        codes_alpha = [d["code"] for d in response_alpha.data["results"]]
        codes_beta = [d["code"] for d in response_beta.data["results"]]

        assert "INFO" in codes_alpha
        assert "FIN" not in codes_alpha
        assert "FIN" in codes_beta
        assert "INFO" not in codes_beta

    def test_filtre_is_active(self, client_alpha, departement_alpha):
        """
        Given : un département actif
        When  : on filtre is_active=true
        Then  : département retourné
        """
        response = client_alpha.get("/api/v1/hr/departements/?is_active=true")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_recherche_full_text(self, client_alpha, departement_alpha):
        """
        Given : département Informatique
        When  : on recherche 'Info'
        Then  : département retourné
        """
        response = client_alpha.get("/api/v1/hr/departements/?search=Info")
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["results"][0]["code"] == "INFO"

    def test_pagination(self, client_alpha, departement_alpha, departement_rh_alpha):
        """
        Given : 2 départements
        When  : on demande page_size=1
        Then  : 1 département retourné + next non null
        """
        response = client_alpha.get("/api/v1/hr/departements/?page=1&page_size=1")
        assert response.status_code == 200
        assert len(response.data["results"]) == 1
        assert response.data["next"] is not None


@pytest.mark.django_db
class TestDepartementsCreate:

    def test_creer_departement_succes(self, client_alpha, db):
        """
        Given : admin connecté
        When  : il crée un département
        Then  : département créé — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/departements/",
            {"nom": "Comptabilité", "code": "COMPTA", "devise": "XAF"},
            format="json",
        )
        assert response.status_code == 201
        assert response.data["nom"] == "Comptabilité"
        assert response.data["code"] == "COMPTA"

    def test_creer_departement_code_existant(self, client_alpha, departement_alpha):
        """
        Given : département avec code INFO existe
        When  : on crée avec le même code
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/departements/", {"nom": "Autre", "code": "INFO"}, format="json"
        )
        assert response.status_code == 400

    def test_creer_departement_sans_nom(self, client_alpha, db):
        """
        Given : admin connecté
        When  : il crée sans nom
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/departements/", {"code": "TEST"}, format="json"
        )
        assert response.status_code == 400

    def test_creer_sous_departement(self, client_alpha, departement_alpha):
        """
        Given : département parent existe
        When  : on crée un sous-département
        Then  : sous-département créé avec parent — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/departements/",
            {
                "nom": "Frontend",
                "code": "INFO-FRONT",
                "parent_id": str(departement_alpha.id),
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["parent"]["id"] == str(departement_alpha.id)

    def test_creer_departement_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on crée un département
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/departements/", {"nom": "Test", "code": "TEST"}, format="json"
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestDepartementsDetail:

    def test_detail_departement_succes(self, client_alpha, departement_alpha):
        """
        Given : département existe dans le tenant
        When  : on demande son détail
        Then  : département retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/departements/{departement_alpha.id}/")
        assert response.status_code == 200
        assert response.data["code"] == "INFO"

    def test_detail_departement_inexistant(self, client_alpha, db):
        """
        Given : aucun département avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        fake_id = uuid.uuid4()
        response = client_alpha.get(f"/api/v1/hr/departements/{fake_id}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, departement_beta):
        """
        Given : département appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/departements/{departement_beta.id}/")
        assert response.status_code == 404

    def test_modifier_departement_succes(self, client_alpha, departement_alpha):
        """
        Given : département existe
        When  : on le modifie
        Then  : département modifié — 200
        """
        response = client_alpha.put(
            f"/api/v1/hr/departements/{departement_alpha.id}/",
            {"nom": "Informatique Updated"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["nom"] == "Informatique Updated"

    def test_supprimer_departement_sans_employes(self, client_alpha, departement_alpha):
        """
        Given : département sans employés actifs
        When  : on le supprime
        Then  : département désactivé — 204
        """
        response = client_alpha.delete(
            f"/api/v1/hr/departements/{departement_alpha.id}/"
        )
        assert response.status_code == 204
        departement_alpha.refresh_from_db()
        assert departement_alpha.is_active is False

    def test_supprimer_departement_avec_employes(
        self, client_alpha, departement_alpha, employe_alpha
    ):
        """
        Given : département avec employés actifs
        When  : on tente de le supprimer
        Then  : erreur 400
        """
        response = client_alpha.delete(
            f"/api/v1/hr/departements/{departement_alpha.id}/"
        )
        assert response.status_code == 400


@pytest.mark.django_db
class TestDepartementsTree:

    def test_organigramme_succes(self, client_alpha, departement_alpha):
        """
        Given : département racine existe
        When  : on demande l'organigramme
        Then  : arbre retourné — 200
        """
        response = client_alpha.get("/api/v1/hr/departements/tree/")
        assert response.status_code == 200
        assert len(response.data) == 1
        assert response.data[0]["code"] == "INFO"

    def test_organigramme_avec_sous_departements(self, client_alpha, departement_alpha):
        """
        Given : département parent avec sous-départements
        When  : on demande l'organigramme
        Then  : arbre imbriqué retourné
        """
        from rh.models import Departement
        from tests.conftest import TENANT_ALPHA

        # Créer sous-département
        Departement.objects.create(
            tenant_id=TENANT_ALPHA,
            nom="Frontend",
            code="INFO-FRONT",
            parent=departement_alpha,
            is_active=True,
        )

        response = client_alpha.get("/api/v1/hr/departements/tree/")
        assert response.status_code == 200
        assert len(response.data[0]["sous_departements"]) == 1
        assert response.data[0]["sous_departements"][0]["code"] == "INFO-FRONT"

    def test_organigramme_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on demande l'organigramme
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/departements/tree/")
        assert response.status_code == 401

"""
Tests Postes — Formuloo OS
Teste tous les endpoints des postes.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, filtres, isolation, validation
"""

import uuid

import pytest


@pytest.mark.django_db
class TestPostesListe:

    def test_lister_postes_succes(self, client_alpha, poste_dev_alpha, poste_rh_alpha):
        """
        Given : deux postes dans tenant Alpha
        When  : l'admin liste les postes
        Then  : 2 postes retournés — 200
        """
        response = client_alpha.get("/api/v1/hr/postes/")
        assert response.status_code == 200
        assert response.data["count"] == 2

    def test_lister_postes_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les postes
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/postes/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, poste_dev_alpha, client_beta, departement_beta
    ):
        """
        Given : chaque tenant a ses postes
        When  : Alpha liste ses postes
        Then  : Alpha ne voit pas les postes de Beta
        """
        from rh.models import Poste
        from tests.conftest import TENANT_BETA

        Poste.objects.create(
            tenant_id=TENANT_BETA,
            departement=departement_beta,
            titre="Comptable",
            code="COMPTA",
            niveau="senior",
            is_active=True,
        )

        response_alpha = client_alpha.get("/api/v1/hr/postes/")
        codes_alpha = [p["code"] for p in response_alpha.data["results"]]
        assert "COMPTA" not in codes_alpha

    def test_filtre_par_departement(
        self, client_alpha, poste_dev_alpha, poste_rh_alpha, departement_alpha
    ):
        """
        Given : postes dans différents départements
        When  : on filtre par département INFO
        Then  : seulement les postes INFO retournés
        """
        response = client_alpha.get(
            f"/api/v1/hr/postes/?departement_id={departement_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["results"][0]["code"] == "DEV-BACK"

    def test_filtre_par_niveau(self, client_alpha, poste_dev_alpha, poste_rh_alpha):
        """
        Given : postes de niveaux différents
        When  : on filtre par niveau senior
        Then  : seulement les postes senior retournés
        """
        response = client_alpha.get("/api/v1/hr/postes/?niveau=senior")
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["results"][0]["code"] == "DEV-BACK"

    def test_recherche_full_text(self, client_alpha, poste_dev_alpha):
        """
        Given : poste Développeur Backend
        When  : on recherche 'Dev'
        Then  : poste retourné
        """
        response = client_alpha.get("/api/v1/hr/postes/?search=Développeur")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestPostesCreate:

    def test_creer_poste_succes(self, client_alpha, departement_alpha):
        """
        Given : admin connecté
        When  : il crée un poste
        Then  : poste créé — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/postes/",
            {
                "titre": "Chef de Projet",
                "code": "CHEF-PROJ",
                "departement_id": str(departement_alpha.id),
                "niveau": "manager",
                "salaire_min": 600000,
                "salaire_max": 1000000,
                "devise": "XAF",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["code"] == "CHEF-PROJ"
        assert response.data["niveau"] == "manager"

    def test_creer_poste_code_existant(
        self, client_alpha, poste_dev_alpha, departement_alpha
    ):
        """
        Given : poste avec code DEV-BACK existe
        When  : on crée avec le même code
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/postes/",
            {
                "titre": "Autre Dev",
                "code": "DEV-BACK",
                "departement_id": str(departement_alpha.id),
                "niveau": "junior",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_poste_salaire_invalide(self, client_alpha, departement_alpha):
        """
        Given : salaire_min > salaire_max
        When  : on crée le poste
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/postes/",
            {
                "titre": "Poste Test",
                "code": "TEST-001",
                "departement_id": str(departement_alpha.id),
                "niveau": "junior",
                "salaire_min": 900000,
                "salaire_max": 400000,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_poste_departement_inexistant(self, client_alpha, db):
        """
        Given : département inexistant
        When  : on crée un poste avec cet ID
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/postes/",
            {
                "titre": "Poste Test",
                "code": "TEST-002",
                "departement_id": str(uuid.uuid4()),
                "niveau": "junior",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_poste_sans_auth(self, client_anonyme, departement_alpha):
        """
        Given : aucun token JWT
        When  : on crée un poste
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/postes/",
            {
                "titre": "Poste Test",
                "code": "TEST-003",
                "departement_id": str(departement_alpha.id),
                "niveau": "junior",
            },
            format="json",
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestPostesDetail:

    def test_detail_poste_succes(self, client_alpha, poste_dev_alpha):
        """
        Given : poste existe dans le tenant
        When  : on demande son détail
        Then  : poste retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/postes/{poste_dev_alpha.id}/")
        assert response.status_code == 200
        assert response.data["code"] == "DEV-BACK"

    def test_detail_poste_inexistant(self, client_alpha, db):
        """
        Given : aucun poste avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/postes/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, departement_beta):
        """
        Given : poste appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import Poste
        from tests.conftest import TENANT_BETA

        poste_beta = Poste.objects.create(
            tenant_id=TENANT_BETA,
            departement=departement_beta,
            titre="Comptable Beta",
            code="COMPTA-BETA",
            niveau="senior",
            is_active=True,
        )
        response = client_alpha.get(f"/api/v1/hr/postes/{poste_beta.id}/")
        assert response.status_code == 404

    def test_modifier_poste_succes(self, client_alpha, poste_dev_alpha):
        """
        Given : poste existe
        When  : on le modifie
        Then  : poste modifié — 200
        """
        response = client_alpha.put(
            f"/api/v1/hr/postes/{poste_dev_alpha.id}/",
            {"titre": "Développeur Backend Senior Updated"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["titre"] == "Développeur Backend Senior Updated"

    def test_supprimer_poste_sans_employes(self, client_alpha, poste_dev_alpha):
        """
        Given : poste sans employés actifs
        When  : on le supprime
        Then  : poste désactivé — 204
        """
        # Désactiver l'employé lié si existe
        response = client_alpha.delete(f"/api/v1/hr/postes/{poste_dev_alpha.id}/")
        assert response.status_code == 204
        poste_dev_alpha.refresh_from_db()
        assert poste_dev_alpha.is_active is False

    def test_supprimer_poste_avec_employes(
        self, client_alpha, poste_dev_alpha, employe_alpha
    ):
        """
        Given : poste avec employés actifs
        When  : on tente de le supprimer
        Then  : erreur 400
        """
        response = client_alpha.delete(f"/api/v1/hr/postes/{poste_dev_alpha.id}/")
        assert response.status_code == 400

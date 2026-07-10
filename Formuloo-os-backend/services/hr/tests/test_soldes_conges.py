"""
Tests Soldes Congés — Formuloo OS
Teste les endpoints des soldes de congés.
Conforme ADR-001 : isolation multi-tenant
Couvre : liste, détail, isolation, calculs
"""

import uuid

import pytest


@pytest.mark.django_db
class TestSoldesCongesListe:

    def test_lister_soldes_succes(self, client_alpha, solde_conges_alpha):
        """
        Given : un solde de congés dans tenant Alpha
        When  : l'admin liste les soldes
        Then  : 1 solde retourné — 200
        """
        response = client_alpha.get("/api/v1/hr/soldes-conges/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_lister_soldes_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les soldes
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/soldes-conges/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, solde_conges_alpha, client_beta, employe_beta
    ):
        """
        Given : solde dans tenant Alpha
        When  : tenant Beta liste ses soldes
        Then  : Beta ne voit pas les soldes de Alpha
        """
        from rh.models import SoldeConges
        from tests.conftest import TENANT_BETA

        SoldeConges.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            type_conge="annuel",
            annee=2024,
            jours_acquis=30,
            jours_pris=0,
        )

        response_alpha = client_alpha.get("/api/v1/hr/soldes-conges/")
        response_beta = client_beta.get("/api/v1/hr/soldes-conges/")

        assert response_alpha.data["count"] == 1
        assert response_beta.data["count"] == 1

    def test_filtre_par_employe(self, client_alpha, solde_conges_alpha, employe_alpha):
        """
        Given : solde de l'employé Alpha
        When  : on filtre par employe_id
        Then  : solde retourné
        """
        response = client_alpha.get(
            f"/api/v1/hr/soldes-conges/?employe_id={employe_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_annee(self, client_alpha, solde_conges_alpha):
        """
        Given : solde pour 2024
        When  : on filtre par annee=2024
        Then  : solde retourné
        """
        response = client_alpha.get("/api/v1/hr/soldes-conges/?annee=2024")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_type_conge(self, client_alpha, solde_conges_alpha):
        """
        Given : solde de type annuel
        When  : on filtre par type_conge=annuel
        Then  : solde retourné
        """
        response = client_alpha.get("/api/v1/hr/soldes-conges/?type_conge=annuel")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestSoldesCongesDetail:

    def test_detail_solde_succes(self, client_alpha, solde_conges_alpha):
        """
        Given : solde existe dans le tenant
        When  : on demande son détail
        Then  : solde retourné avec jours_restants — 200
        """
        response = client_alpha.get(
            f"/api/v1/hr/soldes-conges/{solde_conges_alpha.id}/"
        )
        assert response.status_code == 200
        assert response.data["type_conge"] == "annuel"
        assert response.data["jours_acquis"] == "30.00"
        assert response.data["jours_pris"] == "0.00"
        assert response.data["jours_restants"] == 30.0

    def test_detail_solde_inexistant(self, client_alpha, db):
        """
        Given : aucun solde avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/soldes-conges/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : solde appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import SoldeConges
        from tests.conftest import TENANT_BETA

        solde_beta = SoldeConges.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            type_conge="annuel",
            annee=2024,
            jours_acquis=30,
            jours_pris=0,
        )
        response = client_alpha.get(f"/api/v1/hr/soldes-conges/{solde_beta.id}/")
        assert response.status_code == 404


@pytest.mark.django_db
class TestSoldesCongesCalculs:

    def test_jours_restants_calcule(self, solde_conges_alpha):
        """
        Given : 30 jours acquis, 0 pris
        When  : on calcule les jours restants
        Then  : jours_restants = 30
        """
        assert solde_conges_alpha.jours_restants == 30

    def test_decrementer_solde(self, solde_conges_alpha):
        """
        Given : 30 jours disponibles
        When  : on décrémente de 10 jours
        Then  : jours_pris = 10, jours_restants = 20
        """
        solde_conges_alpha.decrementer(10)
        solde_conges_alpha.refresh_from_db()
        assert solde_conges_alpha.jours_pris == 10
        assert solde_conges_alpha.jours_restants == 20

    def test_decrementer_solde_insuffisant(self, solde_conges_alpha):
        """
        Given : 30 jours disponibles
        When  : on tente de décrémente de 35 jours
        Then  : ValueError levée
        """
        with pytest.raises(ValueError):
            solde_conges_alpha.decrementer(35)

    def test_incrementer_solde(self, solde_conges_alpha):
        """
        Given : 10 jours pris
        When  : on incrémente de 5 jours (annulation)
        Then  : jours_pris = 5
        """
        solde_conges_alpha.jours_pris = 10
        solde_conges_alpha.save()

        solde_conges_alpha.incrementer(5)
        solde_conges_alpha.refresh_from_db()
        assert solde_conges_alpha.jours_pris == 5

    def test_has_enough_days_vrai(self, solde_conges_alpha):
        """
        Given : 30 jours disponibles
        When  : on vérifie si 15 jours sont disponibles
        Then  : True
        """
        assert solde_conges_alpha.has_enough_days(15) is True

    def test_has_enough_days_faux(self, solde_conges_alpha):
        """
        Given : 30 jours disponibles
        When  : on vérifie si 35 jours sont disponibles
        Then  : False
        """
        assert solde_conges_alpha.has_enough_days(35) is False

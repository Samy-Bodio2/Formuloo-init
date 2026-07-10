"""
Tests Contrats — Formuloo OS
Teste tous les endpoints des contrats.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, validation, isolation, workflow
"""

import uuid

import pytest


@pytest.mark.django_db
class TestContratsListe:

    def test_lister_contrats_succes(self, client_alpha, contrat_alpha):
        """
        Given : un contrat dans tenant Alpha
        When  : l'admin liste les contrats
        Then  : 1 contrat retourné — 200
        """
        response = client_alpha.get("/api/v1/hr/contrats/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_lister_contrats_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les contrats
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/contrats/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, contrat_alpha, client_beta, employe_beta
    ):
        """
        Given : contrat dans tenant Alpha
        When  : tenant Beta liste ses contrats
        Then  : Beta ne voit pas les contrats de Alpha
        """
        response_beta = client_beta.get("/api/v1/hr/contrats/")
        assert response_beta.data["count"] == 0

    def test_filtre_par_employe(self, client_alpha, contrat_alpha, employe_alpha):
        """
        Given : contrat de l'employé Alpha
        When  : on filtre par employe_id
        Then  : contrat retourné
        """
        response = client_alpha.get(
            f"/api/v1/hr/contrats/?employe_id={employe_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_type(self, client_alpha, contrat_alpha):
        """
        Given : contrat CDI
        When  : on filtre par type CDI
        Then  : contrat retourné
        """
        response = client_alpha.get("/api/v1/hr/contrats/?type_contrat=CDI")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_statut(self, client_alpha, contrat_alpha):
        """
        Given : contrat actif
        When  : on filtre par statut actif
        Then  : contrat retourné
        """
        response = client_alpha.get("/api/v1/hr/contrats/?statut=actif")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestContratsCreate:

    def test_creer_contrat_cdi_succes(self, client_alpha, employe_alpha):
        """
        Given : admin connecté
        When  : il crée un contrat CDI
        Then  : contrat créé avec numéro auto — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(employe_alpha.id),
                "type": "CDI",
                "start_date": "2024-01-15",
                "gross_salary": 600000,
                "currency": "XAF",
                "work_hours_per_week": 40,
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["type"] == "CDI"
        assert response.data["is_active"] is True
        assert response.data["numero"].startswith("CTR-")

    def test_creer_contrat_cdd_sans_date_fin(self, client_alpha, employe_alpha):
        """
        Given : contrat CDD sans date de fin
        When  : on crée le contrat
        Then  : erreur 400 — CDD doit avoir une date de fin
        """
        response = client_alpha.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(employe_alpha.id),
                "type": "CDD",
                "start_date": "2024-01-15",
                "gross_salary": 400000,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_contrat_cdd_avec_date_fin(self, client_alpha, employe_alpha):
        """
        Given : contrat CDD avec date de fin
        When  : on crée le contrat
        Then  : contrat créé — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(employe_alpha.id),
                "type": "CDD",
                "start_date": "2024-01-15",
                "end_date": "2024-12-31",
                "gross_salary": 400000,
            },
            format="json",
        )
        assert response.status_code == 201

    def test_creer_contrat_date_fin_avant_debut(self, client_alpha, employe_alpha):
        """
        Given : date_fin avant date_debut
        When  : on crée le contrat
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(employe_alpha.id),
                "type": "CDD",
                "start_date": "2024-12-31",
                "end_date": "2024-01-01",
                "gross_salary": 400000,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_contrat_employe_inexistant(self, client_alpha, db):
        """
        Given : employé inexistant
        When  : on crée un contrat
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(uuid.uuid4()),
                "type": "CDI",
                "start_date": "2024-01-15",
                "gross_salary": 400000,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_contrat_sans_auth(self, client_anonyme, employe_alpha):
        """
        Given : aucun token JWT
        When  : on crée un contrat
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/contrats/",
            {
                "employe_id": str(employe_alpha.id),
                "type": "CDI",
                "start_date": "2024-01-15",
                "gross_salary": 400000,
            },
            format="json",
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestContratsDetail:

    def test_detail_contrat_succes(self, client_alpha, contrat_alpha):
        """
        Given : contrat existe dans le tenant
        When  : on demande son détail
        Then  : contrat retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/contrats/{contrat_alpha.id}/")
        assert response.status_code == 200
        assert response.data["type"] == "CDI"

    def test_detail_contrat_inexistant(self, client_alpha, db):
        """
        Given : aucun contrat avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/contrats/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : contrat appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import Contrat
        from tests.conftest import TENANT_BETA

        contrat_beta = Contrat.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            type="CDI",
            start_date="2024-01-15",
            gross_salary=500000,
            statut="actif",
            is_active=True,
        )
        response = client_alpha.get(f"/api/v1/hr/contrats/{contrat_beta.id}/")
        assert response.status_code == 404

    def test_modifier_contrat_succes(self, client_alpha, contrat_alpha):
        """
        Given : contrat existe
        When  : on modifie le salaire
        Then  : contrat modifié — 200
        """
        response = client_alpha.put(
            f"/api/v1/hr/contrats/{contrat_alpha.id}/",
            {"gross_salary": 700000},
            format="json",
        )
        assert response.status_code == 200
        assert float(response.data["gross_salary"]) == 700000

    def test_resilier_contrat_succes(self, client_alpha, contrat_alpha):
        """
        Given : contrat actif
        When  : on le résilie
        Then  : contrat résilié — 204
        """
        response = client_alpha.delete(f"/api/v1/hr/contrats/{contrat_alpha.id}/")
        assert response.status_code == 204
        contrat_alpha.refresh_from_db()
        assert contrat_alpha.statut == "resilie"
        assert contrat_alpha.is_active is False

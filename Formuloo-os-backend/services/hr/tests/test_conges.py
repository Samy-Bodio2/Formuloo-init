"""
Tests Congés — Formuloo OS
Teste tous les endpoints des congés.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, workflow approbation/rejet,
         validation solde, isolation
"""

import uuid
from datetime import date

import pytest


@pytest.mark.django_db
class TestCongesListe:

    def test_lister_conges_succes(self, client_alpha, conge_alpha):
        """
        Given : une demande de congé dans tenant Alpha
        When  : l'admin liste les congés
        Then  : 1 congé retourné — 200
        """
        response = client_alpha.get("/api/v1/hr/leaves/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_lister_conges_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les congés
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/leaves/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, conge_alpha, client_beta, employe_beta
    ):
        """
        Given : congé dans tenant Alpha
        When  : tenant Beta liste ses congés
        Then  : Beta ne voit pas les congés de Alpha
        """
        response_beta = client_beta.get("/api/v1/hr/leaves/")
        assert response_beta.data["count"] == 0

    def test_filtre_par_statut(self, client_alpha, conge_alpha):
        """
        Given : congé en attente
        When  : on filtre par statut pending
        Then  : congé retourné
        """
        response = client_alpha.get("/api/v1/hr/leaves/?statut=pending")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_type_conge(self, client_alpha, conge_alpha):
        """
        Given : congé annuel
        When  : on filtre par type annuel
        Then  : congé retourné
        """
        response = client_alpha.get("/api/v1/hr/leaves/?type_conge=annuel")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_employe(self, client_alpha, conge_alpha, employe_alpha):
        """
        Given : congé de l'employé Alpha
        When  : on filtre par employe_id
        Then  : congé retourné
        """
        response = client_alpha.get(f"/api/v1/hr/leaves/?employe_id={employe_alpha.id}")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestCongesCreate:

    def test_soumettre_conge_succes(self, client_alpha, employe_alpha, user_alpha):
        """
        Given : employé connecté
        When  : il soumet une demande de congé
        Then  : demande créée en statut pending — 201
        """
        # Lier l'employé au user via auth_user_id
        # auth_user_id = UUID du user dans le service Auth
        # extrait du JWT par TenantMiddleware
        employe_alpha.user_id = user_alpha.auth_user_id
        employe_alpha.save()

        response = client_alpha.post(
            "/api/v1/hr/leaves/",
            {
                "type_conge": "annuel",
                "start_date": "2024-08-01",
                "end_date": "2024-08-15",
                "reason": "Vacances annuelles",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["status"] == "pending"
        assert response.data["type_conge"] == "annuel"
        assert response.data["days"] == 15

    def test_soumettre_conge_date_invalide(
        self, client_alpha, employe_alpha, user_alpha
    ):
        """
        Given : date_fin avant date_debut
        When  : on soumet la demande
        Then  : erreur 400
        """
        employe_alpha.user_id = user_alpha.auth_user_id
        employe_alpha.save()

        response = client_alpha.post(
            "/api/v1/hr/leaves/",
            {
                "type_conge": "annuel",
                "start_date": "2024-08-15",
                "end_date": "2024-08-01",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_soumettre_conge_calcul_jours(
        self, client_alpha, employe_alpha, user_alpha
    ):
        """
        Given : congé du 01/08 au 10/08 (10 jours)
        When  : on soumet la demande
        Then  : days = 10 calculé automatiquement
        """
        employe_alpha.user_id = user_alpha.auth_user_id
        employe_alpha.save()

        response = client_alpha.post(
            "/api/v1/hr/leaves/",
            {
                "type_conge": "annuel",
                "start_date": "2024-08-01",
                "end_date": "2024-08-10",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["days"] == 10

    def test_soumettre_conge_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on soumet une demande
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/leaves/",
            {
                "type_conge": "annuel",
                "start_date": "2024-08-01",
                "end_date": "2024-08-10",
            },
            format="json",
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestCongesDetail:

    def test_detail_conge_succes(self, client_alpha, conge_alpha):
        """
        Given : congé existe dans le tenant
        When  : on demande son détail
        Then  : congé retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/leaves/{conge_alpha.id}/")
        assert response.status_code == 200
        assert response.data["type_conge"] == "annuel"
        assert response.data["status"] == "pending"

    def test_detail_conge_inexistant(self, client_alpha, db):
        """
        Given : aucun congé avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/leaves/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : congé appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import Conge
        from tests.conftest import TENANT_BETA

        conge_beta = Conge.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            type_conge="annuel",
            start_date=date(2024, 7, 1),
            end_date=date(2024, 7, 10),
            status="pending",
        )
        response = client_alpha.get(f"/api/v1/hr/leaves/{conge_beta.id}/")
        assert response.status_code == 404

    def test_annuler_conge_pending_succes(self, client_alpha, conge_alpha):
        """
        Given : congé en attente
        When  : on l'annule
        Then  : statut annule — 204
        """
        response = client_alpha.delete(f"/api/v1/hr/leaves/{conge_alpha.id}/")
        assert response.status_code == 204
        conge_alpha.refresh_from_db()
        assert conge_alpha.status == "annule"

    def test_annuler_conge_approuve_impossible(self, client_alpha, conge_alpha):
        """
        Given : congé déjà approuvé
        When  : on tente de l'annuler
        Then  : erreur 400
        """
        conge_alpha.status = "approved"
        conge_alpha.save()

        response = client_alpha.delete(f"/api/v1/hr/leaves/{conge_alpha.id}/")
        assert response.status_code == 400


@pytest.mark.django_db
class TestCongesWorkflow:

    def test_approuver_conge_succes(
        self, client_alpha, conge_alpha, employe_rh_alpha, user_alpha
    ):
        """
        Given : congé en attente
        When  : le RH l'approuve
        Then  : statut approved — 200
        """
        # Lier le RH Manager au user_alpha
        employe_rh_alpha.user_id = user_alpha.auth_user_id
        employe_rh_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/leaves/{conge_alpha.id}/approve/",
            {"commentaire": "Approuvé"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["status"] == "approved"

    def test_approuver_conge_deja_approuve(self, client_alpha, conge_alpha):
        """
        Given : congé déjà approuvé
        When  : on tente d'approuver à nouveau
        Then  : erreur 400
        """
        conge_alpha.status = "approved"
        conge_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/leaves/{conge_alpha.id}/approve/", format="json"
        )
        assert response.status_code == 400

    def test_rejeter_conge_succes(
        self, client_alpha, conge_alpha, employe_rh_alpha, user_alpha
    ):
        """
        Given : congé en attente
        When  : le RH le rejette avec un motif
        Then  : statut rejected — 200
        """
        # Lier le RH Manager au user_alpha
        employe_rh_alpha.user_id = user_alpha.auth_user_id
        employe_rh_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/leaves/{conge_alpha.id}/reject/",
            {"reason": "Période chargée"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["status"] == "rejected"
        assert response.data["commentaire_decision"] == "Période chargée"

    def test_rejeter_conge_sans_motif(self, client_alpha, conge_alpha):
        """
        Given : congé en attente
        When  : on rejette sans motif
        Then  : erreur 400 — motif obligatoire
        """
        response = client_alpha.post(
            f"/api/v1/hr/leaves/{conge_alpha.id}/reject/", {}, format="json"
        )
        assert response.status_code == 400

    def test_approuver_conge_solde_insuffisant(
        self, client_alpha, employe_alpha, employe_rh_alpha, user_alpha
    ):
        """
        Given : employé sans solde suffisant
        When  : on approuve une demande de 15 jours
        Then  : erreur 400 — solde insuffisant
        """
        from rh.models import Conge, SoldeConges
        from tests.conftest import TENANT_ALPHA

        # Créer un solde de 5 jours seulement
        SoldeConges.objects.create(
            tenant_id=TENANT_ALPHA,
            employee=employe_alpha,
            type_conge="annuel",
            annee=2024,
            jours_acquis=5,
            jours_pris=0,
        )

        # Créer une demande de 15 jours
        conge = Conge.objects.create(
            tenant_id=TENANT_ALPHA,
            employee=employe_alpha,
            type_conge="annuel",
            start_date=date(2024, 9, 1),
            end_date=date(2024, 9, 15),
            status="pending",
        )

        # Lier le RH Manager au user_alpha
        employe_rh_alpha.user_id = user_alpha.auth_user_id
        employe_rh_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/leaves/{conge.id}/approve/", format="json"
        )
        assert response.status_code == 400
        assert "insuffisant" in response.data["error"]["message"].lower()

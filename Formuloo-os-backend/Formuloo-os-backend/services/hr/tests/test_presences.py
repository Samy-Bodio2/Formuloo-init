"""
Tests Présences — Formuloo OS
Teste tous les endpoints des présences.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, calcul heures, filtres, isolation
"""

import uuid
from datetime import date, time

import pytest


@pytest.mark.django_db
class TestPresencesListe:

    def test_lister_presences_succes(self, client_alpha, presence_alpha):
        """
        Given : une présence dans tenant Alpha
        When  : l'admin liste les présences
        Then  : 1 présence retournée — 200
        """
        response = client_alpha.get("/api/v1/hr/presences/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_lister_presences_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les présences
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/presences/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, presence_alpha, client_beta, employe_beta
    ):
        """
        Given : présence dans tenant Alpha
        When  : tenant Beta liste ses présences
        Then  : Beta ne voit pas les présences de Alpha
        """
        response_beta = client_beta.get("/api/v1/hr/presences/")
        assert response_beta.data["count"] == 0

    def test_filtre_par_employe(self, client_alpha, presence_alpha, employe_alpha):
        """
        Given : présence de l'employé Alpha
        When  : on filtre par employe_id
        Then  : présence retournée
        """
        response = client_alpha.get(
            f"/api/v1/hr/presences/?employe_id={employe_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_date(self, client_alpha, presence_alpha):
        """
        Given : présence du 10/06/2024
        When  : on filtre par date exacte
        Then  : présence retournée
        """
        response = client_alpha.get("/api/v1/hr/presences/?date=2024-06-10")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_statut(self, client_alpha, presence_alpha):
        """
        Given : présence avec statut present
        When  : on filtre par statut=present
        Then  : présence retournée
        """
        response = client_alpha.get("/api/v1/hr/presences/?statut=present")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_periode(self, client_alpha, presence_alpha):
        """
        Given : présence du 10/06/2024
        When  : on filtre du 01/06 au 30/06
        Then  : présence retournée
        """
        response = client_alpha.get(
            "/api/v1/hr/presences/" "?date_debut=2024-06-01&date_fin=2024-06-30"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestPresencesCreate:

    def test_creer_presence_succes(self, client_alpha, employe_alpha):
        """
        Given : admin connecté
        When  : il enregistre une présence
        Then  : présence créée avec heures calculées — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/presences/",
            {
                "employe_id": str(employe_alpha.id),
                "date": "2024-06-11",
                "heure_arrivee": "08:00:00",
                "heure_depart": "17:00:00",
                "statut": "present",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["statut"] == "present"
        # 9 heures travaillées
        assert float(response.data["heures_travaillees"]) == 9.0

    def test_creer_presence_doublon(self, client_alpha, presence_alpha, employe_alpha):
        """
        Given : présence du 10/06/2024 existe déjà
        When  : on crée une autre pour le même jour
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/presences/",
            {
                "employe_id": str(employe_alpha.id),
                "date": "2024-06-10",
                "statut": "present",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_presence_heure_invalide(self, client_alpha, employe_alpha):
        """
        Given : heure_depart avant heure_arrivee
        When  : on crée la présence
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/presences/",
            {
                "employe_id": str(employe_alpha.id),
                "date": "2024-06-12",
                "heure_arrivee": "17:00:00",
                "heure_depart": "08:00:00",
                "statut": "present",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_presence_absent(self, client_alpha, employe_alpha):
        """
        Given : employé absent
        When  : on enregistre son absence
        Then  : présence créée avec statut absent — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/presences/",
            {
                "employe_id": str(employe_alpha.id),
                "date": "2024-06-12",
                "statut": "absent",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["statut"] == "absent"

    def test_creer_presence_sans_auth(self, client_anonyme, employe_alpha):
        """
        Given : aucun token JWT
        When  : on crée une présence
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/presences/",
            {
                "employe_id": str(employe_alpha.id),
                "date": "2024-06-13",
                "statut": "present",
            },
            format="json",
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestPresencesDetail:

    def test_detail_presence_succes(self, client_alpha, presence_alpha):
        """
        Given : présence existe dans le tenant
        When  : on demande son détail
        Then  : présence retournée — 200
        """
        response = client_alpha.get(f"/api/v1/hr/presences/{presence_alpha.id}/")
        assert response.status_code == 200
        assert response.data["statut"] == "present"

    def test_detail_presence_inexistante(self, client_alpha, db):
        """
        Given : aucune présence avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/presences/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : présence appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import Presence
        from tests.conftest import TENANT_BETA

        presence_beta = Presence.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            date="2024-06-10",
            statut="present",
        )
        response = client_alpha.get(f"/api/v1/hr/presences/{presence_beta.id}/")
        assert response.status_code == 404

    def test_modifier_presence_succes(self, client_alpha, presence_alpha):
        """
        Given : présence existe
        When  : on modifie le statut
        Then  : présence modifiée — 200
        """
        response = client_alpha.put(
            f"/api/v1/hr/presences/{presence_alpha.id}/",
            {
                "employe_id": str(presence_alpha.employee.id),
                "date": "2024-06-10",
                "statut": "retard",
            },
            format="json",
        )
        assert response.status_code == 200
        assert response.data["statut"] == "retard"

    def test_supprimer_presence_succes(self, client_alpha, presence_alpha):
        """
        Given : présence existe
        When  : on la supprime (soft delete)
        Then  : présence archivée — 204
                is_active = False
        """
        response = client_alpha.delete(f"/api/v1/hr/presences/{presence_alpha.id}/")
        assert response.status_code == 204

        # Soft delete → présence toujours en base
        # mais is_active = False
        presence_alpha.refresh_from_db()
        assert presence_alpha.is_active is False


@pytest.mark.django_db
class TestPresencesCalculHeures:

    def test_calcul_heures_travaillees(self, db, employe_alpha):
        """
        Given : présence 08:00 → 17:00
        When  : on sauvegarde
        Then  : heures_travaillees = 9.0
        """
        from rh.models import Presence
        from tests.conftest import TENANT_ALPHA

        presence = Presence.objects.create(
            tenant_id=TENANT_ALPHA,
            employee=employe_alpha,
            date=date(2024, 6, 15),
            heure_arrivee=time(8, 0),
            heure_depart=time(17, 0),
            statut="present",
        )
        assert float(presence.heures_travaillees) == 9.0

    def test_retard_detection(self, db, employe_alpha):
        """
        Given : présence avec arrivée à 09:00
        When  : on vérifie le retard
        Then  : est_en_retard = True
        """
        from rh.models import Presence
        from tests.conftest import TENANT_ALPHA

        presence = Presence.objects.create(
            tenant_id=TENANT_ALPHA,
            employee=employe_alpha,
            date=date(2024, 6, 16),
            heure_arrivee=time(9, 0),
            heure_depart=time(18, 0),
            statut="retard",
        )
        assert presence.est_en_retard is True

    def test_pas_de_retard(self, db, employe_alpha):
        """
        Given : présence avec arrivée à 07:30
        When  : on vérifie le retard
        Then  : est_en_retard = False
        """
        from rh.models import Presence
        from tests.conftest import TENANT_ALPHA

        presence = Presence.objects.create(
            tenant_id=TENANT_ALPHA,
            employee=employe_alpha,
            date=date(2024, 6, 17),
            heure_arrivee=time(7, 30),
            heure_depart=time(16, 30),
            statut="present",
        )
        assert presence.est_en_retard is False

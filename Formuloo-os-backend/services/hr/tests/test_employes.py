"""
Tests Employés — Formuloo OS
Teste tous les endpoints des employés.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, sous-ressources, filtres, isolation
"""

import uuid

import pytest


@pytest.mark.django_db
class TestEmployesListe:

    def test_lister_employes_succes(
        self, client_alpha, employe_alpha, employe_rh_alpha
    ):
        """
        Given : deux employés dans tenant Alpha
        When  : l'admin liste les employés
        Then  : 2 employés retournés — 200
        """
        response = client_alpha.get("/api/v1/hr/employes/")
        assert response.status_code == 200
        assert response.data["count"] == 2

    def test_lister_employes_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les employés
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/employes/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, employe_alpha, client_beta, employe_beta
    ):
        """
        Given : chaque tenant a ses employés
        When  : Alpha liste ses employés
        Then  : Alpha ne voit pas les employés de Beta
        """
        response_alpha = client_alpha.get("/api/v1/hr/employes/")
        response_beta = client_beta.get("/api/v1/hr/employes/")

        emails_alpha = [e["email"] for e in response_alpha.data["results"]]
        emails_beta = [e["email"] for e in response_beta.data["results"]]

        assert "jean.dupont@pme-alpha.com" in emails_alpha
        assert "paul.kamga@pme-beta.com" not in emails_alpha
        assert "paul.kamga@pme-beta.com" in emails_beta
        assert "jean.dupont@pme-alpha.com" not in emails_beta

    def test_filtre_par_departement(
        self, client_alpha, employe_alpha, employe_rh_alpha, departement_alpha
    ):
        """
        Given : employés dans différents départements
        When  : on filtre par département INFO
        Then  : seulement les employés INFO retournés
        """
        response = client_alpha.get(
            f"/api/v1/hr/employes/?departement_id={departement_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["results"][0]["email"] == "jean.dupont@pme-alpha.com"

    def test_filtre_par_statut(self, client_alpha, employe_alpha):
        """
        Given : employé actif
        When  : on filtre par statut active
        Then  : employé retourné
        """
        response = client_alpha.get("/api/v1/hr/employes/?statut=active")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_recherche_full_text(self, client_alpha, employe_alpha):
        """
        Given : employé Jean Dupont
        When  : on recherche 'Dupont'
        Then  : employé retourné
        """
        response = client_alpha.get("/api/v1/hr/employes/?search=Dupont")
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["results"][0]["last_name"] == "Dupont"

    def test_filtre_par_type_employe(self, client_alpha, employe_alpha):
        """
        Given : employé permanent
        When  : on filtre par type permanent
        Then  : employé retourné
        """
        response = client_alpha.get("/api/v1/hr/employes/?type_employe=permanent")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestEmployesCreate:

    def test_creer_employe_succes(
        self, client_alpha, departement_alpha, poste_dev_alpha
    ):
        """
        Given : admin connecté
        When  : il crée un employé
        Then  : employé créé — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/employes/",
            {
                "first_name": "Pierre",
                "last_name": "Martin",
                "gender": "M",
                "email": "pierre.martin@pme-alpha.com",
                "phone": "+237699000099",
                "department_id": str(departement_alpha.id),
                "position_id": str(poste_dev_alpha.id),
                "hire_date": "2024-03-01",
                "status": "active",
                "type_employe": "permanent",
                "salaire_base": 500000,
                "devise": "XAF",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["email"] == "pierre.martin@pme-alpha.com"
        assert "employee_number" in response.data
        assert response.data["employee_number"].startswith("EMP-")

    def test_creer_employe_email_existant(self, client_alpha, employe_alpha):
        """
        Given : employé avec cet email existe
        When  : on crée avec le même email
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/employes/",
            {
                "first_name": "Doublon",
                "last_name": "Test",
                "gender": "M",
                "email": "jean.dupont@pme-alpha.com",
                "phone": "+237699000099",
                "hire_date": "2024-03-01",
                "status": "active",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_employe_sans_champs_obligatoires(self, client_alpha, db):
        """
        Given : admin connecté
        When  : il crée sans email
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/employes/",
            {
                "first_name": "Test",
                "last_name": "User",
            },
            format="json",
        )
        assert response.status_code == 400

    def test_creer_employe_matricule_auto(self, client_alpha, departement_alpha):
        """
        Given : admin connecté
        When  : il crée un employé
        Then  : matricule auto-généré au format EMP-YYYY-NNN
        """
        response = client_alpha.post(
            "/api/v1/hr/employes/",
            {
                "first_name": "Auto",
                "last_name": "Matricule",
                "gender": "F",
                "email": "auto@pme-alpha.com",
                "phone": "+237699000088",
                "hire_date": "2024-06-01",
                "status": "active",
                "type_employe": "stagiaire",
                "salaire_base": 150000,
            },
            format="json",
        )
        assert response.status_code == 201
        import re

        assert re.match(r"EMP-\d{4}-\d{3}", response.data["employee_number"])

    def test_creer_employe_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on crée un employé
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/employes/", {"first_name": "Test"}, format="json"
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestEmployesDetail:

    def test_detail_employe_succes(self, client_alpha, employe_alpha):
        """
        Given : employé existe dans le tenant
        When  : on demande son détail
        Then  : employé retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{employe_alpha.id}/")
        assert response.status_code == 200
        assert response.data["email"] == "jean.dupont@pme-alpha.com"

    def test_detail_employe_inexistant(self, client_alpha, db):
        """
        Given : aucun employé avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : employé appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{employe_beta.id}/")
        assert response.status_code == 404

    def test_modifier_employe_succes(self, client_alpha, employe_alpha):
        """
        Given : employé existe
        When  : on modifie son prénom
        Then  : employé modifié — 200
        """
        response = client_alpha.patch(
            f"/api/v1/hr/employes/{employe_alpha.id}/",
            {"first_name": "Jean-Pierre"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["first_name"] == "Jean-Pierre"

    def test_archiver_employe_succes(self, client_alpha, employe_alpha):
        """
        Given : employé actif
        When  : on l'archive (soft delete)
        Then  : employé désactivé — 204
        """
        response = client_alpha.delete(f"/api/v1/hr/employes/{employe_alpha.id}/")
        assert response.status_code == 204
        employe_alpha.refresh_from_db()
        assert employe_alpha.status == "inactive"


@pytest.mark.django_db
class TestEmployesSousRessources:

    def test_contrats_employe_succes(self, client_alpha, employe_alpha, contrat_alpha):
        """
        Given : employé avec un contrat
        When  : on liste ses contrats
        Then  : contrat retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{employe_alpha.id}/contrats/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_conges_employe_succes(self, client_alpha, employe_alpha, conge_alpha):
        """
        Given : employé avec une demande de congé
        When  : on liste ses congés
        Then  : congé retourné — 200
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{employe_alpha.id}/conges/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_payslips_employe_succes(
        self, client_alpha, employe_alpha, fiche_paie_alpha
    ):
        """
        Given : employé avec une fiche de paie
        When  : on liste ses fiches
        Then  : fiche retournée — 200
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{employe_alpha.id}/payslips/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_sous_ressources_employe_inexistant(self, client_alpha, db):
        """
        Given : employé inexistant
        When  : on liste ses contrats
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/employes/{uuid.uuid4()}/contrats/")
        assert response.status_code == 404

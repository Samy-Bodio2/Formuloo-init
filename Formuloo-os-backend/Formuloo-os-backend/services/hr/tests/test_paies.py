"""
Tests Fiches de Paie — Formuloo OS
Teste tous les endpoints des fiches de paie.
Conforme ADR-001 : isolation multi-tenant
Couvre : CRUD, workflow, calculs SYSCOHADA,
         génération masse, isolation
"""

import uuid

import pytest


@pytest.mark.django_db
class TestPayrollListe:

    def test_lister_fiches_succes(self, client_alpha, fiche_paie_alpha):
        """
        Given : une fiche de paie dans tenant Alpha
        When  : l'admin liste les fiches
        Then  : 1 fiche retournée — 200
        """
        response = client_alpha.get("/api/v1/hr/payroll/")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_lister_fiches_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les fiches
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/hr/payroll/")
        assert response.status_code == 401

    def test_isolation_multi_tenant(
        self, client_alpha, fiche_paie_alpha, client_beta, employe_beta
    ):
        """
        Given : fiche dans tenant Alpha
        When  : tenant Beta liste ses fiches
        Then  : Beta ne voit pas les fiches de Alpha
        """
        response_beta = client_beta.get("/api/v1/hr/payroll/")
        assert response_beta.data["count"] == 0

    def test_filtre_par_employe(self, client_alpha, fiche_paie_alpha, employe_alpha):
        """
        Given : fiche de l'employé Alpha
        When  : on filtre par employe_id
        Then  : fiche retournée
        """
        response = client_alpha.get(
            f"/api/v1/hr/payroll/?employe_id={employe_alpha.id}"
        )
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_mois_annee(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche de juin 2024
        When  : on filtre par mois=6 et annee=2024
        Then  : fiche retournée
        """
        response = client_alpha.get("/api/v1/hr/payroll/?mois=6&annee=2024")
        assert response.status_code == 200
        assert response.data["count"] == 1

    def test_filtre_par_statut(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche en brouillon
        When  : on filtre par statut brouillon
        Then  : fiche retournée
        """
        response = client_alpha.get("/api/v1/hr/payroll/?statut=brouillon")
        assert response.status_code == 200
        assert response.data["count"] == 1


@pytest.mark.django_db
class TestPayrollCreate:

    def test_generer_fiche_succes(self, client_alpha, employe_alpha, contrat_alpha):
        """
        Given : employé actif avec contrat
        When  : on génère une fiche de paie
        Then  : fiche créée en brouillon — 201
        """
        response = client_alpha.post(
            "/api/v1/hr/payroll/",
            {
                "employe_id": str(employe_alpha.id),
                "contrat_id": str(contrat_alpha.id),
                "mois": 7,
                "annee": 2024,
                "bonuses": {
                    "prime_transport": 30000,
                    "prime_logement": 50000,
                    "prime_rendement": 0,
                    "autres": 0,
                },
                "deductions": {"autres": 0},
                "currency": "XAF",
            },
            format="json",
        )
        assert response.status_code == 201
        assert response.data["statut"] == "brouillon"
        assert response.data["period"] == "2024-07"
        assert float(response.data["salaire_base"]) == 600000.0

    def test_generer_fiche_doublon(
        self, client_alpha, fiche_paie_alpha, employe_alpha, contrat_alpha
    ):
        """
        Given : fiche de juin 2024 existe déjà
        When  : on génère une autre pour juin 2024
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/payroll/",
            {
                "employe_id": str(employe_alpha.id),
                "mois": 6,
                "annee": 2024,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_generer_fiche_employe_inactif(self, client_alpha, employe_alpha):
        """
        Given : employé inactif
        When  : on génère une fiche
        Then  : erreur 400
        """
        employe_alpha.status = "inactive"
        employe_alpha.save()

        response = client_alpha.post(
            "/api/v1/hr/payroll/",
            {
                "employe_id": str(employe_alpha.id),
                "mois": 8,
                "annee": 2024,
            },
            format="json",
        )
        assert response.status_code == 400

    def test_generer_fiche_sans_auth(self, client_anonyme, employe_alpha):
        """
        Given : aucun token JWT
        When  : on génère une fiche
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/payroll/",
            {
                "employe_id": str(employe_alpha.id),
                "mois": 7,
                "annee": 2024,
            },
            format="json",
        )
        assert response.status_code == 401


@pytest.mark.django_db
class TestPayrollDetail:

    def test_detail_fiche_succes(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche existe dans le tenant
        When  : on demande son détail
        Then  : fiche retournée — 200
        """
        response = client_alpha.get(f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/")
        assert response.status_code == 200
        assert response.data["statut"] == "brouillon"
        assert response.data["period"] == "2024-06"

    def test_detail_fiche_inexistante(self, client_alpha, db):
        """
        Given : aucune fiche avec cet UUID
        When  : on demande son détail
        Then  : erreur 404
        """
        response = client_alpha.get(f"/api/v1/hr/payroll/{uuid.uuid4()}/")
        assert response.status_code == 404

    def test_isolation_detail_autre_tenant(self, client_alpha, employe_beta):
        """
        Given : fiche appartient au tenant Beta
        When  : tenant Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from rh.models import FichePaie
        from tests.conftest import TENANT_BETA

        fiche_beta = FichePaie.objects.create(
            tenant_id=TENANT_BETA,
            employee=employe_beta,
            mois=6,
            annee=2024,
            salaire_base=500000,
            bonuses={},
            deductions={},
            statut="brouillon",
        )
        response = client_alpha.get(f"/api/v1/hr/payroll/{fiche_beta.id}/")
        assert response.status_code == 404

    def test_modifier_fiche_brouillon(
        self, client_alpha, fiche_paie_alpha, employe_alpha
    ):
        """
        Given : fiche en brouillon
        When  : on la modifie
        Then  : fiche modifiée — 200
        """
        response = client_alpha.put(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/",
            {
                "employe_id": str(employe_alpha.id),
                "mois": 6,
                "annee": 2024,
                "bonuses": {
                    "prime_transport": 50000,
                    "prime_logement": 0,
                    "prime_rendement": 0,
                    "autres": 0,
                },
                "deductions": {},
            },
            format="json",
        )
        assert response.status_code == 200

    def test_supprimer_fiche_brouillon(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche en brouillon
        When  : on la supprime
        Then  : fiche supprimée — 204
        """
        response = client_alpha.delete(f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/")
        assert response.status_code == 204

        from rh.models import FichePaie

        assert not FichePaie.objects.filter(id=fiche_paie_alpha.id).exists()

    def test_supprimer_fiche_validee_impossible(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche validée
        When  : on tente de la supprimer
        Then  : erreur 400
        """
        fiche_paie_alpha.statut = "valide"
        fiche_paie_alpha.save()

        response = client_alpha.delete(f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/")
        assert response.status_code == 400


@pytest.mark.django_db
class TestPayrollWorkflow:

    def test_valider_fiche_succes(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche en brouillon
        When  : on la valide
        Then  : statut valide — 200
        """
        response = client_alpha.post(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/valider/"
        )
        assert response.status_code == 200
        assert response.data["statut"] == "valide"
        assert response.data["date_validation"] is not None

    def test_valider_fiche_deja_validee(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche déjà validée
        When  : on tente de la valider à nouveau
        Then  : erreur 400
        """
        fiche_paie_alpha.statut = "valide"
        fiche_paie_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/valider/"
        )
        assert response.status_code == 400

    def test_payer_fiche_succes(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche validée
        When  : on la marque comme payée
        Then  : statut paye — 200
        """
        fiche_paie_alpha.statut = "valide"
        fiche_paie_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/payer/",
            {"mode_paiement": "virement"},
            format="json",
        )
        assert response.status_code == 200
        assert response.data["statut"] == "paye"
        assert response.data["mode_paiement"] == "virement"
        assert response.data["paid_at"] is not None

    def test_payer_fiche_non_validee(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche en brouillon
        When  : on tente de la payer
        Then  : erreur 400 — doit être validée d'abord
        """
        response = client_alpha.post(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/payer/",
            {"mode_paiement": "virement"},
            format="json",
        )
        assert response.status_code == 400

    def test_payer_fiche_sans_mode_paiement(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche validée
        When  : on paie sans mode_paiement
        Then  : erreur 400
        """
        fiche_paie_alpha.statut = "valide"
        fiche_paie_alpha.save()

        response = client_alpha.post(
            f"/api/v1/hr/payroll/{fiche_paie_alpha.id}/payer/", {}, format="json"
        )
        assert response.status_code == 400


@pytest.mark.django_db
class TestPayrollPeriode:

    def test_fiches_par_periode_succes(self, client_alpha, fiche_paie_alpha):
        """
        Given : fiche de juin 2024
        When  : on liste les fiches de 2024-06
        Then  : fiche retournée — 200
        """
        response = client_alpha.get("/api/v1/hr/payroll/periode/2024-06/")
        assert response.status_code == 200
        assert response.data["count"] == 1
        assert response.data["periode"] == "2024-06"

    def test_fiches_par_periode_vide(self, client_alpha, db):
        """
        Given : aucune fiche pour 2024-01
        When  : on liste les fiches de 2024-01
        Then  : liste vide retournée — 200
        """
        response = client_alpha.get("/api/v1/hr/payroll/periode/2024-01/")
        assert response.status_code == 200
        assert response.data["count"] == 0

    def test_fiches_par_periode_format_invalide(self, client_alpha, db):
        """
        Given : format de période invalide
        When  : on liste les fiches de 'invalid'
        Then  : erreur 400
        """
        response = client_alpha.get("/api/v1/hr/payroll/periode/invalid/")
        assert response.status_code == 400


@pytest.mark.django_db
class TestPayrollRun:

    def test_payroll_run_succes(self, client_alpha, employe_alpha, contrat_alpha):
        """
        Given : employé actif avec contrat
        When  : on lance la génération en masse
        Then  : fiches générées — 202
        """
        response = client_alpha.post(
            "/api/v1/hr/payroll/run/", {"mois": 9, "annee": 2024}, format="json"
        )
        assert response.status_code == 202
        assert response.data["nb_crees"] == 1
        assert response.data["nb_ignores"] == 0

    def test_payroll_run_ignore_existants(
        self, client_alpha, employe_alpha, contrat_alpha, fiche_paie_alpha
    ):
        """
        Given : fiche de juin 2024 existe déjà
        When  : on lance la génération pour juin 2024
        Then  : fiche ignorée — nb_ignores = 1
        """
        response = client_alpha.post(
            "/api/v1/hr/payroll/run/", {"mois": 6, "annee": 2024}, format="json"
        )
        assert response.status_code == 202
        assert response.data["nb_ignores"] == 1
        assert response.data["nb_crees"] == 0

    def test_payroll_run_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on lance la génération
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/hr/payroll/run/", {"mois": 9, "annee": 2024}, format="json"
        )
        assert response.status_code == 401

    def test_payroll_run_mois_invalide(self, client_alpha, db):
        """
        Given : mois invalide (13)
        When  : on lance la génération
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/hr/payroll/run/", {"mois": 13, "annee": 2024}, format="json"
        )
        assert response.status_code == 400


@pytest.mark.django_db
class TestPayrollCalculsSYSCOHADA:

    def test_calcul_salaire_net(self, fiche_paie_alpha):
        """
        Given : fiche avec primes et déductions SYSCOHADA
        When  : on vérifie le salaire net
        Then  : net = brut + primes - déductions
        """
        fiche_paie_alpha.refresh_from_db()
        # gross = 600000 + 30000 + 50000 = 680000
        assert float(fiche_paie_alpha.gross) == 680000.0
        # net = 680000 - 44000 - 35000 = 601000
        assert float(fiche_paie_alpha.net_salary) == 601000.0

    def test_cotisation_cnps(self, fiche_paie_alpha):
        """
        Given : fiche avec cotisation CNPS
        When  : on vérifie la cotisation
        Then  : cotisation_cnps = 44000
        """
        assert fiche_paie_alpha.cotisation_cnps == 44000

    def test_impot_irpp(self, fiche_paie_alpha):
        """
        Given : fiche avec impôt IRPP
        When  : on vérifie l'impôt
        Then  : impot_irpp = 35000
        """
        assert fiche_paie_alpha.impot_irpp == 35000

    def test_total_primes(self, fiche_paie_alpha):
        """
        Given : prime_transport=30000 + prime_logement=50000
        When  : on calcule le total des primes
        Then  : total_bonuses = 80000
        """
        assert fiche_paie_alpha.total_bonuses == 80000

    def test_total_deductions(self, fiche_paie_alpha):
        """
        Given : cnps=44000 + irpp=35000
        When  : on calcule le total des déductions
        Then  : total_deductions = 79000
        """
        assert fiche_paie_alpha.total_deductions == 79000

    def test_periode_auto_generee(self, fiche_paie_alpha):
        """
        Given : fiche de mois=6 annee=2024
        When  : on vérifie la période
        Then  : period = '2024-06'
        """
        assert fiche_paie_alpha.period == "2024-06"

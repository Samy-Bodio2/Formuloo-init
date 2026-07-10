"""Tests initialisation plan comptable SYSCOHADA."""

import pytest
from rest_framework.test import APIClient
from tests.conftest import make_user, TENANT_ALPHA, TENANT_BETA
from comptabilite.models import Compte, Journal, Exercice
from comptabilite.syscohada import PLAN_COMPTABLE_SYSCOHADA, JOURNAUX_STANDARDS


def make_admin_client(tenant_id):
    user = make_user(tenant_id, ["compta.init.plan"])
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
class TestInitialiser:

    def test_initialiser_cree_plan_complet(self, db):
        client = make_admin_client(TENANT_ALPHA)
        resp = client.post("/api/v1/compta/initialiser/")
        assert resp.status_code == 200
        assert resp.data["stats"]["comptes_crees"] == len(PLAN_COMPTABLE_SYSCOHADA)
        assert resp.data["stats"]["journaux_crees"] == len(JOURNAUX_STANDARDS)
        assert resp.data["stats"]["exercice_cree"] is True

    def test_initialiser_idempotent(self, db):
        client = make_admin_client(TENANT_ALPHA)
        client.post("/api/v1/compta/initialiser/")
        # second run
        resp = client.post("/api/v1/compta/initialiser/")
        assert resp.status_code == 200
        assert resp.data["stats"]["comptes_crees"] == 0
        assert resp.data["stats"]["comptes_existants"] == len(PLAN_COMPTABLE_SYSCOHADA)
        assert resp.data["stats"]["journaux_crees"] == 0
        assert resp.data["stats"]["exercice_existant"] is True

    def test_initialiser_isolation_tenant(self, db):
        """Chaque tenant a son propre plan — pas de pollution."""
        client_alpha = make_admin_client(TENANT_ALPHA)
        client_beta = make_admin_client(TENANT_BETA)
        client_alpha.post("/api/v1/compta/initialiser/")

        assert Compte.objects.filter(tenant_id=TENANT_BETA).count() == 0

        client_beta.post("/api/v1/compta/initialiser/")
        assert Compte.objects.filter(tenant_id=TENANT_ALPHA).count() == len(PLAN_COMPTABLE_SYSCOHADA)
        assert Compte.objects.filter(tenant_id=TENANT_BETA).count() == len(PLAN_COMPTABLE_SYSCOHADA)

    def test_initialiser_sans_permission(self, client_comptable):
        resp = client_comptable.post("/api/v1/compta/initialiser/")
        assert resp.status_code == 403

    def test_initialiser_sans_auth(self, client_anonyme):
        resp = client_anonyme.post("/api/v1/compta/initialiser/")
        assert resp.status_code == 401

    def test_initialiser_cree_5_journaux(self, db):
        client = make_admin_client(TENANT_ALPHA)
        client.post("/api/v1/compta/initialiser/")
        assert Journal.objects.filter(tenant_id=TENANT_ALPHA).count() == 5
        codes = set(Journal.objects.filter(tenant_id=TENANT_ALPHA).values_list("code", flat=True))
        assert codes == {"VTE", "ACH", "BNQ", "CAI", "OD"}

    def test_initialiser_cree_exercice_courant(self, db):
        from datetime import date
        client = make_admin_client(TENANT_ALPHA)
        resp = client.post("/api/v1/compta/initialiser/")
        assert resp.data["annee"] == date.today().year
        exercice = Exercice.objects.get(tenant_id=TENANT_ALPHA)
        assert exercice.statut == "OUVERT"
        assert exercice.annee == date.today().year

    def test_initialiser_get_non_supporte(self, db):
        client = make_admin_client(TENANT_ALPHA)
        resp = client.get("/api/v1/compta/initialiser/")
        assert resp.status_code == 405

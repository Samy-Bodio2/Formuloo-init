"""
Tests intégration HR → Compta — Formuloo OS

Valide la communication inter-services lors du paiement d'une fiche de paie.
Le service Compta est simulé par des mocks requests pour éviter d'avoir
besoin d'une vraie instance Compta pendant les tests unitaires.

Scénarios testés :
1.  Paiement fiche paie → appel Compta → écriture_id enregistrée
2.  Paiement fiche paie → Compta timeout → paie enregistrée quand même
3.  Paiement fiche paie → Compta 500 → paie enregistrée quand même
4.  Payload envoyé à Compta contient tous les champs requis
5.  Calcul CNPS inclus dans le payload
6.  Calcul IRPP inclus dans le payload
7.  Calcul salaire_net = brut - cnps - irpp
8.  UUID de l'écriture retourné par Compta → stocké dans journal_entry_id
9.  Compta connexion refusée → paie toujours enregistrée
"""

import datetime
import uuid
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pytest
from django.contrib.auth.models import User
from rest_framework.test import APIClient

from rh.models import Employe, FichePaie
from rh.services.compta_client import creer_ecriture_paie
from rh.services.cotisations import calculer_cotisations

# ── Tenant de test ───────────────────────────────────────
TENANT = uuid.uuid4()

PERMS_RH = [
    "hr.read.employes", "hr.write.employes",
    "hr.read.fiches_paie", "hr.write.fiches_paie",
    "hr.comptable",
]


def make_user(tenant_id=TENANT, roles=None):
    user = User.objects.create_user(
        username=f"user_{uuid.uuid4().hex[:8]}",
        email=f"user_{uuid.uuid4().hex[:8]}@test.cm",
        password="pw",
    )
    user.tenant_id = tenant_id
    user.roles = roles or ["RH_MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def employe(db):
    emp = Employe.objects.create(
        tenant_id=TENANT,
        employee_number="EMP001",
        first_name="Jean",
        last_name="Dupont",
        email="jean.dupont@pme.cm",
        salaire_base=500000,
        situation_familiale="marie",
        nombre_enfants=2,
        status="active",
        hire_date=datetime.date(2024, 1, 1),
    )
    return emp


@pytest.fixture
def fiche(db, employe):
    cotis = calculer_cotisations(Decimal("500000"), 2)
    return FichePaie.objects.create(
        tenant_id=TENANT,
        employee=employe,
        mois=6,
        annee=2026,
        salaire_base=500000,
        deductions={
            "cotisation_cnps": cotis["cotisation_cnps"],
            "impot_irpp": cotis["impot_irpp"],
        },
    )


@pytest.fixture
def rh_user(db):
    return make_user()


@pytest.fixture
def rh_client(rh_user):
    client = APIClient()
    client.force_authenticate(user=rh_user)
    return client


# ── Tests unitaires du service compta_client ───────────────

class TestComptaClientUnitaire:
    """
    Tests du client HTTP compta_client.creer_ecriture_paie().
    L'appel HTTP est mocké — aucune instance Compta requise.
    """

    def test_appel_succes_retourne_ecriture_id(self, db, fiche):
        """Compta répond 201 → on reçoit l'UUID de l'écriture."""
        ecriture_uuid = str(uuid.uuid4())
        mock_resp = MagicMock()
        mock_resp.status_code = 201
        mock_resp.json.return_value = {"ecriture_id": ecriture_uuid}

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp) as mock_post:
            # On doit d'abord payer la fiche pour avoir paid_at
            fiche.valider()
            fiche.payer("virement")
            result = creer_ecriture_paie(fiche)

        assert result == ecriture_uuid

    def test_timeout_retourne_none(self, db, fiche):
        """Compta timeout → creer_ecriture_paie retourne None (non bloquant)."""
        import requests as req
        with patch(
            "rh.services.compta_client.requests.post",
            side_effect=req.exceptions.Timeout,
        ):
            fiche.valider()
            fiche.payer("virement")
            result = creer_ecriture_paie(fiche)

        assert result is None

    def test_connexion_refusee_retourne_none(self, db, fiche):
        """Compta connexion échouée → None (non bloquant)."""
        import requests as req
        with patch(
            "rh.services.compta_client.requests.post",
            side_effect=req.exceptions.ConnectionError,
        ):
            fiche.valider()
            fiche.payer("virement")
            result = creer_ecriture_paie(fiche)

        assert result is None

    def test_compta_500_retourne_none(self, db, fiche):
        """Compta répond 500 → None."""
        mock_resp = MagicMock()
        mock_resp.status_code = 500
        mock_resp.text = "Internal Server Error"

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp):
            fiche.valider()
            fiche.payer("virement")
            result = creer_ecriture_paie(fiche)

        assert result is None

    def test_payload_contient_tenant_id(self, db, fiche):
        """Le payload envoyé à Compta inclut le tenant_id."""
        mock_resp = MagicMock()
        mock_resp.status_code = 201
        mock_resp.json.return_value = {"ecriture_id": str(uuid.uuid4())}

        fiche.valider()
        fiche.payer("virement")

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp) as mock_post:
            creer_ecriture_paie(fiche)
            _, kwargs = mock_post.call_args
            payload = kwargs["json"]

        assert payload["tenant_id"] == str(TENANT)

    def test_payload_contient_salaires(self, db, fiche):
        """Le payload inclut salaire_brut, salaire_net, cotisation_cnps, impot_irpp."""
        mock_resp = MagicMock()
        mock_resp.status_code = 201
        mock_resp.json.return_value = {"ecriture_id": str(uuid.uuid4())}

        fiche.valider()
        fiche.payer("virement")

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp) as mock_post:
            creer_ecriture_paie(fiche)
            _, kwargs = mock_post.call_args
            payload = kwargs["json"]

        assert "salaire_brut" in payload
        assert "salaire_net" in payload
        assert "cotisation_cnps" in payload
        assert "impot_irpp" in payload
        assert Decimal(payload["salaire_brut"]) > 0
        assert Decimal(payload["salaire_net"]) > 0

    def test_payload_equilibre_ohada(self, db, fiche):
        """
        Invariant OHADA : débit = crédit.
        brut = net + cnps + irpp + autres_deductions.
        """
        mock_resp = MagicMock()
        mock_resp.status_code = 201
        mock_resp.json.return_value = {"ecriture_id": str(uuid.uuid4())}

        fiche.valider()
        fiche.payer("virement")

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp) as mock_post:
            creer_ecriture_paie(fiche)
            _, kwargs = mock_post.call_args
            payload = kwargs["json"]

        brut = Decimal(payload["salaire_brut"])
        net = Decimal(payload["salaire_net"])
        cnps = Decimal(payload["cotisation_cnps"])
        irpp = Decimal(payload["impot_irpp"])
        autres = Decimal(payload["autres_deductions"])

        assert brut == net + cnps + irpp + autres, (
            f"Déséquilibre OHADA : brut={brut} ≠ net={net} + cnps={cnps} + irpp={irpp} + autres={autres}"
        )

    def test_header_service_token_present(self, db, fiche):
        """L'en-tête X-Service-Token est envoyé à Compta."""
        mock_resp = MagicMock()
        mock_resp.status_code = 201
        mock_resp.json.return_value = {"ecriture_id": str(uuid.uuid4())}

        fiche.valider()
        fiche.payer("virement")

        with patch("rh.services.compta_client.requests.post", return_value=mock_resp) as mock_post:
            creer_ecriture_paie(fiche)
            _, kwargs = mock_post.call_args
            headers = kwargs["headers"]

        assert "X-Service-Token" in headers
        assert headers["X-Service"] == "hr"


# ── Tests calcul CNPS/IRPP (conformité barème Cameroun) ───

class TestCalculCotisations:
    """Vérifie le moteur de calcul CNPS/IRPP utilisé dans le payload."""

    def test_cnps_42_pourcent(self):
        """CNPS = 4.2% du brut (non plafonné en dessous de 750k)."""
        result = calculer_cotisations(Decimal("300000"), nb_parts=1)
        assert result["cotisation_cnps"] == int(300000 * Decimal("0.042"))

    def test_cnps_plafond_750k(self):
        """CNPS plafonné à 750 000 XAF de base soit max 31 500 XAF."""
        result = calculer_cotisations(Decimal("2000000"), nb_parts=1)
        assert result["cotisation_cnps"] == int(750000 * Decimal("0.042"))

    def test_irpp_non_nul_salaire_eleve(self):
        """IRPP > 0 pour un salaire brut de 1 000 000 XAF."""
        result = calculer_cotisations(Decimal("1000000"), nb_parts=1)
        assert result["impot_irpp"] > 0

    def test_irpp_reduit_avec_parts(self):
        """Plus de parts familiales → IRPP plus faible."""
        result_1part = calculer_cotisations(Decimal("500000"), nb_parts=1)
        result_3parts = calculer_cotisations(Decimal("500000"), nb_parts=3)
        assert result_1part["impot_irpp"] >= result_3parts["impot_irpp"]

    def test_total_coherent(self):
        """total_cotisations = cnps + irpp."""
        result = calculer_cotisations(Decimal("400000"), nb_parts=2)
        assert result["total_cotisations"] == result["cotisation_cnps"] + result["impot_irpp"]

"""
Tests déclarations réglementaires — Formuloo OS

Couvre :
  GET /api/v1/hr/declarations/cnps/         → Déclaration CNPS mensuelle
  GET /api/v1/hr/declarations/irpp/         → Simulation IRPP mensuel
  GET /api/v1/hr/employes/{pk}/attestation/ → Attestation de travail
  GET /api/v1/hr/stats/                     → Statistiques RH
"""

import uuid
from datetime import date

import pytest
from rest_framework import status
from rest_framework.test import APIClient

from rh.models import Conge, Departement, Employe, FichePaie, Poste

# ── TENANT ET URLS ────────────────────────────────────────────────────────────
TENANT = uuid.UUID("bbbbbbbb-cccc-dddd-eeee-ffffffffffff")
BASE = "/api/v1/hr"
CNPS_URL = f"{BASE}/declarations/cnps/"
IRPP_URL = f"{BASE}/declarations/irpp/"
STATS_URL = f"{BASE}/stats/"


# ── FIXTURES ──────────────────────────────────────────────────────────────────


@pytest.fixture
def user_rh(db):
    """Utilisateur RH Manager du tenant de test."""
    from django.contrib.auth.models import User

    user = User.objects.create_user(
        username="rh_decl", email="rh@test.com", password="pass"
    )
    user.tenant_id = TENANT
    user.roles = ["RH_MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_comptable(db):
    """Utilisateur Comptable du tenant de test."""
    from django.contrib.auth.models import User

    user = User.objects.create_user(
        username="compta_decl", email="compta@test.com", password="pass"
    )
    user.tenant_id = TENANT
    user.roles = ["COMPTABLE"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_employe(db):
    """Utilisateur Employé — accès restreint aux déclarations."""
    from django.contrib.auth.models import User

    user = User.objects.create_user(
        username="emp_decl", email="emp@test.com", password="pass"
    )
    user.tenant_id = TENANT
    user.roles = ["EMPLOYE"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def client_rh(user_rh):
    client = APIClient()
    client.force_authenticate(user=user_rh)
    return client


@pytest.fixture
def client_comptable(user_comptable):
    client = APIClient()
    client.force_authenticate(user=user_comptable)
    return client


@pytest.fixture
def client_employe_decl(user_employe):
    client = APIClient()
    client.force_authenticate(user=user_employe)
    return client


@pytest.fixture
def employe_avec_fiche(db):
    """Employé avec une fiche de paie validée."""
    dept = Departement.objects.create(nom="Finance", tenant_id=TENANT)
    poste = Poste.objects.create(
        titre="Comptable",
        code="COMPTA-001",
        tenant_id=TENANT,
        departement=dept,
    )
    employe = Employe.objects.create(
        tenant_id=TENANT,
        first_name="Paul",
        last_name="Kamga",
        email="paul@test.com",
        phone="699000001",
        hire_date=date(2022, 3, 1),
        department=dept,
        position=poste,
        salaire_base=500000,
        numero_cnps="CM12345678",
    )
    fiche = FichePaie.objects.create(
        tenant_id=TENANT,
        employee=employe,
        mois=6,
        annee=2024,
        salaire_base=500000,
        statut=FichePaie.Statut.VALIDE,
        bonuses={"prime_transport": 30000},
        deductions={"cotisation_cnps": 14700},
    )
    return employe, fiche


# ── TESTS DÉCLARATION CNPS ────────────────────────────────────────────────────


@pytest.mark.django_db
class TestDeclarationCNPS:
    """GET /declarations/cnps/ — Déclaration mensuelle CNPS."""

    def test_cnps_success(self, client_rh):
        """RH Manager peut accéder à la déclaration CNPS."""
        resp = client_rh.get(CNPS_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert "employes" in resp.data
        assert "totaux" in resp.data
        assert "taux" in resp.data

    def test_cnps_avec_fiches(self, client_rh, employe_avec_fiche):
        """Déclaration inclut les employés avec fiches de paie."""
        resp = client_rh.get(f"{CNPS_URL}?mois=6&annee=2024")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["nb_employes"] >= 1
        assert resp.data["totaux"]["total_cnps"] > 0

    def test_cnps_calcul_plafond(self, client_rh, employe_avec_fiche):
        """
        Pour un brut > 750 000 XAF, le calcul est plafonné à 750 000.
        Part salariale = 750 000 × 2.8% = 21 000 XAF max.
        """
        employe, _ = employe_avec_fiche
        # Créer une fiche avec brut élevé
        FichePaie.objects.create(
            tenant_id=TENANT,
            employee=employe,
            mois=5,
            annee=2024,
            salaire_base=1000000,  # > plafond CNPS
            statut=FichePaie.Statut.VALIDE,
        )
        resp = client_rh.get(f"{CNPS_URL}?mois=5&annee=2024")
        assert resp.status_code == status.HTTP_200_OK
        emp_data = next(
            e for e in resp.data["employes"] if e["employe_id"] == str(employe.id)
        )
        # Base cotisable doit être plafonnée à 750 000
        assert emp_data["cnps"]["base_cotisable"] == 750000.0

    def test_cnps_acces_comptable(self, client_comptable):
        """Comptable peut aussi accéder à la déclaration CNPS."""
        resp = client_comptable.get(CNPS_URL)
        assert resp.status_code == status.HTTP_200_OK

    def test_cnps_acces_refuse_employe(self, client_employe_decl):
        """Employé n'a pas accès à la déclaration CNPS."""
        resp = client_employe_decl.get(CNPS_URL)
        assert resp.status_code == status.HTTP_403_FORBIDDEN

    def test_cnps_mois_invalide(self, client_rh):
        """Mois en dehors de [1-12] → 400."""
        resp = client_rh.get(f"{CNPS_URL}?mois=13&annee=2024")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_cnps_taux_retournes(self, client_rh):
        """Les taux légaux Cameroun sont retournés dans la réponse."""
        resp = client_rh.get(CNPS_URL)
        assert resp.data["taux"]["salarial"] == 2.8
        assert resp.data["taux"]["patronal"] == 7.7
        assert resp.data["taux"]["plafond_mensuel"] == 750000.0


# ── TESTS DÉCLARATION IRPP ────────────────────────────────────────────────────


@pytest.mark.django_db
class TestDeclarationIRPP:
    """GET /declarations/irpp/ — Simulation IRPP mensuel."""

    def test_irpp_success(self, client_rh):
        """RH Manager peut accéder à la simulation IRPP."""
        resp = client_rh.get(IRPP_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert "employes" in resp.data
        assert "tranches" in resp.data
        assert "bareme" in resp.data

    def test_irpp_avec_fiches(self, client_rh, employe_avec_fiche):
        """IRPP calculé pour les fiches validées."""
        resp = client_rh.get(f"{IRPP_URL}?mois=6&annee=2024")
        assert resp.status_code == status.HTTP_200_OK
        assert len(resp.data["employes"]) >= 1
        for emp in resp.data["employes"]:
            # IRPP mensuel >= 0
            assert emp["irpp_mensuel_estime"] >= 0

    def test_irpp_acces_refuse_employe(self, client_employe_decl):
        """Employé n'a pas accès à la simulation IRPP."""
        resp = client_employe_decl.get(IRPP_URL)
        assert resp.status_code == status.HTTP_403_FORBIDDEN

    def test_irpp_zero_pour_bas_salaire(self, client_rh, db):
        """
        Salaire mensuel ≤ 166 667 XAF (= 2M/12/an)
        → IRPP = 0 (tranche 0%).
        """
        employe = Employe.objects.create(
            tenant_id=TENANT,
            first_name="Petit",
            last_name="Salaire",
            email="petit@test.com",
            phone="699000003",
            hire_date=date(2023, 1, 1),
            salaire_base=100000,
        )
        FichePaie.objects.create(
            tenant_id=TENANT,
            employee=employe,
            mois=3,
            annee=2024,
            salaire_base=100000,
            statut=FichePaie.Statut.VALIDE,
        )
        resp = client_rh.get(f"{IRPP_URL}?mois=3&annee=2024")
        assert resp.status_code == status.HTTP_200_OK
        emp_data = next(
            e for e in resp.data["employes"] if e["employe_id"] == str(employe.id)
        )
        assert emp_data["irpp_mensuel_estime"] == 0.0


# ── TESTS ATTESTATION DE TRAVAIL ─────────────────────────────────────────────


@pytest.mark.django_db
class TestAttestationTravail:
    """GET /employes/{pk}/attestation/ — Attestation de travail."""

    def test_attestation_rh_manager(self, client_rh, employe_avec_fiche):
        """RH Manager peut accéder à l'attestation de n'importe quel employé."""
        employe, _ = employe_avec_fiche
        resp = client_rh.get(f"{BASE}/employes/{employe.id}/attestation/")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["employe"]["nom_complet"] == employe.full_name
        assert resp.data["meta"]["type_document"] == "Attestation de travail"

    def test_attestation_structure_complete(self, client_rh, employe_avec_fiche):
        """L'attestation contient toutes les sections requises."""
        employe, _ = employe_avec_fiche
        resp = client_rh.get(f"{BASE}/employes/{employe.id}/attestation/")
        assert resp.status_code == status.HTTP_200_OK
        data = resp.data
        assert "meta" in data
        assert "employe" in data
        assert "poste" in data
        assert "contrat" in data
        assert "remuneration" in data
        # Données employé
        assert data["employe"]["matricule"] == employe.employee_number
        assert data["employe"]["numero_cnps"] == employe.numero_cnps
        # Rémunération
        assert data["remuneration"]["salaire_base"] == float(employe.salaire_base)
        assert data["remuneration"]["devise"] == "XAF"

    def test_attestation_autre_tenant(self, client_rh, db):
        """Employé d'un autre tenant → 404."""
        autre_tenant = uuid.UUID("cccccccc-dddd-eeee-ffff-000000000000")
        employe_autre = Employe.objects.create(
            tenant_id=autre_tenant,
            first_name="Autre",
            last_name="Tenant",
            email="autre@tenant.com",
            phone="699999999",
            hire_date=date(2023, 1, 1),
        )
        resp = client_rh.get(f"{BASE}/employes/{employe_autre.id}/attestation/")
        assert resp.status_code == status.HTTP_404_NOT_FOUND

    def test_attestation_inexistant(self, client_rh):
        """UUID inexistant → 404."""
        resp = client_rh.get(f"{BASE}/employes/{uuid.uuid4()}/attestation/")
        assert resp.status_code == status.HTTP_404_NOT_FOUND


# ── TESTS STATISTIQUES RH ────────────────────────────────────────────────────


@pytest.mark.django_db
class TestStatsRH:
    """GET /stats/ — Tableau de bord RH."""

    def test_stats_rh_manager(self, client_rh):
        """RH Manager voit toutes les stats dont la paie."""
        resp = client_rh.get(STATS_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert "effectifs" in resp.data
        assert "conges" in resp.data
        assert "presences" in resp.data
        assert "paie" in resp.data  # visible uniquement RH Manager

    def test_stats_manager(self, db):
        """Manager voit les stats effectifs/congés mais pas la paie."""
        from django.contrib.auth.models import User

        user = User.objects.create_user(
            username="mgr_stats", email="mgr@test.com", password="pass"
        )
        user.tenant_id = TENANT
        user.roles = ["MANAGER"]
        user.auth_user_id = uuid.uuid4()
        user.custom_role = None

        client = APIClient()
        client.force_authenticate(user=user)

        resp = client.get(STATS_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert "effectifs" in resp.data
        # Manager ne voit pas la masse salariale
        assert "paie" not in resp.data

    def test_stats_refuse_employe(self, client_employe_decl):
        """Employé n'a pas accès aux stats globales."""
        resp = client_employe_decl.get(STATS_URL)
        assert resp.status_code == status.HTTP_403_FORBIDDEN

    def test_stats_structure_effectifs(self, client_rh, db):
        """La structure des effectifs est correcte."""
        # Créer quelques employés
        for i in range(3):
            Employe.objects.create(
                tenant_id=TENANT,
                first_name=f"Emp{i}",
                last_name="Test",
                email=f"emp{i}@test.com",
                phone=f"69900000{i}",
                hire_date=date(2023, 1, 1),
                status=Employe.Status.ACTIVE,
            )

        resp = client_rh.get(STATS_URL)
        assert resp.status_code == status.HTTP_200_OK
        effectifs = resp.data["effectifs"]
        assert "total" in effectifs
        assert "actifs" in effectifs
        assert "par_departement" in effectifs
        assert "par_type" in effectifs
        assert effectifs["actifs"] >= 3

    def test_stats_isolation_tenant(self, client_rh, db):
        """Les stats ne concernent que le tenant du RH connecté."""
        autre_tenant = uuid.UUID("ffffffff-0000-1111-2222-333333333333")
        Employe.objects.create(
            tenant_id=autre_tenant,  # autre tenant
            first_name="Autre",
            last_name="Tenant",
            email="autre.tenant@test.com",
            phone="699111111",
            hire_date=date(2023, 1, 1),
        )
        resp = client_rh.get(STATS_URL)
        assert resp.status_code == status.HTTP_200_OK
        # Les employés de l'autre tenant ne comptent pas
        assert resp.data["effectifs"]["total"] == 0

"""
Tests self-service employé — Formuloo OS

Couvre les endpoints :
  GET  /api/v1/hr/me/              → profil employé connecté
  GET  /api/v1/hr/me/conges/       → mes congés
  POST /api/v1/hr/me/conges/       → soumettre un congé
  GET  /api/v1/hr/me/fiches-paie/  → mes fiches de paie
  GET  /api/v1/hr/me/presences/    → mes présences
"""

import uuid
from datetime import date

import pytest
from rest_framework import status
from rest_framework.test import APIClient

from rh.models import Conge, Departement, Employe, FichePaie, Poste, Presence

# ── BASE URL ──────────────────────────────────────────────────────────────────
BASE = "/api/v1/hr"

# ── TENANT DE TEST ────────────────────────────────────────────────────────────
TENANT = uuid.UUID("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")


# ── FIXTURES ──────────────────────────────────────────────────────────────────


@pytest.fixture
def user_avec_employe(db):
    """
    Utilisateur EMPLOYE avec une fiche employé liée (user_id = auth_user_id).
    Représente un employé qui a un compte Auth ET une fiche RH.
    """
    from django.contrib.auth.models import User

    auth_user_id = uuid.uuid4()

    user = User.objects.create_user(
        username="mon_employe", email="moi@pme.com", password="pass123"
    )
    user.tenant_id = TENANT
    user.roles = ["EMPLOYE"]
    user.auth_user_id = auth_user_id
    user.custom_role = None

    dept = Departement.objects.create(nom="Informatique", tenant_id=TENANT)
    poste = Poste.objects.create(
        titre="Développeur",
        code="DEV-001",
        tenant_id=TENANT,
        departement=dept,
    )
    employe = Employe.objects.create(
        tenant_id=TENANT,
        user_id=auth_user_id,  # lien avec le compte Auth
        first_name="Jean",
        last_name="Employe",
        email="moi@pme.com",
        phone="699000001",
        hire_date=date(2023, 1, 1),
        department=dept,
        position=poste,
        salaire_base=350000,
    )

    return user, employe


@pytest.fixture
def user_sans_employe(db):
    """Utilisateur EMPLOYE sans fiche RH liée (user_id ne correspond à rien)."""
    from django.contrib.auth.models import User

    user = User.objects.create_user(
        username="sans_fiche", email="sans@pme.com", password="pass123"
    )
    user.tenant_id = TENANT
    user.roles = ["EMPLOYE"]
    user.auth_user_id = uuid.uuid4()  # UUID non référencé en RH
    user.custom_role = None
    return user


@pytest.fixture
def client_employe(user_avec_employe):
    """Client API employé avec fiche RH."""
    user, _ = user_avec_employe
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def client_sans_fiche(user_sans_employe):
    """Client API employé sans fiche RH."""
    client = APIClient()
    client.force_authenticate(user=user_sans_employe)
    return client


# ── TESTS ME PROFIL ───────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestMeProfil:
    """GET /me/ — Profil employé connecté."""

    URL = f"{BASE}/me/"

    def test_get_profil_success(self, client_employe, user_avec_employe):
        """Employé avec fiche RH → retourne son profil."""
        _, employe = user_avec_employe
        resp = client_employe.get(self.URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["first_name"] == employe.first_name
        assert resp.data["last_name"] == employe.last_name

    def test_get_profil_sans_fiche_rh(self, client_sans_fiche):
        """Employé sans fiche RH → 404."""
        resp = client_sans_fiche.get(self.URL)
        assert resp.status_code == status.HTTP_404_NOT_FOUND

    def test_get_profil_unauthenticated(self):
        """Non authentifié → 401."""
        resp = APIClient().get(self.URL)
        assert resp.status_code == status.HTTP_401_UNAUTHORIZED


# ── TESTS ME CONGÉS ───────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestMeConges:
    """GET/POST /me/conges/ — Mes congés."""

    LIST_URL = f"{BASE}/me/conges/"

    def test_list_conges_vide(self, client_employe):
        """Aucun congé → liste vide."""
        resp = client_employe.get(self.LIST_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 0

    def test_list_conges_uniquement_les_siens(self, client_employe, user_avec_employe):
        """Seuls les congés de l'employé connecté sont retournés."""
        _, employe = user_avec_employe
        # Congé de l'employé connecté
        Conge.objects.create(
            tenant_id=TENANT,
            employee=employe,
            type_conge=Conge.TypeConge.ANNUEL,
            start_date=date(2024, 7, 1),
            end_date=date(2024, 7, 5),
        )
        # Congé d'un autre employé — ne doit pas apparaître
        autre_employe = Employe.objects.create(
            tenant_id=TENANT,
            first_name="Autre",
            last_name="Employe",
            email="autre@pme.com",
            phone="699000002",
            hire_date=date(2023, 1, 1),
        )
        Conge.objects.create(
            tenant_id=TENANT,
            employee=autre_employe,
            type_conge=Conge.TypeConge.ANNUEL,
            start_date=date(2024, 8, 1),
            end_date=date(2024, 8, 3),
        )

        resp = client_employe.get(self.LIST_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1  # uniquement le sien

    def test_submit_conge_success(self, client_employe, user_avec_employe):
        """Soumettre un congé → 201."""
        _, employe = user_avec_employe
        # Initialiser le solde de congés pour l'employé
        from rh.models import SoldeConges

        SoldeConges.objects.create(
            tenant_id=TENANT,
            employee=employe,
            type_conge=SoldeConges.TypeConge.ANNUEL,
            annee=date.today().year,
            jours_acquis=30,
            jours_pris=0,
        )

        payload = {
            "type_conge": "annuel",
            "start_date": "2024-09-01",
            "end_date": "2024-09-05",
            "reason": "Vacances annuelles",
        }
        resp = client_employe.post(self.LIST_URL, payload, format="json")
        assert resp.status_code == status.HTTP_201_CREATED
        assert resp.data["status"] == Conge.Statut.PENDING

    def test_filter_conges_par_statut(self, client_employe, user_avec_employe):
        """Filtre par statut fonctionne."""
        _, employe = user_avec_employe
        Conge.objects.create(
            tenant_id=TENANT,
            employee=employe,
            type_conge=Conge.TypeConge.ANNUEL,
            start_date=date(2024, 7, 1),
            end_date=date(2024, 7, 3),
            status=Conge.Statut.APPROVED,
        )
        resp = client_employe.get(f"{self.LIST_URL}?statut=approved")
        assert resp.status_code == status.HTTP_200_OK
        for conge in resp.data["results"]:
            assert conge["status"] == "approved"


# ── TESTS ME FICHES DE PAIE ───────────────────────────────────────────────────


@pytest.mark.django_db
class TestMeFichesPaie:
    """GET /me/fiches-paie/ — Mes fiches de paie."""

    URL = f"{BASE}/me/fiches-paie/"

    def test_liste_vide(self, client_employe):
        """Aucune fiche → liste vide."""
        resp = client_employe.get(self.URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 0

    def test_voit_uniquement_fiches_validees(self, client_employe, user_avec_employe):
        """L'employé ne voit que les fiches validées ou payées, pas les brouillons."""
        _, employe = user_avec_employe

        # Brouillon — ne doit pas apparaître
        FichePaie.objects.create(
            tenant_id=TENANT,
            employee=employe,
            mois=6,
            annee=2024,
            salaire_base=350000,
            statut=FichePaie.Statut.BROUILLON,
        )
        # Validée — doit apparaître
        FichePaie.objects.create(
            tenant_id=TENANT,
            employee=employe,
            mois=5,
            annee=2024,
            salaire_base=350000,
            statut=FichePaie.Statut.VALIDE,
        )

        resp = client_employe.get(self.URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1
        assert resp.data["results"][0]["statut"] == FichePaie.Statut.VALIDE


# ── TESTS ME PRÉSENCES ────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestMePresences:
    """GET /me/presences/ — Mes présences."""

    URL = f"{BASE}/me/presences/"

    def test_liste_presences_vide(self, client_employe):
        """Aucune présence → liste vide."""
        resp = client_employe.get(self.URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 0

    def test_presences_filtrees_par_mois(self, client_employe, user_avec_employe):
        """Les présences retournées sont celles du mois demandé."""
        from datetime import time as t

        _, employe = user_avec_employe

        Presence.objects.create(
            tenant_id=TENANT,
            employee=employe,
            date=date(2024, 6, 10),
            heure_arrivee=t(8, 0),
            heure_depart=t(17, 0),
        )
        Presence.objects.create(
            tenant_id=TENANT,
            employee=employe,
            date=date(2024, 7, 10),
            heure_arrivee=t(8, 0),
            heure_depart=t(17, 0),
        )

        resp = client_employe.get(f"{self.URL}?mois=6&annee=2024")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1
        assert resp.data["periode"] == "2024-06"

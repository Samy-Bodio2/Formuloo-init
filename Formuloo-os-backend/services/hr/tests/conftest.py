"""
Configuration pytest — Service RH
Formuloo OS

Fixtures partagées entre tous les tests.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-002 : authentification JWT

Rôles testés :
    RH_MANAGER  → accès total
    MANAGER     → accès équipe
    EMPLOYE     → accès propres données
    COMPTABLE   → accès fiches de paie
"""

import uuid
from datetime import date, time

import pytest
from django.contrib.auth.models import User
from faker import Faker
from rest_framework.test import APIClient

from rh.models import (
    Conge,
    Contrat,
    Departement,
    Employe,
    FichePaie,
    Poste,
    Presence,
    SoldeConges,
)

fake = Faker("fr_FR")

# ── TENANT IDs ────────────────────────────────────────────
TENANT_ALPHA = uuid.uuid4()
TENANT_BETA = uuid.uuid4()


# ── FIXTURES UTILISATEURS ─────────────────────────────────


@pytest.fixture
def user_alpha(db):
    """
    Utilisateur RH Manager du tenant Alpha.
    Accès total au module RH.
    """
    user = User.objects.create_user(
        username="admin_alpha", email="admin@pme-alpha.com", password="password123"
    )
    user.tenant_id = TENANT_ALPHA
    user.roles = ["RH_MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_manager_alpha(db):
    """
    Utilisateur Manager du tenant Alpha.
    Accès à son équipe uniquement.
    """
    user = User.objects.create_user(
        username="manager_alpha", email="manager@pme-alpha.com", password="password123"
    )
    user.tenant_id = TENANT_ALPHA
    user.roles = ["MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_employe_alpha(db):
    """
    Utilisateur Employé du tenant Alpha.
    Accès à ses propres données uniquement.
    """
    user = User.objects.create_user(
        username="employe_alpha", email="employe@pme-alpha.com", password="password123"
    )
    user.tenant_id = TENANT_ALPHA
    user.roles = ["EMPLOYE"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_comptable_alpha(db):
    """
    Utilisateur Comptable du tenant Alpha.
    Accès aux fiches de paie uniquement.
    """
    user = User.objects.create_user(
        username="comptable_alpha",
        email="comptable@pme-alpha.com",
        password="password123",
    )
    user.tenant_id = TENANT_ALPHA
    user.roles = ["COMPTABLE"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def user_beta(db):
    """
    Utilisateur RH Manager du tenant Beta.
    Pour tester l'isolation multi-tenant.
    """
    user = User.objects.create_user(
        username="admin_beta", email="admin@pme-beta.com", password="password123"
    )
    user.tenant_id = TENANT_BETA
    user.roles = ["RH_MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


# ── FIXTURES CLIENTS API ──────────────────────────────────


@pytest.fixture
def client_alpha(user_alpha):
    """
    Client API RH Manager — tenant Alpha.
    Accès total au module RH.
    """
    user_alpha.tenant_id = TENANT_ALPHA
    client = APIClient()
    client.force_authenticate(user=user_alpha)
    return client


@pytest.fixture
def client_manager_alpha(user_manager_alpha):
    """
    Client API Manager — tenant Alpha.
    Accès à son équipe uniquement.
    """
    user_manager_alpha.tenant_id = TENANT_ALPHA
    client = APIClient()
    client.force_authenticate(user=user_manager_alpha)
    return client


@pytest.fixture
def client_employe_alpha(user_employe_alpha):
    """
    Client API Employé — tenant Alpha.
    Accès à ses propres données uniquement.
    """
    user_employe_alpha.tenant_id = TENANT_ALPHA
    client = APIClient()
    client.force_authenticate(user=user_employe_alpha)
    return client


@pytest.fixture
def client_comptable_alpha(user_comptable_alpha):
    """
    Client API Comptable — tenant Alpha.
    Accès aux fiches de paie uniquement.
    """
    user_comptable_alpha.tenant_id = TENANT_ALPHA
    client = APIClient()
    client.force_authenticate(user=user_comptable_alpha)
    return client


@pytest.fixture
def client_beta(user_beta):
    """
    Client API RH Manager — tenant Beta.
    Pour tester l'isolation multi-tenant.
    """
    user_beta.tenant_id = TENANT_BETA
    client = APIClient()
    client.force_authenticate(user=user_beta)
    return client


@pytest.fixture
def client_anonyme():
    """Client API non authentifié."""
    return APIClient()


# ── FIXTURES DÉPARTEMENTS ─────────────────────────────────


@pytest.fixture
def departement_alpha(db):
    """Département Informatique du tenant Alpha."""
    return Departement.objects.create(
        tenant_id=TENANT_ALPHA,
        nom="Informatique",
        code="INFO",
        description="Département informatique",
        budget=5000000,
        devise="XAF",
        is_active=True,
    )


@pytest.fixture
def departement_rh_alpha(db):
    """Département RH du tenant Alpha."""
    return Departement.objects.create(
        tenant_id=TENANT_ALPHA,
        nom="Ressources Humaines",
        code="RH",
        description="Département RH",
        is_active=True,
    )


@pytest.fixture
def departement_beta(db):
    """Département du tenant Beta."""
    return Departement.objects.create(
        tenant_id=TENANT_BETA, nom="Finance", code="FIN", is_active=True
    )


# ── FIXTURES POSTES ───────────────────────────────────────


@pytest.fixture
def poste_dev_alpha(db, departement_alpha):
    """Poste Développeur du tenant Alpha."""
    return Poste.objects.create(
        tenant_id=TENANT_ALPHA,
        departement=departement_alpha,
        titre="Développeur Backend",
        code="DEV-BACK",
        niveau="senior",
        salaire_min=400000,
        salaire_max=800000,
        devise="XAF",
        is_active=True,
    )


@pytest.fixture
def poste_rh_alpha(db, departement_rh_alpha):
    """Poste RH Manager du tenant Alpha."""
    return Poste.objects.create(
        tenant_id=TENANT_ALPHA,
        departement=departement_rh_alpha,
        titre="RH Manager",
        code="RH-MGR",
        niveau="manager",
        salaire_min=500000,
        salaire_max=900000,
        devise="XAF",
        is_active=True,
    )


# ── FIXTURES EMPLOYÉS ─────────────────────────────────────


@pytest.fixture
def employe_alpha(db, departement_alpha, poste_dev_alpha):
    """Employé actif du tenant Alpha."""
    return Employe.objects.create(
        tenant_id=TENANT_ALPHA,
        first_name="Jean",
        last_name="Dupont",
        gender="M",
        email="jean.dupont@pme-alpha.com",
        phone="+237699000001",
        department=departement_alpha,
        position=poste_dev_alpha,
        hire_date=date(2024, 1, 15),
        status="active",
        type_employe="permanent",
        salaire_base=600000,
        devise="XAF",
    )


@pytest.fixture
def employe_rh_alpha(db, departement_rh_alpha, poste_rh_alpha):
    """Employé RH Manager du tenant Alpha."""
    return Employe.objects.create(
        tenant_id=TENANT_ALPHA,
        first_name="Marie",
        last_name="Ngo",
        gender="F",
        email="marie.ngo@pme-alpha.com",
        phone="+237699000002",
        department=departement_rh_alpha,
        position=poste_rh_alpha,
        hire_date=date(2023, 6, 1),
        status="active",
        type_employe="permanent",
        salaire_base=750000,
        devise="XAF",
    )


@pytest.fixture
def employe_beta(db, departement_beta):
    """Employé du tenant Beta."""
    return Employe.objects.create(
        tenant_id=TENANT_BETA,
        first_name="Paul",
        last_name="Kamga",
        gender="M",
        email="paul.kamga@pme-beta.com",
        phone="+237699000003",
        department=departement_beta,
        hire_date=date(2024, 3, 1),
        status="active",
        type_employe="permanent",
        salaire_base=500000,
        devise="XAF",
    )


# ── FIXTURES CONTRATS ─────────────────────────────────────


@pytest.fixture
def contrat_alpha(db, employe_alpha):
    """Contrat CDI de l'employé Alpha."""
    return Contrat.objects.create(
        tenant_id=TENANT_ALPHA,
        employee=employe_alpha,
        type="CDI",
        start_date=date(2024, 1, 15),
        gross_salary=600000,
        currency="XAF",
        work_hours_per_week=40,
        jours_conge_annuel=30,
        statut="actif",
        is_active=True,
    )


# ── FIXTURES CONGÉS ───────────────────────────────────────


@pytest.fixture
def conge_alpha(db, employe_alpha):
    """Demande de congé en attente de l'employé Alpha."""
    return Conge.objects.create(
        tenant_id=TENANT_ALPHA,
        employee=employe_alpha,
        type_conge="annuel",
        start_date=date(2024, 7, 1),
        end_date=date(2024, 7, 15),
        reason="Congé annuel estival",
        status="pending",
    )


# ── FIXTURES SOLDES CONGÉS ────────────────────────────────


@pytest.fixture
def solde_conges_alpha(db, employe_alpha):
    """Solde de congés annuel de l'employé Alpha."""
    return SoldeConges.objects.create(
        tenant_id=TENANT_ALPHA,
        employee=employe_alpha,
        type_conge="annuel",
        annee=2024,
        jours_acquis=30,
        jours_pris=0,
    )


# ── FIXTURES PRÉSENCES ────────────────────────────────────


@pytest.fixture
def presence_alpha(db, employe_alpha):
    """Présence de l'employé Alpha."""
    return Presence.objects.create(
        tenant_id=TENANT_ALPHA,
        employee=employe_alpha,
        date=date(2024, 6, 10),
        heure_arrivee=time(8, 0),
        heure_depart=time(17, 0),
        statut="present",
    )


# ── FIXTURES FICHES DE PAIE ───────────────────────────────


@pytest.fixture
def fiche_paie_alpha(db, employe_alpha, contrat_alpha):
    """Fiche de paie brouillon de l'employé Alpha."""
    return FichePaie.objects.create(
        tenant_id=TENANT_ALPHA,
        employee=employe_alpha,
        contrat=contrat_alpha,
        mois=6,
        annee=2024,
        salaire_base=600000,
        bonuses={
            "prime_transport": 30000,
            "prime_logement": 50000,
            "prime_rendement": 0,
            "autres": 0,
        },
        deductions={
            "cotisation_cnps": 44000,
            "impot_irpp": 35000,
            "credit_logement": 0,
            "autres": 0,
        },
        currency="XAF",
        statut="brouillon",
    )

"""
Configuration pytest — Formuloo OS
Fixtures partagées entre tous les tests.
Conforme ADR-002 : multi-tenant + RBAC
"""

import pytest
from faker import Faker
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from authentification.models import (
    Organisation,
    User,
    Role,
    Permission,
)

fake = Faker("fr_FR")


# ── FIXTURES ORGANISATIONS ────────────────────────


@pytest.fixture
def org_alpha(db):
    """Organisation PME Alpha — tenant de test principal."""
    return Organisation.objects.create(
        slug="pme-alpha", name="PME Alpha Cameroun", currency="XAF", locale="fr"
    )


@pytest.fixture
def org_beta(db):
    """Organisation PME Beta — pour tester l'isolation multi-tenant."""
    return Organisation.objects.create(
        slug="pme-beta", name="PME Beta Cameroun", currency="XAF", locale="fr"
    )


# ── FIXTURES UTILISATEURS ─────────────────────────


@pytest.fixture
def admin_alpha(db, org_alpha):
    """Utilisateur ADMIN_PME de PME Alpha."""
    user = User.objects.create_user(
        email="admin@pme-alpha.com",
        tenant=org_alpha,
        password="password123",
        first_name="Jean",
        last_name="Dupont",
        is_active=True,
        is_verified=True,
    )
    return user


@pytest.fixture
def employe_alpha(db, org_alpha):
    """Utilisateur EMPLOYE de PME Alpha."""
    user = User.objects.create_user(
        email="employe@pme-alpha.com",
        tenant=org_alpha,
        password="password123",
        first_name="Marie",
        last_name="Ngo",
        is_active=True,
        is_verified=True,
    )
    return user


@pytest.fixture
def admin_beta(db, org_beta):
    """Utilisateur ADMIN_PME de PME Beta — pour tester l'isolation."""
    user = User.objects.create_user(
        email="admin@pme-beta.com",
        tenant=org_beta,
        password="password123",
        first_name="Paul",
        last_name="Kamga",
        is_active=True,
        is_verified=True,
    )
    return user


@pytest.fixture
def super_admin(db):
    """Super administrateur de la plateforme."""
    user = User.objects.create_superuser(
        email="superadmin@formuloo.com",
        password="superpassword123",
    )
    return user


# ── FIXTURES TOKENS JWT ───────────────────────────


@pytest.fixture
def token_admin_alpha(admin_alpha):
    """Token JWT pour l'admin de PME Alpha."""
    refresh = RefreshToken.for_user(admin_alpha)
    refresh["tenant_id"] = str(admin_alpha.tenant.id)
    refresh["roles"] = []
    return {
        "access": str(refresh.access_token),
        "refresh": str(refresh),
    }


@pytest.fixture
def token_employe_alpha(employe_alpha):
    """Token JWT pour l'employé de PME Alpha."""
    refresh = RefreshToken.for_user(employe_alpha)
    refresh["tenant_id"] = str(employe_alpha.tenant.id)
    refresh["roles"] = []
    return {
        "access": str(refresh.access_token),
        "refresh": str(refresh),
    }


@pytest.fixture
def token_admin_beta(admin_beta):
    """Token JWT pour l'admin de PME Beta."""
    refresh = RefreshToken.for_user(admin_beta)
    refresh["tenant_id"] = str(admin_beta.tenant.id)
    refresh["roles"] = []
    return {
        "access": str(refresh.access_token),
        "refresh": str(refresh),
    }


@pytest.fixture
def token_super_admin(super_admin):
    """Token JWT pour le super admin."""
    refresh = RefreshToken.for_user(super_admin)
    refresh["roles"] = []
    return {
        "access": str(refresh.access_token),
        "refresh": str(refresh),
    }


# ── FIXTURES CLIENTS API ──────────────────────────


@pytest.fixture
def client_alpha(token_admin_alpha):
    """Client API authentifié en tant qu'admin de PME Alpha."""
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {token_admin_alpha['access']}")
    return client


@pytest.fixture
def client_beta(token_admin_beta):
    """Client API authentifié en tant qu'admin de PME Beta."""
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {token_admin_beta['access']}")
    return client


@pytest.fixture
def client_super_admin(token_super_admin):
    """Client API authentifié en tant que super admin."""
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {token_super_admin['access']}")
    return client


@pytest.fixture
def client_anonyme():
    """Client API non authentifié."""
    return APIClient()


# ── FIXTURES ROLES ────────────────────────────────


@pytest.fixture
def role_admin_alpha(db, org_alpha):
    """Rôle ADMIN_PME pour PME Alpha."""
    return Role.objects.create(
        tenant=org_alpha, name="Administrateur PME", code="ADMIN_PME", is_system=True
    )


@pytest.fixture
def role_rh_alpha(db, org_alpha):
    """Rôle RH_MANAGER pour PME Alpha."""
    return Role.objects.create(
        tenant=org_alpha, name="Responsable RH", code="RH_MANAGER", is_system=False
    )


# ── FIXTURES PERMISSIONS ──────────────────────────


@pytest.fixture
def permissions_hr(db):
    """Permissions du module HR."""
    permissions = []
    for action in ["read", "write", "delete"]:
        perm = Permission.objects.create(
            module="hr",
            action=action,
            resource="employes",
        )
        permissions.append(perm)
    return permissions

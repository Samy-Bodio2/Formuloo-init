"""
Tests — Inscription, invitation et gestion de compte — Formuloo OS

Couvre :
  - POST /register/              → créer une PME + admin
  - POST /verify-email/          → activer le compte via token
  - POST /resend-verification/   → renvoyer le lien de vérification
  - POST /invite/                → inviter un employé (RH Manager)
  - POST /invite/accept/         → activer le compte via lien d'invitation
  - POST /forgot-password/       → demander un reset
  - POST /reset-password/        → réinitialiser le mot de passe
"""

import pytest
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from authentification.models import OneTimeToken, Organisation, Role, User

# ── BASE URL ──────────────────────────────────────────────────────────────────
BASE = "/api/v1/auth"


# ── FIXTURES ──────────────────────────────────────────────────────────────────


@pytest.fixture
def org(db):
    """Organisation PME de test."""
    return Organisation.objects.create(
        slug="pme-registration-test",
        name="PME Registration Test",
        currency="XAF",
    )


@pytest.fixture
def role_rh(db):
    """Rôle système RH_MANAGER — requis pour le register et l'invite."""
    return Role.objects.create(
        name="Responsable RH",
        code="RH_MANAGER",
        is_system=True,
    )


@pytest.fixture
def role_employe(db):
    """Rôle système EMPLOYE."""
    return Role.objects.create(
        name="Employé",
        code="EMPLOYE",
        is_system=True,
    )


@pytest.fixture
def rh_manager(db, org, role_rh):
    """Utilisateur RH Manager actif et vérifié."""
    user = User.objects.create_user(
        email="rh@pme-test.com",
        tenant=org,
        password="RhManager2024!",
        first_name="Alice",
        last_name="Martin",
        is_active=True,
        is_verified=True,
    )
    user.roles.add(role_rh)
    return user


@pytest.fixture
def client_rh(rh_manager):
    """Client API authentifié en tant que RH Manager."""
    refresh = RefreshToken.for_user(rh_manager)
    refresh["tenant_id"] = str(rh_manager.tenant.id)
    refresh["roles"] = ["RH_MANAGER"]
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {str(refresh.access_token)}")
    return client


@pytest.fixture
def client_anon():
    """Client API non authentifié."""
    return APIClient()


# ── TESTS REGISTER ────────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestRegister:
    """POST /register/ — inscription publique."""

    URL = f"{BASE}/register/"

    def test_register_success(self, client_anon, role_rh):
        """Créer une PME + admin — doit retourner 201."""
        payload = {
            "company_name": "Ma Nouvelle PME",
            "company_slug": "ma-nouvelle-pme",
            "first_name": "Bob",
            "last_name": "Dupont",
            "email": "bob@nouvelle-pme.com",
            "password": "SecurePass2024!",
            "confirm_password": "SecurePass2024!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_201_CREATED
        assert "email de vérification" in resp.data["message"]
        assert resp.data["user"]["email"] == "bob@nouvelle-pme.com"
        assert resp.data["user"]["is_verified"] is False

    def test_register_slug_already_taken(self, client_anon, org, role_rh):
        """Slug déjà utilisé → 400."""
        payload = {
            "company_name": "Autre PME",
            "company_slug": org.slug,  # slug existant
            "first_name": "Jean",
            "last_name": "Ngo",
            "email": "jean@autre-pme.com",
            "password": "SecurePass2024!",
            "confirm_password": "SecurePass2024!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_register_email_already_taken(self, client_anon, rh_manager, role_rh):
        """Email déjà enregistré → 400."""
        payload = {
            "company_name": "Autre PME 2",
            "company_slug": "autre-pme-2",
            "first_name": "Alice",
            "last_name": "Ngo",
            "email": rh_manager.email,  # email existant
            "password": "SecurePass2024!",
            "confirm_password": "SecurePass2024!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_register_passwords_mismatch(self, client_anon, role_rh):
        """Mots de passe différents → 400."""
        payload = {
            "company_name": "PME Test",
            "company_slug": "pme-test-mismatch",
            "first_name": "X",
            "last_name": "Y",
            "email": "x@pme.com",
            "password": "SecurePass2024!",
            "confirm_password": "DifferentPass!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_register_creates_organisation_and_user(self, client_anon, role_rh):
        """Le register crée bien l'Organisation et le User en base."""
        payload = {
            "company_name": "PME Vérif DB",
            "company_slug": "pme-verif-db",
            "first_name": "Test",
            "last_name": "User",
            "email": "testdb@pme.com",
            "password": "SecurePass2024!",
            "confirm_password": "SecurePass2024!",
        }
        client_anon.post(self.URL, payload, format="json")
        assert Organisation.objects.filter(slug="pme-verif-db").exists()
        assert User.objects.filter(email="testdb@pme.com").exists()

    def test_register_creates_verify_email_token(self, client_anon, role_rh):
        """Un token VERIFY_EMAIL est créé après inscription."""
        payload = {
            "company_name": "PME Token Test",
            "company_slug": "pme-token-test",
            "first_name": "Token",
            "last_name": "Test",
            "email": "tokentest@pme.com",
            "password": "SecurePass2024!",
            "confirm_password": "SecurePass2024!",
        }
        client_anon.post(self.URL, payload, format="json")
        user = User.objects.get(email="tokentest@pme.com")
        assert OneTimeToken.objects.filter(
            user=user, token_type=OneTimeToken.Type.VERIFY_EMAIL
        ).exists()


# ── TESTS VERIFY EMAIL ────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestVerifyEmail:
    """POST /verify-email/ — vérification de l'adresse email."""

    URL = f"{BASE}/verify-email/"

    def test_verify_email_success(self, client_anon, rh_manager):
        """Token valide → is_verified passe à True."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.VERIFY_EMAIL)
        rh_manager.is_verified = False
        rh_manager.save()

        resp = client_anon.post(self.URL, {"token": str(ott.token)}, format="json")
        assert resp.status_code == status.HTTP_200_OK
        rh_manager.refresh_from_db()
        assert rh_manager.is_verified is True

    def test_verify_email_token_consumed(self, client_anon, rh_manager):
        """Token consommé une fois → ne peut pas être réutilisé."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.VERIFY_EMAIL)
        client_anon.post(self.URL, {"token": str(ott.token)}, format="json")
        # Deuxième utilisation → 400
        resp = client_anon.post(self.URL, {"token": str(ott.token)}, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_verify_email_invalid_token(self, client_anon):
        """Token UUID valide mais inexistant → 400."""
        import uuid

        resp = client_anon.post(self.URL, {"token": str(uuid.uuid4())}, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_verify_email_expired_token(self, client_anon, rh_manager):
        """Token expiré → 400."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.VERIFY_EMAIL)
        # Simuler l'expiration
        ott.expires_at = timezone.now() - timezone.timedelta(hours=1)
        ott.save()

        resp = client_anon.post(self.URL, {"token": str(ott.token)}, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST


# ── TESTS RESEND VERIFICATION ─────────────────────────────────────────────────


@pytest.mark.django_db
class TestResendVerification:
    """POST /resend-verification/ — renvoi du lien de vérification."""

    URL = f"{BASE}/resend-verification/"

    def test_resend_returns_200_always(self, client_anon):
        """Email inconnu → toujours 200 (anti-énumération)."""
        resp = client_anon.post(
            self.URL, {"email": "inexistant@test.com"}, format="json"
        )
        assert resp.status_code == status.HTTP_200_OK

    def test_resend_for_unverified_user(self, client_anon, rh_manager):
        """Email non vérifié → 200 + nouveau token créé."""
        rh_manager.is_verified = False
        rh_manager.save()
        resp = client_anon.post(self.URL, {"email": rh_manager.email}, format="json")
        assert resp.status_code == status.HTTP_200_OK
        assert OneTimeToken.objects.filter(
            user=rh_manager,
            token_type=OneTimeToken.Type.VERIFY_EMAIL,
            used_at__isnull=True,
        ).exists()


# ── TESTS INVITE ──────────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestInvite:
    """POST /invite/ — invitation d'un employé."""

    URL = f"{BASE}/invite/"

    def test_invite_success(self, client_rh, org, role_employe):
        """RH Manager invite un employé → 201."""
        payload = {
            "first_name": "Nouveau",
            "last_name": "Employe",
            "email": "employe.invite@pme.com",
            "roles": ["EMPLOYE"],
        }
        resp = client_rh.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_201_CREATED
        assert resp.data["user"]["email"] == "employe.invite@pme.com"
        assert resp.data["user"]["is_active"] is False

    def test_invite_creates_invitation_token(self, client_rh, org, role_employe):
        """Un token INVITATION doit être créé."""
        payload = {
            "first_name": "Token",
            "last_name": "Invite",
            "email": "tokeninvite@pme.com",
            "roles": ["EMPLOYE"],
        }
        client_rh.post(self.URL, payload, format="json")
        user = User.objects.get(email="tokeninvite@pme.com")
        assert OneTimeToken.objects.filter(
            user=user, token_type=OneTimeToken.Type.INVITATION
        ).exists()

    def test_invite_invalid_role(self, client_rh):
        """Rôle inexistant → 400."""
        payload = {
            "first_name": "Test",
            "last_name": "User",
            "email": "test@pme.com",
            "roles": ["ROLE_INEXISTANT"],
        }
        resp = client_rh.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_invite_requires_rh_manager(self, client_anon, role_employe, org):
        """Client non authentifié → 401."""
        payload = {
            "first_name": "Jean",
            "last_name": "Test",
            "email": "jean@pme.com",
            "roles": ["EMPLOYE"],
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_401_UNAUTHORIZED

    def test_invite_non_rh_manager(self, rh_manager, org, role_employe):
        """Utilisateur sans rôle RH_MANAGER → 403."""
        # Créer un user sans rôle RH_MANAGER
        user = User.objects.create_user(
            email="employe-norh@pme.com",
            tenant=org,
            password="Pass2024!",
            first_name="No",
            last_name="RH",
        )
        refresh = RefreshToken.for_user(user)
        refresh["roles"] = ["EMPLOYE"]
        client = APIClient()
        client.credentials(HTTP_AUTHORIZATION=f"Bearer {str(refresh.access_token)}")

        payload = {
            "first_name": "Jean",
            "last_name": "Test",
            "email": "jean2@pme.com",
            "roles": ["EMPLOYE"],
        }
        resp = client.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_403_FORBIDDEN


# ── TESTS ACCEPT INVITATION ───────────────────────────────────────────────────


@pytest.mark.django_db
class TestAcceptInvitation:
    """POST /invite/accept/ — activation du compte via lien d'invitation."""

    URL = f"{BASE}/invite/accept/"

    @pytest.fixture
    def invited_user(self, org, role_employe):
        """Utilisateur invité sans mot de passe."""
        user = User.objects.create_user(
            email="invite@pme.com",
            tenant=org,
            password=None,
            first_name="Invite",
            last_name="User",
            is_active=False,
            is_verified=False,
        )
        user.roles.add(role_employe)
        return user

    def test_accept_success(self, client_anon, invited_user):
        """Token valide + mot de passe → compte activé."""
        ott = OneTimeToken.create_for(invited_user, OneTimeToken.Type.INVITATION)
        payload = {
            "token": str(ott.token),
            "password": "NouveauPass2024!",
            "confirm_password": "NouveauPass2024!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_200_OK
        invited_user.refresh_from_db()
        assert invited_user.is_active is True
        assert invited_user.is_verified is True

    def test_accept_enables_login(self, client_anon, invited_user):
        """Après activation, le login doit fonctionner."""
        ott = OneTimeToken.create_for(invited_user, OneTimeToken.Type.INVITATION)
        client_anon.post(
            self.URL,
            {
                "token": str(ott.token),
                "password": "LoginPass2024!",
                "confirm_password": "LoginPass2024!",
            },
            format="json",
        )
        # Tenter le login
        resp = client_anon.post(
            f"{BASE}/login/",
            {"email": invited_user.email, "password": "LoginPass2024!"},
            format="json",
        )
        assert resp.status_code == status.HTTP_200_OK
        assert "access" in resp.data

    def test_accept_invalid_token(self, client_anon):
        """Token inexistant → 400."""
        import uuid

        resp = client_anon.post(
            self.URL,
            {
                "token": str(uuid.uuid4()),
                "password": "Pass2024!",
                "confirm_password": "Pass2024!",
            },
            format="json",
        )
        assert resp.status_code == status.HTTP_400_BAD_REQUEST


# ── TESTS FORGOT PASSWORD ─────────────────────────────────────────────────────


@pytest.mark.django_db
class TestForgotPassword:
    """POST /forgot-password/ — demande de reset."""

    URL = f"{BASE}/forgot-password/"

    def test_forgot_known_email_returns_200(self, client_anon, rh_manager):
        """Email connu → 200 (lien envoyé)."""
        resp = client_anon.post(self.URL, {"email": rh_manager.email}, format="json")
        assert resp.status_code == status.HTTP_200_OK

    def test_forgot_unknown_email_returns_200(self, client_anon):
        """Email inconnu → 200 quand même (anti-énumération)."""
        resp = client_anon.post(self.URL, {"email": "inconnu@pme.com"}, format="json")
        assert resp.status_code == status.HTTP_200_OK

    def test_forgot_creates_reset_token(self, client_anon, rh_manager):
        """Pour un email connu, un token RESET_PASSWORD est créé."""
        client_anon.post(self.URL, {"email": rh_manager.email}, format="json")
        assert OneTimeToken.objects.filter(
            user=rh_manager,
            token_type=OneTimeToken.Type.RESET_PASSWORD,
            used_at__isnull=True,
        ).exists()


# ── TESTS RESET PASSWORD ──────────────────────────────────────────────────────


@pytest.mark.django_db
class TestResetPassword:
    """POST /reset-password/ — réinitialisation du mot de passe."""

    URL = f"{BASE}/reset-password/"
    LOGIN_URL = f"{BASE}/login/"

    def test_reset_success(self, client_anon, rh_manager):
        """Token valide + nouveau mdp → mdp modifié."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.RESET_PASSWORD)
        payload = {
            "token": str(ott.token),
            "nouveau_mot_de_passe": "NouveauMDP2024!",
            "confirm_mot_de_passe": "NouveauMDP2024!",
        }
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_200_OK

    def test_reset_new_password_works(self, client_anon, rh_manager):
        """Après reset, le nouveau mot de passe permet de se connecter."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.RESET_PASSWORD)
        new_password = "NouveauMDP2024!"
        client_anon.post(
            self.URL,
            {
                "token": str(ott.token),
                "nouveau_mot_de_passe": new_password,
                "confirm_mot_de_passe": new_password,
            },
            format="json",
        )
        # Login avec le nouveau mot de passe
        resp = client_anon.post(
            self.LOGIN_URL,
            {"email": rh_manager.email, "password": new_password},
            format="json",
        )
        assert resp.status_code == status.HTTP_200_OK

    def test_reset_token_single_use(self, client_anon, rh_manager):
        """Le token de reset ne peut être utilisé qu'une fois."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.RESET_PASSWORD)
        payload = {
            "token": str(ott.token),
            "nouveau_mot_de_passe": "NouveauMDP2024!",
            "confirm_mot_de_passe": "NouveauMDP2024!",
        }
        client_anon.post(self.URL, payload, format="json")
        resp = client_anon.post(self.URL, payload, format="json")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_reset_passwords_mismatch(self, client_anon, rh_manager):
        """Mots de passe différents → 400."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.RESET_PASSWORD)
        resp = client_anon.post(
            self.URL,
            {
                "token": str(ott.token),
                "nouveau_mot_de_passe": "Pass1!",
                "confirm_mot_de_passe": "Pass2!",
            },
            format="json",
        )
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_reset_expired_token(self, client_anon, rh_manager):
        """Token expiré → 400."""
        ott = OneTimeToken.create_for(rh_manager, OneTimeToken.Type.RESET_PASSWORD)
        ott.expires_at = timezone.now() - timezone.timedelta(hours=3)
        ott.save()
        resp = client_anon.post(
            self.URL,
            {
                "token": str(ott.token),
                "nouveau_mot_de_passe": "NouveauMDP2024!",
                "confirm_mot_de_passe": "NouveauMDP2024!",
            },
            format="json",
        )
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

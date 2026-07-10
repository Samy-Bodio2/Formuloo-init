"""
Tests d'authentification — Formuloo OS
Teste les endpoints login, logout, me, changer-mot-de-passe.
Conforme ADR-002 : authentification SSO + JWT
"""

import pytest
from rest_framework.test import APIClient


# ── LOGIN ─────────────────────────────────────────


@pytest.mark.django_db
class TestLogin:

    def test_login_succes(self, admin_alpha):
        """
        Given : un utilisateur actif existe
        When  : il envoie email + password corrects
        Then  : il reçoit access + refresh tokens
        """
        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/",
            {"email": "admin@pme-alpha.com", "password": "password123"},
            format="json",
        )

        assert response.status_code == 200
        assert "access" in response.data
        assert "refresh" in response.data
        assert "user" in response.data
        assert response.data["user"]["email"] == "admin@pme-alpha.com"

    def test_login_mauvais_password(self, admin_alpha):
        """
        Given : un utilisateur existe
        When  : il envoie un mauvais mot de passe
        Then  : il reçoit une erreur 400
        """
        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/",
            {"email": "admin@pme-alpha.com", "password": "mauvais_password"},
            format="json",
        )

        assert response.status_code == 400

    def test_login_email_inexistant(self, db):
        """
        Given : aucun utilisateur avec cet email
        When  : on tente de se connecter
        Then  : erreur 400
        """
        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/",
            {"email": "inexistant@pme.com", "password": "password123"},
            format="json",
        )

        assert response.status_code == 400

    def test_login_utilisateur_inactif(self, admin_alpha):
        """
        Given : un utilisateur désactivé
        When  : il tente de se connecter
        Then  : erreur 400
        """
        admin_alpha.is_active = False
        admin_alpha.save()

        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/",
            {"email": "admin@pme-alpha.com", "password": "password123"},
            format="json",
        )

        assert response.status_code == 400

    def test_login_sans_email(self, db):
        """
        Given : requête sans email
        When  : on tente de se connecter
        Then  : erreur 400
        """
        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/", {"password": "password123"}, format="json"
        )

        assert response.status_code == 400

    def test_login_retourne_tenant_id(self, admin_alpha, org_alpha):
        """
        Given : un utilisateur avec un tenant
        When  : il se connecte
        Then  : tenant_id est présent dans la réponse
        """
        client = APIClient()
        response = client.post(
            "/api/v1/auth/login/",
            {"email": "admin@pme-alpha.com", "password": "password123"},
            format="json",
        )

        assert response.status_code == 200
        assert response.data["user"]["tenant_id"] == str(org_alpha.id)


# ── LOGOUT ────────────────────────────────────────


@pytest.mark.django_db
class TestLogout:

    def test_logout_succes(self, client_alpha, token_admin_alpha):
        """
        Given : un utilisateur connecté
        When  : il envoie son refresh token
        Then  : déconnexion réussie — 204
        """
        response = client_alpha.post(
            "/api/v1/auth/logout/",
            {"refresh": token_admin_alpha["refresh"]},
            format="json",
        )

        assert response.status_code == 204

    def test_logout_sans_token(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on appelle logout
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/auth/logout/", {"refresh": "token_invalide"}, format="json"
        )

        assert response.status_code == 401

    def test_logout_token_invalide(self, client_alpha):
        """
        Given : un utilisateur connecté
        When  : il envoie un refresh token invalide
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/logout/", {"refresh": "token_invalide_xxx"}, format="json"
        )

        assert response.status_code == 400


# ── ME ────────────────────────────────────────────


@pytest.mark.django_db
class TestMe:

    def test_me_succes(self, client_alpha, admin_alpha):
        """
        Given : un utilisateur connecté
        When  : il appelle GET /me/
        Then  : il reçoit son profil
        """
        response = client_alpha.get("/api/v1/auth/me/")

        assert response.status_code == 200
        assert response.data["email"] == admin_alpha.email
        assert response.data["first_name"] == admin_alpha.first_name
        assert response.data["last_name"] == admin_alpha.last_name

    def test_me_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on appelle GET /me/
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/me/")
        assert response.status_code == 401

    def test_me_patch_succes(self, client_alpha, admin_alpha):
        """
        Given : un utilisateur connecté
        When  : il modifie son profil
        Then  : profil mis à jour — 200
        """
        response = client_alpha.patch(
            "/api/v1/auth/me/",
            {"first_name": "Jean-Pierre", "last_name": "Dupont-Kamga"},
            format="json",
        )

        assert response.status_code == 200
        assert response.data["first_name"] == "Jean-Pierre"
        assert response.data["last_name"] == "Dupont-Kamga"


# ── CHANGER MOT DE PASSE ──────────────────────────


@pytest.mark.django_db
class TestChangerMotDePasse:

    def test_changer_mdp_succes(self, client_alpha):
        """
        Given : un utilisateur connecté
        When  : il fournit l'ancien + nouveau mot de passe
        Then  : mot de passe changé — 204
        """
        response = client_alpha.post(
            "/api/v1/auth/me/changer-mot-de-passe/",
            {
                "ancien_mot_de_passe": "password123",
                "nouveau_mot_de_passe": "nouveau_password456",
            },
            format="json",
        )

        assert response.status_code == 204

    def test_changer_mdp_mauvais_ancien(self, client_alpha):
        """
        Given : un utilisateur connecté
        When  : il fournit un mauvais ancien mot de passe
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/me/changer-mot-de-passe/",
            {
                "ancien_mot_de_passe": "mauvais_password",
                "nouveau_mot_de_passe": "nouveau_password456",
            },
            format="json",
        )

        assert response.status_code == 400

    def test_changer_mdp_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on appelle changer-mot-de-passe
        Then  : erreur 401
        """
        response = client_anonyme.post(
            "/api/v1/auth/me/changer-mot-de-passe/",
            {
                "ancien_mot_de_passe": "password123",
                "nouveau_mot_de_passe": "nouveau_password456",
            },
            format="json",
        )

        assert response.status_code == 401

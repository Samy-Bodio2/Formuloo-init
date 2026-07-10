"""
Tests d'intégration — Passerelle Formuloo OS

Valide :
  AC1 — Communication inter-services (Auth → HR via JWT)
  AC2 — Passerelle nginx route correctement les requêtes
  AC4 — SSO propagé (token Auth accepté par HR)

Prérequis :
  - Stack complète lancée : docker-compose up -d
  - Variables d'env : GATEWAY_URL, TEST_RH_EMAIL, TEST_RH_PASSWORD

Lancement :
  pytest tests/integration/ -m integration -v
"""

import pytest
import requests


@pytest.mark.integration
class TestPasserelle:
    """La passerelle répond et route les deux services."""

    def test_health_gateway(self, gateway_url):
        """GET /health → passerelle nginx répond 200."""
        resp = requests.get(f"{gateway_url}/health", timeout=5)
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"

    def test_route_auth_accessible(self, auth_base):
        """La route auth est accessible via la passerelle (login endpoint)."""
        resp = requests.post(
            f"{auth_base}/login/",
            json={"email": "inexistant@test.cm", "password": "wrong"},
            timeout=5,
        )
        assert resp.status_code in (400, 401), "Auth service doit répondre (pas 502)"

    def test_route_hr_accessible(self, hr_base, rh_headers):
        """La route HR est accessible via la passerelle."""
        resp = requests.get(f"{hr_base}/employes/", headers=rh_headers, timeout=5)
        assert resp.status_code == 200


@pytest.mark.integration
class TestSSOPropagation:
    """Le JWT Auth est accepté par le service HR via la passerelle."""

    def test_token_auth_accepte_par_hr(self, auth_base, hr_base, rh_headers):
        """AC4 — Token obtenu sur /auth/ est valide sur /hr/."""
        resp = requests.get(f"{hr_base}/employes/", headers=rh_headers, timeout=5)
        assert resp.status_code == 200, "HR doit accepter le JWT d'Auth"

    def test_sans_token_hr_renvoie_401(self, hr_base):
        """Sans JWT, HR renvoie 401."""
        resp = requests.get(f"{hr_base}/employes/", timeout=5)
        assert resp.status_code == 401

    def test_token_expire_renvoie_401(self, hr_base):
        """JWT invalide → 401."""
        headers = {"Authorization": "Bearer token.invalide.ici"}
        resp = requests.get(f"{hr_base}/employes/", headers=headers, timeout=5)
        assert resp.status_code == 401

    def test_flux_complet_login_puis_hr(self, auth_base, hr_base, rh_token):
        """AC1+AC4 — Flux complet : login via auth, appel HR avec le token."""
        headers = {"Authorization": f"Bearer {rh_token}"}
        resp = requests.get(f"{hr_base}/departements/", headers=headers, timeout=5)
        assert resp.status_code == 200
        assert "results" in resp.json()


@pytest.mark.integration
class TestConventionsContrats:
    """AC3 — Les deux services respectent les mêmes conventions de réponse."""

    def test_pagination_auth(self, auth_base, rh_headers):
        """Auth utilise la pagination standard (count/results)."""
        resp = requests.get(f"{auth_base}/utilisateurs/", headers=rh_headers, timeout=5)
        assert resp.status_code == 200
        body = resp.json()
        assert "count" in body
        assert "results" in body

    def test_pagination_hr(self, hr_base, rh_headers):
        """HR utilise la pagination standard (count/results)."""
        resp = requests.get(f"{hr_base}/employes/", headers=rh_headers, timeout=5)
        assert resp.status_code == 200
        body = resp.json()
        assert "count" in body
        assert "results" in body

    def test_erreur_400_format_coherent_auth(self, auth_base):
        """Auth retourne un objet JSON structuré sur erreur 400."""
        resp = requests.post(f"{auth_base}/login/", json={}, timeout=5)
        assert resp.status_code in (400, 401)
        assert resp.headers["Content-Type"].startswith("application/json")

    def test_erreur_400_format_coherent_hr(self, hr_base, rh_headers):
        """HR retourne un objet JSON structuré sur erreur 400."""
        resp = requests.post(
            f"{hr_base}/employes/",
            json={},
            headers=rh_headers,
            timeout=5,
        )
        assert resp.status_code == 400
        assert resp.headers["Content-Type"].startswith("application/json")

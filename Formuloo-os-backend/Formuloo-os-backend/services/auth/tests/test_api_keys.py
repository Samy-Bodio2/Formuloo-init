"""
Tests Clés API — Formuloo OS
Teste les endpoints CRUD des clés API.
Conforme ADR-002 : sécurité + multi-tenant
"""

import pytest


@pytest.mark.django_db
class TestAPIKeys:

    # ── LIST ──────────────────────────────────────

    def test_lister_api_keys_succes(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : une clé API existe dans le tenant
        When  : l'admin liste les clés
        Then  : liste retournée — 200
        """
        from authentification.models import APIKey

        APIKey.objects.create(
            tenant=org_alpha,
            owner=admin_alpha,
            name="Clé Test",
            key_hash="abc123hash",
            scopes=[],
            rate_limit=100,
        )

        response = client_alpha.get("/api/v1/auth/api-keys/")

        assert response.status_code == 200
        assert len(response.data) == 1

    def test_lister_api_keys_sans_auth(self, client_anonyme):
        """
        Given : aucun token JWT
        When  : on liste les clés API
        Then  : erreur 401
        """
        response = client_anonyme.get("/api/v1/auth/api-keys/")
        assert response.status_code == 401

    def test_isolation_api_keys_multi_tenant(
        self, client_alpha, admin_alpha, org_alpha, client_beta, admin_beta, org_beta
    ):
        """
        Given : chaque PME a une clé API
        When  : PME Alpha liste ses clés
        Then  : elle ne voit pas les clés de PME Beta
        """
        from authentification.models import APIKey

        APIKey.objects.create(
            tenant=org_alpha,
            owner=admin_alpha,
            name="Clé Alpha",
            key_hash="hash_alpha",
            scopes=[],
            rate_limit=100,
        )
        APIKey.objects.create(
            tenant=org_beta,
            owner=admin_beta,
            name="Clé Beta",
            key_hash="hash_beta",
            scopes=[],
            rate_limit=100,
        )

        response = client_alpha.get("/api/v1/auth/api-keys/")
        noms = [k["name"] for k in response.data]

        assert "Clé Alpha" in noms
        assert "Clé Beta" not in noms

    # ── CREATE ────────────────────────────────────

    def test_creer_api_key_succes(self, client_alpha, db):
        """
        Given : admin connecté
        When  : il crée une clé API
        Then  : clé créée — 201 + clé brute retournée
        """
        response = client_alpha.post(
            "/api/v1/auth/api-keys/",
            {
                "name": "Intégration Mobile Money",
                "scopes": ["compta.read.factures"],
                "rate_limit": 100,
            },
            format="json",
        )

        assert response.status_code == 201
        assert "key" in response.data
        assert "message" in response.data
        assert response.data["name"] == "Intégration Mobile Money"
        # La clé brute doit être présente
        assert len(response.data["key"]) > 0

    def test_creer_api_key_sans_nom(self, client_alpha, db):
        """
        Given : admin connecté
        When  : il crée une clé sans nom
        Then  : erreur 400
        """
        response = client_alpha.post(
            "/api/v1/auth/api-keys/", {"scopes": []}, format="json"
        )

        assert response.status_code == 400

    def test_creer_api_key_nom_existant(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : une clé active avec ce nom existe
        When  : on tente de créer avec le même nom
        Then  : erreur 400
        """
        from authentification.models import APIKey

        APIKey.objects.create(
            tenant=org_alpha,
            owner=admin_alpha,
            name="Clé Existante",
            key_hash="hash_existant",
            scopes=[],
            rate_limit=100,
            is_active=True,
        )

        response = client_alpha.post(
            "/api/v1/auth/api-keys/", {"name": "Clé Existante"}, format="json"
        )

        assert response.status_code == 400

    # ── DETAIL ────────────────────────────────────

    def test_detail_api_key_succes(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : une clé API existe dans le tenant
        When  : on demande son détail
        Then  : clé retournée — 200 (sans clé brute)
        """
        from authentification.models import APIKey

        api_key = APIKey.objects.create(
            tenant=org_alpha,
            owner=admin_alpha,
            name="Clé Détail",
            key_hash="hash_detail",
            scopes=[],
            rate_limit=100,
        )

        response = client_alpha.get(f"/api/v1/auth/api-keys/{api_key.id}/")

        assert response.status_code == 200
        assert response.data["name"] == "Clé Détail"
        # La clé brute ne doit jamais être retournée
        assert "key" not in response.data
        assert "key_hash" not in response.data

    def test_detail_api_key_autre_tenant(self, client_alpha, admin_beta, org_beta):
        """
        Given : une clé appartient à PME Beta
        When  : PME Alpha essaie d'y accéder
        Then  : erreur 404
        """
        from authentification.models import APIKey

        api_key = APIKey.objects.create(
            tenant=org_beta,
            owner=admin_beta,
            name="Clé Beta",
            key_hash="hash_beta_detail",
            scopes=[],
            rate_limit=100,
        )

        response = client_alpha.get(f"/api/v1/auth/api-keys/{api_key.id}/")

        assert response.status_code == 404

    # ── DELETE (révoquer) ─────────────────────────

    def test_revoquer_api_key_succes(self, client_alpha, admin_alpha, org_alpha):
        """
        Given : une clé API active existe
        When  : l'admin la révoque
        Then  : clé désactivée — 204
        """
        from authentification.models import APIKey

        api_key = APIKey.objects.create(
            tenant=org_alpha,
            owner=admin_alpha,
            name="Clé à Révoquer",
            key_hash="hash_revoquer",
            scopes=[],
            rate_limit=100,
            is_active=True,
        )

        response = client_alpha.delete(f"/api/v1/auth/api-keys/{api_key.id}/")

        assert response.status_code == 204

        # Vérifier que la clé est désactivée
        api_key.refresh_from_db()
        assert api_key.is_active is False

    def test_revoquer_api_key_inexistante(self, client_alpha, db):
        """
        Given : aucune clé avec cet UUID
        When  : on tente de la révoquer
        Then  : erreur 404
        """
        import uuid

        fake_id = uuid.uuid4()
        response = client_alpha.delete(f"/api/v1/auth/api-keys/{fake_id}/")

        assert response.status_code == 404

"""
Tests workflow DemandeDocument — Formuloo OS

Couvre :
  POST /api/v1/hr/me/demandes-document/               → soumettre une demande
  GET  /api/v1/hr/me/demandes-document/               → mes demandes
  GET  /api/v1/hr/me/demandes-document/{pk}/          → détail (+ document si approuvée)
  DELETE /api/v1/hr/me/demandes-document/{pk}/        → annuler
  GET  /api/v1/hr/demandes-document/                  → RH : toutes les demandes
  POST /api/v1/hr/demandes-document/{pk}/approuver/   → RH : approuver
  POST /api/v1/hr/demandes-document/{pk}/rejeter/     → RH : rejeter

Règle métier : l'employé ne peut pas se délivrer lui-même son attestation.
Il soumet une demande, le RH approuve, l'employé récupère le document.
"""

import uuid
from datetime import date

import pytest
from rest_framework import status
from rest_framework.test import APIClient

from rh.models import DemandeDocument, Departement, Employe, Poste

# ── TENANT ET URLS ────────────────────────────────────────────────────────────
TENANT = uuid.UUID("dddddddd-eeee-ffff-0000-111111111111")
BASE = "/api/v1/hr"
ME_DEMANDES_URL = f"{BASE}/me/demandes-document/"
RH_DEMANDES_URL = f"{BASE}/demandes-document/"


# ── FIXTURES ──────────────────────────────────────────────────────────────────


@pytest.fixture
def employe_avec_compte(db):
    """Employé avec un compte Auth lié (user_id = auth_user_id)."""
    from django.contrib.auth.models import User

    auth_user_id = uuid.uuid4()

    user = User.objects.create_user(
        username="emp_demande", email="emp@pme.cm", password="pass"
    )
    user.tenant_id = TENANT
    user.roles = ["EMPLOYE"]
    user.auth_user_id = auth_user_id
    user.custom_role = None

    dept = Departement.objects.create(nom="RH", tenant_id=TENANT)
    poste = Poste.objects.create(
        titre="Assistant RH",
        code="ARH-001",
        tenant_id=TENANT,
        departement=dept,
    )
    employe = Employe.objects.create(
        tenant_id=TENANT,
        user_id=auth_user_id,
        first_name="Alice",
        last_name="Biya",
        email="emp@pme.cm",
        phone="699000001",
        hire_date=date(2022, 1, 15),
        department=dept,
        position=poste,
        salaire_base=400000,
    )
    return user, employe


@pytest.fixture
def user_rh(db):
    """RH Manager du tenant de test."""
    from django.contrib.auth.models import User

    user = User.objects.create_user(
        username="rh_demande", email="rh@pme.cm", password="pass"
    )
    user.tenant_id = TENANT
    user.roles = ["RH_MANAGER"]
    user.auth_user_id = uuid.uuid4()
    user.custom_role = None
    return user


@pytest.fixture
def client_employe(employe_avec_compte):
    user, _ = employe_avec_compte
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def client_rh(user_rh):
    client = APIClient()
    client.force_authenticate(user=user_rh)
    return client


@pytest.fixture
def demande_en_attente(db, employe_avec_compte):
    """Demande d'attestation de travail en attente."""
    _, employe = employe_avec_compte
    return DemandeDocument.objects.create(
        tenant_id=TENANT,
        employee=employe,
        type_document=DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL,
        motif_demande="Demande de visa Schengen",
    )


# ── TESTS SOUMISSION EMPLOYÉ ──────────────────────────────────────────────────


@pytest.mark.django_db
class TestSoumissionDemande:
    """POST /me/demandes-document/ — soumettre une demande."""

    def test_submit_attestation_travail(self, client_employe):
        """Employé peut soumettre une demande d'attestation de travail."""
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_travail", "motif_demande": "Prêt bancaire"},
            format="json",
        )
        assert resp.status_code == status.HTTP_201_CREATED
        assert resp.data["type_document"] == "attestation_travail"
        assert resp.data["statut"] == DemandeDocument.Statut.EN_ATTENTE
        assert resp.data["document_data"] is None  # pas encore approuvée

    def test_submit_attestation_salaire(self, client_employe):
        """Employé peut soumettre une demande d'attestation de salaire."""
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_salaire"},
            format="json",
        )
        assert resp.status_code == status.HTTP_201_CREATED
        assert resp.data["statut"] == DemandeDocument.Statut.EN_ATTENTE

    def test_submit_type_invalide(self, client_employe):
        """Type de document invalide → 400."""
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "faux_document"},
            format="json",
        )
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_submit_unauthenticated(self):
        """Non authentifié → 401."""
        resp = APIClient().post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_travail"},
            format="json",
        )
        assert resp.status_code == status.HTTP_401_UNAUTHORIZED

    def test_document_data_null_avant_approbation(self, client_employe):
        """Le champ document_data est NULL tant que la demande n'est pas approuvée."""
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_travail"},
            format="json",
        )
        assert resp.status_code == status.HTTP_201_CREATED
        assert resp.data["document_data"] is None


# ── TESTS LISTE EMPLOYÉ ───────────────────────────────────────────────────────


@pytest.mark.django_db
class TestListeMesDemandes:
    """GET /me/demandes-document/ — liste des demandes de l'employé."""

    def test_liste_vide(self, client_employe):
        """Aucune demande → liste vide."""
        resp = client_employe.get(ME_DEMANDES_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 0

    def test_liste_uniquement_les_siennes(
        self, client_employe, employe_avec_compte, db
    ):
        """L'employé ne voit que SES propres demandes."""
        _, employe = employe_avec_compte

        # Sa demande
        DemandeDocument.objects.create(
            tenant_id=TENANT,
            employee=employe,
            type_document=DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL,
        )

        # Demande d'un autre employé — ne doit pas apparaître
        autre = Employe.objects.create(
            tenant_id=TENANT,
            first_name="Bob",
            last_name="Autre",
            email="autre@pme.cm",
            phone="699000002",
            hire_date=date(2023, 1, 1),
        )
        DemandeDocument.objects.create(
            tenant_id=TENANT,
            employee=autre,
            type_document=DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL,
        )

        resp = client_employe.get(ME_DEMANDES_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1

    def test_filtre_par_statut(self, client_employe, demande_en_attente):
        """Filtre par statut fonctionne."""
        resp = client_employe.get(f"{ME_DEMANDES_URL}?statut=en_attente")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1

        resp = client_employe.get(f"{ME_DEMANDES_URL}?statut=approuvee")
        assert resp.data["count"] == 0


# ── TESTS ANNULATION EMPLOYÉ ──────────────────────────────────────────────────


@pytest.mark.django_db
class TestAnnulationDemande:
    """DELETE /me/demandes-document/{pk}/ — annuler une demande."""

    def test_annuler_demande_en_attente(self, client_employe, demande_en_attente):
        """L'employé peut annuler sa demande si elle est encore EN_ATTENTE."""
        resp = client_employe.delete(f"{ME_DEMANDES_URL}{demande_en_attente.id}/")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["statut"] == DemandeDocument.Statut.ANNULEE

    def test_annuler_demande_deja_approuvee(
        self, db, employe_avec_compte, client_employe
    ):
        """Impossible d'annuler une demande déjà approuvée → 400."""
        _, employe = employe_avec_compte
        demande = DemandeDocument.objects.create(
            tenant_id=TENANT,
            employee=employe,
            type_document=DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL,
            statut=DemandeDocument.Statut.APPROUVEE,
        )
        resp = client_employe.delete(f"{ME_DEMANDES_URL}{demande.id}/")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST


# ── TESTS WORKFLOW RH ─────────────────────────────────────────────────────────


@pytest.mark.django_db
class TestWorkflowRH:
    """Workflow RH : liste, approbation, rejet."""

    def test_rh_voit_toutes_les_demandes(self, client_rh, demande_en_attente):
        """Le RH voit toutes les demandes du tenant."""
        resp = client_rh.get(RH_DEMANDES_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 1

    def test_employe_ne_peut_pas_voir_toutes_demandes(self, client_employe):
        """Un EMPLOYE ne peut pas accéder à la liste globale des demandes → 403."""
        resp = client_employe.get(RH_DEMANDES_URL)
        assert resp.status_code == status.HTTP_403_FORBIDDEN

    def test_approuver_genere_document_data(self, client_rh, demande_en_attente):
        """L'approbation RH génère les données du document."""
        resp = client_rh.post(f"{RH_DEMANDES_URL}{demande_en_attente.id}/approuver/")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["statut"] == DemandeDocument.Statut.APPROUVEE
        assert resp.data["document_data"] is not None
        # Vérifie la structure de l'attestation de travail
        doc = resp.data["document_data"]
        assert "meta" in doc
        assert "employe" in doc
        assert "poste" in doc
        assert "contrat" in doc
        assert "remuneration" in doc
        assert doc["meta"]["type_document"] == "Attestation de travail"

    def test_approuver_deux_fois_interdit(self, client_rh, demande_en_attente):
        """On ne peut pas approuver une demande déjà approuvée → 400."""
        client_rh.post(f"{RH_DEMANDES_URL}{demande_en_attente.id}/approuver/")
        resp = client_rh.post(f"{RH_DEMANDES_URL}{demande_en_attente.id}/approuver/")
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_rejeter_avec_motif(self, client_rh, demande_en_attente):
        """RH peut rejeter une demande avec un motif."""
        resp = client_rh.post(
            f"{RH_DEMANDES_URL}{demande_en_attente.id}/rejeter/",
            {"motif_rejet": "Document non nécessaire pour ce type de démarche."},
            format="json",
        )
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["statut"] == DemandeDocument.Statut.REJETEE
        assert "Document non nécessaire" in resp.data["motif_rejet"]

    def test_rejeter_sans_motif_interdit(self, client_rh, demande_en_attente):
        """Rejet sans motif → 400."""
        resp = client_rh.post(
            f"{RH_DEMANDES_URL}{demande_en_attente.id}/rejeter/",
            {},
            format="json",
        )
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_isolation_tenant(self, client_rh, db):
        """Le RH ne voit que les demandes de son propre tenant."""
        autre_tenant = uuid.UUID("aaaaaaaa-1111-2222-3333-444444444444")
        employe_autre = Employe.objects.create(
            tenant_id=autre_tenant,
            first_name="Autre",
            last_name="Tenant",
            email="autre@autre.cm",
            phone="699999999",
            hire_date=date(2023, 1, 1),
        )
        DemandeDocument.objects.create(
            tenant_id=autre_tenant,
            employee=employe_autre,
            type_document=DemandeDocument.TypeDocument.ATTESTATION_TRAVAIL,
        )

        resp = client_rh.get(RH_DEMANDES_URL)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["count"] == 0  # rien du tenant DDDD...


# ── TESTS ACCÈS AU DOCUMENT APPROUVÉ ─────────────────────────────────────────


@pytest.mark.django_db
class TestAccesDocumentApprouve:
    """L'employé accède aux données du document uniquement après approbation."""

    def test_employe_voit_document_apres_approbation(
        self, client_employe, employe_avec_compte, client_rh, db
    ):
        """Après approbation RH, l'employé peut lire document_data."""
        _, employe = employe_avec_compte

        # 1. L'employé soumet
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_travail", "motif_demande": "Visa"},
            format="json",
        )
        assert resp.status_code == status.HTTP_201_CREATED
        demande_id = resp.data["id"]

        # 2. Le RH approuve
        client_rh.post(f"{RH_DEMANDES_URL}{demande_id}/approuver/")

        # 3. L'employé lit son document
        resp = client_employe.get(f"{ME_DEMANDES_URL}{demande_id}/")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["statut"] == DemandeDocument.Statut.APPROUVEE
        assert resp.data["document_data"] is not None
        assert resp.data["document_data"]["employe"]["nom_complet"] == employe.full_name

    def test_employe_voit_motif_rejet(
        self, client_employe, employe_avec_compte, client_rh, db
    ):
        """Après rejet, l'employé voit le motif de rejet."""
        _, employe = employe_avec_compte

        # 1. Soumission
        resp = client_employe.post(
            ME_DEMANDES_URL,
            {"type_document": "attestation_travail"},
            format="json",
        )
        demande_id = resp.data["id"]

        # 2. Rejet
        client_rh.post(
            f"{RH_DEMANDES_URL}{demande_id}/rejeter/",
            {"motif_rejet": "Dossier incomplet — fournir votre CNI d'abord."},
            format="json",
        )

        # 3. L'employé voit le motif
        resp = client_employe.get(f"{ME_DEMANDES_URL}{demande_id}/")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data["statut"] == DemandeDocument.Statut.REJETEE
        assert "CNI" in resp.data["motif_rejet"]
        assert resp.data["document_data"] is None  # aucune donnée si rejetée

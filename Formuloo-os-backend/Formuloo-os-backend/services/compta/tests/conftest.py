"""
Configuration pytest — Service Compta
Fixtures partagées. Isolation multi-tenant.
"""

import uuid
from datetime import date, timedelta

import pytest
from django.contrib.auth.models import User
from rest_framework.test import APIClient

from comptabilite.models import Exercice, Compte, Journal, Ecriture, LigneEcriture, Facture, LigneFacture

# ── Tenants ───────────────────────────────────────────────
TENANT_ALPHA = uuid.uuid4()
TENANT_BETA = uuid.uuid4()

PERMS_COMPTABLE = [
    "compta.read.ecritures", "compta.write.ecritures", "compta.validate.ecritures",
    "compta.delete.ecritures",
    "compta.read.comptes", "compta.write.comptes", "compta.delete.comptes",
    "compta.read.journaux", "compta.write.journaux",
    "compta.read.exercices", "compta.write.exercices",
    "compta.read.factures", "compta.write.factures", "compta.delete.factures",
    "compta.read.paiements", "compta.write.paiements",
    "compta.read.etats",
]

PERMS_DAF = PERMS_COMPTABLE + ["compta.close.exercices"]

PERMS_AUDITEUR = ["compta.read.ecritures", "compta.read.etats", "compta.read.factures",
                  "compta.read.comptes", "compta.read.journaux", "compta.read.exercices",
                  "compta.read.paiements"]


def make_user(tenant_id, permissions):
    user = User.objects.create_user(
        username=f"user_{uuid.uuid4().hex[:8]}",
        email=f"user_{uuid.uuid4().hex[:8]}@test.cm",
        password="pw",
    )
    user.tenant_id = tenant_id
    user.permissions = permissions
    user.roles = ["COMPTABLE"]
    user.auth_user_id = uuid.uuid4()
    return user


def make_client(user):
    client = APIClient()
    client.force_authenticate(user=user)
    return client


# ── FIXTURES CLIENTS ──────────────────────────────────────

@pytest.fixture
def comptable_alpha(db):
    return make_user(TENANT_ALPHA, PERMS_COMPTABLE)


@pytest.fixture
def daf_alpha(db):
    return make_user(TENANT_ALPHA, PERMS_DAF)


@pytest.fixture
def auditeur_alpha(db):
    return make_user(TENANT_ALPHA, PERMS_AUDITEUR)


@pytest.fixture
def comptable_beta(db):
    return make_user(TENANT_BETA, PERMS_COMPTABLE)


@pytest.fixture
def client_comptable(comptable_alpha):
    return make_client(comptable_alpha)


@pytest.fixture
def client_daf(daf_alpha):
    return make_client(daf_alpha)


@pytest.fixture
def client_auditeur(auditeur_alpha):
    return make_client(auditeur_alpha)


@pytest.fixture
def client_beta(comptable_beta):
    return make_client(comptable_beta)


@pytest.fixture
def client_anonyme():
    return APIClient()


# ── FIXTURES DONNÉES ──────────────────────────────────────

@pytest.fixture
def exercice_alpha(db):
    return Exercice.objects.create(
        tenant_id=TENANT_ALPHA,
        annee=2024,
        date_debut=date(2024, 1, 1),
        date_fin=date(2024, 12, 31),
        statut="OUVERT",
    )


@pytest.fixture
def exercice_cloture(db):
    return Exercice.objects.create(
        tenant_id=TENANT_ALPHA,
        annee=2023,
        date_debut=date(2023, 1, 1),
        date_fin=date(2023, 12, 31),
        statut="CLOTURE",
    )


@pytest.fixture
def compte_client(db):
    return Compte.objects.create(
        tenant_id=TENANT_ALPHA,
        numero="411000",
        libelle="Clients",
        classe=4,
        type_compte="ACTIF",
    )


@pytest.fixture
def compte_ventes(db):
    return Compte.objects.create(
        tenant_id=TENANT_ALPHA,
        numero="701000",
        libelle="Ventes de marchandises",
        classe=7,
        type_compte="PRODUIT",
    )


@pytest.fixture
def compte_banque(db):
    return Compte.objects.create(
        tenant_id=TENANT_ALPHA,
        numero="521000",
        libelle="Banque",
        classe=5,
        type_compte="ACTIF",
    )


@pytest.fixture
def journal_ventes(db):
    return Journal.objects.create(
        tenant_id=TENANT_ALPHA,
        code="VTE",
        libelle="Journal des ventes",
        type="VENTES",
    )


@pytest.fixture
def journal_banque(db):
    return Journal.objects.create(
        tenant_id=TENANT_ALPHA,
        code="BNQ",
        libelle="Journal de banque",
        type="BANQUE",
    )


@pytest.fixture
def ecriture_brouillon(db, exercice_alpha, journal_ventes, compte_client, compte_ventes):
    ecriture = Ecriture.objects.create(
        tenant_id=TENANT_ALPHA,
        journal=journal_ventes,
        exercice=exercice_alpha,
        date_ecriture=date(2024, 6, 1),
        libelle="Vente prestation",
        statut="BROUILLON",
    )
    LigneEcriture.objects.create(
        ecriture=ecriture, compte=compte_client,
        libelle="Client Alpha", debit=500000, credit=0,
    )
    LigneEcriture.objects.create(
        ecriture=ecriture, compte=compte_ventes,
        libelle="Prestation", debit=0, credit=500000,
    )
    return ecriture


@pytest.fixture
def facture_brouillon(db):
    facture = Facture.objects.create(
        tenant_id=TENANT_ALPHA,
        numero="FAC-2024-0001",
        client_nom="Client Test SARL",
        client_email="client@test.cm",
        devise="XAF",
        statut="BROUILLON",
        date_echeance=date(2024, 7, 31),
    )
    LigneFacture.objects.create(
        facture=facture,
        description="Développement API",
        quantite=1,
        prix_unitaire=500000,
    )
    return facture


@pytest.fixture
def facture_emise(db):
    facture = Facture.objects.create(
        tenant_id=TENANT_ALPHA,
        numero="FAC-2024-0002",
        client_nom="Client Émis SARL",
        client_email="emis@test.cm",
        devise="XAF",
        statut="EMISE",
        date_emission=date(2024, 6, 1),
        date_echeance=date(2024, 7, 31),
    )
    LigneFacture.objects.create(
        facture=facture,
        description="Maintenance",
        quantite=1,
        prix_unitaire=200000,
    )
    return facture

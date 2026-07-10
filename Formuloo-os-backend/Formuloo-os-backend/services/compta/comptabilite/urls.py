from django.urls import path
from comptabilite.views.exercice import ExercicesListView, ExerciceDetailView, ExerciceCloturerView
from comptabilite.views.compte import ComptesListView, CompteDetailView
from comptabilite.views.journal import JournauxListView, JournalDetailView
from comptabilite.views.ecriture import EcrituresListView, EcritureDetailView, EcritureValiderView
from comptabilite.views.facture import FacturesListView, FactureDetailView, FactureEmettreView
from comptabilite.views.facture_pdf import FacturePDFView
from comptabilite.views.facture_fournisseur import (
    FacturesFournisseursListView, FactureFournisseurDetailView,
    FactureFournisseurRecevoirView, FactureFournisseurValiderView,
)
from comptabilite.views.avoir import AvoirClientView, AvoirFournisseurView
from comptabilite.views.paiement import PaiementsListView, PaiementDetailView
from comptabilite.views.paiement_fournisseur import (
    PayerFactureFournisseurView,
    PaiementsFournisseursListView, PaiementFournisseurDetailView,
)
from comptabilite.views.etats import GrandLivreView, BalanceView, BilanView, CompteResultatView
from comptabilite.views.declarations import DeclarationTVAView
from comptabilite.views.stats import StatsView
from comptabilite.views.initialiser import InitialiserView
from comptabilite.views.internal import EcriturePaieInternalView, EcritureAchatInternalView
from comptabilite.views.lettrage import (
    LettrerEcrituresView, DelettrerEcrituresView, LigreEcritureLettrageListView,
)
from comptabilite.views.immobilisation import (
    ImmobilisationsListView, ImmobilisationDetailView,
    ImmobilisationAmortirView, ImmobilisationPlanView, DotationsListView,
)

urlpatterns = [
    # ── Initialisation SYSCOHADA ─────────────────────────────
    path("initialiser/", InitialiserView.as_view(), name="initialiser"),

    # ── Exercices ────────────────────────────────────────────
    path("exercices/", ExercicesListView.as_view(), name="exercices-list"),
    path("exercices/<int:pk>/", ExerciceDetailView.as_view(), name="exercice-detail"),
    path("exercices/<int:pk>/cloturer/", ExerciceCloturerView.as_view(), name="exercice-cloturer"),

    # ── Plan de comptes ──────────────────────────────────────
    path("comptes/", ComptesListView.as_view(), name="comptes-list"),
    path("comptes/<int:pk>/", CompteDetailView.as_view(), name="compte-detail"),

    # ── Journaux ─────────────────────────────────────────────
    path("journaux/", JournauxListView.as_view(), name="journaux-list"),
    path("journaux/<int:pk>/", JournalDetailView.as_view(), name="journal-detail"),

    # ── Écritures ────────────────────────────────────────────
    path("ecritures/", EcrituresListView.as_view(), name="ecritures-list"),
    path("ecritures/<int:pk>/", EcritureDetailView.as_view(), name="ecriture-detail"),
    path("ecritures/<int:pk>/valider/", EcritureValiderView.as_view(), name="ecriture-valider"),

    # ── Factures clients (ventes) ────────────────────────────
    path("factures/", FacturesListView.as_view(), name="factures-list"),
    path("factures/<int:pk>/", FactureDetailView.as_view(), name="facture-detail"),
    path("factures/<int:pk>/emettre/", FactureEmettreView.as_view(), name="facture-emettre"),
    path("factures/<int:pk>/pdf/", FacturePDFView.as_view(), name="facture-pdf"),
    path("factures/<int:pk>/avoir/", AvoirClientView.as_view(), name="facture-avoir"),

    # ── Factures fournisseurs (achats) ───────────────────────
    path("achats/", FacturesFournisseursListView.as_view(), name="achats-list"),
    path("achats/<int:pk>/", FactureFournisseurDetailView.as_view(), name="achat-detail"),
    path("achats/<int:pk>/recevoir/", FactureFournisseurRecevoirView.as_view(), name="achat-recevoir"),
    path("achats/<int:pk>/valider/", FactureFournisseurValiderView.as_view(), name="achat-valider"),
    path("achats/<int:pk>/payer/", PayerFactureFournisseurView.as_view(), name="achat-payer"),
    path("achats/<int:pk>/avoir/", AvoirFournisseurView.as_view(), name="achat-avoir"),

    # ── Paiements clients ────────────────────────────────────
    path("paiements/", PaiementsListView.as_view(), name="paiements-list"),
    path("paiements/<int:pk>/", PaiementDetailView.as_view(), name="paiement-detail"),

    # ── Paiements fournisseurs ───────────────────────────────
    path("paiements-fournisseurs/", PaiementsFournisseursListView.as_view(), name="paiements-fournisseurs-list"),
    path("paiements-fournisseurs/<int:pk>/", PaiementFournisseurDetailView.as_view(), name="paiement-fournisseur-detail"),

    # ── États financiers ─────────────────────────────────────
    path("grand-livre/", GrandLivreView.as_view(), name="grand-livre"),
    path("balance/", BalanceView.as_view(), name="balance"),
    path("bilan/", BilanView.as_view(), name="bilan"),
    path("compte-resultat/", CompteResultatView.as_view(), name="compte-resultat"),

    # ── Déclarations fiscales ────────────────────────────────
    path("declarations/tva/", DeclarationTVAView.as_view(), name="declaration-tva"),

    # ── Dashboard statistiques ───────────────────────────────
    path("stats/", StatsView.as_view(), name="stats"),

    # ── Lettrage des écritures ───────────────────────────────
    path("ecritures/lettrer/", LettrerEcrituresView.as_view(), name="ecritures-lettrer"),
    path("ecritures/delettrer/", DelettrerEcrituresView.as_view(), name="ecritures-delettrer"),
    path("ecritures/lettrage/", LigreEcritureLettrageListView.as_view(), name="ecritures-lettrage"),

    # ── Immobilisations & Amortissements (OHADA Classe 2) ───
    path("immobilisations/", ImmobilisationsListView.as_view(), name="immobilisations-list"),
    path("immobilisations/dotations/", DotationsListView.as_view(), name="dotations-list"),
    path("immobilisations/<int:pk>/", ImmobilisationDetailView.as_view(), name="immobilisation-detail"),
    path("immobilisations/<int:pk>/amortir/", ImmobilisationAmortirView.as_view(), name="immobilisation-amortir"),
    path("immobilisations/<int:pk>/plan/", ImmobilisationPlanView.as_view(), name="immobilisation-plan"),

    # ── Endpoints internes (inter-services — non exposés via gateway) ──
    path("_internal/ecritures-paie/", EcriturePaieInternalView.as_view(), name="internal-ecritures-paie"),
    path("_internal/ecritures-achat/", EcritureAchatInternalView.as_view(), name="internal-ecritures-achat"),
]

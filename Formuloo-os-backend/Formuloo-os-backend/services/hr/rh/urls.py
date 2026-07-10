"""
URLs du module RH — Formuloo OS
Conforme ADR-003 : versionnement /api/v1/
Conforme CDC Formuloo OS v1.0
"""

from django.urls import path

from rh.views import (
    AttestationTravailView,
    PayrollPDFView,
    CongeApprouverView,
    CongeDetailView,
    CongeRejeterView,
    # Congé
    CongesListView,
    ContratDetailView,
    # Contrat
    ContratsListView,
    # Déclarations
    DeclarationCNPSView,
    DeclarationIRPPView,
    DemandeDocumentApprouverView,
    DemandeDocumentRejeterView,
    DemandesDocumentRHView,
    DepartementDetailView,
    # Département
    DepartementsListView,
    DepartementTreeView,
    EmployeCongesView,
    EmployeContratsView,
    EmployeDetailView,
    EmployePayslipsView,
    # Employé
    EmployesListView,
    MeCongesView,
    MeDemandeDocumentDetailView,
    # Demandes Documents
    MeDemandesDocumentView,
    MeFichesPaieView,
    MePresencesView,
    # Self-service Employé
    MeView,
    PayrollDetailView,
    # Fiche de Paie
    PayrollListView,
    PayrollPayerView,
    PayrollPeriodeView,
    PayrollRunView,
    PayrollValiderView,
    PosteDetailView,
    # Poste
    PostesListView,
    PresenceDetailView,
    # Présence
    PresencesListView,
    SoldeCongesDetailView,
    # Solde Congés
    SoldesCongesListView,
    # Statistiques
    StatsRHView,
)

urlpatterns = [
    # ── DÉPARTEMENTS ──────────────────────────────────────
    path("departements/", DepartementsListView.as_view(), name="departements-list"),
    path("departements/tree/", DepartementTreeView.as_view(), name="departements-tree"),
    path(
        "departements/<uuid:pk>/",
        DepartementDetailView.as_view(),
        name="departements-detail",
    ),
    # ── POSTES ────────────────────────────────────────────
    path("postes/", PostesListView.as_view(), name="postes-list"),
    path("postes/<uuid:pk>/", PosteDetailView.as_view(), name="postes-detail"),
    # ── EMPLOYÉS ──────────────────────────────────────────
    path("employes/", EmployesListView.as_view(), name="employes-list"),
    path("employes/<uuid:pk>/", EmployeDetailView.as_view(), name="employes-detail"),
    path(
        "employes/<uuid:pk>/contrats/",
        EmployeContratsView.as_view(),
        name="employes-contrats",
    ),
    path(
        "employes/<uuid:pk>/conges/",
        EmployeCongesView.as_view(),
        name="employes-conges",
    ),
    path(
        "employes/<uuid:pk>/payslips/",
        EmployePayslipsView.as_view(),
        name="employes-payslips",
    ),
    # ── CONTRATS ──────────────────────────────────────────
    path("contrats/", ContratsListView.as_view(), name="contrats-list"),
    path("contrats/<uuid:pk>/", ContratDetailView.as_view(), name="contrats-detail"),
    # ── CONGÉS ────────────────────────────────────────────
    path("leaves/", CongesListView.as_view(), name="leaves-list"),
    path("leaves/<uuid:pk>/", CongeDetailView.as_view(), name="leaves-detail"),
    path(
        "leaves/<uuid:pk>/approve/",
        CongeApprouverView.as_view(),
        name="leaves-approuver",
    ),
    path("leaves/<uuid:pk>/reject/", CongeRejeterView.as_view(), name="leaves-rejeter"),
    # ── SOLDES CONGÉS ─────────────────────────────────────
    path("soldes-conges/", SoldesCongesListView.as_view(), name="soldes-conges-list"),
    path(
        "soldes-conges/<uuid:pk>/",
        SoldeCongesDetailView.as_view(),
        name="soldes-conges-detail",
    ),
    # ── PRÉSENCES ─────────────────────────────────────────
    path("presences/", PresencesListView.as_view(), name="presences-list"),
    path("presences/<uuid:pk>/", PresenceDetailView.as_view(), name="presences-detail"),
    # ── FICHES DE PAIE ────────────────────────────────────
    path("payroll/", PayrollListView.as_view(), name="payroll-list"),
    path("payroll/run/", PayrollRunView.as_view(), name="payroll-run"),
    path(
        "payroll/periode/<str:periode>/",
        PayrollPeriodeView.as_view(),
        name="payroll-periode",
    ),
    path("payroll/<uuid:pk>/", PayrollDetailView.as_view(), name="payroll-detail"),
    path(
        "payroll/<uuid:pk>/valider/",
        PayrollValiderView.as_view(),
        name="payroll-valider",
    ),
    path("payroll/<uuid:pk>/payer/", PayrollPayerView.as_view(), name="payroll-payer"),
    path("payroll/<uuid:pk>/pdf/", PayrollPDFView.as_view(), name="payroll-pdf"),
    # ── SELF-SERVICE EMPLOYÉ ──────────────────────────────
    path("me/", MeView.as_view(), name="hr-me"),
    path("me/conges/", MeCongesView.as_view(), name="hr-me-conges"),
    path("me/fiches-paie/", MeFichesPaieView.as_view(), name="hr-me-fiches-paie"),
    path("me/presences/", MePresencesView.as_view(), name="hr-me-presences"),
    # ── DÉCLARATIONS RÉGLEMENTAIRES ───────────────────────
    path(
        "declarations/cnps/",
        DeclarationCNPSView.as_view(),
        name="declarations-cnps",
    ),
    path(
        "declarations/irpp/",
        DeclarationIRPPView.as_view(),
        name="declarations-irpp",
    ),
    path(
        "employes/<uuid:pk>/attestation/",
        AttestationTravailView.as_view(),
        name="employes-attestation",
    ),
    # ── STATISTIQUES RH ───────────────────────────────────
    path("stats/", StatsRHView.as_view(), name="hr-stats"),
    # ── DEMANDES DOCUMENTS — SELF-SERVICE ─────────────────
    path(
        "me/demandes-document/",
        MeDemandesDocumentView.as_view(),
        name="me-demandes-document-list",
    ),
    path(
        "me/demandes-document/<uuid:pk>/",
        MeDemandeDocumentDetailView.as_view(),
        name="me-demandes-document-detail",
    ),
    # ── DEMANDES DOCUMENTS — RH ───────────────────────────
    path(
        "demandes-document/",
        DemandesDocumentRHView.as_view(),
        name="demandes-document-list",
    ),
    path(
        "demandes-document/<uuid:pk>/approuver/",
        DemandeDocumentApprouverView.as_view(),
        name="demandes-document-approuver",
    ),
    path(
        "demandes-document/<uuid:pk>/rejeter/",
        DemandeDocumentRejeterView.as_view(),
        name="demandes-document-rejeter",
    ),
]

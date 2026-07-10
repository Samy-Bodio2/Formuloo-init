"""
Views du module RH — Formuloo OS
Point d'entrée de toutes les views.
"""

from .conge import (
    CongeApprouverView,
    CongeDetailView,
    CongeRejeterView,
    CongesListView,
)
from .contrat import (
    ContratDetailView,
    ContratsListView,
)
from .declaration import (
    AttestationTravailView,
    DeclarationCNPSView,
    DeclarationIRPPView,
)
from .demande_document import (
    DemandeDocumentApprouverView,
    DemandeDocumentRejeterView,
    DemandesDocumentRHView,
    MeDemandeDocumentDetailView,
    MeDemandesDocumentView,
)
from .departement import (
    DepartementDetailView,
    DepartementsListView,
    DepartementTreeView,
)
from .employe import (
    EmployeCongesView,
    EmployeContratsView,
    EmployeDetailView,
    EmployePayslipsView,
    EmployesListView,
)
from .fiche_paie import (
    PayrollDetailView,
    PayrollListView,
    PayrollPayerView,
    PayrollPeriodeView,
    PayrollRunView,
    PayrollValiderView,
)
from .fiche_paie_pdf import PayrollPDFView
from .me import (
    MeCongesView,
    MeFichesPaieView,
    MePresencesView,
    MeView,
)
from .poste import (
    PosteDetailView,
    PostesListView,
)
from .presence import (
    PresenceDetailView,
    PresencesListView,
)
from .solde_conges import (
    SoldeCongesDetailView,
    SoldesCongesListView,
)
from .stats import StatsRHView

__all__ = [
    # Département
    "DepartementsListView",
    "DepartementTreeView",
    "DepartementDetailView",
    # Poste
    "PostesListView",
    "PosteDetailView",
    # Employé
    "EmployesListView",
    "EmployeDetailView",
    "EmployeContratsView",
    "EmployeCongesView",
    "EmployePayslipsView",
    # Contrat
    "ContratsListView",
    "ContratDetailView",
    # Congé
    "CongesListView",
    "CongeDetailView",
    "CongeApprouverView",
    "CongeRejeterView",
    # Solde Congés
    "SoldesCongesListView",
    "SoldeCongesDetailView",
    # Présence
    "PresencesListView",
    "PresenceDetailView",
    # Fiche de Paie
    "PayrollListView",
    "PayrollRunView",
    "PayrollPeriodeView",
    "PayrollDetailView",
    "PayrollValiderView",
    "PayrollPayerView",
    "PayrollPDFView",
    # Self-service Employé
    "MeView",
    "MeCongesView",
    "MeFichesPaieView",
    "MePresencesView",
    # Déclarations
    "DeclarationCNPSView",
    "DeclarationIRPPView",
    "AttestationTravailView",
    # Demandes Documents
    "MeDemandesDocumentView",
    "MeDemandeDocumentDetailView",
    "DemandesDocumentRHView",
    "DemandeDocumentApprouverView",
    "DemandeDocumentRejeterView",
    # Stats
    "StatsRHView",
]

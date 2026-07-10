"""
Serializers du module RH — Formuloo OS
Point d'entrée de tous les serializers.
"""

from .conge import (
    CongeApprouverSerializer,
    CongeCreateSerializer,
    CongeRejeterSerializer,
    CongeSerializer,
)
from .contrat import (
    ContratBriefSerializer,
    ContratCreateSerializer,
    ContratSerializer,
)
from .demande_document import (
    DemandeDocumentCreateSerializer,
    DemandeDocumentSerializer,
    RejeterDemandeSerializer,
)
from .departement import (
    DepartementBriefSerializer,
    DepartementCreateSerializer,
    DepartementSerializer,
    DepartementTreeSerializer,
)
from .employe import (
    EmployeBriefSerializer,
    EmployeCreateSerializer,
    EmployeSerializer,
    EmployeUpdateSerializer,
)
from .fiche_paie import (
    FichePaieCreateSerializer,
    FichePaiePayerSerializer,
    FichePaieSerializer,
    FichePaieValiderSerializer,
    PayrollRunSerializer,
)
from .poste import (
    PosteBriefSerializer,
    PosteCreateSerializer,
    PosteSerializer,
)
from .presence import (
    PresenceCreateSerializer,
    PresenceSerializer,
)
from .solde_conges import (
    SoldeCongesSerializer,
)

__all__ = [
    # Département
    "DepartementBriefSerializer",
    "DepartementSerializer",
    "DepartementCreateSerializer",
    "DepartementTreeSerializer",
    # Poste
    "PosteBriefSerializer",
    "PosteSerializer",
    "PosteCreateSerializer",
    # Employé
    "EmployeBriefSerializer",
    "EmployeSerializer",
    "EmployeCreateSerializer",
    "EmployeUpdateSerializer",
    # Contrat
    "ContratBriefSerializer",
    "ContratSerializer",
    "ContratCreateSerializer",
    # Congé
    "CongeSerializer",
    "CongeCreateSerializer",
    "CongeApprouverSerializer",
    "CongeRejeterSerializer",
    # Solde Congés
    "SoldeCongesSerializer",
    # Présence
    "PresenceSerializer",
    "PresenceCreateSerializer",
    # Fiche de Paie
    "FichePaieSerializer",
    "FichePaieCreateSerializer",
    "FichePaieValiderSerializer",
    "FichePaiePayerSerializer",
    "PayrollRunSerializer",
    # Demande Document
    "DemandeDocumentSerializer",
    "DemandeDocumentCreateSerializer",
    "RejeterDemandeSerializer",
]

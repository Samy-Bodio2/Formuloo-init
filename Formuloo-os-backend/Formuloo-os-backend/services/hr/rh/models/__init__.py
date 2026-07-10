"""
Models du module RH — Formuloo OS
Point d'entrée de tous les modèles.
"""

from .conge import Conge
from .contrat import Contrat
from .demande_document import DemandeDocument
from .departement import Departement
from .employe import Employe
from .fiche_paie import FichePaie
from .poste import Poste
from .presence import Presence
from .solde_conges import SoldeConges

__all__ = [
    "Departement",
    "Poste",
    "Employe",
    "Contrat",
    "Conge",
    "SoldeConges",
    "Presence",
    "FichePaie",
    "DemandeDocument",
]

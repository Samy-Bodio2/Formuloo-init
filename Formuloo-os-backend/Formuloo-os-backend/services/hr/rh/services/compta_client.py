"""
ComptaServiceClient — Formuloo OS Service RH
Client HTTP vers le service Compta pour la génération automatique
des écritures comptables lors du paiement des salaires.

Écriture générée lors de la paie (OHADA SYSCOHADA) :
    6411 Charges de personnel (débit) : salaire brut
    4311 CNPS à reverser (crédit)     : cotisation CNPS
    4471 IRPP à reverser (crédit)     : impôt IRPP
    4211 Personnel, rémunérations dues (crédit) : salaire net

Débit = Crédit : brut = net + cnps + irpp ✓

Appel non-bloquant : si Compta est indisponible, la paie
est enregistrée quand même. journal_entry_id reste None.
"""

import logging

import requests
from django.conf import settings

logger = logging.getLogger(__name__)

COMPTA_SERVICE_URL = getattr(settings, "COMPTA_SERVICE_URL", "http://localhost:8002")
INTERNAL_SERVICE_TOKEN = getattr(settings, "INTERNAL_SERVICE_TOKEN", "formuloo-internal-secret-change-in-prod")

PAIE_ENDPOINT = f"{COMPTA_SERVICE_URL}/api/v1/compta/_internal/ecritures-paie/"


def creer_ecriture_paie(fiche_paie):
    """
    Crée l'écriture comptable de paie dans le service Compta.

    Args:
        fiche_paie: instance FichePaie avec statut PAYE

    Returns:
        str | None: UUID de l'écriture créée, ou None en cas d'échec
    """
    try:
        payload = {
            "tenant_id": str(fiche_paie.tenant_id),
            "fiche_paie_id": str(fiche_paie.id),
            "employe_nom": fiche_paie.employee.full_name,
            "periode": fiche_paie.period,
            "date_paiement": str(fiche_paie.paid_at.date()) if fiche_paie.paid_at else None,
            "salaire_brut": str(fiche_paie.gross),
            "salaire_net": str(fiche_paie.net_salary),
            "cotisation_cnps": str(fiche_paie.cotisation_cnps),
            "impot_irpp": str(fiche_paie.impot_irpp),
            "autres_deductions": str(
                fiche_paie.total_deductions - fiche_paie.cotisation_cnps - fiche_paie.impot_irpp
            ),
            "currency": fiche_paie.currency,
        }

        response = requests.post(
            PAIE_ENDPOINT,
            json=payload,
            headers={
                "Content-Type": "application/json",
                "X-Service-Token": INTERNAL_SERVICE_TOKEN,
                "X-Service": "hr",
            },
            timeout=5,
        )

        if response.status_code == 201:
            data = response.json()
            ecriture_id = data.get("ecriture_id")
            logger.info(f"Écriture paie créée: {ecriture_id} pour fiche {fiche_paie.id}")
            return ecriture_id
        else:
            logger.warning(
                f"Écriture paie non créée — Compta a retourné {response.status_code} "
                f"pour fiche {fiche_paie.id}: {response.text[:200]}"
            )
            return None

    except requests.exceptions.Timeout:
        logger.warning(f"Écriture paie timeout — Compta indisponible pour fiche {fiche_paie.id}")
        return None

    except requests.exceptions.ConnectionError:
        logger.error(f"Écriture paie impossible — Connexion Compta échouée pour fiche {fiche_paie.id}")
        return None

    except Exception as e:
        logger.error(f"Écriture paie erreur inattendue — {str(e)} pour fiche {fiche_paie.id}")
        return None

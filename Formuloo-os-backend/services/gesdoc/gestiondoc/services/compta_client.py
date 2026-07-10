"""
ComptaServiceClient — Formuloo OS Service GesDoc
Client HTTP vers le service Compta pour la création automatique de
l'écriture comptable lors de la certification blockchain d'une pièce
d'achat — même pattern que rh.services.compta_client pour la paie.

Appel non-bloquant : si Compta est indisponible ou si des comptes du
plan comptable sont manquants, le document reste certifié quand même —
journal_entry_id reste None et POST /link-journal-entry/ permet un
rattachement manuel ultérieur (voir Acteur "Comptable" du contrat).
"""

import logging

import requests
from django.conf import settings

logger = logging.getLogger(__name__)

COMPTA_SERVICE_URL = getattr(settings, "COMPTA_SERVICE_URL", "http://localhost:8002")
INTERNAL_SERVICE_TOKEN = getattr(
    settings, "INTERNAL_SERVICE_TOKEN", "formuloo-internal-secret-change-in-prod",
)

ACHAT_ENDPOINT = f"{COMPTA_SERVICE_URL}/api/v1/compta/_internal/ecritures-achat/"


def creer_ecriture_achat(document):
    """
    Crée l'écriture comptable OHADA dans Compta pour un document certifié.

    Args:
        document: instance AccountingDocument avec statut CERTIFIED

    Returns:
        str | None: UUID de l'écriture créée, ou None en cas d'échec
    """
    try:
        payload = {
            "tenant_id": str(document.tenant_id),
            "document_id": str(document.id),
            "document_number": document.document_number,
            "document_type": document.document_type,
            "supplier": document.supplier,
            "date": str(document.invoice_date) if document.invoice_date else None,
            "amount_ht": str(document.amount_ht) if document.amount_ht is not None else "0",
            "tva_rate": str(document.tva_rate) if document.tva_rate is not None else "0",
            "amount_ttc": str(document.amount_ttc) if document.amount_ttc is not None else "0",
            "currency": document.currency,
        }

        response = requests.post(
            ACHAT_ENDPOINT,
            json=payload,
            headers={
                "Content-Type": "application/json",
                "X-Service-Token": INTERNAL_SERVICE_TOKEN,
                "X-Service": "gesdoc",
            },
            timeout=5,
        )

        if response.status_code == 201:
            data = response.json()
            ecriture_id = data.get("ecriture_id")
            logger.info(f"Écriture achat créée: {ecriture_id} pour document {document.id}")
            return ecriture_id
        else:
            logger.warning(
                f"Écriture achat non créée — Compta a retourné {response.status_code} "
                f"pour document {document.id}: {response.text[:200]}"
            )
            return None

    except requests.exceptions.Timeout:
        logger.warning(f"Écriture achat timeout — Compta indisponible pour document {document.id}")
        return None

    except requests.exceptions.ConnectionError:
        logger.error(f"Écriture achat impossible — Connexion Compta échouée pour document {document.id}")
        return None

    except Exception as e:
        logger.error(f"Écriture achat erreur inattendue — {str(e)} pour document {document.id}")
        return None

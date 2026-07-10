"""
Suggestion d'écriture SYSCOHADA — Formuloo OS Service GesDoc

GesDoc n'a pas de visibilité directe sur le plan de comptes ou les
journaux du module Compta (bases de données séparées, architecture
microservices) : ce module calcule une suggestion d'écriture à partir
des comptes OHADA standards, que le comptable valide/ajuste dans
Compta via le formulaire pré-rempli (POST /api/v1/compta/ecritures/).
"""

from decimal import Decimal, ROUND_HALF_UP

CHARGE_ACCOUNTS = {
    "invoice": ("6011", "Achats de matières et fournitures"),
    "purchase_order": ("6011", "Achats de matières et fournitures"),
    "receipt": ("6068", "Autres achats non stockés"),
}

TVA_DEDUCTIBLE_ACCOUNT = ("4451", "TVA déductible sur biens et services")
FOURNISSEURS_ACCOUNT = ("4011", "Fournisseurs")


def _quantize(amount: Decimal) -> Decimal:
    return amount.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def suggest_journal_entry(document) -> dict:
    """
    Construit la suggestion d'écriture SYSCOHADA pour un AccountingDocument
    certifié. Retourne le dict au format `SuggestedJournalEntry`.
    """
    amount_ht = document.amount_ht or Decimal("0")
    amount_ttc = document.amount_ttc or Decimal("0")
    tva_amount = _quantize(amount_ttc - amount_ht)

    charge_code, charge_label = CHARGE_ACCOUNTS.get(
        document.document_type, CHARGE_ACCOUNTS["invoice"]
    )

    lines = [
        {
            "account_code": charge_code,
            "label": charge_label,
            "debit": str(_quantize(amount_ht)),
            "credit": "0.00",
        },
    ]
    if tva_amount > 0:
        lines.append({
            "account_code": TVA_DEDUCTIBLE_ACCOUNT[0],
            "label": TVA_DEDUCTIBLE_ACCOUNT[1],
            "debit": str(tva_amount),
            "credit": "0.00",
        })
    lines.append({
        "account_code": FOURNISSEURS_ACCOUNT[0],
        "label": FOURNISSEURS_ACCOUNT[1],
        "debit": "0.00",
        "credit": str(_quantize(amount_ttc)),
    })

    description = f"{document.get_document_type_display()} {document.supplier}".strip()
    if document.document_number:
        description = f"{description} — {document.document_number}"

    return {
        "date": document.invoice_date.isoformat() if document.invoice_date else None,
        "reference": document.document_number,
        "description": description,
        "journal_id": None,
        "journal_type": "ACHATS",
        "lines": lines,
    }


def suggestion_confidence(document) -> int:
    """Confiance de la suggestion = confiance OCR globale du document."""
    return document.confidence or 0

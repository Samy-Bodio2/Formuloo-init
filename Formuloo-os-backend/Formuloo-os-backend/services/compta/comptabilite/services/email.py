"""
Service email pour le module Compta — Formuloo OS.

Cas d'usage :
- Envoi de la facture émise au client
- Relance de facture impayée (future)

Utilise le backend email Django configuré dans settings.
"""

import logging
from django.core.mail import send_mail
from django.conf import settings

logger = logging.getLogger("comptabilite")


def _send(subject: str, message: str, recipient: str) -> bool:
    """Envoie un email et retourne True si réussi."""
    if not recipient:
        logger.warning("Email non envoyé : adresse client vide.")
        return False
    try:
        send_mail(
            subject=subject,
            message=message,
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[recipient],
            fail_silently=False,
        )
        logger.info("Email facture envoyé à %s : %s", recipient, subject)
        return True
    except Exception as exc:
        logger.error("Échec envoi email facture à %s : %s", recipient, exc)
        return False


def envoyer_facture_client(facture) -> bool:
    """
    Envoie une notification email au client lors de l'émission de la facture.

    Args:
        facture: instance Facture (statut EMISE, client_email renseigné)

    Returns:
        True si l'email a été envoyé, False sinon (adresse vide ou erreur SMTP)
    """
    if not facture.client_email:
        logger.info(
            "Facture %s : pas d'email client — notification non envoyée.", facture.numero
        )
        return False

    subject = f"Formuloo — Facture {facture.numero}"
    echeance = facture.date_echeance.strftime("%d/%m/%Y") if facture.date_echeance else "—"
    message = (
        f"Bonjour {facture.client_nom},\n\n"
        f"Veuillez trouver ci-après le récapitulatif de votre facture :\n\n"
        f"  Numéro      : {facture.numero}\n"
        f"  Date        : {facture.date_emission.strftime('%d/%m/%Y')}\n"
        f"  Montant HT  : {facture.montant_ht:,.2f} {facture.devise}\n"
        f"  TVA         : {facture.tva:,.2f} {facture.devise}\n"
        f"  Montant TTC : {facture.montant_ttc:,.2f} {facture.devise}\n"
        f"  Échéance    : {echeance}\n\n"
        f"Pour toute question, contactez notre service comptabilité.\n\n"
        f"Cordialement,\n"
        f"L'équipe Formuloo"
    )
    return _send(subject, message, facture.client_email)

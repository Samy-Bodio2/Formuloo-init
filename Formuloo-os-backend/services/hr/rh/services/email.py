"""
Notifications email pour le module RH — Formuloo OS.

Deux cas d'usage :
1. Congé approuvé / rejeté → email à l'employé
2. Fiche de paie validée   → email à l'employé (bulletin disponible)

Utilise le backend email Django configuré dans settings.
En dev (DEBUG=True), le backend console est recommandé.
En prod, configurer SMTP via variables d'env EMAIL_HOST, etc.
"""

import logging
from django.core.mail import send_mail
from django.conf import settings

logger = logging.getLogger("rh")


def _send(subject: str, message: str, recipient: str) -> bool:
    """Envoie un email et retourne True si envoi réussi."""
    if not recipient:
        logger.warning("Email non envoyé : destinataire vide.")
        return False
    try:
        send_mail(
            subject=subject,
            message=message,
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=[recipient],
            fail_silently=False,
        )
        logger.info("Email envoyé à %s : %s", recipient, subject)
        return True
    except Exception as exc:
        logger.error("Échec envoi email à %s : %s", recipient, exc)
        return False


def notifier_conge_approuve(employe, conge) -> bool:
    """
    Notifie l'employé que sa demande de congé a été approuvée.
    """
    subject = f"Formuloo RH — Congé approuvé du {conge.start_date} au {conge.end_date}"
    message = (
        f"Bonjour {employe.first_name},\n\n"
        f"Votre demande de congé du {conge.start_date} au {conge.end_date} "
        f"({conge.days} jours) a été approuvée.\n\n"
        f"Type : {conge.type_conge}\n\n"
        f"Bonne retraite !\n\n"
        f"— L'équipe RH Formuloo"
    )
    return _send(subject, message, employe.email)


def notifier_conge_rejete(employe, conge) -> bool:
    """
    Notifie l'employé que sa demande de congé a été rejetée.
    """
    subject = f"Formuloo RH — Congé refusé du {conge.start_date} au {conge.end_date}"
    message = (
        f"Bonjour {employe.first_name},\n\n"
        f"Votre demande de congé du {conge.start_date} au {conge.end_date} "
        f"a été refusée.\n\n"
        f"Motif du refus : {conge.commentaire_decision or 'Non communiqué'}\n\n"
        f"Veuillez contacter votre responsable RH pour plus d'informations.\n\n"
        f"— L'équipe RH Formuloo"
    )
    return _send(subject, message, employe.email)


def notifier_paie_prete(employe, fiche_paie) -> bool:
    """
    Notifie l'employé que sa fiche de paie est disponible.
    """
    subject = f"Formuloo RH — Bulletin de paie {fiche_paie.mois:02d}/{fiche_paie.annee} disponible"
    message = (
        f"Bonjour {employe.first_name},\n\n"
        f"Votre bulletin de paie pour {fiche_paie.mois:02d}/{fiche_paie.annee} est disponible.\n\n"
        f"Salaire brut  : {fiche_paie.gross:,.0f} {fiche_paie.currency}\n"
        f"Salaire net   : {fiche_paie.net_salary:,.0f} {fiche_paie.currency}\n\n"
        f"Connectez-vous à votre espace RH pour consulter et télécharger votre bulletin.\n\n"
        f"— L'équipe RH Formuloo"
    )
    return _send(subject, message, employe.email)

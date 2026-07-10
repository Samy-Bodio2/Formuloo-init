"""
Gestion des erreurs globale — Formuloo OS Service RH
Handler central qui garantit que TOUTES les erreurs
retournent un format JSON uniforme.

Sans ce handler :
→ Erreurs Django  = HTML (page d'erreur)
→ Erreurs DRF     = JSON mais format variable
→ Erreurs 500     = HTML

Avec ce handler :
→ Toutes les erreurs = JSON uniforme
→ Frontend sait toujours quoi afficher

Format garanti :
{
  "error": {
    "code":    "VALIDATION_ERROR",
    "message": "Description lisible",
    "details": { ... }  ← optionnel
  }
}

Codes d'erreur :
    VALIDATION_ERROR   → 400 données invalides
    AUTHENTICATION     → 401 non authentifié
    FORBIDDEN          → 403 accès refusé
    NOT_FOUND          → 404 ressource introuvable
    METHOD_NOT_ALLOWED → 405 méthode non autorisée
    CONFLICT           → 409 conflit de données
    INTERNAL_ERROR     → 500 erreur serveur

Configuration dans settings.py :
    REST_FRAMEWORK = {
        'EXCEPTION_HANDLER': 'rh.exceptions.custom_exception_handler'
    }
"""

import logging

from rest_framework import status
from rest_framework.exceptions import (
    AuthenticationFailed,
    MethodNotAllowed,
    NotAuthenticated,
    NotFound,
    PermissionDenied,
    ValidationError,
)
from rest_framework.response import Response
from rest_framework.views import exception_handler

# Logger pour les erreurs serveur
logger = logging.getLogger(__name__)


def _format_details(detail):
    """
    Convertit le format d'erreur DRF (dict ou liste) en liste uniforme
    [{field, message}] attendue par le client mobile Kotlin.
    """
    if isinstance(detail, str):
        return [{"field": "non_field_errors", "message": detail}]
    if isinstance(detail, list):
        return [{"field": "non_field_errors", "message": str(m)} for m in detail]
    if isinstance(detail, dict):
        result = []
        for field, messages in detail.items():
            if isinstance(messages, list):
                for msg in messages:
                    result.append({"field": field, "message": str(msg)})
            else:
                result.append({"field": field, "message": str(messages)})
        return result
    return [{"field": "error", "message": str(detail)}]


def _user_message(detail):
    """
    Extrait le premier message lisible de l'erreur DRF.
    Évite d'afficher le générique 'Données invalides.'
    quand on a déjà un message métier explicite.
    """
    if isinstance(detail, str):
        return detail
    if isinstance(detail, list) and detail:
        return str(detail[0])
    if isinstance(detail, dict):
        # Priorité : non_field_errors, puis premier champ
        for key in ("non_field_errors", *detail.keys()):
            if key in detail:
                v = detail[key]
                if isinstance(v, list) and v:
                    return str(v[0])
                return str(v)
    return "Données invalides."


def custom_exception_handler(exc, context):
    """
    Handler global des exceptions pour Formuloo OS.

    Intercepte TOUTES les exceptions et les
    convertit en réponse JSON uniforme.

    Appelé automatiquement par DRF quand
    une exception est levée dans une view.

    Args:
        exc     : l'exception levée
        context : contexte Django (request, view...)

    Returns:
        Response : réponse JSON avec format uniforme
    """

    # Appeler d'abord le handler DRF par défaut
    # pour gérer les exceptions DRF standard
    response = exception_handler(exc, context)

    # ── Erreur 400 — Validation ───────────────────────────
    if isinstance(exc, ValidationError):
        return Response(
            {
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": _user_message(exc.detail),
                    "details": _format_details(exc.detail),
                }
            },
            status=status.HTTP_400_BAD_REQUEST,
        )

    # ── Erreur 401 — Non authentifié ──────────────────────
    if isinstance(exc, (NotAuthenticated, AuthenticationFailed)):
        return Response(
            {
                "error": {
                    "code": "AUTHENTICATION",
                    "message": "Token JWT absent, " "invalide ou expiré.",
                }
            },
            status=status.HTTP_401_UNAUTHORIZED,
        )

    # ── Erreur 403 — Accès refusé ─────────────────────────
    if isinstance(exc, PermissionDenied):
        return Response(
            {
                "error": {
                    "code": "FORBIDDEN",
                    "message": (
                        exc.detail if hasattr(exc, "detail") else "Accès refusé."
                    ),
                }
            },
            status=status.HTTP_403_FORBIDDEN,
        )

    # ── Erreur 404 — Ressource introuvable ────────────────
    if isinstance(exc, NotFound):
        return Response(
            {"error": {"code": "NOT_FOUND", "message": "Ressource introuvable."}},
            status=status.HTTP_404_NOT_FOUND,
        )

    # ── Erreur 405 — Méthode non autorisée ───────────────
    if isinstance(exc, MethodNotAllowed):
        return Response(
            {
                "error": {
                    "code": "METHOD_NOT_ALLOWED",
                    "message": f"Méthode {exc.args[0]} " f"non autorisée.",
                }
            },
            status=status.HTTP_405_METHOD_NOT_ALLOWED,
        )

    # ── Erreur 500 — Erreur serveur ───────────────────────
    # Toute exception non gérée ci-dessus
    if response is None:
        # Logger l'erreur pour le monitoring
        logger.error(
            f"Erreur serveur non gérée : " f"{exc.__class__.__name__} — {str(exc)}",
            exc_info=True,
        )
        return Response(
            {
                "error": {
                    "code": "INTERNAL_ERROR",
                    "message": "Une erreur interne "
                    "est survenue. "
                    "Veuillez réessayer "
                    "ou contacter le support.",
                }
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    return response


# ── Exceptions métier personnalisées ─────────────────────
# Levées dans les serializers et views
# pour des cas métier spécifiques à Formuloo OS


class ConflitCongeException(Exception):
    """
    Levée quand un congé chevauche un congé existant.

    Ex:
        Congé existant : 01/07 → 15/07
        Nouveau congé  : 10/07 → 20/07
        → ConflitCongeException levée
    """

    pass


class ConflitPresenceCongeException(Exception):
    """
    Levée quand on crée une présence
    pour un employé en congé approuvé.

    Ex:
        Congé approuvé : 01/07 → 15/07
        Présence       : 05/07
        → ConflitPresenceCongeException levée
    """

    pass


class ContratActifExistantException(Exception):
    """
    Levée quand on crée un contrat actif
    alors qu'un contrat actif existe déjà.

    Un employé ne peut avoir qu'un seul
    contrat actif à la fois.

    Ex:
        Contrat CDI   actif → existe déjà
        Nouveau Stage actif → impossible
        → ContratActifExistantException levée
    """

    pass


class SalaireMinimuException(Exception):
    """
    Levée quand le salaire est inférieur
    au SMIG camerounais.

    SMIG Cameroun : 36 270 XAF/mois
    (Décret n°2014/2217/PM du 24 juillet 2014)

    Ex:
        salaire_base = 30 000 XAF
        → SalaireMinimuException levée
    """

    pass

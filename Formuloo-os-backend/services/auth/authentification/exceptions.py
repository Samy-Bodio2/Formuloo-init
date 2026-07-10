"""
Gestion des erreurs globale — Formuloo OS Service Auth
Format JSON uniforme pour toutes les erreurs.

Format garanti :
{
  "error": {
    "code":    "VALIDATION_ERROR",
    "message": "Description lisible",
    "details": { ... }  ← optionnel
  }
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

logger = logging.getLogger(__name__)


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if isinstance(exc, ValidationError):
        return Response(
            {
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": "Données invalides.",
                    "details": exc.detail,
                }
            },
            status=status.HTTP_400_BAD_REQUEST,
        )

    if isinstance(exc, (NotAuthenticated, AuthenticationFailed)):
        return Response(
            {
                "error": {
                    "code": "AUTHENTICATION",
                    "message": "Token JWT absent, invalide ou expiré.",
                }
            },
            status=status.HTTP_401_UNAUTHORIZED,
        )

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

    if isinstance(exc, NotFound):
        return Response(
            {"error": {"code": "NOT_FOUND", "message": "Ressource introuvable."}},
            status=status.HTTP_404_NOT_FOUND,
        )

    if isinstance(exc, MethodNotAllowed):
        return Response(
            {
                "error": {
                    "code": "METHOD_NOT_ALLOWED",
                    "message": f"Méthode {exc.args[0]} non autorisée.",
                }
            },
            status=status.HTTP_405_METHOD_NOT_ALLOWED,
        )

    if response is None:
        logger.error(
            f"Erreur serveur non gérée : {exc.__class__.__name__} — {str(exc)}",
            exc_info=True,
        )
        return Response(
            {
                "error": {
                    "code": "INTERNAL_ERROR",
                    "message": "Une erreur interne est survenue.",
                }
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    return response

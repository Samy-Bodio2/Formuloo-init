"""
Permissions RBAC — Formuloo OS Service GesDoc

Basé sur les permissions granulaires du JWT (pas les noms de rôles).
Format : module.action.resource — ex. gesdoc.read.documents.

Matrice par défaut (cohérente avec Compta/HR) :
    SUPER_ADMIN / ADMIN_PME → toutes les permissions gesdoc
    COMPTABLE               → read/write documents (pas l'audit log)
    ADMIN                   → read.audit en plus
    EMPLOYE                 → aucune permission gesdoc
"""

from rest_framework.permissions import BasePermission


def _has_permission(request, permission_code):
    return permission_code in getattr(request.user, "permissions", [])


def GesdocPermission(required_permission):
    """Factory : crée une classe DRF Permission pour une permission donnée."""

    class _GesdocPermission(BasePermission):
        message = {
            "error": {
                "code": "FORBIDDEN",
                "message": f"Permission requise : {required_permission}",
            }
        }

        def has_permission(self, request, view):
            return _has_permission(request, required_permission)

    _GesdocPermission.__name__ = f"Requires_{required_permission.replace('.', '_')}"
    return _GesdocPermission


# ── Permissions prédéfinies ───────────────────────────────

CanReadDocuments = GesdocPermission("gesdoc.read.documents")
CanWriteDocuments = GesdocPermission("gesdoc.write.documents")
CanReadAudit = GesdocPermission("gesdoc.read.audit")

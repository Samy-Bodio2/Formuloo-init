"""
Permissions RBAC — Formuloo OS Service Compta

Basé sur les permissions granulaires du JWT (pas les noms de rôles).
Cela permet aux rôles custom (définis par chaque PME dans Auth)
de fonctionner sans aucune modification du service Compta.

Format des permissions : module.action.resource
Exemples : compta.read.ecritures, compta.validate.ecritures

Matrice par défaut (configurée dans Auth migration 0006) :
    SUPER_ADMIN / ADMIN_PME → toutes les permissions compta
    COMPTABLE               → read/write/validate (pas close exercice)
    RH_MANAGER              → compta.read.ecritures + compta.read.etats
    EMPLOYE                 → aucune permission compta

Pour un rôle custom : l'organisation lui assigne les permissions
souhaitées dans le service Auth → propagées dans le JWT.
"""

from rest_framework.permissions import BasePermission


def _has_permission(request, permission_code):
    return permission_code in getattr(request.user, "permissions", [])


def ComptaPermission(required_permission):
    """Factory : crée une classe DRF Permission pour une permission donnée."""

    class _ComptaPermission(BasePermission):
        message = {
            "error": {
                "code": "FORBIDDEN",
                "message": f"Permission requise : {required_permission}",
            }
        }

        def has_permission(self, request, view):
            return _has_permission(request, required_permission)

    _ComptaPermission.__name__ = f"Requires_{required_permission.replace('.', '_')}"
    return _ComptaPermission


# ── Permissions prédéfinies ───────────────────────────────

CanReadExercices = ComptaPermission("compta.read.exercices")
CanWriteExercices = ComptaPermission("compta.write.exercices")
CanCloseExercices = ComptaPermission("compta.close.exercices")

CanReadComptes = ComptaPermission("compta.read.comptes")
CanWriteComptes = ComptaPermission("compta.write.comptes")
CanDeleteComptes = ComptaPermission("compta.delete.comptes")

CanReadJournaux = ComptaPermission("compta.read.journaux")
CanWriteJournaux = ComptaPermission("compta.write.journaux")

CanReadEcritures = ComptaPermission("compta.read.ecritures")
CanWriteEcritures = ComptaPermission("compta.write.ecritures")
CanDeleteEcritures = ComptaPermission("compta.delete.ecritures")
CanValidateEcritures = ComptaPermission("compta.validate.ecritures")

CanReadFactures = ComptaPermission("compta.read.factures")
CanWriteFactures = ComptaPermission("compta.write.factures")
CanDeleteFactures = ComptaPermission("compta.delete.factures")

CanReadPaiements = ComptaPermission("compta.read.paiements")
CanWritePaiements = ComptaPermission("compta.write.paiements")

CanReadEtats = ComptaPermission("compta.read.etats")

CanInitPlan = ComptaPermission("compta.init.plan")

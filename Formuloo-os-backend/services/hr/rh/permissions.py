"""
Permissions — Formuloo OS Service RH
Contrôle d'accès basé sur les rôles (RBAC).

Les rôles sont définis dans le service Auth
et transmis via le JWT à chaque requête.
Le middleware TenantMiddleware les extrait
et les injecte dans request.user.roles.

Rôles système Formuloo OS :
    RH_MANAGER  → accès total au module RH
                  CRUD employés, congés, paie...
    MANAGER     → accès à son équipe uniquement
                  voir employés, approuver congés
    EMPLOYE     → accès à ses propres données
                  soumettre congés, voir ses fiches
    COMPTABLE   → accès aux fiches de paie
                  valider et payer les salaires

Rôles custom (par PME) :
    Définis par chaque PME dans le service Auth
    Mappés vers les rôles système ci-dessus
    Ex: ASSOCIE_PRINCIPAL → [RH_MANAGER, COMPTABLE]
        MEDECIN_CHEF      → [MANAGER]
        STAGIAIRE_COMPTA  → [EMPLOYE]

Matrice des permissions :
    DÉPARTEMENTS :
    GET    /departements/        → IsManagerOrRH
    POST   /departements/        → IsRHManager
    GET    /departements/{id}/   → IsManagerOrRH
    PUT    /departements/{id}/   → IsRHManager
    DELETE /departements/{id}/   → IsRHManager
    GET    /departements/tree/   → IsManagerOrRH

    POSTES :
    GET    /postes/              → IsManagerOrRH
    POST   /postes/              → IsRHManager
    GET    /postes/{id}/         → IsManagerOrRH
    PUT    /postes/{id}/         → IsRHManager
    DELETE /postes/{id}/         → IsRHManager

    EMPLOYÉS :
    GET    /employes/            → IsManagerOrRH
    POST   /employes/            → IsRHManager
    GET    /employes/{id}/       → IsOwnerOrRH
    PATCH  /employes/{id}/       → IsRHManager
    DELETE /employes/{id}/       → IsRHManager
    GET    /employes/{id}/contrats/  → IsOwnerOrRH
    GET    /employes/{id}/conges/    → IsOwnerOrRH
    GET    /employes/{id}/payslips/  → IsComptableOrRH

    CONTRATS :
    GET    /contrats/            → IsManagerOrRH
    POST   /contrats/            → IsRHManager
    GET    /contrats/{id}/       → IsOwnerOrRH
    PUT    /contrats/{id}/       → IsRHManager
    DELETE /contrats/{id}/       → IsRHManager

    CONGÉS :
    GET    /leaves/              → IsManagerOrRH
    POST   /leaves/              → IsEmployeOrRH
    GET    /leaves/{id}/         → IsOwnerOrRH
    PUT    /leaves/{id}/         → IsOwnerOrRH
    DELETE /leaves/{id}/         → IsOwnerOrRH
    POST   /leaves/{id}/approve/ → IsManagerOrRH
    POST   /leaves/{id}/reject/  → IsManagerOrRH

    SOLDES CONGÉS :
    GET    /soldes-conges/       → IsManagerOrRH
    GET    /soldes-conges/{id}/  → IsOwnerOrRH

    PRÉSENCES :
    GET    /presences/           → IsManagerOrRH
    POST   /presences/           → IsRHManager
    GET    /presences/{id}/      → IsOwnerOrRH
    PUT    /presences/{id}/      → IsRHManager
    DELETE /presences/{id}/      → IsRHManager

    FICHES DE PAIE :
    GET    /payroll/             → IsComptableOrRH
    POST   /payroll/             → IsComptableOrRH
    GET    /payroll/{id}/        → IsOwnerOrRH
    PUT    /payroll/{id}/        → IsComptableOrRH
    DELETE /payroll/{id}/        → IsComptableOrRH
    POST   /payroll/run/         → IsComptableOrRH
    GET    /payroll/periode/{p}/ → IsComptableOrRH
    POST   /payroll/{id}/valider/→ IsComptableOrRH
    POST   /payroll/{id}/payer/  → IsComptableOrRH

Utilisation dans les views :
    from rest_framework.permissions import IsAuthenticated
    from rh.permissions import IsRHManager

    class MaView(APIView):
        permission_classes = [IsAuthenticated, IsRHManager]
"""

from rest_framework.permissions import BasePermission

# ── Constantes des rôles système ──────────────────────────
# Définis dans le service Auth
# Doivent correspondre EXACTEMENT aux valeurs du JWT
# Toute modification doit être coordonnée avec Auth
ROLE_RH_MANAGER = "RH_MANAGER"
ROLE_MANAGER = "MANAGER"
ROLE_EMPLOYE = "EMPLOYE"
ROLE_COMPTABLE = "COMPTABLE"


def get_user_roles(request):
    """
    Retourne la liste des rôles système
    de l'utilisateur connecté.

    Les rôles sont extraits du JWT par
    TenantMiddleware et stockés dans
    request.user.roles.

    Retourne une liste vide si roles absent
    pour éviter les AttributeError.

    Args:
        request: la requête HTTP Django

    Returns:
        list: liste des rôles système
              Ex: ['RH_MANAGER', 'COMPTABLE']
    """
    return getattr(request.user, "roles", [])


class IsRHManager(BasePermission):
    """
    Accès réservé au RH Manager uniquement.

    Utilisé pour les actions sensibles :
    → Créer / modifier / supprimer des employés
    → Approuver / rejeter des congés
    → Valider des fiches de paie
    → Gérer les départements et postes
    → Lancer la génération de paie en masse
    """

    message = {
        "error": {"code": "FORBIDDEN", "message": "Accès réservé au RH Manager."}
    }

    def has_permission(self, request, view):
        """
        Vérifie que l'utilisateur a le rôle RH_MANAGER.

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si RH_MANAGER, False sinon
        """
        roles = get_user_roles(request)
        return ROLE_RH_MANAGER in roles


class IsManagerOrRH(BasePermission):
    """
    Accès pour Manager ou RH Manager.

    Utilisé pour :
    → Voir la liste des employés de son équipe
    → Approuver les congés de son équipe
    → Voir les présences de son équipe
    → Consulter les contrats
    → Voir les départements et postes
    """

    message = {
        "error": {
            "code": "FORBIDDEN",
            "message": "Accès réservé au Manager " "ou RH Manager.",
        }
    }

    def has_permission(self, request, view):
        """
        Vérifie que l'utilisateur a le rôle
        MANAGER ou RH_MANAGER.

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si MANAGER ou RH_MANAGER
        """
        roles = get_user_roles(request)
        return any(r in roles for r in [ROLE_RH_MANAGER, ROLE_MANAGER])


class IsComptableOrRH(BasePermission):
    """
    Accès pour Comptable ou RH Manager.

    Utilisé pour :
    → Voir les fiches de paie
    → Générer les fiches de paie
    → Valider les fiches de paie
    → Marquer les fiches comme payées
    → Lancer la génération de paie en masse
    → Consulter les rapports financiers RH
    """

    message = {
        "error": {
            "code": "FORBIDDEN",
            "message": "Accès réservé au Comptable " "ou RH Manager.",
        }
    }

    def has_permission(self, request, view):
        """
        Vérifie que l'utilisateur a le rôle
        COMPTABLE ou RH_MANAGER.

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si COMPTABLE ou RH_MANAGER
        """
        roles = get_user_roles(request)
        return any(r in roles for r in [ROLE_RH_MANAGER, ROLE_COMPTABLE])


class IsEmployeOrRH(BasePermission):
    """
    Accès pour tout employé authentifié
    ou RH Manager.

    Utilisé pour :
    → Soumettre une demande de congé
    → Voir son propre solde de congés
    → Voir ses propres présences
    → Voir ses propres fiches de paie
    """

    message = {"error": {"code": "FORBIDDEN", "message": "Accès non autorisé."}}

    def has_permission(self, request, view):
        """
        Vérifie que l'utilisateur a au moins
        un rôle valide dans Formuloo OS.

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si au moins un rôle valide
        """
        roles = get_user_roles(request)
        return any(
            r in roles
            for r in [ROLE_RH_MANAGER, ROLE_MANAGER, ROLE_EMPLOYE, ROLE_COMPTABLE]
        )


class IsOwnerOrRH(BasePermission):
    """
    Accès à ses propres données ou RH Manager.

    Vérifie deux niveaux :
    1. has_permission       : utilisateur authentifié
                              avec un rôle valide
    2. has_object_permission: propriétaire de la
                              ressource ou RH Manager

    Utilisé pour :
    → Voir sa propre fiche employé
    → Voir ses propres congés
    → Voir ses propres présences
    → Voir ses propres fiches de paie
    → Modifier ses propres demandes de congé

    NOTE : Appeler check_object_permissions()
           dans la view pour activer
           has_object_permission().

    Ex dans la view :
        obj = self.get_object(pk, tenant_id)
        self.check_object_permissions(request, obj)
    """

    message = {
        "error": {
            "code": "FORBIDDEN",
            "message": "Vous ne pouvez accéder " "qu'à vos propres données.",
        }
    }

    def has_permission(self, request, view):
        """
        Niveau 1 : vérifie que l'utilisateur
        est authentifié avec un rôle valide.

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si au moins un rôle valide
        """
        roles = get_user_roles(request)
        return any(
            r in roles
            for r in [ROLE_RH_MANAGER, ROLE_MANAGER, ROLE_EMPLOYE, ROLE_COMPTABLE]
        )

    def has_object_permission(self, request, view, obj):
        """
        Niveau 2 : vérifie que l'objet appartient
        à l'utilisateur connecté ou que
        l'utilisateur est RH Manager.

        Supporte les types d'objets :
        → Employe   : obj.user_id
        → Conge     : obj.employee.user_id
        → Presence  : obj.employee.user_id
        → FichePaie : obj.employee.user_id
        → Contrat   : obj.employee.user_id

        Args:
            request: la requête HTTP Django
            view: la view appelée
            obj: l'objet Django à vérifier

        Returns:
            bool: True si propriétaire ou RH Manager
        """
        roles = get_user_roles(request)

        # RH Manager a accès à toutes les ressources
        if ROLE_RH_MANAGER in roles:
            return True

        # Manager a accès aux données de son équipe
        # Vérifié au niveau de la view
        # via le filtre manager_id
        if ROLE_MANAGER in roles:
            if hasattr(obj, "employee"):
                return str(obj.employee.manager_id) == str(request.user.auth_user_id)
            if hasattr(obj, "manager_id"):
                return str(obj.manager_id) == str(request.user.auth_user_id)

        # Récupérer l'UUID du user connecté
        # injecté par TenantMiddleware depuis le JWT
        auth_user_id = getattr(request.user, "auth_user_id", None)
        if not auth_user_id:
            return False

        # Vérifier selon le type d'objet

        # Cas 1 : Employe directement
        # Ex: GET /employes/{id}/
        if hasattr(obj, "user_id"):
            return str(obj.user_id) == str(auth_user_id)

        # Cas 2 : Conge, Presence, FichePaie, Contrat
        # → accès via employee.user_id
        # Ex: GET /leaves/{id}/
        #     GET /presences/{id}/
        #     GET /payroll/{id}/
        #     GET /contrats/{id}/
        if hasattr(obj, "employee"):
            return str(obj.employee.user_id) == str(auth_user_id)

        return False


class ReadOnly(BasePermission):
    """
    Accès en lecture seule.
    Autorise GET, HEAD, OPTIONS uniquement.
    Refuse POST, PUT, PATCH, DELETE.

    Peut être combiné avec d'autres permissions :
    permission_classes = [
        IsAuthenticated,
        IsManagerOrRH
    ]
    → Manager peut lire ET écrire

    permission_classes = [
        IsAuthenticated,
        ReadOnly
    ]
    → Tout le monde peut lire seulement
    """

    message = {"error": {"code": "FORBIDDEN", "message": "Accès en lecture seule."}}

    def has_permission(self, request, view):
        """
        Autorise uniquement les méthodes
        de lecture HTTP (safe methods).

        Args:
            request: la requête HTTP Django
            view: la view appelée

        Returns:
            bool: True si méthode de lecture
        """
        return request.method in ["GET", "HEAD", "OPTIONS"]

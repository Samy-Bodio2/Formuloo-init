"""
Middlewares — Formuloo OS Service RH

1. TenantMiddleware
   Extrait les données du JWT et les injecte
   dans request.user pour toutes les views.

2. HttpMethodMiddleware
   Bloque les méthodes HTTP non supportées
   avant le middleware JWT.
   Retourne 405 au lieu de 401.
   Corrige l'erreur Schemathesis :
   'Unsupported methods returned 401,
    expected 405'
"""

from django.http import JsonResponse

from rh.authentication import HRJWTAuthentication

# ── Méthodes HTTP autorisées ──────────────────────────────
# Toute autre méthode retourne 405
ALLOWED_HTTP_METHODS = [
    "GET",
    "POST",
    "PUT",
    "PATCH",
    "DELETE",
    "HEAD",
    "OPTIONS",
]


class HttpMethodMiddleware:
    """
    Middleware de validation des méthodes HTTP.

    Intercepte les méthodes non supportées
    AVANT le middleware JWT.

    Méthodes bloquées :
    → TRACE   → 405
    → CONNECT → 405
    → PROPFIND→ 405
    → etc.

    Doit être placé AVANT TenantMiddleware
    dans MIDDLEWARE de settings.py.
    """

    def __init__(self, get_response):
        """
        Appelé une seule fois au démarrage.
        """
        self.get_response = get_response

    def __call__(self, request):
        """
        Vérifie la méthode HTTP avant
        de passer à la vue.
        """
        if request.method not in ALLOWED_HTTP_METHODS:
            return JsonResponse(
                {
                    "error": {
                        "code": "METHOD_NOT_ALLOWED",
                        "message": f"Méthode {request.method} "
                        f"non autorisée. "
                        f"Méthodes supportées : "
                        f"{', '.join(ALLOWED_HTTP_METHODS)}",
                    }
                },
                status=405,
            )

        return self.get_response(request)


class TenantMiddleware:
    """
    Middleware d'isolation multi-tenant.

    Enrichit request.user avec les données
    extraites du payload JWT à chaque requête.

    Attributs injectés sur request.user :
        tenant_id    (UUID)  : identifiant du tenant
                               Ex: uuid-alpha
        roles        (list)  : rôles système
                               Ex: ['RH_MANAGER']
        auth_user_id (UUID)  : UUID du user dans Auth
                               Pour les logs d'audit
        custom_role  (str)   : rôle custom de la PME
                               Ex: 'ASSOCIE_PRINCIPAL'
                               None si pas de rôle custom

    En production :
    → Frontend envoie Authorization: Bearer <token>
    → Middleware lit le JWT
    → Injecte tenant_id et roles dans request.user
    → Views et permissions utilisent ces données

    NOTE : Ce middleware s'exécute en production.
           En tests, on utilise force_authenticate()
           car pytest bypasse le stack HTTP complet.
    """

    def __init__(self, get_response):
        """
        Appelé une seule fois au démarrage de Django.

        get_response : la prochaine couche middleware
                       ou la view finale.
        """
        self.get_response = get_response

    def __call__(self, request):
        """
        Appelé à CHAQUE requête HTTP.

        Tente d'extraire les données JWT et
        les injecte dans request.user.

        Si le token est absent ou invalide,
        on laisse passer sans erreur —
        IsAuthenticated s'en chargera.
        """
        try:
            # Instancier l'authentificateur JWT HR (sans DB lookup)
            jwt_auth = HRJWTAuthentication()

            # Tenter d'authentifier la requête
            # Retourne (user, token) si token valide
            # Retourne None si pas de token
            result = jwt_auth.authenticate(request)

            if result is not None:
                user, token = result

                # ── Extraire tenant_id ────────────────────
                # UUID de l'organisation dans Formuloo OS
                # Utilisé pour isoler les données
                # entre les différentes PME
                # Ex: PME Alpha ne voit pas les données
                #     de PME Beta
                user.tenant_id = token.get("tenant_id", None)

                # ── Extraire les rôles système ────────────
                # Liste des rôles système de l'utilisateur
                # définis et mappés par le service Auth
                # Ex: ['RH_MANAGER', 'COMPTABLE']
                # Utilisés par rh/permissions.py
                # pour contrôler les accès aux endpoints
                user.roles = token.get("roles", [])

                # ── Extraire auth_user_id ─────────────────
                # UUID du user dans le service Auth
                # Utilisé pour :
                # → Envoyer les logs d'audit vers Auth
                # → Lier un employé à son compte Auth
                # → Vérifier ownership des ressources
                user.auth_user_id = token.get("auth_user_id", None)

                # ── Extraire le rôle custom ───────────────
                # Rôle personnalisé défini par la PME
                # Ex: "ASSOCIE_PRINCIPAL" (Cabinet comptable)
                #     "MEDECIN_CHEF" (Hôpital)
                #     "STAGIAIRE_COMPTA" (Cabinet)
                # Mappé vers rôles système par Auth
                # Affiché dans l'interface Frontend
                # None si la PME n'a pas de rôles custom
                user.custom_role = token.get("custom_role", None)

                # Mettre à jour request.user
                # avec toutes les données enrichies
                request.user = user

        except Exception:
            # Token invalide ou expiré
            # → On laisse passer sans erreur
            # → IsAuthenticated retournera 401
            pass

        # Passer à la prochaine couche
        # (middleware suivant ou view finale)
        return self.get_response(request)

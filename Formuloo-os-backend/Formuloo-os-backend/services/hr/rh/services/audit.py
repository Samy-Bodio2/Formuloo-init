"""
Service Audit — Formuloo OS Service RH
Client HTTP vers le service Auth pour
enregistrer les actions importantes du module RH.

Architecture :
    Service RH → HTTP → Service Auth → AuditLog créé

Le service Auth gère un journal immuable
de toutes les actions utilisateurs.
Ce service envoie les événements RH vers Auth.

Actions auditées :
    EMPLOYES :
    → CREATE_EMPLOYE    création d'un employé
    → UPDATE_EMPLOYE    modification d'un employé
    → ARCHIVE_EMPLOYE   archivage d'un employé

    CONTRATS :
    → CREATE_CONTRAT    création d'un contrat
    → UPDATE_CONTRAT    modification d'un contrat
    → RESILIER_CONTRAT  résiliation d'un contrat

    CONGÉS :
    → CREATE_CONGE      soumission d'une demande
    → APPROUVER_CONGE   approbation d'une demande
    → REJETER_CONGE     rejet d'une demande
    → ANNULER_CONGE     annulation d'une demande

    PRÉSENCES :
    → CREATE_PRESENCE   enregistrement d'un pointage
    → UPDATE_PRESENCE   modification d'un pointage
    → ARCHIVE_PRESENCE  archivage d'un pointage

    FICHES DE PAIE :
    → CREATE_FICHE_PAIE   génération d'une fiche
    → VALIDER_FICHE_PAIE  validation d'une fiche
    → PAYER_FICHE_PAIE    paiement d'une fiche
    → PAYROLL_RUN         génération en masse

    DÉPARTEMENTS :
    → CREATE_DEPARTEMENT  création
    → UPDATE_DEPARTEMENT  modification
    → ARCHIVE_DEPARTEMENT archivage

    POSTES :
    → CREATE_POSTE        création
    → UPDATE_POSTE        modification
    → ARCHIVE_POSTE       archivage

Configuration dans settings.py :
    AUTH_SERVICE_URL = "http://auth:8000"
    AUTH_SERVICE_TOKEN = "service-token-interne"
"""

import logging

import requests
from django.conf import settings

# Logger pour les erreurs d'audit
logger = logging.getLogger(__name__)

# ── URL du service Auth ───────────────────────────────────
# Configurée dans settings.py
# Ex: "http://auth:8000" en Docker
#     "http://localhost:8000" en développement
AUTH_SERVICE_URL = getattr(settings, "AUTH_SERVICE_URL", "http://localhost:8000")
INTERNAL_SERVICE_TOKEN = getattr(settings, "INTERNAL_SERVICE_TOKEN", "formuloo-internal-secret-change-in-prod")

# Rétrocompatibilité
AUTH_SERVICE_TOKEN = INTERNAL_SERVICE_TOKEN

AUDIT_ENDPOINT = f"{AUTH_SERVICE_URL}/api/v1/auth/audit-logs/"


def log_action(action, resource, resource_id=None, payload=None, request=None):
    """
    Envoie un événement d'audit vers le service Auth.

    Cette fonction est appelée après chaque action
    importante dans le service RH.

    Elle est NON BLOQUANTE : si Auth est indisponible,
    l'action RH n'est pas annulée — on log simplement
    l'erreur et on continue.

    Args:
        action      (str)  : code de l'action
                             Ex: 'CREATE_EMPLOYE'
        resource    (str)  : type de ressource
                             Ex: 'Employe'
        resource_id (UUID) : UUID de la ressource
                             Ex: employe.id
        payload     (dict) : détails de l'action
                             Ex: {'email': '...'}
        request            : requête HTTP Django
                             Pour extraire user et tenant

    Returns:
        bool: True si envoyé avec succès, False sinon

    Exemple d'utilisation dans une view :
        from rh.services.audit import log_action

        # Après création d'un employé
        log_action(
            action      = 'CREATE_EMPLOYE',
            resource    = 'Employe',
            resource_id = employe.id,
            payload     = {
                'email':           employe.email,
                'employee_number': employe.employee_number,
                'department':      employe.department.nom
                                   if employe.department
                                   else None,
            },
            request = request
        )
    """
    try:
        # ── Extraire les infos du request ─────────────────
        tenant_id = None
        user_id = None
        ip_address = None

        if request:
            # tenant_id extrait par TenantMiddleware
            tenant_id = str(getattr(request.user, "tenant_id", None))
            # auth_user_id extrait par TenantMiddleware
            user_id = str(getattr(request.user, "auth_user_id", None))
            # IP du client
            ip_address = request.META.get(
                "HTTP_X_FORWARDED_FOR", request.META.get("REMOTE_ADDR", None)
            )

        # ── Construire le payload d'audit ─────────────────
        audit_data = {
            "action": action,
            "resource": resource,
            "resource_id": str(resource_id) if resource_id else None,
            "tenant_id": tenant_id,
            "user_id": user_id,
            "payload": payload or {},
            "ip_address": ip_address,
        }

        # ── Envoyer vers le service Auth ──────────────────
        # Timeout de 3 secondes pour ne pas bloquer
        # l'action RH si Auth est lent
        response = requests.post(
            AUDIT_ENDPOINT,
            json=audit_data,
            headers={
                "Content-Type": "application/json",
                "X-Service-Token": INTERNAL_SERVICE_TOKEN,
                "X-Service": "hr",
            },
            timeout=3,
        )

        # Vérifier la réponse
        if response.status_code in [200, 201]:
            return True
        else:
            logger.warning(
                f"Audit log non créé — "
                f"Auth a retourné {response.status_code} "
                f"pour action {action}"
            )
            return False

    except requests.exceptions.Timeout:
        # Auth trop lent → on log et on continue
        logger.warning(
            f"Audit log timeout — " f"Auth indisponible pour action {action}"
        )
        return False

    except requests.exceptions.ConnectionError:
        # Auth inaccessible → on log et on continue
        logger.error(
            f"Audit log impossible — " f"Connexion Auth échouée pour action {action}"
        )
        return False

    except Exception as e:
        # Erreur inattendue → on log et on continue
        logger.error(f"Audit log erreur inattendue — " f"{str(e)} pour action {action}")
        return False


# ── Fonctions utilitaires par ressource ───────────────────
# Raccourcis pour les actions les plus courantes
# Utilisés dans les views pour simplifier le code


def audit_employe(action, employe, request):
    """
    Audit des actions sur les employés.

    Args:
        action  : CREATE_EMPLOYE, UPDATE_EMPLOYE,
                  ARCHIVE_EMPLOYE
        employe : instance du modèle Employe
        request : requête HTTP Django
    """
    return log_action(
        action=action,
        resource="Employe",
        resource_id=employe.id,
        payload={
            "employee_number": employe.employee_number,
            "full_name": employe.full_name,
            "email": employe.email,
            "status": employe.status,
            "department": employe.department.nom if employe.department else None,
            "position": employe.position.titre if employe.position else None,
        },
        request=request,
    )


def audit_contrat(action, contrat, request):
    """
    Audit des actions sur les contrats.

    Args:
        action  : CREATE_CONTRAT, UPDATE_CONTRAT,
                  RESILIER_CONTRAT
        contrat : instance du modèle Contrat
        request : requête HTTP Django
    """
    return log_action(
        action=action,
        resource="Contrat",
        resource_id=contrat.id,
        payload={
            "numero": contrat.numero,
            "type": contrat.type,
            "employe": contrat.employee.full_name,
            "gross_salary": str(contrat.gross_salary),
            "currency": contrat.currency,
            "statut": contrat.statut,
        },
        request=request,
    )


def audit_conge(action, conge, request, extra=None):
    """
    Audit des actions sur les congés.

    Args:
        action  : CREATE_CONGE, APPROUVER_CONGE,
                  REJETER_CONGE, ANNULER_CONGE
        conge   : instance du modèle Conge
        request : requête HTTP Django
        extra   : données supplémentaires
                  Ex: motif de rejet
    """
    payload = {
        "employe": conge.employee.full_name,
        "type_conge": conge.type_conge,
        "start_date": str(conge.start_date),
        "end_date": str(conge.end_date),
        "days": conge.days,
        "status": conge.status,
    }
    if extra:
        payload.update(extra)

    return log_action(
        action=action,
        resource="Conge",
        resource_id=conge.id,
        payload=payload,
        request=request,
    )


def audit_presence(action, presence, request):
    """
    Audit des actions sur les présences.

    Args:
        action   : CREATE_PRESENCE, UPDATE_PRESENCE,
                   ARCHIVE_PRESENCE
        presence : instance du modèle Presence
        request  : requête HTTP Django
    """
    return log_action(
        action=action,
        resource="Presence",
        resource_id=presence.id,
        payload={
            "employe": presence.employee.full_name,
            "date": str(presence.date),
            "statut": presence.statut,
            "heures_travaillees": str(presence.heures_travaillees or 0),
        },
        request=request,
    )


def audit_fiche_paie(action, fiche, request, extra=None):
    """
    Audit des actions sur les fiches de paie.

    Args:
        action  : CREATE_FICHE_PAIE, VALIDER_FICHE_PAIE,
                  PAYER_FICHE_PAIE, PAYROLL_RUN
        fiche   : instance du modèle FichePaie
        request : requête HTTP Django
        extra   : données supplémentaires
    """
    payload = {
        "employe": fiche.employee.full_name,
        "period": fiche.period,
        "net_salary": str(fiche.net_salary),
        "currency": fiche.currency,
        "statut": fiche.statut,
    }
    if extra:
        payload.update(extra)

    return log_action(
        action=action,
        resource="FichePaie",
        resource_id=fiche.id,
        payload=payload,
        request=request,
    )


def audit_departement(action, departement, request):
    """
    Audit des actions sur les départements.

    Args:
        action       : CREATE_DEPARTEMENT,
                       UPDATE_DEPARTEMENT,
                       ARCHIVE_DEPARTEMENT
        departement  : instance du modèle Departement
        request      : requête HTTP Django
    """
    return log_action(
        action=action,
        resource="Departement",
        resource_id=departement.id,
        payload={
            "nom": departement.nom,
            "code": departement.code,
            "is_active": departement.is_active,
        },
        request=request,
    )


def audit_poste(action, poste, request):
    """
    Audit des actions sur les postes.

    Args:
        action  : CREATE_POSTE, UPDATE_POSTE,
                  ARCHIVE_POSTE
        poste   : instance du modèle Poste
        request : requête HTTP Django
    """
    return log_action(
        action=action,
        resource="Poste",
        resource_id=poste.id,
        payload={
            "titre": poste.titre,
            "code": poste.code,
            "niveau": poste.niveau,
            "departement": poste.departement.nom,
            "is_active": poste.is_active,
        },
        request=request,
    )

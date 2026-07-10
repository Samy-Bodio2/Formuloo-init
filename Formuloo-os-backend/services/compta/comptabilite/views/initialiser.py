"""
Initialisation du plan comptable SYSCOHADA pour un tenant.
POST /api/v1/compta/initialiser/

Crée :
 - 130+ comptes SYSCOHADA (idempotent — skip existants)
 - 5 journaux standards (VTE, ACH, BNQ, CAI, OD)
 - L'exercice de l'année courante (si inexistant)
"""

from datetime import date
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import Compte, Journal, Exercice
from comptabilite.permissions import CanInitPlan
from comptabilite.syscohada import PLAN_COMPTABLE_SYSCOHADA, JOURNAUX_STANDARDS


class InitialiserView(APIView):
    """
    POST /initialiser/ — idempotent, safe à relancer.
    Requiert permission compta.init.plan (SUPER_ADMIN / ADMIN_PME).
    """

    permission_classes = [IsAuthenticated, CanInitPlan]

    def post(self, request):
        tenant_id = request.user.tenant_id
        annee = date.today().year

        stats = {
            "comptes_crees": 0,
            "comptes_existants": 0,
            "journaux_crees": 0,
            "journaux_existants": 0,
            "exercice_cree": False,
            "exercice_existant": False,
        }

        # ── 1. Plan comptable ──────────────────────────────────
        for numero, libelle, classe, type_compte in PLAN_COMPTABLE_SYSCOHADA:
            _, created = Compte.objects.get_or_create(
                tenant_id=tenant_id,
                numero=numero,
                defaults={
                    "libelle": libelle,
                    "classe": classe,
                    "type_compte": type_compte,
                    "is_systeme": True,
                },
            )
            if created:
                stats["comptes_crees"] += 1
            else:
                stats["comptes_existants"] += 1

        # ── 2. Journaux standards ──────────────────────────────
        for code, libelle, type_journal in JOURNAUX_STANDARDS:
            _, created = Journal.objects.get_or_create(
                tenant_id=tenant_id,
                code=code,
                defaults={"libelle": libelle, "type": type_journal},
            )
            if created:
                stats["journaux_crees"] += 1
            else:
                stats["journaux_existants"] += 1

        # ── 3. Exercice courant ────────────────────────────────
        exercice, created = Exercice.objects.get_or_create(
            tenant_id=tenant_id,
            annee=annee,
            defaults={
                "date_debut": date(annee, 1, 1),
                "date_fin": date(annee, 12, 31),
                "statut": Exercice.Statut.OUVERT,
            },
        )
        if created:
            stats["exercice_cree"] = True
        else:
            stats["exercice_existant"] = True

        return Response({
            "message": "Initialisation SYSCOHADA terminée.",
            "tenant_id": str(tenant_id),
            "annee": annee,
            "stats": stats,
        }, status=status.HTTP_200_OK)

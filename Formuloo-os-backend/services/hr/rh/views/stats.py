"""
Views Statistiques RH — Formuloo OS

Tableau de bord chiffré pour le RH Manager et le Management.
Toutes les données sont agrégées par tenant.

Endpoint :
    GET /api/v1/hr/stats/ → Vue d'ensemble RH

Métriques retournées :
  - Effectifs : total, actifs, par département, par type
  - Congés : en attente, approuvés ce mois
  - Fiches de paie : masse salariale du mois, brouillons en attente
  - Présences : taux de présence du mois courant

Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/
"""

from decimal import Decimal

from django.db.models import Count, Q, Sum
from django.utils import timezone
from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Conge, Employe, FichePaie, Presence
from rh.permissions import IsManagerOrRH


class StatsRHView(APIView):
    """
    GET /api/v1/hr/stats/

    Retourne les indicateurs clés RH pour le tenant connecté.
    Accès : RH_MANAGER ou MANAGER.

    Les statistiques de paie (masse salariale) ne sont visibles
    que par le RH Manager — les Managers voient les effectifs seulement.
    """

    permission_classes = [IsAuthenticated, IsManagerOrRH]

    @extend_schema(
        summary="Tableau de bord RH",
        description=(
            "Retourne les indicateurs RH clés : effectifs, congés en attente, "
            "masse salariale du mois, présences. "
            "Accès réservé au Manager et au RH Manager."
        ),
        tags=["Statistiques RH"],
        responses={
            200: {"description": "Statistiques RH du tenant"},
        },
    )
    def get(self, request):
        tenant_id = request.user.tenant_id
        roles = getattr(request.user, "roles", [])
        is_rh_manager = "RH_MANAGER" in roles

        now = timezone.now()
        mois = now.month
        annee = now.year

        # ── EFFECTIFS ────────────────────────────────────────────────────────
        effectifs_qs = Employe.objects.filter(tenant_id=tenant_id)

        stats_effectifs = {
            "total": effectifs_qs.count(),
            "actifs": effectifs_qs.filter(status=Employe.Status.ACTIVE).count(),
            "inactifs": effectifs_qs.filter(status=Employe.Status.INACTIVE).count(),
            "en_conge": effectifs_qs.filter(status=Employe.Status.ON_LEAVE).count(),
            "par_type": list(
                effectifs_qs.values("type_employe")
                .annotate(count=Count("id"))
                .order_by("-count")
            ),
            "par_departement": list(
                effectifs_qs.filter(department__isnull=False)
                .values("department__nom")
                .annotate(count=Count("id"))
                .order_by("-count")
            ),
        }

        # ── CONGÉS ───────────────────────────────────────────────────────────
        conges_qs = Conge.objects.filter(tenant_id=tenant_id)

        stats_conges = {
            "en_attente": conges_qs.filter(status=Conge.Statut.PENDING).count(),
            "approuves_ce_mois": conges_qs.filter(
                status=Conge.Statut.APPROVED,
                start_date__month=mois,
                start_date__year=annee,
            ).count(),
            "rejetes_ce_mois": conges_qs.filter(
                status=Conge.Statut.REJECTED,
                updated_at__month=mois,
                updated_at__year=annee,
            ).count(),
        }

        # ── PRÉSENCES ────────────────────────────────────────────────────────
        presences_qs = Presence.objects.filter(
            tenant_id=tenant_id,
            date__month=mois,
            date__year=annee,
        )

        stats_presences = {
            "jours_presence_ce_mois": presences_qs.count(),
            "employes_presents_ce_mois": presences_qs.values("employee_id")
            .distinct()
            .count(),
        }

        # ── PAIE (visible RH Manager seulement) ──────────────────────────────
        stats_paie = None
        if is_rh_manager:
            fiches_qs = FichePaie.objects.filter(
                tenant_id=tenant_id, mois=mois, annee=annee
            )
            masse_salariale = fiches_qs.filter(
                statut__in=[FichePaie.Statut.VALIDE, FichePaie.Statut.PAYE]
            ).aggregate(total=Sum("net_salary"))["total"] or Decimal("0")

            stats_paie = {
                "periode": f"{annee}-{mois:02d}",
                "fiches_brouillon": fiches_qs.filter(
                    statut=FichePaie.Statut.BROUILLON
                ).count(),
                "fiches_validees": fiches_qs.filter(
                    statut=FichePaie.Statut.VALIDE
                ).count(),
                "fiches_payees": fiches_qs.filter(statut=FichePaie.Statut.PAYE).count(),
                "masse_salariale_nette": float(masse_salariale),
                "devise": "XAF",
            }

        response_data = {
            "periode_reference": f"{annee}-{mois:02d}",
            "effectifs": stats_effectifs,
            "conges": stats_conges,
            "presences": stats_presences,
        }

        if stats_paie:
            response_data["paie"] = stats_paie

        return Response(response_data, status=status.HTTP_200_OK)

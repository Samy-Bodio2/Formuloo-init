"""
Déclarations fiscales OHADA.
GET /declarations/tva/   → Déclaration de TVA pour une période
"""

from decimal import Decimal
from django.db.models import Sum
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import Compte, LigneEcriture, Ecriture
from comptabilite.permissions import CanReadEtats


class DeclarationTVAView(APIView):
    """
    GET /declarations/tva/?date_debut=YYYY-MM-DD&date_fin=YYYY-MM-DD

    Calcule :
      - TVA collectée  (compte 4431)
      - TVA déductible (compte 4452)
      - Solde TVA à payer ou crédit de TVA
    """

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        tenant_id = request.user.tenant_id
        date_debut = request.query_params.get("date_debut")
        date_fin = request.query_params.get("date_fin")

        if not date_debut or not date_fin:
            return Response(
                {"error": {"code": "MISSING_PARAM", "message": "date_debut et date_fin sont requis (YYYY-MM-DD)."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        def solde_compte(numero_prefix, cote):
            """
            Retourne le solde (crédit ou débit) d'un compte sur la période.
            cote = 'credit' pour TVA collectée (4431), 'debit' pour TVA déductible (4452)
            """
            comptes = Compte.objects.filter(
                tenant_id=tenant_id, numero__startswith=numero_prefix
            )
            total = Decimal("0")
            for compte in comptes:
                agg = LigneEcriture.objects.filter(
                    compte=compte,
                    ecriture__tenant_id=tenant_id,
                    ecriture__statut=Ecriture.Statut.VALIDEE,
                    ecriture__date_ecriture__range=[date_debut, date_fin],
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                td = agg["td"] or Decimal("0")
                tc = agg["tc"] or Decimal("0")
                total += tc if cote == "credit" else td
            return total

        tva_collectee = solde_compte("4431", "credit")
        tva_deductible = solde_compte("4452", "debit")
        # TVA sur immobilisations (4451) également déductible
        tva_immobilisations = solde_compte("4451", "debit")
        tva_services = solde_compte("4454", "debit")

        total_tva_deductible = tva_deductible + tva_immobilisations + tva_services
        solde = tva_collectee - total_tva_deductible

        return Response({
            "periode": {"date_debut": date_debut, "date_fin": date_fin},
            "devise": "XAF",
            "tva_collectee": str(tva_collectee),
            "tva_deductible": {
                "sur_achats": str(tva_deductible),
                "sur_immobilisations": str(tva_immobilisations),
                "sur_services": str(tva_services),
                "total": str(total_tva_deductible),
            },
            "solde": str(solde),
            "resultat": "TVA_A_PAYER" if solde > 0 else "CREDIT_TVA",
            "montant_a_payer": str(max(solde, Decimal("0"))),
            "credit_reporte": str(max(-solde, Decimal("0"))),
        })

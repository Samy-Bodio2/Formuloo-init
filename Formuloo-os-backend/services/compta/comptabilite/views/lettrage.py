"""
Lettrage des écritures comptables.
POST /api/v1/compta/ecritures/lettrer/   → lettrer un ensemble de lignes
DELETE /api/v1/compta/ecritures/delettrer/ → annuler le lettrage

Le lettrage rapproche une facture et son paiement en assignant
un même code alphanumérique (ex: "A1") aux lignes concernées.
Règle OHADA : la somme des débits lettrés = somme des crédits lettrés.
"""

import string
from decimal import Decimal
from django.db.models import Sum
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import LigneEcriture
from comptabilite.permissions import CanValidateEcritures


def _prochain_code_lettrage(tenant_id: str) -> str:
    """
    Génère le prochain code de lettrage disponible pour ce tenant.
    Format : A, B, ..., Z, AA, AB, ..., ZZ, AAA, ...
    """
    existants = set(
        LigneEcriture.objects.filter(
            ecriture__tenant_id=tenant_id,
            lettre__gt="",
        ).values_list("lettre", flat=True).distinct()
    )

    # Génère séquentiellement
    alphabet = string.ascii_uppercase
    i = 1
    while True:
        # Convertit i en base 26 alphabétique
        code = ""
        n = i
        while n > 0:
            n, r = divmod(n - 1, 26)
            code = alphabet[r] + code
        if code not in existants:
            return code
        i += 1


class LettrerEcrituresView(APIView):
    """
    POST /ecritures/lettrer/

    Body:
        {
            "ligne_ids": [1, 2, 3, ...]   ← IDs des LigneEcriture à lettrer
        }

    Règles :
    - Toutes les lignes doivent appartenir au même tenant
    - Aucune ne doit être déjà lettrée
    - Somme débits == somme crédits (équilibre du lettrage)
    """

    permission_classes = [IsAuthenticated, CanValidateEcritures]

    def post(self, request):
        tenant_id = request.user.tenant_id
        ligne_ids = request.data.get("ligne_ids", [])

        if not ligne_ids or len(ligne_ids) < 2:
            return Response(
                {"error": {"code": "INVALID_INPUT", "message": "Au moins 2 lignes sont requises pour lettrer."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        lignes = LigneEcriture.objects.filter(
            id__in=ligne_ids,
            ecriture__tenant_id=tenant_id,
        ).select_related("ecriture")

        if lignes.count() != len(ligne_ids):
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Certaines lignes sont introuvables ou appartiennent à un autre tenant."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier qu'aucune n'est déjà lettrée
        deja_lettrees = [l.id for l in lignes if l.lettre]
        if deja_lettrees:
            return Response(
                {"error": {"code": "DEJA_LETTREE", "message": f"Les lignes {deja_lettrees} sont déjà lettrées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Vérifier l'équilibre débit = crédit
        agg = lignes.aggregate(total_debit=Sum("debit"), total_credit=Sum("credit"))
        total_debit = agg["total_debit"] or Decimal("0")
        total_credit = agg["total_credit"] or Decimal("0")

        if total_debit != total_credit:
            return Response(
                {
                    "error": {
                        "code": "DESEQUILIBRE",
                        "message": f"Le lettrage doit être équilibré. Débit={total_debit}, Crédit={total_credit}.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Assigner le code de lettrage
        code = _prochain_code_lettrage(tenant_id)
        lignes.update(lettre=code)

        return Response({
            "code_lettrage": code,
            "lignes_lettrees": len(ligne_ids),
            "total_debit": str(total_debit),
            "total_credit": str(total_credit),
        }, status=status.HTTP_200_OK)


class DelettrerEcrituresView(APIView):
    """
    POST /ecritures/delettrer/

    Body:
        {
            "code_lettrage": "A1"
        }

    Annule le lettrage d'un code — remet lettre="" sur toutes les lignes concernées.
    """

    permission_classes = [IsAuthenticated, CanValidateEcritures]

    def post(self, request):
        tenant_id = request.user.tenant_id
        code = request.data.get("code_lettrage", "").strip()

        if not code:
            return Response(
                {"error": {"code": "INVALID_INPUT", "message": "code_lettrage est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        nb_updated = LigneEcriture.objects.filter(
            ecriture__tenant_id=tenant_id,
            lettre=code,
        ).update(lettre="")

        if nb_updated == 0:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": f"Aucune ligne trouvée avec le code '{code}'."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        return Response({
            "message": f"Lettrage '{code}' annulé.",
            "lignes_delettrees": nb_updated,
        }, status=status.HTTP_200_OK)


class LigreEcritureLettrageListView(APIView):
    """
    GET /ecritures/lettrage/?compte=411&non_lettre=true
    Liste les lignes lettrées ou non lettrées d'un compte.
    """

    permission_classes = [IsAuthenticated, CanValidateEcritures]

    def get(self, request):
        tenant_id = request.user.tenant_id
        compte_numero = request.query_params.get("compte")
        non_lettre = request.query_params.get("non_lettre", "false").lower() == "true"

        qs = LigneEcriture.objects.filter(
            ecriture__tenant_id=tenant_id,
            ecriture__statut="VALIDEE",
        ).select_related("ecriture", "compte")

        if compte_numero:
            qs = qs.filter(compte__numero__startswith=compte_numero)

        if non_lettre:
            qs = qs.filter(lettre="")
        else:
            qs = qs.exclude(lettre="")

        results = [
            {
                "id": l.id,
                "ecriture_id": l.ecriture_id,
                "date_ecriture": str(l.ecriture.date_ecriture),
                "libelle": l.libelle,
                "compte": l.compte.numero,
                "debit": str(l.debit),
                "credit": str(l.credit),
                "lettre": l.lettre,
            }
            for l in qs[:200]
        ]

        return Response({"count": len(results), "results": results})

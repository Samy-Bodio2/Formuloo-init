from decimal import Decimal
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import Exercice, Compte, LigneEcriture
from comptabilite.permissions import CanReadEtats


class GrandLivreView(APIView):
    """Grand livre : tous les mouvements d'un compte sur un exercice."""

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        exercice_id = request.query_params.get("exercice_id")
        if not exercice_id:
            return Response(
                {"error": {"code": "MISSING_PARAM", "message": "exercice_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            exercice = Exercice.objects.get(pk=exercice_id, tenant_id=request.user.tenant_id)
        except Exercice.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Exercice introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        compte_id = request.query_params.get("compte_id")
        date_debut = request.query_params.get("date_debut")
        date_fin = request.query_params.get("date_fin")

        comptes_qs = Compte.objects.filter(tenant_id=request.user.tenant_id)
        if compte_id:
            comptes_qs = comptes_qs.filter(pk=compte_id)

        result = []
        for compte in comptes_qs:
            lignes_qs = LigneEcriture.objects.filter(
                compte=compte,
                ecriture__exercice=exercice,
                ecriture__tenant_id=request.user.tenant_id,
            ).select_related("ecriture__journal").order_by("ecriture__date_ecriture")

            if date_debut:
                lignes_qs = lignes_qs.filter(ecriture__date_ecriture__gte=date_debut)
            if date_fin:
                lignes_qs = lignes_qs.filter(ecriture__date_ecriture__lte=date_fin)

            if not lignes_qs.exists():
                continue

            total_debit = Decimal("0")
            total_credit = Decimal("0")
            solde_cumule = Decimal("0")
            lignes_out = []

            for l in lignes_qs:
                total_debit += l.debit
                total_credit += l.credit
                solde_cumule += l.debit - l.credit
                lignes_out.append({
                    "date_ecriture": str(l.ecriture.date_ecriture),
                    "libelle": l.ecriture.libelle,
                    "journal_code": l.ecriture.journal.code,
                    "debit": str(l.debit),
                    "credit": str(l.credit),
                    "solde_cumule": str(solde_cumule),
                })

            result.append({
                "exercice_id": exercice.id,
                "compte_numero": compte.numero,
                "compte_libelle": compte.libelle,
                "total_debit": str(total_debit),
                "total_credit": str(total_credit),
                "solde_final": str(total_debit - total_credit),
                "lignes": lignes_out,
            })

        if compte_id and result:
            return Response(result[0])
        return Response(result)


class BalanceView(APIView):
    """Balance des comptes : totaux débit/crédit par compte."""

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        exercice_id = request.query_params.get("exercice_id")
        if not exercice_id:
            return Response(
                {"error": {"code": "MISSING_PARAM", "message": "exercice_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            exercice = Exercice.objects.get(pk=exercice_id, tenant_id=request.user.tenant_id)
        except Exercice.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Exercice introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        classe_filtre = request.query_params.get("classe")
        comptes = Compte.objects.filter(tenant_id=request.user.tenant_id)
        if classe_filtre:
            comptes = comptes.filter(classe=classe_filtre)

        total_debit_global = Decimal("0")
        total_credit_global = Decimal("0")
        lignes = []

        for compte in comptes:
            from django.db.models import Sum
            agg = LigneEcriture.objects.filter(
                compte=compte,
                ecriture__exercice=exercice,
                ecriture__tenant_id=request.user.tenant_id,
            ).aggregate(td=Sum("debit"), tc=Sum("credit"))

            td = agg["td"] or Decimal("0")
            tc = agg["tc"] or Decimal("0")
            if td == 0 and tc == 0:
                continue

            total_debit_global += td
            total_credit_global += tc
            solde_debiteur = max(td - tc, Decimal("0"))
            solde_crediteur = max(tc - td, Decimal("0"))

            lignes.append({
                "compte_numero": compte.numero,
                "compte_libelle": compte.libelle,
                "classe": compte.classe,
                "total_debit": str(td),
                "total_credit": str(tc),
                "solde_debiteur": str(solde_debiteur),
                "solde_crediteur": str(solde_crediteur),
            })

        return Response({
            "exercice_id": exercice.id,
            "devise": "XAF",
            "total_debit": str(total_debit_global),
            "total_credit": str(total_credit_global),
            "lignes": lignes,
        })


class BilanView(APIView):
    """Bilan OHADA : actif (classes 2,3,4 actif,5 actif) / passif (classe 1, 4 passif, 5 passif)."""

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        exercice_id = request.query_params.get("exercice_id")
        if not exercice_id:
            return Response(
                {"error": {"code": "MISSING_PARAM", "message": "exercice_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            exercice = Exercice.objects.get(pk=exercice_id, tenant_id=request.user.tenant_id)
        except Exercice.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Exercice introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        def solde_comptes(classes, type_compte):
            from django.db.models import Sum
            comptes = Compte.objects.filter(
                tenant_id=request.user.tenant_id,
                classe__in=classes,
                type_compte=type_compte,
            )
            total = Decimal("0")
            for c in comptes:
                agg = LigneEcriture.objects.filter(
                    compte=c, ecriture__exercice=exercice,
                    ecriture__tenant_id=request.user.tenant_id,
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                td = agg["td"] or Decimal("0")
                tc = agg["tc"] or Decimal("0")
                if type_compte == "ACTIF":
                    total += td - tc
                else:
                    total += tc - td
            return max(total, Decimal("0"))

        immobilisations = solde_comptes([2], "ACTIF")
        actif_circulant = solde_comptes([3, 4], "ACTIF")
        tresorerie_actif = solde_comptes([5], "ACTIF")
        total_actif = immobilisations + actif_circulant + tresorerie_actif

        capitaux_propres = solde_comptes([1], "PASSIF")
        dettes = solde_comptes([4], "PASSIF")
        tresorerie_passif = solde_comptes([5], "PASSIF")
        total_passif = capitaux_propres + dettes + tresorerie_passif

        return Response({
            "exercice_id": exercice.id,
            "devise": request.query_params.get("devise", "XAF"),
            "actif": {
                "immobilisations": str(immobilisations),
                "actif_circulant": str(actif_circulant),
                "tresorerie_actif": str(tresorerie_actif),
                "total_actif": str(total_actif),
            },
            "passif": {
                "capitaux_propres": str(capitaux_propres),
                "dettes": str(dettes),
                "tresorerie_passif": str(tresorerie_passif),
                "total_passif": str(total_passif),
            },
            "equilibre": total_actif == total_passif,
        })


class CompteResultatView(APIView):
    """Compte de résultat : produits (classe 7) - charges (classe 6)."""

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        exercice_id = request.query_params.get("exercice_id")
        if not exercice_id:
            return Response(
                {"error": {"code": "MISSING_PARAM", "message": "exercice_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            exercice = Exercice.objects.get(pk=exercice_id, tenant_id=request.user.tenant_id)
        except Exercice.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Exercice introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        from django.db.models import Sum

        def total_classe(classe, cote):
            comptes = Compte.objects.filter(tenant_id=request.user.tenant_id, classe=classe)
            total = Decimal("0")
            for c in comptes:
                agg = LigneEcriture.objects.filter(
                    compte=c, ecriture__exercice=exercice,
                    ecriture__tenant_id=request.user.tenant_id,
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                td = agg["td"] or Decimal("0")
                tc = agg["tc"] or Decimal("0")
                total += td if cote == "debit" else tc
            return total

        chiffre_affaires = total_classe(7, "credit")
        charges_exploitation = total_classe(6, "debit")
        autres_charges = total_classe(8, "debit")
        autres_produits = total_classe(8, "credit")

        total_produits = chiffre_affaires + autres_produits
        total_charges = charges_exploitation + autres_charges
        resultat_net = total_produits - total_charges

        return Response({
            "exercice_id": exercice.id,
            "devise": request.query_params.get("devise", "XAF"),
            "produits": {
                "chiffre_affaires": str(chiffre_affaires),
                "autres_produits": str(autres_produits),
                "total_produits": str(total_produits),
            },
            "charges": {
                "charges_exploitation": str(charges_exploitation),
                "autres_charges": str(autres_charges),
                "total_charges": str(total_charges),
            },
            "resultat_net": str(resultat_net),
        })

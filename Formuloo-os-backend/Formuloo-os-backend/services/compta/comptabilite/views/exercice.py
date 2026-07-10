from datetime import date
from decimal import Decimal
from django.db import transaction
from django.db.models import Sum

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Exercice, Ecriture, LigneEcriture, Compte, Journal
from comptabilite.serializers import ExerciceSerializer, ExerciceCreateSerializer
from comptabilite.permissions import CanReadExercices, CanWriteExercices, CanCloseExercices


class ExercicesListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadExercices()]
        return [IsAuthenticated(), CanWriteExercices()]

    def get(self, request):
        qs = Exercice.objects.filter(tenant_id=request.user.tenant_id)
        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            ExerciceSerializer(page, many=True).data
        )

    def post(self, request):
        ser = ExerciceCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        exercice = Exercice.objects.create(
            tenant_id=request.user.tenant_id,
            **ser.validated_data,
        )
        return Response(
            ExerciceSerializer(exercice).data, status=status.HTTP_201_CREATED
        )


class ExerciceDetailView(APIView):

    def get_permissions(self):
        return [IsAuthenticated(), CanReadExercices()]

    def _get_exercice(self, pk, tenant_id):
        try:
            return Exercice.objects.get(pk=pk, tenant_id=tenant_id)
        except Exercice.DoesNotExist:
            raise NotFound()

    def get(self, request, pk):
        exercice = self._get_exercice(pk, request.user.tenant_id)
        return Response(ExerciceSerializer(exercice).data)


class ExerciceCloturerView(APIView):
    """
    POST /exercices/{id}/cloturer/

    Clôture OHADA complète :
    1. Vérifie que toutes les écritures sont validées
    2. Calcule le résultat net (produits - charges)
    3. Enregistre l'écriture de résultat (OD) : soldes produits/charges → compte 120/139
    4. Marque l'exercice clôturé
    5. Crée automatiquement l'exercice N+1
    """

    permission_classes = [IsAuthenticated, CanCloseExercices]

    def post(self, request, pk):
        tenant_id = request.user.tenant_id
        try:
            exercice = Exercice.objects.get(pk=pk, tenant_id=tenant_id)
        except Exercice.DoesNotExist:
            raise NotFound()

        if exercice.statut == Exercice.Statut.CLOTURE:
            return Response(
                {"error": {"code": "ALREADY_CLOSED", "message": "Exercice déjà clôturé."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── 1. Vérifier qu'il n'y a plus d'écritures en brouillon ──
        brouillons = Ecriture.objects.filter(
            tenant_id=tenant_id,
            exercice=exercice,
            statut=Ecriture.Statut.BROUILLON,
        ).count()
        if brouillons > 0:
            return Response(
                {
                    "error": {
                        "code": "ECRITURES_NON_VALIDEES",
                        "message": f"{brouillons} écriture(s) en brouillon doivent être validées avant clôture.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── 2. Calculer le résultat net ────────────────────────────
        def somme_classe(classes, cote):
            comptes = Compte.objects.filter(tenant_id=tenant_id, classe__in=classes)
            total = Decimal("0")
            for c in comptes:
                agg = LigneEcriture.objects.filter(
                    compte=c,
                    ecriture__exercice=exercice,
                    ecriture__statut=Ecriture.Statut.VALIDEE,
                    ecriture__tenant_id=tenant_id,
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                td = agg["td"] or Decimal("0")
                tc = agg["tc"] or Decimal("0")
                total += td if cote == "debit" else tc
            return total

        total_produits = somme_classe([7], "credit")
        total_charges = somme_classe([6, 8], "debit")
        resultat_net = total_produits - total_charges

        with transaction.atomic():
            # ── 3. Écriture de résultat si activité ───────────────
            if total_produits > 0 or total_charges > 0:
                journal_od = Journal.objects.filter(
                    tenant_id=tenant_id, type="OD"
                ).first()

                if journal_od:
                    # Compte résultat : 130 si bénéfice, 139 si perte
                    compte_resultat_numero = "130" if resultat_net >= 0 else "139"
                    libelle_resultat = (
                        "Résultat net de l'exercice (bénéfice)"
                        if resultat_net >= 0
                        else "Résultat net de l'exercice (perte)"
                    )
                    compte_resultat = Compte.objects.filter(
                        tenant_id=tenant_id, numero=compte_resultat_numero
                    ).first()

                    if compte_resultat:
                        ecriture_cloture = Ecriture.objects.create(
                            tenant_id=tenant_id,
                            journal=journal_od,
                            exercice=exercice,
                            date_ecriture=exercice.date_fin,
                            libelle=f"Clôture exercice {exercice.annee} — Report à nouveau",
                            statut=Ecriture.Statut.VALIDEE,
                            created_by=getattr(request.user, "auth_user_id", None),
                        )
                        # Solde des produits (classe 7) → débit
                        comptes_produits = Compte.objects.filter(
                            tenant_id=tenant_id, classe=7
                        )
                        for cp in comptes_produits:
                            agg = LigneEcriture.objects.filter(
                                compte=cp,
                                ecriture__exercice=exercice,
                                ecriture__statut=Ecriture.Statut.VALIDEE,
                                ecriture__tenant_id=tenant_id,
                            ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                            solde = (agg["tc"] or Decimal("0")) - (agg["td"] or Decimal("0"))
                            if solde > 0:
                                LigneEcriture.objects.create(
                                    ecriture=ecriture_cloture,
                                    compte=cp,
                                    libelle=f"Clôture {cp.numero}",
                                    debit=solde,
                                    credit=Decimal("0"),
                                )

                        # Solde des charges (classe 6+8) → crédit
                        comptes_charges = Compte.objects.filter(
                            tenant_id=tenant_id, classe__in=[6, 8]
                        )
                        for cc in comptes_charges:
                            agg = LigneEcriture.objects.filter(
                                compte=cc,
                                ecriture__exercice=exercice,
                                ecriture__statut=Ecriture.Statut.VALIDEE,
                                ecriture__tenant_id=tenant_id,
                            ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                            solde = (agg["td"] or Decimal("0")) - (agg["tc"] or Decimal("0"))
                            if solde > 0:
                                LigneEcriture.objects.create(
                                    ecriture=ecriture_cloture,
                                    compte=cc,
                                    libelle=f"Clôture {cc.numero}",
                                    debit=Decimal("0"),
                                    credit=solde,
                                )

                        # Ligne résultat net → compte 130 ou 139
                        if resultat_net >= 0:
                            LigneEcriture.objects.create(
                                ecriture=ecriture_cloture,
                                compte=compte_resultat,
                                libelle=libelle_resultat,
                                debit=Decimal("0"),
                                credit=abs(resultat_net),
                            )
                        else:
                            LigneEcriture.objects.create(
                                ecriture=ecriture_cloture,
                                compte=compte_resultat,
                                libelle=libelle_resultat,
                                debit=abs(resultat_net),
                                credit=Decimal("0"),
                            )

            # ── 4. Clôturer l'exercice ─────────────────────────────
            exercice.statut = Exercice.Statut.CLOTURE
            exercice.date_cloture = date.today()
            exercice.resultat_net = resultat_net
            exercice.save()

            # ── 5. Créer l'exercice N+1 automatiquement ────────────
            annee_suivante = exercice.annee + 1
            exercice_suivant, cree = Exercice.objects.get_or_create(
                tenant_id=tenant_id,
                annee=annee_suivante,
                defaults={
                    "date_debut": date(annee_suivante, 1, 1),
                    "date_fin": date(annee_suivante, 12, 31),
                    "statut": Exercice.Statut.OUVERT,
                },
            )

        return Response({
            "exercice": ExerciceSerializer(exercice).data,
            "resultat_net": str(resultat_net),
            "type_resultat": "BENEFICE" if resultat_net >= 0 else "PERTE",
            "exercice_suivant": {
                "id": exercice_suivant.id,
                "annee": exercice_suivant.annee,
                "cree": cree,
            },
        })

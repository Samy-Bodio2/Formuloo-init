"""
Views Immobilisations — Formuloo OS
Module SYSCOHADA Classe 2 : actifs immobilisés + amortissements.

Endpoints :
  GET  /immobilisations/                   → liste
  POST /immobilisations/                   → créer
  GET  /immobilisations/{id}/              → détail
  PUT  /immobilisations/{id}/              → modifier
  DELETE /immobilisations/{id}/            → sortir (cession)
  POST /immobilisations/{id}/amortir/      → passer la dotation de l'exercice
  GET  /immobilisations/{id}/plan/         → plan d'amortissement complet
  GET  /immobilisations/dotations/         → toutes les dotations du tenant
"""

from decimal import Decimal
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import NotFound

from comptabilite.models import (
    Immobilisation, DotationAmortissement, Exercice, Journal, Compte, Ecriture, LigneEcriture,
)
from comptabilite.serializers import (
    ImmobilisationSerializer, ImmobilisationCreateSerializer, DotationAmortissementSerializer,
)
from comptabilite.permissions import CanWriteFactures, CanReadFactures


def _creer_ecriture_dotation(dotation, tenant_id):
    """
    Génère l'écriture OHADA pour une dotation :
      6811 Dotations amortissements (débit)
      28xx Amortissements (crédit, numéro = "28" + immo.numero_compte[1:])
    """
    try:
        journal = Journal.objects.filter(tenant_id=tenant_id, type="OD").first()
        if not journal:
            journal = Journal.objects.filter(tenant_id=tenant_id).first()
        if not journal:
            return None

        compte_dot = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="6811").first()
        if not compte_dot:
            return None

        # Compte amortissement = 28 + suite du numéro (ex: 2183 → 2818 non, on cherche 2818 ou 28xx)
        # OHADA : amortissement de 2183 → compte 2818x (on cherche le plus proche)
        num_immo = dotation.immobilisation.numero_compte
        num_amort = "28" + num_immo[2:] if len(num_immo) >= 2 else "2818"
        compte_amort = Compte.objects.filter(
            tenant_id=tenant_id, numero__startswith=num_amort[:4]
        ).first()
        if not compte_amort:
            # Fallback : n'importe quel compte 28xx
            compte_amort = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="28").first()
        if not compte_amort:
            return None

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=dotation.exercice,
            date_ecriture=dotation.date_comptabilisation.date(),
            libelle=f"Dotation amortissement {dotation.annee} — {dotation.immobilisation.designation}",
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_dot,
            libelle=dotation.immobilisation.designation,
            debit=dotation.montant,
            credit=Decimal("0"),
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_amort,
            libelle=dotation.immobilisation.designation,
            debit=Decimal("0"),
            credit=dotation.montant,
        )
        return ecriture
    except Exception:
        return None


class ImmobilisationsListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadFactures()]
        return [IsAuthenticated(), CanWriteFactures()]

    def get(self, request):
        qs = Immobilisation.objects.filter(tenant_id=request.user.tenant_id)

        statut = request.query_params.get("statut")
        if statut:
            qs = qs.filter(statut=statut)
        categorie = request.query_params.get("categorie")
        if categorie:
            qs = qs.filter(categorie=categorie)

        return Response({
            "count": qs.count(),
            "results": ImmobilisationSerializer(qs, many=True).data,
        })

    def post(self, request):
        ser = ImmobilisationCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        immo = ser.save(tenant_id=request.user.tenant_id)
        return Response(ImmobilisationSerializer(immo).data, status=status.HTTP_201_CREATED)


class ImmobilisationDetailView(APIView):

    def _get(self, pk, tenant_id):
        try:
            return Immobilisation.objects.get(pk=pk, tenant_id=tenant_id)
        except Immobilisation.DoesNotExist:
            raise NotFound()

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadFactures()]
        return [IsAuthenticated(), CanWriteFactures()]

    def get(self, request, pk):
        return Response(ImmobilisationSerializer(self._get(pk, request.user.tenant_id)).data)

    def put(self, request, pk):
        immo = self._get(pk, request.user.tenant_id)
        if immo.statut == Immobilisation.Statut.CEDEE:
            return Response(
                {"error": {"code": "IMMO_CEDEE", "message": "Immobilisation déjà cédée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ser = ImmobilisationCreateSerializer(immo, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return Response(ImmobilisationSerializer(immo).data)

    def delete(self, request, pk):
        """Sortie d'immobilisation (cession) — ne supprime pas, change le statut."""
        immo = self._get(pk, request.user.tenant_id)
        if immo.statut == Immobilisation.Statut.CEDEE:
            return Response(
                {"error": {"code": "DEJA_CEDEE", "message": "Immobilisation déjà cédée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        valeur_cession = request.data.get("valeur_nette_cession", 0)
        from django.utils import timezone
        immo.statut = Immobilisation.Statut.CEDEE
        immo.date_cession = timezone.now().date()
        immo.valeur_nette_cession = Decimal(str(valeur_cession))
        immo.save()
        return Response(ImmobilisationSerializer(immo).data)


class ImmobilisationAmortirView(APIView):
    """
    POST /immobilisations/{id}/amortir/
    Passe la dotation d'amortissement pour l'exercice courant.
    Génère l'écriture OHADA 6811 / 28xx.
    """

    permission_classes = [IsAuthenticated, CanWriteFactures]

    def post(self, request, pk):
        tenant_id = request.user.tenant_id
        try:
            immo = Immobilisation.objects.get(pk=pk, tenant_id=tenant_id)
        except Immobilisation.DoesNotExist:
            raise NotFound()

        if immo.statut != Immobilisation.Statut.ACTIVE:
            return Response(
                {"error": {"code": "IMMO_INACTIVE", "message": "Seules les immobilisations actives peuvent être amorties."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if immo.methode == Immobilisation.MethodeAmortissement.NON_AMORTISSABLE:
            return Response(
                {"error": {"code": "NON_AMORTISSABLE", "message": "Cette immobilisation n'est pas amortissable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Exercice fourni ou exercice courant
        exercice_id = request.data.get("exercice_id")
        if exercice_id:
            try:
                exercice = Exercice.objects.get(id=exercice_id, tenant_id=tenant_id)
            except Exercice.DoesNotExist:
                return Response(
                    {"error": {"code": "EXERCICE_INTROUVABLE", "message": "Exercice introuvable."}},
                    status=status.HTTP_404_NOT_FOUND,
                )
        else:
            exercice = Exercice.objects.filter(tenant_id=tenant_id, statut="OUVERT").first()
            if not exercice:
                return Response(
                    {"error": {"code": "PAS_EXERCICE", "message": "Aucun exercice ouvert pour ce tenant."}},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        annee = exercice.date_fin.year

        # Vérifier si déjà amorti cette année
        if DotationAmortissement.objects.filter(immobilisation=immo, annee=annee).exists():
            return Response(
                {"error": {"code": "DEJA_AMORTI", "message": f"Dotation {annee} déjà passée pour cette immobilisation."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Calculer la dotation
        annuite = immo.calculer_annuite(annee)
        if annuite <= 0:
            return Response(
                {"error": {"code": "AMORTISSEMENT_TERMINE", "message": "Cette immobilisation est totalement amortie."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Créer la dotation
        dotation = DotationAmortissement.objects.create(
            tenant_id=tenant_id,
            immobilisation=immo,
            exercice=exercice,
            annee=annee,
            montant=annuite,
        )

        # Générer l'écriture OHADA
        ecriture = _creer_ecriture_dotation(dotation, tenant_id)
        if ecriture:
            dotation.ecriture = ecriture
            dotation.save(update_fields=["ecriture"])

        # Mettre à jour le cumul sur l'immo
        immo.cumul_amortissements += annuite
        if immo.cumul_amortissements >= (immo.valeur_origine - immo.valeur_residuelle):
            immo.statut = Immobilisation.Statut.AMORTIE
        immo.save(update_fields=["cumul_amortissements", "statut"])

        return Response({
            "dotation": DotationAmortissementSerializer(dotation).data,
            "valeur_nette_comptable": str(immo.valeur_nette_comptable),
            "ecriture_id": ecriture.id if ecriture else None,
        }, status=status.HTTP_201_CREATED)


class ImmobilisationPlanView(APIView):
    """
    GET /immobilisations/{id}/plan/
    Retourne le plan d'amortissement complet (année par année).
    """

    permission_classes = [IsAuthenticated, CanReadFactures]

    def get(self, request, pk):
        try:
            immo = Immobilisation.objects.get(pk=pk, tenant_id=request.user.tenant_id)
        except Immobilisation.DoesNotExist:
            raise NotFound()

        plan = []
        vnc = immo.valeur_origine
        base = immo.valeur_origine - immo.valeur_residuelle
        annee_debut = immo.date_mise_en_service.year

        # On simule le plan depuis le début
        cumul_simule = Decimal("0")
        for i in range(immo.duree_vie):
            annee = annee_debut + i
            if immo.methode == Immobilisation.MethodeAmortissement.LINEAIRE:
                annuite = (base / Decimal(str(immo.duree_vie))).quantize(Decimal("1"))
                # Dernier exercice : ajustement
                if i == immo.duree_vie - 1:
                    annuite = base - cumul_simule
            elif immo.methode == Immobilisation.MethodeAmortissement.NON_AMORTISSABLE:
                annuite = Decimal("0")
            else:
                if immo.duree_vie <= 4:
                    coeff = Decimal("1.5")
                elif immo.duree_vie <= 6:
                    coeff = Decimal("2.0")
                else:
                    coeff = Decimal("2.5")
                taux = immo.taux_lineaire * coeff
                vnc_periode = immo.valeur_origine - cumul_simule
                annuite = (vnc_periode * taux).quantize(Decimal("1"))
                restant = vnc_periode - immo.valeur_residuelle
                annuite = min(annuite, max(restant, Decimal("0")))

            cumul_simule += annuite
            vnc = immo.valeur_origine - cumul_simule

            # Dotation réelle (si déjà passée)
            dotation_reelle = DotationAmortissement.objects.filter(
                immobilisation=immo, annee=annee
            ).first()

            plan.append({
                "annee": annee,
                "annuite_prevue": str(annuite),
                "cumul_prevu": str(cumul_simule),
                "vnc_fin": str(max(vnc, immo.valeur_residuelle)),
                "passe": dotation_reelle is not None,
                "montant_reel": str(dotation_reelle.montant) if dotation_reelle else None,
            })

        return Response({
            "immobilisation": ImmobilisationSerializer(immo).data,
            "plan": plan,
        })


class DotationsListView(APIView):
    """
    GET /immobilisations/dotations/ — toutes les dotations du tenant
    """

    permission_classes = [IsAuthenticated, CanReadFactures]

    def get(self, request):
        qs = DotationAmortissement.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("immobilisation", "exercice")

        annee = request.query_params.get("annee")
        if annee:
            qs = qs.filter(annee=annee)

        return Response({
            "count": qs.count(),
            "results": DotationAmortissementSerializer(qs, many=True).data,
        })

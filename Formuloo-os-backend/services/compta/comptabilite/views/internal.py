"""
Endpoints internes — Service Compta
Appelés uniquement depuis d'autres services (HR, GesDoc) via X-Service-Token.
Jamais exposés via le gateway nginx public.
"""

from decimal import Decimal
from datetime import date as date_type

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import (
    Ecriture, LigneEcriture, Exercice, Journal, Compte,
)
from comptabilite.service_auth import ServiceTokenAuthentication, ServiceUser


class EcriturePaieInternalView(APIView):
    """
    POST /api/v1/compta/_internal/ecritures-paie/
    Crée l'écriture comptable OHADA pour le paiement d'un salaire.

    Écriture générée :
        6411 Charges de personnel (débit) = salaire brut
        4311 CNPS à reverser (crédit)     = cotisation CNPS
        4471 IRPP à reverser (crédit)     = impôt IRPP
        4211 Salaires à payer (crédit)    = salaire net + autres déductions

    Invariant débit = crédit :
        brut = net + cnps + irpp + autres ✓
    """

    authentication_classes = [ServiceTokenAuthentication]
    permission_classes = [IsAuthenticated]

    def post(self, request):
        if not isinstance(request.user, ServiceUser):
            return Response(
                {"error": {"code": "FORBIDDEN", "message": "Endpoint réservé aux services internes."}},
                status=status.HTTP_403_FORBIDDEN,
            )

        data = request.data
        tenant_id = data.get("tenant_id")
        if not tenant_id:
            return Response(
                {"error": {"code": "INVALID", "message": "tenant_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        salaire_brut = Decimal(str(data.get("salaire_brut", "0")))
        salaire_net = Decimal(str(data.get("salaire_net", "0")))
        cotisation_cnps = Decimal(str(data.get("cotisation_cnps", "0")))
        impot_irpp = Decimal(str(data.get("impot_irpp", "0")))
        autres_deductions = Decimal(str(data.get("autres_deductions", "0")))
        employe_nom = data.get("employe_nom", "Employé")
        periode = data.get("periode", "")
        fiche_paie_id = data.get("fiche_paie_id", "")

        date_paiement_raw = data.get("date_paiement")
        try:
            from datetime import datetime
            date_paiement = datetime.strptime(date_paiement_raw, "%Y-%m-%d").date() if date_paiement_raw else date_type.today()
        except (ValueError, TypeError):
            date_paiement = date_type.today()

        # Trouver l'exercice ouvert couvrant la date
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id,
            statut="OUVERT",
            date_debut__lte=date_paiement,
            date_fin__gte=date_paiement,
        ).first()

        if not exercice:
            return Response(
                {"error": {"code": "NO_EXERCICE", "message": "Aucun exercice ouvert pour cette date."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Trouver ou utiliser le journal OD (Opérations diverses) par défaut
        journal = Journal.objects.filter(tenant_id=tenant_id, type="OD").first()
        if not journal:
            return Response(
                {"error": {"code": "NO_JOURNAL", "message": "Journal OD introuvable. Créer un journal de type OD."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Résoudre les comptes OHADA nécessaires
        compte_6411 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="6411").first()
        compte_4211 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4211").first()
        compte_4311 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4311").first()
        compte_4471 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4471").first()

        comptes_manquants = []
        if not compte_6411:
            comptes_manquants.append("6411 Charges de personnel")
        if not compte_4211:
            comptes_manquants.append("4211 Personnel, rémunérations dues")
        if not compte_4311 and cotisation_cnps > 0:
            comptes_manquants.append("4311 CNPS à reverser")
        if not compte_4471 and impot_irpp > 0:
            comptes_manquants.append("4471 IRPP à reverser")

        if comptes_manquants:
            return Response(
                {
                    "error": {
                        "code": "COMPTES_MANQUANTS",
                        "message": f"Comptes manquants dans le plan comptable : {', '.join(comptes_manquants)}. "
                                   f"Créer ces comptes avant de payer les salaires.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Créer l'écriture
        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=date_paiement,
            libelle=f"Paie {periode} — {employe_nom}",
            statut="BROUILLON",
        )

        # Ligne débit : 6411 Personnel (brut complet)
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_6411,
            libelle=f"Salaire brut {employe_nom}",
            debit=salaire_brut,
            credit=Decimal("0"),
        )

        # Ligne crédit : 4311 CNPS
        if cotisation_cnps > 0 and compte_4311:
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=compte_4311,
                libelle=f"CNPS {employe_nom}",
                debit=Decimal("0"),
                credit=cotisation_cnps,
            )

        # Ligne crédit : 4471 IRPP
        if impot_irpp > 0 and compte_4471:
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=compte_4471,
                libelle=f"IRPP {employe_nom}",
                debit=Decimal("0"),
                credit=impot_irpp,
            )

        # Ligne crédit : 4211 Rémunérations dues (net + éventuels autres)
        montant_4211 = salaire_brut - cotisation_cnps - impot_irpp
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_4211,
            libelle=f"Net à payer {employe_nom}",
            debit=Decimal("0"),
            credit=montant_4211,
        )

        return Response(
            {
                "ecriture_id": str(ecriture.id),
                "libelle": ecriture.libelle,
                "date_ecriture": str(ecriture.date_ecriture),
                "total_debit": str(salaire_brut),
                "total_credit": str(salaire_brut),
                "fiche_paie_id": fiche_paie_id,
            },
            status=status.HTTP_201_CREATED,
        )


class EcritureAchatInternalView(APIView):
    """
    POST /api/v1/compta/_internal/ecritures-achat/
    Crée l'écriture comptable OHADA pour une pièce d'achat certifiée par GesDoc.

    Écriture générée (comptes SYSCOHADA) :
        601x Achats (débit)              = amount_ht
        4451 TVA déductible (débit)      = tva_amount  (si tva > 0)
        4011 Fournisseurs (crédit)       = amount_ttc

    Invariant débit = crédit :
        amount_ht + tva_amount = amount_ttc ✓
    """

    authentication_classes = [ServiceTokenAuthentication]
    permission_classes = [IsAuthenticated]

    JOURNAL_ACHATS_TYPES = ("ACHATS",)

    CHARGE_ACCOUNTS = {
        "invoice":        "6011",
        "purchase_order": "6011",
        "receipt":        "6068",
    }

    def post(self, request):
        if not isinstance(request.user, ServiceUser):
            return Response(
                {"error": {"code": "FORBIDDEN", "message": "Endpoint réservé aux services internes."}},
                status=status.HTTP_403_FORBIDDEN,
            )

        data = request.data
        tenant_id = data.get("tenant_id")
        if not tenant_id:
            return Response(
                {"error": {"code": "INVALID", "message": "tenant_id est requis."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            amount_ht = Decimal(str(data.get("amount_ht", "0")))
            tva_rate = Decimal(str(data.get("tva_rate", "0")))
            amount_ttc = Decimal(str(data.get("amount_ttc", "0")))
        except Exception:
            return Response(
                {"error": {"code": "INVALID", "message": "Montants invalides."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        document_number = data.get("document_number", "")
        supplier = data.get("supplier", "Fournisseur")
        document_type = data.get("document_type", "invoice")
        document_id = data.get("document_id", "")

        date_raw = data.get("date")
        try:
            from datetime import datetime
            date_doc = datetime.strptime(date_raw, "%Y-%m-%d").date() if date_raw else date_type.today()
        except (ValueError, TypeError):
            date_doc = date_type.today()

        exercice = Exercice.objects.filter(
            tenant_id=tenant_id,
            statut="OUVERT",
            date_debut__lte=date_doc,
            date_fin__gte=date_doc,
        ).first()

        if not exercice:
            return Response(
                {"error": {"code": "NO_EXERCICE", "message": "Aucun exercice ouvert pour cette date."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        journal = Journal.objects.filter(
            tenant_id=tenant_id, type__in=self.JOURNAL_ACHATS_TYPES
        ).first()
        if not journal:
            journal = Journal.objects.filter(tenant_id=tenant_id, type="OD").first()
        if not journal:
            return Response(
                {"error": {"code": "NO_JOURNAL", "message": "Journal Achats (AC) introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        charge_prefix = self.CHARGE_ACCOUNTS.get(document_type, "6011")
        compte_charge = Compte.objects.filter(tenant_id=tenant_id, numero__startswith=charge_prefix).first()
        compte_4011 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4011").first()
        compte_4451 = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4451").first()

        comptes_manquants = []
        if not compte_charge:
            comptes_manquants.append(f"{charge_prefix} Achats")
        if not compte_4011:
            comptes_manquants.append("4011 Fournisseurs")
        tva_amount = (amount_ttc - amount_ht).quantize(Decimal("0.01"))
        if tva_amount > 0 and not compte_4451:
            comptes_manquants.append("4451 TVA déductible")

        if comptes_manquants:
            return Response(
                {
                    "error": {
                        "code": "COMPTES_MANQUANTS",
                        "message": f"Comptes manquants : {', '.join(comptes_manquants)}.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        libelle = f"Achat {supplier}"
        if document_number:
            libelle = f"{libelle} — {document_number}"

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=date_doc,
            libelle=libelle,
            statut="BROUILLON",
            reference_piece=document_number or document_id,
        )

        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_charge,
            libelle=libelle,
            debit=amount_ht,
            credit=Decimal("0"),
        )

        if tva_amount > 0 and compte_4451:
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=compte_4451,
                libelle=f"TVA déductible {tva_rate}%",
                debit=tva_amount,
                credit=Decimal("0"),
            )

        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_4011,
            libelle=f"Fournisseur {supplier}",
            debit=Decimal("0"),
            credit=amount_ttc,
        )

        return Response(
            {
                "ecriture_id": str(ecriture.id),
                "libelle": ecriture.libelle,
                "date_ecriture": str(ecriture.date_ecriture),
                "total_debit": str(amount_ttc),
                "total_credit": str(amount_ttc),
                "document_id": document_id,
            },
            status=status.HTTP_201_CREATED,
        )

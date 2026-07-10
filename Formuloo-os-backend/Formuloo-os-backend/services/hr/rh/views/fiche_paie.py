"""
Views FichePaie — Formuloo OS
Génération et consultation des fiches de paie SYSCOHADA.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET    /payroll/              → IsComptableOrRH
    POST   /payroll/              → IsComptableOrRH
    GET    /payroll/{id}/         → IsOwnerOrRH
    PUT    /payroll/{id}/         → IsComptableOrRH
    DELETE /payroll/{id}/         → IsComptableOrRH
    POST   /payroll/run/          → IsComptableOrRH
    GET    /payroll/periode/{p}/  → IsComptableOrRH
    POST   /payroll/{id}/valider/ → IsComptableOrRH
    POST   /payroll/{id}/payer/   → IsComptableOrRH

Audit :
    POST          → CREATE_FICHE_PAIE
    POST valider/ → VALIDER_FICHE_PAIE
    POST payer/   → PAYER_FICHE_PAIE
    POST run/     → PAYROLL_RUN
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from decimal import Decimal
from rh.models import Employe, FichePaie
from rh.services.cotisations import calculer_cotisations
from rh.serializers.fiche_paie import _calculer_nb_parts
from rh.permissions import (
    IsComptableOrRH,
    IsOwnerOrRH,
)
from rh.serializers import (
    FichePaieCreateSerializer,
    FichePaiePayerSerializer,
    FichePaieSerializer,
    FichePaieValiderSerializer,
    PayrollRunSerializer,
)
from rh.services.audit import audit_fiche_paie, log_action
from rh.services.email import notifier_paie_prete


class PayrollListView(APIView):
    """
    GET  /api/v1/hr/payroll/ — Liste des fiches de paie
    POST /api/v1/hr/payroll/ — Générer une fiche de paie
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Lister les fiches de paie",
        description="Retourne la liste paginée " "des fiches de paie du tenant.",
        tags=["Paies"],
        responses={
            200: FichePaieSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        fiches = FichePaie.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("employee", "contrat")

        # Filtre employé
        employe_id = request.query_params.get("employe_id")
        if employe_id:
            fiches = fiches.filter(employee_id=employe_id)

        # Filtre mois
        mois = request.query_params.get("mois")
        if mois:
            fiches = fiches.filter(mois=mois)

        # Filtre année
        annee = request.query_params.get("annee")
        if annee:
            fiches = fiches.filter(annee=annee)

        # Filtre statut
        statut = request.query_params.get("statut")
        if statut:
            fiches = fiches.filter(statut=statut)

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = fiches.count()
        page_data = fiches[start:end]

        serializer = FichePaieSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/payroll/" f"?page={page + 1}" if end < total else None
                ),
                "previous": (
                    f"/api/v1/hr/payroll/" f"?page={page - 1}" if page > 1 else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Générer une fiche de paie",
        tags=["Paies"],
        request=FichePaieCreateSerializer,
        responses={
            201: FichePaieSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = FichePaieCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        fiche = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_fiche_paie("CREATE_FICHE_PAIE", fiche, request)

        return Response(FichePaieSerializer(fiche).data, status=status.HTTP_201_CREATED)


class PayrollRunView(APIView):
    """
    POST /api/v1/hr/payroll/run/
    Lancer la génération de paie en masse.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Lancer la génération de paie en masse",
        description="Génère les fiches de paie pour "
        "tous les employés actifs du tenant.",
        tags=["Paies"],
        request=PayrollRunSerializer,
        responses={
            202: {"description": "Génération lancée"},
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = PayrollRunSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        mois = serializer.validated_data["mois"]
        annee = serializer.validated_data["annee"]

        # Récupérer tous les employés actifs
        employes = Employe.objects.filter(
            tenant_id=request.user.tenant_id, status="active"
        )

        nb_crees = 0
        nb_ignores = 0

        for employe in employes:
            # Vérifier si une fiche existe déjà
            if FichePaie.objects.filter(
                employee=employe, mois=mois, annee=annee
            ).exists():
                nb_ignores += 1
                continue

            # Récupérer le contrat actif
            contrat = employe.contrats.filter(is_active=True, statut="actif").first()

            # Calcul automatique CNPS/IRPP selon barème camerounais
            brut = Decimal(str(employe.salaire_base))
            nb_parts = _calculer_nb_parts(employe)
            cotisations = calculer_cotisations(brut, nb_parts)

            FichePaie.objects.create(
                tenant_id=request.user.tenant_id,
                employee=employe,
                contrat=contrat,
                mois=mois,
                annee=annee,
                salaire_base=employe.salaire_base,
                bonuses={
                    "prime_transport": 0,
                    "prime_logement": 0,
                    "prime_rendement": 0,
                    "autres": 0,
                },
                deductions={
                    "cotisation_cnps": cotisations["cotisation_cnps"],
                    "impot_irpp": cotisations["impot_irpp"],
                    "credit_logement": 0,
                    "autres": 0,
                },
            )
            nb_crees += 1

        # ── Audit ─────────────────────────────────────────
        log_action(
            action="PAYROLL_RUN",
            resource="FichePaie",
            payload={
                "mois": mois,
                "annee": annee,
                "nb_crees": nb_crees,
                "nb_ignores": nb_ignores,
            },
            request=request,
        )

        return Response(
            {
                "message": f"Génération de paie lancée " f"pour {mois:02d}/{annee}",
                "nb_employes": employes.count(),
                "nb_crees": nb_crees,
                "nb_ignores": nb_ignores,
            },
            status=status.HTTP_202_ACCEPTED,
        )


class PayrollPeriodeView(APIView):
    """
    GET /api/v1/hr/payroll/periode/{periode}/
    Fiches de paie d'une période donnée.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Fiches de paie d'une période",
        description="Retourne toutes les fiches " "d'une période (format YYYY-MM).",
        tags=["Paies"],
        responses={
            200: FichePaieSerializer(many=True),
            400: {"description": "Format de période invalide"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request, periode):
        # Valider le format YYYY-MM
        import re

        if not re.match(r"^\d{4}-\d{2}$", periode):
            return Response(
                {
                    "error": {
                        "code": "INVALID_FORMAT",
                        "message": "Format de période invalide " "— attendu: YYYY-MM",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        annee, mois = periode.split("-")
        fiches = FichePaie.objects.filter(
            tenant_id=request.user.tenant_id, annee=int(annee), mois=int(mois)
        ).select_related("employee", "contrat")

        serializer = FichePaieSerializer(fiches, many=True)
        return Response(
            {"count": fiches.count(), "periode": periode, "results": serializer.data},
            status=status.HTTP_200_OK,
        )


class PayrollDetailView(APIView):
    """
    GET    /api/v1/hr/payroll/{id}/ — Détail
    PUT    /api/v1/hr/payroll/{id}/ — Modifier
    DELETE /api/v1/hr/payroll/{id}/ — Supprimer
    """

    def get_permissions(self):
        """
        GET    → IsOwnerOrRH
        PUT    → IsComptableOrRH
        DELETE → IsComptableOrRH
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsOwnerOrRH()]
        return [IsAuthenticated(), IsComptableOrRH()]

    def get_object(self, pk, tenant_id):
        try:
            return FichePaie.objects.get(id=pk, tenant_id=tenant_id)
        except FichePaie.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'une fiche de paie",
        tags=["Paies"],
        responses={
            200: FichePaieSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Fiche introuvable"},
        },
    )
    def get(self, request, pk):
        fiche = self.get_object(pk, request.user.tenant_id)
        if not fiche:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Fiche de paie introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, fiche)

        serializer = FichePaieSerializer(fiche)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier une fiche de paie",
        tags=["Paies"],
        request=FichePaieCreateSerializer,
        responses={
            200: FichePaieSerializer,
            400: {"description": "Fiche déjà validée"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Fiche introuvable"},
        },
    )
    def put(self, request, pk):
        fiche = self.get_object(pk, request.user.tenant_id)
        if not fiche:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Fiche de paie introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        if fiche.statut != FichePaie.Statut.BROUILLON:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Impossible de modifier "
                        "une fiche validée ou payée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        serializer = FichePaieCreateSerializer(
            fiche, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        fiche = serializer.save()

        return Response(FichePaieSerializer(fiche).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Supprimer une fiche de paie",
        tags=["Paies"],
        responses={
            204: {"description": "Fiche supprimée"},
            400: {"description": "Fiche déjà validée"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Fiche introuvable"},
        },
    )
    def delete(self, request, pk):
        fiche = self.get_object(pk, request.user.tenant_id)
        if not fiche:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Fiche de paie introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        if fiche.statut != FichePaie.Statut.BROUILLON:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Impossible de supprimer "
                        "une fiche validée ou payée.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        fiche.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PayrollValiderView(APIView):
    """
    POST /api/v1/hr/payroll/{id}/valider/
    Valider une fiche de paie.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Valider une fiche de paie",
        tags=["Paies"],
        responses={
            200: FichePaieSerializer,
            400: {"description": "Fiche déjà validée"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Fiche introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            fiche = FichePaie.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except FichePaie.DoesNotExist:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Fiche de paie introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        try:
            fiche.valider()
        except ValueError as e:
            return Response(
                {"error": {"code": "CONFLICT", "message": str(e)}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── Audit ─────────────────────────────────────────
        audit_fiche_paie("VALIDER_FICHE_PAIE", fiche, request)

        # ── Notification email employé (non bloquante) ─────
        notifier_paie_prete(fiche.employee, fiche)

        return Response(FichePaieSerializer(fiche).data, status=status.HTTP_200_OK)


class PayrollPayerView(APIView):
    """
    POST /api/v1/hr/payroll/{id}/payer/
    Marquer une fiche comme payée.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Marquer une fiche comme payée",
        tags=["Paies"],
        request=FichePaiePayerSerializer,
        responses={
            200: FichePaieSerializer,
            400: {"description": "Fiche non validée"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Fiche introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            fiche = FichePaie.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except FichePaie.DoesNotExist:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Fiche de paie introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = FichePaiePayerSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            fiche.payer(serializer.validated_data["mode_paiement"])
        except ValueError as e:
            return Response(
                {"error": {"code": "CONFLICT", "message": str(e)}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── Écriture comptable OHADA ───────────────────────
        # 6411 Personnel (débit) / 4211+4311+4471 (crédit)
        # Non-bloquant : si Compta est indisponible, la paie
        # est enregistrée quand même.
        from rh.services.compta_client import creer_ecriture_paie
        ecriture_id = creer_ecriture_paie(fiche)
        if ecriture_id:
            import uuid as _uuid
            try:
                fiche.journal_entry_id = _uuid.UUID(str(ecriture_id))
                fiche.save(update_fields=["journal_entry_id"])
            except (ValueError, AttributeError):
                pass

        # ── Audit ─────────────────────────────────────────
        audit_fiche_paie(
            "PAYER_FICHE_PAIE",
            fiche,
            request,
            extra={
                "mode_paiement": fiche.mode_paiement,
                "journal_entry_id": str(fiche.journal_entry_id) if fiche.journal_entry_id else None,
            },
        )

        return Response(FichePaieSerializer(fiche).data, status=status.HTTP_200_OK)

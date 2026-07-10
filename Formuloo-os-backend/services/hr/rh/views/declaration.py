"""
Views Déclarations RH — Formuloo OS

Génère les documents de déclaration réglementaires
spécifiques au Cameroun / Afrique centrale :

  CNPS :
    GET /api/v1/hr/declarations/cnps/
    Déclaration mensuelle CNPS (Caisse Nationale de Prévoyance Sociale).
    Liste des employés avec leurs cotisations du mois.
    Taux légaux Cameroun 2024 :
      - Part salariale  : 2.8% du salaire brut plafonné à 750 000 XAF
      - Part patronale  : 7.7% du salaire brut plafonné à 750 000 XAF
    Destinée à l'export CSV ou au remplissage du bordereau CNPS.

  IRPP :
    GET /api/v1/hr/declarations/irpp/
    Simulation IRPP mensuel par employé.
    Barème DGI Cameroun 2024 (tranches annuelles) :
      - 0%  : 0 à 2 000 000 XAF/an
      - 10% : 2 000 001 à 3 000 000 XAF/an
      - 15% : 3 000 001 à 5 000 000 XAF/an
      - 25% : > 5 000 000 XAF/an

  ATTESTATION :
    GET /api/v1/hr/employes/{pk}/attestation/
    Données structurées pour générer une attestation de travail.
    Le frontend utilise ces données pour imprimer le document.

Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/
"""

from decimal import Decimal

from django.utils import timezone
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Employe, FichePaie
from rh.permissions import IsComptableOrRH, IsOwnerOrRH, IsRHManager

# ── CONSTANTES FISCALES CAMEROUN — VALEURS 2026 ──────────────────────────────
#
# Sources : Code Général des Impôts Cameroun 2026 (CGI 2026)
#           Loi de Finances 2026 — Direction Générale des Impôts (DGI)
#           Code du Travail — Décret CNPS en vigueur
#
# CNPS : les taux et le plafond sont inchangés depuis la réforme de 2018.
#   Circulaire DGE n°0023/MINFI/DGI — confirme plafond 750 000 XAF/mois.
#
# IRPP : tranches inchangées depuis la réforme fiscale de 2015.
#   Art. 69 CGI — barème progressif 0/10/15/25%.
#   Déduction forfaitaire 30% pour frais professionnels (Art. 70 CGI).
#
# NOTE : mettre à jour ces valeurs si la Loi de Finances modifie les tranches.

# Plafond mensuel de cotisation CNPS — XAF/mois
CNPS_PLAFOND_MENSUEL = Decimal("750000")

# Taux de cotisation CNPS Cameroun (inchangés 2018–2026)
CNPS_TAUX_SALARIAL = Decimal("0.028")  # 2.8% — part salariale (employé)
CNPS_TAUX_PATRONAL = Decimal("0.077")  # 7.7% — part patronale (employeur)

# Barème IRPP progressif DGI Cameroun (tranches annuelles en XAF — inchangé 2015–2026)
IRPP_TRANCHES = [
    (Decimal("2000000"), Decimal("0.00")),  # 0%  : revenu imposable ≤ 2 000 000 XAF/an
    (Decimal("3000000"), Decimal("0.10")),  # 10% : de 2 000 001 à 3 000 000 XAF/an
    (Decimal("5000000"), Decimal("0.15")),  # 15% : de 3 000 001 à 5 000 000 XAF/an
    (None, Decimal("0.25")),  # 25% : au-delà de 5 000 000 XAF/an
]


def _calculer_cnps(salaire_brut: Decimal) -> dict:
    """
    Calcule les cotisations CNPS pour un salaire brut mensuel.

    Le calcul est plafonné à CNPS_PLAFOND_MENSUEL (750 000 XAF/mois).

    Args:
        salaire_brut : salaire brut mensuel en XAF

    Returns:
        dict avec :
          - base_cotisable  : salaire brut plafonné
          - part_salariale  : cotisation employé
          - part_patronale  : cotisation employeur
          - total           : total des deux parts
    """
    base = min(salaire_brut, CNPS_PLAFOND_MENSUEL)
    part_salariale = (base * CNPS_TAUX_SALARIAL).quantize(Decimal("1"))
    part_patronale = (base * CNPS_TAUX_PATRONAL).quantize(Decimal("1"))
    return {
        "base_cotisable": float(base),
        "part_salariale": float(part_salariale),
        "part_patronale": float(part_patronale),
        "total": float(part_salariale + part_patronale),
    }


def _calculer_irpp_mensuel(salaire_brut_mensuel: Decimal) -> float:
    """
    Calcule l'IRPP mensuel selon le barème DGI Cameroun 2024.

    Méthode :
      1. Annualise le salaire brut mensuel (× 12)
      2. Applique le barème progressif par tranches
      3. Divise par 12 pour obtenir le montant mensuel

    Note : en pratique la DGI Cameroun applique une déduction forfaitaire
    de 30% pour frais professionnels avant le calcul. On applique cette
    déduction ici pour un résultat réaliste.

    Args:
        salaire_brut_mensuel : salaire brut mensuel en XAF

    Returns:
        float : IRPP mensuel en XAF (arrondi à l'entier)
    """
    # Annualiser
    revenu_annuel = salaire_brut_mensuel * 12

    # Déduction forfaitaire 30% frais professionnels (DGI Cameroun)
    revenu_imposable = revenu_annuel * Decimal("0.70")

    irpp_annuel = Decimal("0")
    tranche_bas = Decimal("0")

    for plafond, taux in IRPP_TRANCHES:
        if plafond is None:
            # Dernière tranche : tout ce qui dépasse 5M
            irpp_annuel += (revenu_imposable - tranche_bas) * taux
            break
        if revenu_imposable <= plafond:
            irpp_annuel += (revenu_imposable - tranche_bas) * taux
            break
        irpp_annuel += (plafond - tranche_bas) * taux
        tranche_bas = plafond

    irpp_mensuel = irpp_annuel / 12
    return float(irpp_mensuel.quantize(Decimal("1")))


# ── DÉCLARATION CNPS ─────────────────────────────────────────────────────────


class DeclarationCNPSView(APIView):
    """
    GET /api/v1/hr/declarations/cnps/

    Génère la déclaration mensuelle CNPS pour un mois/année donné.
    Liste tous les employés actifs avec leurs cotisations calculées.

    Paramètres :
      - mois  (int) : mois concerné (1-12). Défaut : mois précédent.
      - annee (int) : année concernée. Défaut : année courante.

    Accès : RH_MANAGER ou COMPTABLE uniquement.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Déclaration mensuelle CNPS",
        description=(
            "Génère la déclaration mensuelle CNPS pour export. "
            "Inclut pour chaque employé : numéro CNPS, salaire brut, "
            "cotisations salariales et patronales calculées selon les taux légaux "
            "Cameroun 2024 (plafond 750 000 XAF/mois). "
            "Accès réservé au RH Manager ou Comptable."
        ),
        tags=["Déclarations Réglementaires"],
        parameters=[
            OpenApiParameter(
                "mois",
                OpenApiTypes.INT,
                description="Mois (1-12). Défaut : mois précédent.",
            ),
            OpenApiParameter(
                "annee",
                OpenApiTypes.INT,
                description="Année. Défaut : année courante.",
            ),
        ],
        responses={
            200: {"description": "Liste des employés avec cotisations CNPS"},
        },
    )
    def get(self, request):
        now = timezone.now()

        # Valeurs par défaut : mois précédent
        mois_precedent = now.month - 1 if now.month > 1 else 12
        annee_precedente = now.year if now.month > 1 else now.year - 1

        try:
            mois = int(request.query_params.get("mois", mois_precedent))
            annee = int(request.query_params.get("annee", annee_precedente))
        except ValueError:
            return Response(
                {
                    "error": {
                        "code": "INVALID_PARAMS",
                        "message": "mois et annee doivent être des entiers.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not (1 <= mois <= 12):
            return Response(
                {
                    "error": {
                        "code": "INVALID_PARAMS",
                        "message": "mois doit être entre 1 et 12.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        tenant_id = request.user.tenant_id

        # Récupérer toutes les fiches de paie du mois (validées ou payées)
        fiches = (
            FichePaie.objects.filter(
                tenant_id=tenant_id,
                mois=mois,
                annee=annee,
                statut__in=[FichePaie.Statut.VALIDE, FichePaie.Statut.PAYE],
            )
            .select_related("employee")
            .order_by("employee__last_name", "employee__first_name")
        )

        lignes = []
        total_part_salariale = Decimal("0")
        total_part_patronale = Decimal("0")

        for fiche in fiches:
            emp = fiche.employee
            cnps = _calculer_cnps(fiche.gross)

            lignes.append(
                {
                    "employe_id": str(emp.id),
                    "matricule": emp.employee_number,
                    "nom_complet": emp.full_name,
                    "numero_cnps": emp.numero_cnps or "N/A",
                    "salaire_brut": float(fiche.gross),
                    "cnps": cnps,
                }
            )
            total_part_salariale += Decimal(str(cnps["part_salariale"]))
            total_part_patronale += Decimal(str(cnps["part_patronale"]))

        # Employés actifs sans fiche de paie ce mois (salaire de base)
        employes_sans_fiche = Employe.objects.filter(
            tenant_id=tenant_id,
            status=Employe.Status.ACTIVE,
        ).exclude(id__in=fiches.values_list("employee_id", flat=True))

        for emp in employes_sans_fiche:
            if emp.salaire_base > 0:
                cnps = _calculer_cnps(emp.salaire_base)
                lignes.append(
                    {
                        "employe_id": str(emp.id),
                        "matricule": emp.employee_number,
                        "nom_complet": emp.full_name,
                        "numero_cnps": emp.numero_cnps or "N/A",
                        "salaire_brut": float(emp.salaire_base),
                        "cnps": cnps,
                        "note": "Fiche de paie non générée ce mois — estimation sur salaire de base",
                    }
                )
                total_part_salariale += Decimal(str(cnps["part_salariale"]))
                total_part_patronale += Decimal(str(cnps["part_patronale"]))

        return Response(
            {
                "periode": f"{annee}-{mois:02d}",
                "mois": mois,
                "annee": annee,
                "nb_employes": len(lignes),
                "totaux": {
                    "part_salariale": float(total_part_salariale),
                    "part_patronale": float(total_part_patronale),
                    "total_cnps": float(total_part_salariale + total_part_patronale),
                },
                "taux": {
                    "salarial": float(CNPS_TAUX_SALARIAL * 100),
                    "patronal": float(CNPS_TAUX_PATRONAL * 100),
                    "plafond_mensuel": float(CNPS_PLAFOND_MENSUEL),
                },
                "employes": lignes,
            },
            status=status.HTTP_200_OK,
        )


# ── DÉCLARATION IRPP ─────────────────────────────────────────────────────────


class DeclarationIRPPView(APIView):
    """
    GET /api/v1/hr/declarations/irpp/

    Calcule l'IRPP mensuel estimé pour chaque employé
    selon le barème DGI Cameroun 2024.

    Utile pour la préparation des fiches de paie
    et la déclaration DIPE annuelle.

    Accès : RH_MANAGER ou COMPTABLE uniquement.
    """

    permission_classes = [IsAuthenticated, IsComptableOrRH]

    @extend_schema(
        summary="Simulation IRPP mensuel",
        description=(
            "Calcule l'IRPP mensuel estimé par employé selon le barème progressif "
            "DGI Cameroun 2024. Inclut la déduction forfaitaire de 30% pour frais "
            "professionnels. Résultat non contractuel — dépend des charges de famille. "
            "Accès réservé au RH Manager ou Comptable."
        ),
        tags=["Déclarations Réglementaires"],
        parameters=[
            OpenApiParameter(
                "mois",
                OpenApiTypes.INT,
                description="Mois (1-12).",
            ),
            OpenApiParameter(
                "annee",
                OpenApiTypes.INT,
                description="Année.",
            ),
        ],
        responses={
            200: {"description": "IRPP estimé par employé"},
        },
    )
    def get(self, request):
        now = timezone.now()
        try:
            mois = int(request.query_params.get("mois", now.month))
            annee = int(request.query_params.get("annee", now.year))
        except ValueError:
            return Response(
                {
                    "error": {
                        "code": "INVALID_PARAMS",
                        "message": "Paramètres invalides.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        tenant_id = request.user.tenant_id

        fiches = (
            FichePaie.objects.filter(
                tenant_id=tenant_id,
                mois=mois,
                annee=annee,
                statut__in=[FichePaie.Statut.VALIDE, FichePaie.Statut.PAYE],
            )
            .select_related("employee")
            .order_by("employee__last_name")
        )

        lignes = []
        total_irpp = 0

        for fiche in fiches:
            irpp = _calculer_irpp_mensuel(fiche.gross)
            lignes.append(
                {
                    "employe_id": str(fiche.employee.id),
                    "matricule": fiche.employee.employee_number,
                    "nom_complet": fiche.employee.full_name,
                    "salaire_brut": float(fiche.gross),
                    "irpp_mensuel_estime": irpp,
                    "irpp_annuel_estime": irpp * 12,
                }
            )
            total_irpp += irpp

        return Response(
            {
                "periode": f"{annee}-{mois:02d}",
                "bareme": "DGI Cameroun 2024 — déduction 30% frais professionnels",
                "tranches": [
                    {"jusqu_a": "2 000 000 XAF/an", "taux": "0%"},
                    {"de": "2 000 001", "jusqu_a": "3 000 000 XAF/an", "taux": "10%"},
                    {"de": "3 000 001", "jusqu_a": "5 000 000 XAF/an", "taux": "15%"},
                    {"au_dela_de": "5 000 000 XAF/an", "taux": "25%"},
                ],
                "total_irpp_mensuel": total_irpp,
                "nb_employes": len(lignes),
                "employes": lignes,
            },
            status=status.HTTP_200_OK,
        )


# ── ATTESTATION DE TRAVAIL ───────────────────────────────────────────────────


def generer_donnees_attestation_travail(employe) -> dict:
    """
    Construit le dictionnaire de données d'une attestation de travail.

    Utilisé par :
    - AttestationTravailView (prévisualisation RH)
    - DemandeDocumentApprouverView (génération officielle)

    Args:
        employe : instance Employe avec select_related("department", "position")

    Returns:
        dict structuré prêt à être rendu en PDF par le frontend
    """
    contrat_actif = (
        employe.contrats.filter(statut="actif").order_by("-start_date").first()
    )

    now = timezone.now()

    return {
        "meta": {
            "date_generation": now.isoformat(),
            "type_document": "Attestation de travail",
        },
        "employe": {
            "matricule": employe.employee_number,
            "nom_complet": employe.full_name,
            "first_name": employe.first_name,
            "last_name": employe.last_name,
            "genre": employe.gender,
            "nationalite": employe.nationality,
            "email": employe.email,
            "phone": employe.phone,
            "numero_cnps": employe.numero_cnps,
        },
        "poste": {
            "titre": employe.position.titre if employe.position else None,
            "departement": employe.department.nom if employe.department else None,
            "type_employe": employe.type_employe,
            "statut": employe.status,
        },
        "contrat": {
            "date_embauche": str(employe.hire_date) if employe.hire_date else None,
            "type_contrat": contrat_actif.type if contrat_actif else None,
            "date_debut_contrat": (
                str(contrat_actif.start_date) if contrat_actif else None
            ),
            "date_fin_contrat": str(contrat_actif.end_date) if contrat_actif else None,
        },
        "remuneration": {
            "salaire_base": float(employe.salaire_base),
            "devise": employe.devise,
            "mode_paiement": employe.mode_paiement,
        },
    }


class AttestationTravailView(APIView):
    """
    GET /api/v1/hr/employes/{pk}/attestation/

    Prévisualisation d'attestation de travail réservée au RH Manager.
    Pour la délivrance officielle, utiliser le workflow DemandeDocument :
    l'employé soumet une demande, le RH approuve et génère le document.

    Accès : RH_MANAGER uniquement.
    """

    permission_classes = [IsAuthenticated, IsRHManager]

    @extend_schema(
        summary="Prévisualisation attestation de travail (RH)",
        description=(
            "Retourne les données brutes d'une attestation de travail pour un employé. "
            "Réservé au RH Manager — pour la délivrance officielle à l'employé, "
            "utiliser le workflow POST /me/demandes-document/ + approbation RH."
        ),
        tags=["Déclarations Réglementaires"],
        responses={
            200: {"description": "Données de l'attestation de travail"},
            403: {"description": "RH Manager requis"},
            404: {"description": "Employé introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            employe = Employe.objects.select_related(
                "department", "position", "manager"
            ).get(pk=pk, tenant_id=request.user.tenant_id)
        except Employe.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Employé introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        return Response(
            generer_donnees_attestation_travail(employe),
            status=status.HTTP_200_OK,
        )

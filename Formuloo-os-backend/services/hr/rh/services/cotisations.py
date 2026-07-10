"""
Calcul automatique des cotisations sociales et fiscales — Cameroun
Conforme au Code du Travail camerounais et aux barèmes CNPS/DGI.

CNPS (Caisse Nationale de Prévoyance Sociale) :
  - Part salariale : 4.2% du salaire brut, plafonné à 750 000 XAF/mois
  - Part patronale : 10.5% (non calculée ici — charge de l'employeur)

IRPP (Impôt sur le Revenu des Personnes Physiques) :
  - Base imposable = brut - CNPS salarié
  - Barème progressif mensuel (Art. 69 CGI Cameroun)
  - Abattement 30% pour frais professionnels (plafonné 500 000/an soit 41 667/mois)
  - Crédit d'impôt parts familiales

Ces valeurs sont les barèmes en vigueur. Ajustable via les constantes.
"""

from decimal import Decimal, ROUND_HALF_UP


# ── Constantes CNPS ─────────────────────────────────────────
CNPS_TAUX_SALARIE = Decimal("0.042")          # 4.2%
CNPS_PLAFOND_MENSUEL = Decimal("750000")       # 750 000 XAF/mois

# ── Constantes IRPP (barème mensuel Cameroun) ────────────────
# Format : (limite_haute, taux)   — limite_haute=None → tranche supérieure
IRPP_TRANCHES_MENSUELLES = [
    (Decimal("41666"),  Decimal("0.10")),   # 0 – 500 000/an
    (Decimal("125000"), Decimal("0.165")),  # 500 001 – 1 500 000/an
    (Decimal("291666"), Decimal("0.275")),  # 1 500 001 – 3 500 000/an
    (Decimal("666666"), Decimal("0.385")),  # 3 500 001 – 8 000 000/an
    (None,              Decimal("0.385")),  # > 8 000 000/an
]

# Abattement frais professionnels : 30%, plafonné à 41 667 XAF/mois
ABATTEMENT_TAUX = Decimal("0.30")
ABATTEMENT_PLAFOND_MENSUEL = Decimal("41667")

# Parts familiales (crédit d'impôt mensuel par part)
VALEUR_PART_MENSUELLE = Decimal("5000")


def calculer_cnps(salaire_brut: Decimal) -> Decimal:
    """Cotisation CNPS salariale (4.2% plafonné à 750 000 XAF brut)."""
    brut = min(salaire_brut, CNPS_PLAFOND_MENSUEL)
    cnps = (brut * CNPS_TAUX_SALARIE).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return cnps


def calculer_irpp(salaire_brut: Decimal, cnps: Decimal, nb_parts: int = 1) -> Decimal:
    """
    Calcule l'IRPP mensuel camerounais.

    Args:
        salaire_brut : salaire brut mensuel
        cnps         : cotisation CNPS salariale du mois
        nb_parts     : nombre de parts fiscales (1 = célibataire sans enfant)
                       Parts standard Cameroun :
                         1   → célibataire / divorcé sans enfant
                         1.5 → marié sans enfant
                         2   → marié + 1 enfant (ou célibataire + 2 enfants)
                         +0.5 par enfant à charge supplémentaire

    Returns:
        IRPP mensuel en XAF (arrondi à l'entier)
    """
    # 1. Base imposable = brut - CNPS
    base_avant_abattement = salaire_brut - cnps

    # 2. Abattement frais professionnels (30%, plafonné)
    abattement = min(
        base_avant_abattement * ABATTEMENT_TAUX,
        ABATTEMENT_PLAFOND_MENSUEL,
    )
    revenu_net_imposable = base_avant_abattement - abattement

    if revenu_net_imposable <= 0:
        return Decimal("0")

    # 3. Calcul progressif par tranches
    impot_brut = Decimal("0")
    tranche_precedente = Decimal("0")

    for limite, taux in IRPP_TRANCHES_MENSUELLES:
        if limite is None:
            # Dernière tranche
            impot_brut += (revenu_net_imposable - tranche_precedente) * taux
            break
        if revenu_net_imposable <= limite:
            impot_brut += (revenu_net_imposable - tranche_precedente) * taux
            break
        impot_brut += (limite - tranche_precedente) * taux
        tranche_precedente = limite

    # 4. Crédit d'impôt parts familiales
    credit_parts = Decimal(str(nb_parts)) * VALEUR_PART_MENSUELLE
    irpp = max(impot_brut - credit_parts, Decimal("0"))

    return irpp.quantize(Decimal("1"), rounding=ROUND_HALF_UP)


def calculer_cotisations(salaire_brut: Decimal, nb_parts: int = 1) -> dict:
    """
    Calcule l'ensemble des cotisations à partir du salaire brut.

    Returns:
        dict avec cotisation_cnps, impot_irpp, salaire_net_apres_cotisations
    """
    brut = Decimal(str(salaire_brut))
    cnps = calculer_cnps(brut)
    irpp = calculer_irpp(brut, cnps, nb_parts)
    return {
        "cotisation_cnps": int(cnps),
        "impot_irpp": int(irpp),
        "total_cotisations": int(cnps + irpp),
    }

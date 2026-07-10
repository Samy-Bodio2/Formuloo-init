"""
Génération PDF des bulletins de paie — Formuloo OS
Utilise ReportLab (pure Python, compatible Docker Toolbox).

Produit un bulletin de paie A4 conforme au Code du Travail camerounais.
"""

import io
from decimal import Decimal
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Table, TableStyle, Spacer, HRFlowable,
)
from reportlab.lib.enums import TA_RIGHT, TA_CENTER, TA_LEFT


def _xaf(val) -> str:
    try:
        return f"{int(Decimal(str(val))):,}".replace(",", " ") + " XAF"
    except Exception:
        return str(val)


def generer_pdf_bulletin_paie(fiche) -> bytes:
    """
    Génère le bulletin de paie en PDF.
    fiche : instance FichePaie avec employee et contrat liés.
    Retourne les bytes du PDF.
    """
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=A4,
        rightMargin=2 * cm,
        leftMargin=2 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
    )

    styles = getSampleStyleSheet()
    story = []

    blue = colors.HexColor("#1a3c5e")
    light = colors.HexColor("#f0f4f8")

    title_style = ParagraphStyle("T", parent=styles["Heading1"], fontSize=16, textColor=blue)
    h2_style = ParagraphStyle("H2", parent=styles["Heading2"], fontSize=11, textColor=blue)
    normal = ParagraphStyle("N", parent=styles["Normal"], fontSize=9)
    right = ParagraphStyle("R", parent=styles["Normal"], fontSize=9, alignment=TA_RIGHT)
    center = ParagraphStyle("C", parent=styles["Normal"], fontSize=9, alignment=TA_CENTER)

    employe = fiche.employee
    periode = f"{fiche.mois:02d}/{fiche.annee}"

    # ── En-tête ──────────────────────────────────────────────
    header_data = [
        [
            Paragraph("<b>FORMULOO OS</b>", title_style),
            Paragraph(f"<b>BULLETIN DE PAIE</b>", title_style),
        ],
        [
            Paragraph("ERP Cloud | Cameroun", normal),
            Paragraph(f"Période : <b>{periode}</b>", right),
        ],
    ]
    ht = Table(header_data, colWidths=[9 * cm, 8 * cm])
    ht.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP")]))
    story.append(ht)
    story.append(HRFlowable(width="100%", thickness=2, color=blue))
    story.append(Spacer(1, 0.4 * cm))

    # ── Infos employé ─────────────────────────────────────────
    story.append(Paragraph("<b>Informations employé</b>", h2_style))
    emp_data = [
        ["Nom", f"{employe.first_name} {employe.last_name}"],
        ["Matricule", employe.employee_number or "—"],
        ["Email", employe.email or "—"],
        ["Situation familiale", getattr(employe, "situation_familiale", "—") or "—"],
        ["Nombre d'enfants", str(getattr(employe, "nombre_enfants", 0) or 0)],
    ]
    if fiche.contrat:
        emp_data.append(["Type de contrat", fiche.contrat.type or "—"])

    emp_table = Table(emp_data, colWidths=[5 * cm, 12 * cm])
    emp_table.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (0, -1), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [colors.white, light]),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
    ]))
    story.append(emp_table)
    story.append(Spacer(1, 0.5 * cm))

    # ── Éléments de rémunération ──────────────────────────────
    story.append(Paragraph("<b>Rémunération</b>", h2_style))

    bonuses = fiche.bonuses or {}
    deductions = fiche.deductions or {}

    remun_data = [["Désignation", "Montant"]]
    remun_data.append(["Salaire de base", _xaf(fiche.salaire_base)])

    for label, val in bonuses.items():
        if val:
            label_fr = label.replace("_", " ").title()
            remun_data.append([f"  + {label_fr}", _xaf(val)])

    heures_supp = fiche.heures_supplementaires or 0
    taux_supp = fiche.taux_horaire_supp or 0
    if heures_supp and taux_supp:
        remun_data.append([
            f"  + Heures supplémentaires ({heures_supp}h × {_xaf(taux_supp)}/h)",
            _xaf(heures_supp * taux_supp),
        ])

    remun_data.append(["<b>Salaire brut</b>", f"<b>{_xaf(fiche.gross)}</b>"])

    remun_table = Table(remun_data, colWidths=[12 * cm, 5 * cm])
    remun_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), blue),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("ALIGN", (1, 0), (-1, -1), "RIGHT"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ROWBACKGROUNDS", (0, 1), (-1, -2), [colors.white, light]),
        ("BACKGROUND", (0, -1), (-1, -1), colors.HexColor("#ddeeff")),
        ("FONTNAME", (0, -1), (-1, -1), "Helvetica-Bold"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#cccccc")),
    ]))
    story.append(remun_table)
    story.append(Spacer(1, 0.4 * cm))

    # ── Déductions ────────────────────────────────────────────
    story.append(Paragraph("<b>Déductions</b>", h2_style))

    deduct_data = [["Désignation", "Montant"]]
    label_map = {
        "cotisation_cnps": "Cotisation CNPS (4.2%)",
        "impot_irpp": "IRPP (barème progressif)",
        "credit_logement": "Crédit logement",
        "autres": "Autres déductions",
    }
    for key, val in deductions.items():
        if val:
            label_fr = label_map.get(key, key.replace("_", " ").title())
            deduct_data.append([f"  − {label_fr}", _xaf(val)])

    total_deductions = sum(Decimal(str(v)) for v in deductions.values() if v)
    deduct_data.append(["<b>Total déductions</b>", f"<b>{_xaf(total_deductions)}</b>"])

    deduct_table = Table(deduct_data, colWidths=[12 * cm, 5 * cm])
    deduct_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#c0392b")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("ALIGN", (1, 0), (-1, -1), "RIGHT"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ROWBACKGROUNDS", (0, 1), (-1, -2), [colors.white, light]),
        ("BACKGROUND", (0, -1), (-1, -1), colors.HexColor("#fde8e6")),
        ("FONTNAME", (0, -1), (-1, -1), "Helvetica-Bold"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#cccccc")),
    ]))
    story.append(deduct_table)
    story.append(Spacer(1, 0.4 * cm))

    # ── Salaire net ───────────────────────────────────────────
    net_data = [
        ["", "SALAIRE NET À PAYER", _xaf(fiche.net_salary)],
    ]
    if fiche.mode_paiement:
        net_data.append(["", f"Mode de paiement : {fiche.mode_paiement}", ""])

    net_table = Table(net_data, colWidths=[5 * cm, 8 * cm, 4 * cm])
    net_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), blue),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, 0), 12),
        ("ALIGN", (2, 0), (2, 0), "RIGHT"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
    ]))
    story.append(net_table)
    story.append(Spacer(1, 1 * cm))

    # ── Pied de page ──────────────────────────────────────────
    footer_style = ParagraphStyle("Footer", parent=styles["Normal"], fontSize=7, textColor=colors.grey, alignment=TA_CENTER)
    story.append(HRFlowable(width="100%", thickness=0.5, color=colors.grey))
    story.append(Spacer(1, 0.2 * cm))
    story.append(Paragraph(
        f"Document généré par Formuloo OS — Bulletin confidentiel — {periode}",
        footer_style,
    ))

    doc.build(story)
    buffer.seek(0)
    return buffer.read()

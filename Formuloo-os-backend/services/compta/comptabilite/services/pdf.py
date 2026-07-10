"""
Génération PDF des factures clients — Formuloo OS
Utilise ReportLab (pure Python, compatible Docker Toolbox).

Produit un PDF A4 conforme aux standards de facturation camerounaise OHADA.
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
    """Formate un montant en XAF."""
    try:
        return f"{Decimal(str(val)):,.0f} XAF".replace(",", " ")
    except Exception:
        return str(val)


def generer_pdf_facture(facture) -> bytes:
    """
    Génère le PDF d'une facture client.
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

    # ── En-tête ──────────────────────────────────────────────
    title_style = ParagraphStyle(
        "Title", parent=styles["Heading1"], fontSize=18, textColor=colors.HexColor("#1a3c5e")
    )
    sub_style = ParagraphStyle(
        "Sub", parent=styles["Normal"], fontSize=10, textColor=colors.grey
    )
    right_style = ParagraphStyle(
        "Right", parent=styles["Normal"], alignment=TA_RIGHT, fontSize=10
    )

    header_data = [
        [
            Paragraph("<b>FORMULOO OS</b>", title_style),
            Paragraph(f"<b>FACTURE N° {facture.numero}</b>", title_style),
        ],
        [
            Paragraph("ERP Cloud pour PME africaines<br/>Cameroun", sub_style),
            Paragraph(
                f"Date d'émission : {facture.date_emission.strftime('%d/%m/%Y') if facture.date_emission else '—'}<br/>"
                f"Échéance : {facture.date_echeance.strftime('%d/%m/%Y') if facture.date_echeance else '—'}",
                right_style,
            ),
        ],
    ]
    header_table = Table(header_data, colWidths=[9 * cm, 8 * cm])
    header_table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(header_table)
    story.append(HRFlowable(width="100%", thickness=2, color=colors.HexColor("#1a3c5e")))
    story.append(Spacer(1, 0.5 * cm))

    # ── Client ────────────────────────────────────────────────
    client_style = ParagraphStyle("Client", parent=styles["Normal"], fontSize=10)
    story.append(Paragraph("<b>Facturé à :</b>", client_style))
    story.append(Paragraph(f"<b>{facture.client_nom}</b>", client_style))
    if facture.client_email:
        story.append(Paragraph(facture.client_email, client_style))
    story.append(Spacer(1, 0.5 * cm))

    # ── Lignes de facturation ─────────────────────────────────
    lignes_data = [
        ["Description", "Qté", "Prix unitaire", "Total HT"],
    ]
    for ligne in facture.lignes.all():
        total = ligne.quantite * ligne.prix_unitaire
        lignes_data.append([
            ligne.description,
            str(ligne.quantite),
            _xaf(ligne.prix_unitaire),
            _xaf(total),
        ])

    lignes_table = Table(
        lignes_data,
        colWidths=[9 * cm, 2 * cm, 3.5 * cm, 3 * cm],
    )
    lignes_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1a3c5e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, 0), 10),
        ("ALIGN", (1, 0), (-1, -1), "RIGHT"),
        ("ALIGN", (0, 0), (0, -1), "LEFT"),
        ("FONTSIZE", (0, 1), (-1, -1), 9),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f0f4f8")]),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cccccc")),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(lignes_table)
    story.append(Spacer(1, 0.5 * cm))

    # ── Totaux ────────────────────────────────────────────────
    taux_tva = facture.tva_taux or 0
    totaux_data = [
        ["", "Montant HT", _xaf(facture.montant_ht)],
        ["", f"TVA ({taux_tva}%)", _xaf(facture.tva)],
        ["", "TOTAL TTC", _xaf(facture.montant_ttc)],
    ]
    totaux_table = Table(totaux_data, colWidths=[9 * cm, 4 * cm, 4 * cm])
    totaux_table.setStyle(TableStyle([
        ("ALIGN", (1, 0), (-1, -1), "RIGHT"),
        ("FONTSIZE", (0, 0), (-1, -1), 10),
        ("FONTNAME", (0, 2), (-1, 2), "Helvetica-Bold"),
        ("BACKGROUND", (0, 2), (-1, 2), colors.HexColor("#1a3c5e")),
        ("TEXTCOLOR", (0, 2), (-1, 2), colors.white),
        ("LINEABOVE", (1, 0), (-1, 0), 0.5, colors.HexColor("#cccccc")),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(totaux_table)
    story.append(Spacer(1, 1 * cm))

    # ── Pied de page ──────────────────────────────────────────
    footer_style = ParagraphStyle(
        "Footer", parent=styles["Normal"], fontSize=8, textColor=colors.grey, alignment=TA_CENTER
    )
    story.append(HRFlowable(width="100%", thickness=0.5, color=colors.grey))
    story.append(Spacer(1, 0.3 * cm))
    story.append(Paragraph(
        "Merci de votre confiance. Paiement à réception de facture. "
        "Tout retard de paiement entraîne des pénalités conformément aux CGV.",
        footer_style,
    ))
    story.append(Paragraph(
        "Formuloo OS — ERP Cloud | contact@formuloo.cm | www.formuloo.cm",
        footer_style,
    ))

    doc.build(story)
    buffer.seek(0)
    return buffer.read()

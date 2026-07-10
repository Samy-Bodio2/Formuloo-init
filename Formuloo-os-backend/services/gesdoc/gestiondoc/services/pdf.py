"""
Génération du certificat PDF de certification blockchain — Formuloo OS
Utilise ReportLab (pure Python, compatible Docker Toolbox).
"""

import io

from django.conf import settings
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    HRFlowable, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle,
)


def generer_certificat_pdf(document) -> bytes:
    """Génère le certificat PDF de certification blockchain d'un document."""
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
    title_style = ParagraphStyle(
        "Title", parent=styles["Heading1"], fontSize=18,
        alignment=TA_CENTER, textColor=colors.HexColor("#1a3c5e"),
    )
    sub_style = ParagraphStyle(
        "Sub", parent=styles["Normal"], fontSize=10,
        alignment=TA_CENTER, textColor=colors.grey,
    )

    story = [
        Paragraph("CERTIFICAT DE CERTIFICATION BLOCKCHAIN", title_style),
        Paragraph("Formuloo OS — Module GesDoc", sub_style),
        Spacer(1, 1 * cm),
        HRFlowable(width="100%", color=colors.HexColor("#1a3c5e")),
        Spacer(1, 0.5 * cm),
    ]

    rows = [
        ["Document", document.document_number or str(document.id)],
        ["Fournisseur", document.supplier or "—"],
        ["Type", document.get_document_type_display()],
        ["Hash SHA-256", document.hash_sha256],
        ["Transaction blockchain", document.tx_hash or "—"],
        ["Numéro de bloc", str(document.block_number or "—")],
        ["Réseau", f"{document.blockchain} ({settings.BLOCKCHAIN_NETWORK})"],
        ["Ancré le", document.anchored_at.strftime("%d/%m/%Y %H:%M:%S UTC") if document.anchored_at else "—"],
    ]
    table = Table(rows, colWidths=[5 * cm, 10 * cm])
    table.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (0, -1), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.lightgrey),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ROWBACKGROUNDS", (0, 0), (-1, -1), [colors.whitesmoke, colors.white]),
    ]))
    story.append(table)
    story.append(Spacer(1, 1 * cm))
    story.append(Paragraph(
        "Ce document atteste que l'empreinte cryptographique (hash SHA-256) "
        "de la pièce comptable ci-dessus a été ancrée sur la blockchain "
        "Ethereum et peut être vérifiée publiquement via Etherscan.",
        styles["Normal"],
    ))

    doc.build(story)
    return buffer.getvalue()

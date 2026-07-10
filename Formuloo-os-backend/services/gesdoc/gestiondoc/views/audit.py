import csv
import io

from django.http import HttpResponse
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from gestiondoc.models import DocumentAuditLog
from gestiondoc.pagination import paginate_by_cursor
from gestiondoc.permissions import CanReadAudit

_EXPORT_HEADERS = ["Horodatage", "Action", "Libellé", "Document", "N° pièce", "Détail", "Acteur", "Utilisateur"]
_MAX_EXPORT_ROWS = 5000


def _build_qs(tenant_id, params):
    qs = DocumentAuditLog.objects.filter(tenant_id=tenant_id).select_related("document")

    category = params.get("category")
    if category == "system":
        qs = qs.filter(actor_type=DocumentAuditLog.ActorType.SYSTEM)
    elif category == "user":
        qs = qs.filter(actor_type=DocumentAuditLog.ActorType.USER)
    elif category == "alerts":
        qs = qs.filter(action=DocumentAuditLog.Action.INTEGRITY_ALERT)

    document_id = params.get("document_id")
    if document_id:
        qs = qs.filter(document_id=document_id)

    user_id = params.get("user_id")
    if user_id:
        qs = qs.filter(user_id=user_id)

    action = params.get("action")
    if action:
        qs = qs.filter(action=action)

    date_from = params.get("date_from")
    if date_from:
        qs = qs.filter(timestamp__date__gte=date_from)

    date_to = params.get("date_to")
    if date_to:
        qs = qs.filter(timestamp__date__lte=date_to)

    return qs


def _entry_to_row(entry):
    return [
        entry.timestamp.strftime("%d/%m/%Y %H:%M:%S"),
        entry.action,
        entry.label,
        str(entry.document_id),
        entry.document.document_number or "—",
        entry.detail or "—",
        entry.actor_type,
        str(entry.user_id) if entry.user_id else "—",
    ]


def _export_csv(qs) -> HttpResponse:
    response = HttpResponse(content_type="text/csv; charset=utf-8")
    response["Content-Disposition"] = 'attachment; filename="audit_log.csv"'
    response.write("﻿")  # BOM UTF-8 pour Excel
    writer = csv.writer(response)
    writer.writerow(_EXPORT_HEADERS)
    for entry in qs[:_MAX_EXPORT_ROWS]:
        writer.writerow(_entry_to_row(entry))
    return response


def _export_pdf(qs) -> HttpResponse:
    buf = io.BytesIO()
    doc = SimpleDocTemplate(
        buf,
        pagesize=landscape(A4),
        rightMargin=1.5 * cm,
        leftMargin=1.5 * cm,
        topMargin=1.5 * cm,
        bottomMargin=1.5 * cm,
    )
    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        "Title", parent=styles["Heading1"], fontSize=14,
        alignment=TA_CENTER, textColor=colors.HexColor("#1a3c5e"),
    )

    data = [_EXPORT_HEADERS]
    for entry in qs[:_MAX_EXPORT_ROWS]:
        data.append(_entry_to_row(entry))

    col_widths = [3.5 * cm, 3 * cm, 4.5 * cm, 4 * cm, 2.5 * cm, 5 * cm, 2 * cm, 3.5 * cm]
    table = Table(data, colWidths=col_widths, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1a3c5e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 7),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.lightgrey),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.whitesmoke, colors.white]),
        ("WORDWRAP", (0, 0), (-1, -1), True),
    ]))

    doc.build([
        Paragraph("Journal d'audit — Formuloo OS GesDoc", title_style),
        Spacer(1, 0.5 * cm),
        table,
    ])

    response = HttpResponse(buf.getvalue(), content_type="application/pdf")
    response["Content-Disposition"] = 'attachment; filename="audit_log.pdf"'
    return response


class AuditLogView(APIView):
    """GET /documents/audit-log/ — journal d'audit complet."""

    permission_classes = [IsAuthenticated, CanReadAudit]

    def get(self, request):
        tenant_id = request.user.tenant_id
        qs = _build_qs(tenant_id, request.query_params)

        export = request.query_params.get("export")
        if export == "csv":
            return _export_csv(qs)
        if export == "pdf":
            return _export_pdf(qs)

        cursor = request.query_params.get("cursor")
        results, next_cursor = paginate_by_cursor(qs, cursor, 20, time_field="timestamp")

        return Response({
            "results": [
                {
                    "id": str(entry.id),
                    "action": entry.action,
                    "label": entry.label,
                    "document_id": str(entry.document_id),
                    "document_number": entry.document.document_number,
                    "detail": entry.detail,
                    "actor_type": entry.actor_type,
                    "user_id": str(entry.user_id) if entry.user_id else None,
                    "timestamp": entry.timestamp,
                }
                for entry in results
            ],
            "next_cursor": next_cursor,
        })

"""
Export PDF des factures clients — Formuloo OS
GET /api/v1/compta/factures/{id}/pdf/
"""

from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import NotFound

from comptabilite.models import Facture
from comptabilite.permissions import CanReadFactures
from comptabilite.services.pdf import generer_pdf_facture


class FacturePDFView(APIView):
    """
    GET /factures/{id}/pdf/
    Retourne le PDF de la facture en Content-Type application/pdf.
    La facture doit être dans le statut EMISE ou PAYEE.
    """

    permission_classes = [IsAuthenticated, CanReadFactures]

    def get(self, request, pk):
        try:
            facture = Facture.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except Facture.DoesNotExist:
            raise NotFound()

        if facture.statut == Facture.Statut.BROUILLON:
            from rest_framework.response import Response
            from rest_framework import status
            return Response(
                {"error": {"code": "BROUILLON", "message": "Impossible de générer le PDF d'un brouillon."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        pdf_bytes = generer_pdf_facture(facture)

        response = HttpResponse(pdf_bytes, content_type="application/pdf")
        response["Content-Disposition"] = f'attachment; filename="facture_{facture.numero}.pdf"'
        response["Content-Length"] = len(pdf_bytes)
        return response

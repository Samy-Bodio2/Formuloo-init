"""
Export PDF des bulletins de paie — Formuloo OS
GET /api/v1/hr/payroll/{id}/pdf/
"""

from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import NotFound

from rh.models import FichePaie
from rh.permissions import IsOwnerOrRH
from rh.services.pdf import generer_pdf_bulletin_paie


class PayrollPDFView(APIView):
    """
    GET /payroll/{id}/pdf/
    Génère et télécharge le bulletin de paie en PDF.
    Accessible à l'employé concerné ou à un RH/comptable.
    """

    permission_classes = [IsAuthenticated, IsOwnerOrRH]

    def get(self, request, pk):
        try:
            fiche = FichePaie.objects.select_related("employee", "contrat").get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except FichePaie.DoesNotExist:
            raise NotFound()

        # Vérifier ownership (l'employé ne peut voir que ses propres fiches)
        self.check_object_permissions(request, fiche)

        if fiche.statut == FichePaie.Statut.BROUILLON:
            from rest_framework.response import Response
            from rest_framework import status
            return Response(
                {"error": {"code": "BROUILLON", "message": "Le bulletin brouillon ne peut pas être téléchargé."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        pdf_bytes = generer_pdf_bulletin_paie(fiche)

        filename = f"bulletin_paie_{fiche.employee.employee_number}_{fiche.annee}{fiche.mois:02d}.pdf"
        response = HttpResponse(pdf_bytes, content_type="application/pdf")
        response["Content-Disposition"] = f'attachment; filename="{filename}"'
        response["Content-Length"] = len(pdf_bytes)
        return response

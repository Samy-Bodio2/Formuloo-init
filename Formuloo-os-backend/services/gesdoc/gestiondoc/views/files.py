import io
import mimetypes

from django.http import FileResponse, Http404
from rest_framework.permissions import AllowAny
from rest_framework.views import APIView

from gestiondoc.services import storage


class SignedFileView(APIView):
    """
    GET /documents/files/{token}/ — sert un fichier stocké localement.

    Équivalent local d'une pre-signed URL S3 (choix d'architecture : volume
    Docker plutôt que S3/MinIO). Le token encode déjà l'autorisation
    d'accès et expire après SIGNED_URL_EXPIRY_SECONDS — pas de JWT requis ici.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    def get(self, request, token):
        relative_path = storage.resolve_signed_token(token)
        if relative_path is None:
            raise Http404("Lien expiré ou invalide.")

        try:
            content = storage.read_file(relative_path)
        except FileNotFoundError:
            raise Http404("Fichier introuvable.")

        content_type, _ = mimetypes.guess_type(relative_path)
        return FileResponse(
            io.BytesIO(content), content_type=content_type or "application/octet-stream",
        )

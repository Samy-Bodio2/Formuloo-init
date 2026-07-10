"""
Tests fichier signé — Formuloo OS Service GesDoc
GET /documents/files/{token}/ — sert un fichier stocké localement via
un token signé (équivalent local d'une pre-signed URL S3).
"""

import pytest

from gestiondoc.services import storage


@pytest.mark.django_db
class TestSignedFile:

    def test_signed_file_serves_content(self, client_anonyme, document_pending):
        url = storage.generate_signed_url(document_pending.file_path)
        resp = client_anonyme.get(url)

        assert resp.status_code == 200
        assert b"".join(resp.streaming_content) == b"%PDF-1.4 contenu factice"

    def test_signed_file_invalid_token_returns_404(self, client_anonyme):
        resp = client_anonyme.get("/api/v1/documents/files/not-a-valid-token/")
        assert resp.status_code == 404

    def test_signed_file_no_auth_required(self, client_anonyme, document_pending):
        """Le token encode déjà l'autorisation — pas de JWT requis sur cette route."""
        url = storage.generate_signed_url(document_pending.file_path)
        resp = client_anonyme.get(url)
        assert resp.status_code == 200

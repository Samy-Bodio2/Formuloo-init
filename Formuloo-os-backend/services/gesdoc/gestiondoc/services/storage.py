"""
Stockage des fichiers — Formuloo OS Service GesDoc

Volume Docker local (pas de S3) : les fichiers sont écrits sous
MEDIA_ROOT/{tenant_id}/... et ne sont jamais servis directement — le
backend génère des URLs signées à durée limitée, même sémantique que
les "pre-signed URLs" S3 évoquées dans le contrat.
"""

import hashlib
import os
import uuid

from django.conf import settings
from django.core import signing

SIGNED_URL_SALT = "gestiondoc.storage.signed_url"

ALLOWED_CONTENT_TYPES = {
    "application/pdf": "pdf",
    "image/jpeg": "jpg",
    "image/png": "png",
}


def compute_sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def save_raw_upload(tenant_id, document_id, uploaded_file) -> tuple[str, str]:
    """
    Sauvegarde le fichier brut sous MEDIA_ROOT/raw/{tenant_id}/{document_id}.{ext}.
    Retourne (chemin_relatif, hash_sha256).
    """
    ext = ALLOWED_CONTENT_TYPES.get(uploaded_file.content_type, "bin")
    relative_path = f"raw/{tenant_id}/{document_id}.{ext}"
    absolute_path = os.path.join(settings.MEDIA_ROOT, relative_path)
    os.makedirs(os.path.dirname(absolute_path), exist_ok=True)

    content = uploaded_file.read()
    with open(absolute_path, "wb") as fh:
        fh.write(content)

    return relative_path, compute_sha256(content)


def save_derived_file(tenant_id, document_id, subdir, filename, content: bytes) -> str:
    """Sauvegarde un fichier dérivé (preview, certificat PDF) et retourne son chemin relatif."""
    relative_path = f"{subdir}/{tenant_id}/{filename}"
    absolute_path = os.path.join(settings.MEDIA_ROOT, relative_path)
    os.makedirs(os.path.dirname(absolute_path), exist_ok=True)
    with open(absolute_path, "wb") as fh:
        fh.write(content)
    return relative_path


def read_file(relative_path: str) -> bytes:
    absolute_path = os.path.join(settings.MEDIA_ROOT, relative_path)
    with open(absolute_path, "rb") as fh:
        return fh.read()


def generate_signed_url(relative_path: str, request=None) -> str:
    """
    Génère une URL signée à expiration (SIGNED_URL_EXPIRY_SECONDS) pointant
    vers GET /documents/files/<token>/ — équivalent local d'une pre-signed URL S3.
    """
    if not relative_path:
        return ""
    token = signing.dumps(relative_path, salt=SIGNED_URL_SALT)
    path = f"/api/v1/documents/files/{token}/"
    if request is not None:
        return request.build_absolute_uri(path)
    return path


def resolve_signed_token(token: str) -> str | None:
    """Retourne le chemin relatif si le token est valide et non expiré, sinon None."""
    try:
        return signing.loads(
            token, salt=SIGNED_URL_SALT, max_age=settings.SIGNED_URL_EXPIRY_SECONDS,
        )
    except signing.BadSignature:
        return None


def new_document_id() -> uuid.UUID:
    return uuid.uuid4()

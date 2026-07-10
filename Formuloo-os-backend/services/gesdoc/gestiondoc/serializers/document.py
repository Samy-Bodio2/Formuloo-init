from rest_framework import serializers

from gestiondoc.models import AccountingDocument

ALLOWED_CONTENT_TYPES = ("application/pdf", "image/jpeg", "image/png")


class UploadSerializer(serializers.Serializer):
    file = serializers.FileField()
    document_type = serializers.ChoiceField(choices=AccountingDocument.DocumentType.choices)
    fiscal_year = serializers.CharField(required=False, allow_blank=True, max_length=4)
    notes = serializers.CharField(required=False, allow_blank=True)

    def validate_file(self, uploaded_file):
        from django.conf import settings

        if uploaded_file.content_type not in ALLOWED_CONTENT_TYPES:
            raise serializers.ValidationError(
                "Type de fichier invalide — PDF, JPEG ou PNG uniquement."
            )
        if uploaded_file.size > settings.DOCUMENT_MAX_UPLOAD_SIZE:
            raise serializers.ValidationError("Fichier trop volumineux (max 20 Mo).")
        return uploaded_file

from rest_framework import serializers


class LinkJournalEntrySerializer(serializers.Serializer):
    journal_entry_id = serializers.IntegerField()

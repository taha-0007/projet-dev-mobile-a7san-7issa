from rest_framework import serializers
from .models import Document, Favori, Historique

class DocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Document
        fields = '__all__'
        extra_kwargs = {
            'fichier': {'required': False}   # ← permet les mises à jour sans fichier
        }

class FavoriSerializer(serializers.ModelSerializer):
    document = DocumentSerializer(read_only=True)
    document_id = serializers.PrimaryKeyRelatedField(
        queryset=Document.objects.all(),
        write_only=True,
        source='document'
    )

    class Meta:
        model = Favori
        fields = ('id', 'document', 'document_id', 'date_ajout')

class HistoriqueSerializer(serializers.ModelSerializer):
    document = DocumentSerializer(read_only=True)
    class Meta:
        model = Historique
        fields = ('id', 'document', 'date_consultation')
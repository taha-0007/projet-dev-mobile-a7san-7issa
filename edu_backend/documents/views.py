from rest_framework import generics, permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView
from django.http import FileResponse
from django.db.models import Count, Q
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import timedelta
from django.db.models import Sum
import os
from .models import Document, Favori, Historique
from .serializers import DocumentSerializer, FavoriSerializer, HistoriqueSerializer
from django.db.models import Count
User = get_user_model()

ACADEMIC_STRUCTURE = {
    "Sciences Mathématiques A (SM A)": {
        "langues": ["ar", "fr"],
        "matieres": [
            "Mathématiques", "Mathématiques (BIOF)",
            "Physique et Chimie", "Physique et Chimie (BIOF)",
            "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
            "Anglais", "Philosophie"
        ]
    },
    "Sciences Mathématiques B (SM B)": {
        "langues": ["ar", "fr"],
        "matieres": [
            "Mathématiques", "Mathématiques (BIOF)",
            "Physique et Chimie", "Physique et Chimie (BIOF)",
            "Sciences de l'Ingénieur",
            "Anglais", "Philosophie"
        ]
    },
    "Sciences Physiques (PC)": {
        "langues": ["ar", "fr"],
        "matieres": [
            "Mathématiques", "Mathématiques (BIOF)",
            "Physique et Chimie", "Physique et Chimie (BIOF)",
            "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
            "Anglais", "Philosophie"
        ]
    },
    "Sciences de la Vie et de la Terre (SVT)": {
        "langues": ["ar", "fr"],
        "matieres": [
            "Mathématiques", "Mathématiques (BIOF)",
            "Physique et Chimie", "Physique et Chimie (BIOF)",
            "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
            "Anglais", "Philosophie"
        ]
    },
    "Sciences Économiques (ECO)": {
        "langues": ["fr"],  # uniquement français
        "matieres": [
            "Mathématiques appliquées", "Économie générale",
            "Comptabilité", "Gestion", "Anglais", "Philosophie"
        ]
    }
}

class AcademicStructureView(APIView):
    permission_classes = [permissions.AllowAny]
    def get(self, request):
        return Response(ACADEMIC_STRUCTURE)

class FilterOptionsView(APIView):
    permission_classes = [permissions.AllowAny]
    def get(self, request):
        filiere = request.query_params.get('filiere')
        langue = request.query_params.get('langue')
        annee = request.query_params.get('annee')
        matiere = request.query_params.get('matiere')
        type_doc = request.query_params.get('type')
        queryset = Document.objects.all()
        if filiere:
            queryset = queryset.filter(filiere=filiere)
        if langue:
            queryset = queryset.filter(langue=langue)
        if annee:
            queryset = queryset.filter(annee=annee)
        if matiere:
            queryset = queryset.filter(matiere=matiere)
        if type_doc:
            queryset = queryset.filter(type_document=type_doc)
        data = {
            'filieres': sorted(queryset.values_list('filiere', flat=True).distinct()),
            'langues': sorted(queryset.values_list('langue', flat=True).distinct()),
            'annees': sorted(queryset.values_list('annee', flat=True).distinct()),
            'matieres': sorted(queryset.values_list('matiere', flat=True).distinct()),
            'types': sorted(queryset.values_list('type_document', flat=True).distinct()),
        }
        return Response(data)

class DocumentListView(generics.ListAPIView):
    serializer_class = DocumentSerializer
    permission_classes = [permissions.AllowAny]
    def get_queryset(self):
        queryset = Document.objects.all()
        filiere = self.request.query_params.get('filiere')
        langue = self.request.query_params.get('langue')
        annee = self.request.query_params.get('annee')
        matiere = self.request.query_params.get('matiere')
        type_doc = self.request.query_params.get('type')
        search = self.request.query_params.get('search')
        if filiere:
            queryset = queryset.filter(filiere=filiere)
        if langue:
            queryset = queryset.filter(langue=langue)
        if annee:
            queryset = queryset.filter(annee=annee)
        if matiere:
            queryset = queryset.filter(matiere=matiere)
        if type_doc:
            queryset = queryset.filter(type_document=type_doc)
        if search:
            queryset = queryset.filter(
                Q(titre__icontains=search) |
                Q(description__icontains=search) |
                Q(matiere__icontains=search)
            )
        return queryset

class DocumentDetailView(generics.RetrieveAPIView):
    queryset = Document.objects.all()
    serializer_class = DocumentSerializer
    permission_classes = [permissions.AllowAny]
    def retrieve(self, request, *args, **kwargs):
        instance = self.get_object()
        instance.vues += 1
        instance.save(update_fields=['vues'])
        if request.user.is_authenticated:
            Historique.objects.create(utilisateur=request.user, document=instance)
        return super().retrieve(request, *args, **kwargs)

class DocumentDownloadView(APIView):
    permission_classes = [permissions.AllowAny]
    def get(self, request, pk):
        try:
            document = Document.objects.get(pk=pk)
        except Document.DoesNotExist:
            return Response({"error": "Document introuvable"}, status=status.HTTP_404_NOT_FOUND)
        document.telechargements += 1
        document.save(update_fields=['telechargements'])
        file_path = document.fichier.path
        response = FileResponse(open(file_path, 'rb'))
        response['Content-Disposition'] = f'attachment; filename="{os.path.basename(file_path)}"'
        return response

class FavoriListCreateView(generics.ListCreateAPIView):
    serializer_class = FavoriSerializer
    permission_classes = [permissions.IsAuthenticated]
    def get_queryset(self):
        return Favori.objects.filter(utilisateur=self.request.user).select_related('document')
    def perform_create(self, serializer):
        serializer.save(utilisateur=self.request.user)

class FavoriDeleteView(generics.DestroyAPIView):
    serializer_class = FavoriSerializer
    permission_classes = [permissions.IsAuthenticated]
    def get_queryset(self):
        return Favori.objects.filter(utilisateur=self.request.user)
class HistoriqueListView(generics.ListAPIView):
    serializer_class = HistoriqueSerializer
    permission_classes = [permissions.IsAuthenticated]
    pagination_class = None   # <-- désactive la pagination

    def get_queryset(self):
        return Historique.objects.filter(utilisateur=self.request.user) \
            .select_related('document') \
            .order_by('-date_consultation')[:50]

class DocumentCreateView(generics.CreateAPIView):
    queryset = Document.objects.all()
    serializer_class = DocumentSerializer
    permission_classes = [permissions.IsAdminUser]

class DocumentUpdateDeleteView(generics.RetrieveUpdateDestroyAPIView):
    queryset = Document.objects.all()
    serializer_class = DocumentSerializer
    permission_classes = [permissions.IsAdminUser]
    lookup_field = 'pk'

class AdminStatsView(APIView):
    permission_classes = [permissions.IsAdminUser]
    def get(self, request):
        now = timezone.now()
        last_week = now - timedelta(days=7)
        total_users = User.objects.count()
        total_docs = Document.objects.count()
        new_users_week = User.objects.filter(date_joined__gte=last_week).count()
        new_docs_week = Document.objects.filter(date_ajout__gte=last_week).count()

        popular_docs = Document.objects.annotate(
            nb_vues=Count('historique')
        ).order_by('-nb_vues')[:10].values('id', 'titre', 'filiere', 'nb_vues')

        top_downloads = Document.objects.order_by('-telechargements')[:10].values('id', 'titre', 'telechargements')

        recent_activites = Historique.objects.select_related('document', 'utilisateur')\
            .order_by('-date_consultation')[:10]\
            .values('utilisateur__username', 'document__titre', 'date_consultation')

        return Response({
            "total_utilisateurs": total_users,
            "total_documents": total_docs,
            "nouveaux_utilisateurs_cette_semaine": new_users_week,
            "nouveaux_documents_cette_semaine": new_docs_week,
            "documents_populaires": list(popular_docs),
            "documents_les_plus_telecharges": list(top_downloads),
            "activites_recentes": list(recent_activites),
        })

class AdminDocumentListView(generics.ListAPIView):
    serializer_class = DocumentSerializer
    permission_classes = [permissions.IsAdminUser]
    pagination_class = None
    def get_queryset(self):
        queryset = Document.objects.all().order_by('-date_ajout')



class StatsByFiliereView(APIView):
    permission_classes = [permissions.IsAdminUser]
    def get(self, request):
        data = Document.objects.values('filiere').annotate(total=Count('id')).order_by('filiere')
        return Response(data)
    


class StatsByMatiereView(APIView):
    permission_classes = [permissions.IsAdminUser]
    def get(self, request):
        # Regroupe les vues par matière (toutes filières confondues)
        data = Historique.objects.values('document__matiere') \
            .annotate(total_vues=Count('id')) \
            .order_by('-total_vues')[:10]
        return Response(data)
from django.urls import path
from . import views

urlpatterns = [
    path('structure/', views.AcademicStructureView.as_view(), name='academic-structure'),
    path('options/', views.FilterOptionsView.as_view(), name='filter-options'),
    path('', views.DocumentListView.as_view(), name='document-list'),
    path('<int:pk>/', views.DocumentDetailView.as_view(), name='document-detail'),
    path('<int:pk>/download/', views.DocumentDownloadView.as_view(), name='document-download'),
    path('favoris/', views.FavoriListCreateView.as_view(), name='favori-list-create'),
    path('favoris/<int:pk>/', views.FavoriDeleteView.as_view(), name='favori-delete'),
    path('historique/', views.HistoriqueListView.as_view(), name='historique-list'),
    # Admin
    path('admin/', views.AdminDocumentListView.as_view(), name='admin-document-list'),
    path('admin/create/', views.DocumentCreateView.as_view(), name='document-create'),
    path('admin/<int:pk>/', views.DocumentUpdateDeleteView.as_view(), name='document-update-delete'),
    path('admin-stats/', views.AdminStatsView.as_view(), name='admin-stats'),
    path('admin-stats-by-filiere/', views.StatsByFiliereView.as_view()),
    path('admin-stats-by-matiere/', views.StatsByMatiereView.as_view(), name='admin-stats-by-matiere'),
]
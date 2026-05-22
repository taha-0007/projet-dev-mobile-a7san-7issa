from django.db import models
from django.conf import settings

class Document(models.Model):
    DOC_TYPE_CHOICES = [
        ('cours', 'Cours'),
        ('national', 'Examen National'),
        ('rattrapage', 'Rattrapage'),
    ]
    titre = models.CharField(max_length=255)
    description = models.TextField(blank=True)
    filiere = models.CharField(max_length=100)
    langue = models.CharField(max_length=50)
    annee = models.CharField(max_length=20)
    matiere = models.CharField(max_length=100)
    type_document = models.CharField(max_length=20, choices=DOC_TYPE_CHOICES)
    fichier = models.FileField(upload_to='pdfs/')
    date_ajout = models.DateTimeField(auto_now_add=True)
    vues = models.PositiveIntegerField(default=0)
    telechargements = models.PositiveIntegerField(default=0)

    def __str__(self):
        return self.titre

class Favori(models.Model):
    utilisateur = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='favoris')
    document = models.ForeignKey(Document, on_delete=models.CASCADE)
    date_ajout = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('utilisateur', 'document')

class Historique(models.Model):
    utilisateur = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='historique')
    document = models.ForeignKey(Document, on_delete=models.CASCADE)
    date_consultation = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-date_consultation']
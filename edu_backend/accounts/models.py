from django.contrib.auth.models import AbstractUser
from django.db import models

class User(AbstractUser):
    filiere = models.CharField(max_length=100, blank=True, null=True)
    langue = models.CharField(max_length=50, blank=True, null=True)
    avatar = models.ImageField(upload_to='avatars/', blank=True, null=True)

    def __str__(self):
        return self.username
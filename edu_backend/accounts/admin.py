from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User

class CustomUserAdmin(UserAdmin):
    model = User
    list_display = ['username', 'email', 'filiere', 'langue', 'is_staff', 'is_active', 'date_joined']
    list_filter = ['filiere', 'is_active', 'date_joined']
    fieldsets = UserAdmin.fieldsets + (
        ('Infos étudiant', {'fields': ('filiere', 'langue')}),
    )
    add_fieldsets = UserAdmin.add_fieldsets + (
        ('Infos étudiant', {'fields': ('filiere', 'langue')}),
    )

admin.site.register(User, CustomUserAdmin)
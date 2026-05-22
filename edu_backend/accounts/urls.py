from django.urls import path
from .views import (
    SignupView, LoginView, LogoutView, ProfileView,
    UserListView, UserDeleteView, UserUpdateView,
    FirebaseLoginView
)

urlpatterns = [
    path('signup/', SignupView.as_view(), name='signup'),
    path('login/', LoginView.as_view(), name='login'),
    path('logout/', LogoutView.as_view(), name='logout'),
    path('profile/', ProfileView.as_view(), name='profile'),
    path('users/', UserListView.as_view(), name='user-list'),
    path('users/<int:pk>/delete/', UserDeleteView.as_view(), name='user-delete'),
    path('users/<int:pk>/update/', UserUpdateView.as_view(), name='user-update'),
    path('firebase-login/', FirebaseLoginView.as_view(), name='firebase-login'),
]
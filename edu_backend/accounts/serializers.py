from rest_framework import serializers
from django.contrib.auth import get_user_model
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

User = get_user_model()

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ('id', 'username', 'email', 'password', 'filiere', 'langue', 'is_staff', 'avatar')
        extra_kwargs = {
            'password': {'write_only': True},
            'is_staff': {'read_only': True},
            'avatar': {'required': False}   # optionnel
        }

    def create(self, validated_data):
        password = validated_data.pop('password')
        user = User.objects.create_user(**validated_data, password=password)
        return user

class MyTokenObtainPairSerializer(TokenObtainPairSerializer):
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token['username'] = user.username
        token['email'] = user.email
        token['filiere'] = user.filiere
        token['is_staff'] = user.is_staff
        token['avatar'] = user.avatar.url if user.avatar else None
        return token
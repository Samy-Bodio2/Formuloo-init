"""
Modèle User — Formuloo OS
Utilisateur d'une PME cliente.
Conforme ADR-002 : authentification SSO + multi-tenant
"""

import uuid
from django.db import models
from django.contrib.auth.models import (
    AbstractBaseUser,
    BaseUserManager,
    PermissionsMixin,
)


class UserManager(BaseUserManager):
    """
    Manager personnalisé — login par email (pas username)
    """

    def create_user(self, email, tenant, password=None, **extra_fields):
        if not email:
            raise ValueError("L'email est obligatoire")
        email = self.normalize_email(email)
        user = self.model(email=email, tenant=tenant, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        extra_fields.setdefault("is_active", True)
        return self.create_user(email, tenant=None, password=password, **extra_fields)

    def get_by_natural_key(self, email):
        """
        Permet la connexion par email
        sans contrainte unique globale.
        L'unicité est gérée par tenant + email.
        """
        return self.get(email=email)


class User(AbstractBaseUser, PermissionsMixin):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    tenant = models.ForeignKey(
        "authentification.Organisation",
        on_delete=models.CASCADE,
        related_name="users",
        null=True,
        blank=True,
    )
    email = models.EmailField(max_length=255, unique=True)
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    avatar_url = models.URLField(max_length=500, null=True, blank=True)
    is_active = models.BooleanField(default=True)
    is_verified = models.BooleanField(default=False)
    is_staff = models.BooleanField(default=False)
    roles = models.ManyToManyField(
        "authentification.Role", related_name="users", blank=True
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    objects = UserManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []

    class Meta:
        db_table = "users"
        verbose_name = "Utilisateur"
        verbose_name_plural = "Utilisateurs"
        ordering = ["last_name", "first_name"]

    def __str__(self):
        return f"{self.get_full_name()} <{self.email}>"

    def get_full_name(self):
        return f"{self.first_name} {self.last_name}".strip()

    def has_permission(self, permission_code):
        """
        Vérifie si l'utilisateur possède une permission
        via ses rôles RBAC.
        """
        return self.roles.filter(permissions__code=permission_code).exists()

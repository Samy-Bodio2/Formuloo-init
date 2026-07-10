"""
Modèle Employé — Formuloo OS
Entité centrale du module RH.
Toutes les autres entités y sont rattachées.
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

NOTE SÉCURITÉ :
- national_id sera chiffré en base (Sprint 3)
- tenant_id extrait du JWT — pas de FK Organisation
- employee_number auto-généré à la création
"""

import uuid

from django.db import models


class Employe(models.Model):

    # ── Statut de l'employé ───────────────────────────────
    class Status(models.TextChoices):
        ACTIVE = "active", "Actif"
        INACTIVE = "inactive", "Inactif"
        ON_LEAVE = "on_leave", "En congé"
        TERMINATED = "terminated", "Licencié/Démissionné"
        SUSPENDED = "suspended", "Suspendu"

    # ── Genre ─────────────────────────────────────────────
    class Gender(models.TextChoices):
        M = "M", "Masculin"
        F = "F", "Féminin"
        OTHER = "Other", "Autre"

    # ── Type d'employé ────────────────────────────────────
    class TypeEmploye(models.TextChoices):
        PERMANENT = "permanent", "Permanent"
        CONTRACTUEL = "contractuel", "Contractuel"
        STAGIAIRE = "stagiaire", "Stagiaire"
        CONSULTANT = "consultant", "Consultant"

    # ── Situation familiale ───────────────────────────────
    class SituationFamiliale(models.TextChoices):
        CELIBATAIRE = "celibataire", "Célibataire"
        MARIE = "marie", "Marié(e)"
        DIVORCE = "divorce", "Divorcé(e)"
        VEUF = "veuf", "Veuf/Veuve"

    # ── Mode de paiement ──────────────────────────────────
    class ModePaiement(models.TextChoices):
        VIREMENT = "virement", "Virement bancaire"
        ESPECES = "especes", "Espèces"
        MOBILE_MONEY = "mobile_money", "Mobile Money"
        CHEQUE = "cheque", "Chèque"

    # ── Identifiant ───────────────────────────────────────
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Lien avec le compte utilisateur Auth ──────────────
    # nullable — un employé peut ne pas avoir de compte
    user_id = models.UUIDField(
        null=True, blank=True, help_text="UUID du User dans le service Auth"
    )

    # ── Matricule auto-généré ─────────────────────────────
    employee_number = models.CharField(
        max_length=50,
        unique=False,
        help_text="Matricule unique dans le tenant — auto-généré",
    )

    # ── Informations personnelles ─────────────────────────
    first_name = models.CharField(max_length=100, help_text="Prénom")
    last_name = models.CharField(max_length=100, help_text="Nom de famille")
    birth_date = models.DateField(null=True, blank=True, help_text="Date de naissance")
    gender = models.CharField(
        max_length=10,
        choices=Gender.choices,
        default=Gender.M,
        help_text="Genre — M/F/Other",
    )
    nationality = models.CharField(
        max_length=100,
        null=True,
        blank=True,
        default="Camerounaise",
        help_text="Nationalité",
    )
    # Numéro CNI — sera chiffré en Sprint 3
    national_id = models.CharField(
        max_length=50,
        null=True,
        blank=True,
        help_text="Numéro CNI — sera chiffré en base",
    )
    situation_familiale = models.CharField(
        max_length=20, choices=SituationFamiliale.choices, null=True, blank=True
    )
    nombre_enfants = models.IntegerField(
        default=0, help_text="Pour calcul allocations familiales"
    )
    # Numéro CNPS — spécifique Cameroun/Afrique
    numero_cnps = models.CharField(
        max_length=50,
        null=True,
        blank=True,
        help_text="Numéro CNPS — Caisse Nationale Prévoyance Sociale",
    )

    # ── Coordonnées ───────────────────────────────────────
    address = models.TextField(null=True, blank=True, help_text="Adresse complète")
    phone = models.CharField(max_length=20, help_text="Téléphone professionnel")
    email = models.EmailField(help_text="Email professionnel unique dans le tenant")
    email_perso = models.EmailField(null=True, blank=True, help_text="Email personnel")
    phone_perso = models.CharField(
        max_length=20, null=True, blank=True, help_text="Téléphone personnel"
    )
    ville = models.CharField(max_length=100, null=True, blank=True, default="Douala")
    pays = models.CharField(max_length=100, default="Cameroun")

    # ── Informations professionnelles ─────────────────────
    department = models.ForeignKey(
        "rh.Departement",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="employes",
        help_text="Département d'affectation",
    )
    position = models.ForeignKey(
        "rh.Poste",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="employes",
        help_text="Poste occupé",
    )
    # Hiérarchie — supérieur direct
    manager = models.ForeignKey(
        "self",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="subordonnes",
        help_text="Manager / supérieur hiérarchique direct",
    )
    hire_date = models.DateField(help_text="Date d'embauche")
    date_fin_periode_essai = models.DateField(
        null=True, blank=True, help_text="Fin de la période d'essai"
    )
    status = models.CharField(
        max_length=20,
        choices=Status.choices,
        default=Status.ACTIVE,
        help_text="Statut de l'employé",
    )
    type_employe = models.CharField(
        max_length=20, choices=TypeEmploye.choices, default=TypeEmploye.PERMANENT
    )

    # ── Rémunération ──────────────────────────────────────
    salaire_base = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        default=0,
        help_text="Salaire de base mensuel brut",
    )
    devise = models.CharField(max_length=10, default="XAF")
    mode_paiement = models.CharField(
        max_length=20, choices=ModePaiement.choices, null=True, blank=True
    )
    numero_compte = models.CharField(
        max_length=50, null=True, blank=True, help_text="Coordonnées bancaires"
    )
    banque = models.CharField(max_length=100, null=True, blank=True)

    # ── Documents ─────────────────────────────────────────
    photo_url = models.URLField(
        null=True, blank=True, help_text="URL de la photo de profil"
    )
    cv_url = models.URLField(null=True, blank=True, help_text="URL du CV")

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "employes"
        verbose_name = "Employé"
        verbose_name_plural = "Employés"
        # Email unique par tenant
        unique_together = [
            ["tenant_id", "email"],
            ["tenant_id", "employee_number"],
        ]
        ordering = ["last_name", "first_name"]

    def __str__(self):
        return f"{self.first_name} {self.last_name} ({self.employee_number})"

    @property
    def full_name(self):
        """Nom complet de l'employé."""
        return f"{self.first_name} {self.last_name}"

    def save(self, *args, **kwargs):
        """
        Auto-génère le matricule à la création.
        Format : EMP-YYYY-NNN
        Ex: EMP-2024-001
        """
        if not self.employee_number:
            from django.utils import timezone

            year = timezone.now().year
            count = Employe.objects.filter(tenant_id=self.tenant_id).count() + 1
            self.employee_number = f"EMP-{year}-{count:03d}"
        super().save(*args, **kwargs)

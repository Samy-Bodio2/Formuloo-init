"""
Modèle Département — Formuloo OS
Unité organisationnelle de la PME.
Supporte la hiérarchie parent/enfant (sous-départements).
Conforme ADR-001 : isolation multi-tenant
Conforme CDC Formuloo OS v1.0

NOTE ARCHITECTURE :
- tenant_id est un UUID extrait du JWT
- Le service RH est indépendant du service Auth
- parent FK self permet max 3-4 niveaux (PME africaines)
"""

import uuid

from django.db import models


class Departement(models.Model):

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    # UUID extrait du JWT — pas de FK vers Organisation
    # car le service RH est indépendant du service Auth
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Informations du département ───────────────────────
    nom = models.CharField(max_length=100, help_text="Nom du département")
    code = models.CharField(
        max_length=50, help_text="Code unique du département dans le tenant"
    )
    description = models.TextField(
        null=True, blank=True, help_text="Description du rôle du département"
    )

    # ── Hiérarchie parent/enfant ──────────────────────────
    # Permet de créer des sous-départements
    # Ex: Informatique → Frontend → Mobile
    # null = département racine (pas de parent)
    parent = models.ForeignKey(
        "self",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="sous_departements",
        help_text="Département parent — null si racine",
    )

    # ── Responsable ───────────────────────────────────────
    # SET_NULL car un responsable peut quitter l'entreprise
    responsable = models.ForeignKey(
        "rh.Employe",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="departements_diriges",
        help_text="Employé responsable du département",
    )

    # ── Budget ────────────────────────────────────────────
    budget = models.DecimalField(
        max_digits=15,
        decimal_places=2,
        null=True,
        blank=True,
        help_text="Budget annuel alloué au département",
    )
    devise = models.CharField(
        max_length=10, default="XAF", help_text="Devise du budget (XAF par défaut)"
    )

    # ── Statut ────────────────────────────────────────────
    is_active = models.BooleanField(
        default=True, help_text="False = département archivé"
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "departements"
        verbose_name = "Département"
        verbose_name_plural = "Départements"
        # Code unique par tenant — deux PME peuvent avoir
        # le même code sans conflit
        unique_together = [["tenant_id", "code"]]
        ordering = ["nom"]

    def __str__(self):
        return f"{self.nom} ({self.code})"

    @property
    def nb_employes(self):
        """
        Nombre d'employés actifs dans ce département.
        Calculé dynamiquement — pas stocké en base.
        """
        return self.employes.filter(status="active").count()

    def get_ancestors(self):
        """
        Retourne la liste des départements parents
        jusqu'à la racine.
        Ex: Mobile → Frontend → Informatique
        """
        ancestors = []
        current = self.parent
        while current is not None:
            ancestors.append(current)
            current = current.parent
        return ancestors

    def get_children(self):
        """
        Retourne les sous-départements directs.
        """
        return self.sous_departements.filter(is_active=True)

    def get_all_descendants(self):
        """
        Retourne tous les sous-départements
        de manière récursive.
        """
        descendants = []
        for child in self.get_children():
            descendants.append(child)
            descendants.extend(child.get_all_descendants())
        return descendants

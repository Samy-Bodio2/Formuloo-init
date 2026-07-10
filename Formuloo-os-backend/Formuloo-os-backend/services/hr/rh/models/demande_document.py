"""
Modèle DemandeDocument — Formuloo OS
Workflow de demande officielle de document RH.

Un employé soumet une demande (ATTESTATION_TRAVAIL, etc.).
Le RH valide ou rejette. L'employé ne peut télécharger
le document que si la demande est APPROUVEE.

Conforme ADR-001 : isolation multi-tenant
"""

import uuid

from django.db import models


class DemandeDocument(models.Model):

    # ── Type de document demandé ──────────────────────────
    class TypeDocument(models.TextChoices):
        ATTESTATION_TRAVAIL = "attestation_travail", "Attestation de travail"
        ATTESTATION_SALAIRE = "attestation_salaire", "Attestation de salaire"
        BULLETIN_PAIE_COPIE = "bulletin_paie_copie", "Copie de bulletin de paie"

    # ── Statut du workflow ────────────────────────────────
    class Statut(models.TextChoices):
        EN_ATTENTE = "en_attente", "En attente de traitement"
        APPROUVEE = "approuvee", "Approuvée"
        REJETEE = "rejetee", "Rejetée"
        ANNULEE = "annulee", "Annulée par l'employé"

    # ── Identifiant ───────────────────────────────────────
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    # ── Isolation multi-tenant ────────────────────────────
    tenant_id = models.UUIDField(
        db_index=True, help_text="UUID du tenant extrait du JWT"
    )

    # ── Employé demandeur ─────────────────────────────────
    employee = models.ForeignKey(
        "rh.Employe",
        on_delete=models.CASCADE,
        related_name="demandes_document",
        help_text="Employé ayant soumis la demande",
    )

    # ── Détails de la demande ─────────────────────────────
    type_document = models.CharField(
        max_length=30,
        choices=TypeDocument.choices,
        help_text="Type de document demandé",
    )
    statut = models.CharField(
        max_length=20,
        choices=Statut.choices,
        default=Statut.EN_ATTENTE,
        help_text="Statut de traitement de la demande",
    )
    motif_demande = models.TextField(
        null=True,
        blank=True,
        help_text="Raison de la demande (optionnel — ex: visa, prêt bancaire)",
    )

    # ── Traitement par le RH ──────────────────────────────
    # auth_user_id du RH Manager qui a traité la demande
    traitee_par = models.UUIDField(
        null=True,
        blank=True,
        help_text="auth_user_id du RH Manager ayant traité la demande",
    )
    traitee_le = models.DateTimeField(
        null=True,
        blank=True,
        help_text="Date et heure du traitement",
    )
    motif_rejet = models.TextField(
        null=True,
        blank=True,
        help_text="Motif du rejet — obligatoire si statut=REJETEE",
    )

    # ── Document généré ───────────────────────────────────
    # Renseigné à l'approbation — le frontend génère le PDF à partir de ces données
    document_data = models.JSONField(
        null=True,
        blank=True,
        help_text="Données structurées du document généré — disponibles après approbation",
    )

    # ── Timestamps ────────────────────────────────────────
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "demandes_document"
        verbose_name = "Demande de document"
        verbose_name_plural = "Demandes de documents"
        ordering = ["-created_at"]

    def __str__(self):
        return (
            f"{self.employee.full_name} — "
            f"{self.get_type_document_display()} — "
            f"{self.get_statut_display()}"
        )

    @property
    def est_traitable(self):
        """True si la demande peut encore être traitée par le RH."""
        return self.statut == self.Statut.EN_ATTENTE

"""
Migration de données : permissions du module Compta + assignation aux rôles système.

Permissions créées (format module.action.resource) :
  compta.read.ecritures        compta.write.ecritures       compta.validate.ecritures
  compta.read.comptes          compta.write.comptes         compta.delete.comptes
  compta.read.journaux         compta.write.journaux
  compta.read.exercices        compta.write.exercices       compta.close.exercices
  compta.read.factures         compta.write.factures        compta.delete.factures
  compta.read.paiements        compta.write.paiements
  compta.read.etats

Assignation aux rôles système :
  SUPER_ADMIN  → toutes les permissions compta
  ADMIN_PME    → toutes les permissions compta
  COMPTABLE    → read/write/validate (pas close exercice, pas delete comptes)
  RH_MANAGER   → compta.read.ecritures + compta.read.etats (voir écritures de paie)
  EMPLOYE      → aucune permission compta
"""

from django.db import migrations


COMPTA_PERMISSIONS = [
    # Écritures
    ("compta", "read",     "ecritures"),
    ("compta", "write",    "ecritures"),
    ("compta", "validate", "ecritures"),
    ("compta", "delete",   "ecritures"),
    # Plan de comptes
    ("compta", "read",     "comptes"),
    ("compta", "write",    "comptes"),
    ("compta", "delete",   "comptes"),
    # Journaux
    ("compta", "read",     "journaux"),
    ("compta", "write",    "journaux"),
    # Exercices
    ("compta", "read",     "exercices"),
    ("compta", "write",    "exercices"),
    ("compta", "close",    "exercices"),
    # Factures
    ("compta", "read",     "factures"),
    ("compta", "write",    "factures"),
    ("compta", "delete",   "factures"),
    # Paiements
    ("compta", "read",     "paiements"),
    ("compta", "write",    "paiements"),
    # États financiers (grand livre, balance, bilan, compte de résultat)
    ("compta", "read",     "etats"),
]

# Permissions accordées à chaque rôle système
ROLE_PERMISSIONS = {
    "SUPER_ADMIN": [f"{m}.{a}.{r}" for m, a, r in COMPTA_PERMISSIONS],
    "ADMIN_PME":   [f"{m}.{a}.{r}" for m, a, r in COMPTA_PERMISSIONS],
    "COMPTABLE": [
        "compta.read.ecritures",
        "compta.write.ecritures",
        "compta.validate.ecritures",
        "compta.read.comptes",
        "compta.write.comptes",
        "compta.read.journaux",
        "compta.write.journaux",
        "compta.read.exercices",
        "compta.write.exercices",
        "compta.read.factures",
        "compta.write.factures",
        "compta.delete.factures",
        "compta.read.paiements",
        "compta.write.paiements",
        "compta.read.etats",
    ],
    "RH_MANAGER": [
        "compta.read.ecritures",
        "compta.read.etats",
    ],
}


def create_compta_permissions(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    Role = apps.get_model("authentification", "Role")

    created_permissions = {}
    for module, action, resource in COMPTA_PERMISSIONS:
        code = f"{module}.{action}.{resource}"
        perm, _ = Permission.objects.get_or_create(
            module=module,
            action=action,
            resource=resource,
            defaults={"code": code},
        )
        created_permissions[code] = perm

    for role_code, perm_codes in ROLE_PERMISSIONS.items():
        roles = Role.objects.filter(code=role_code, is_system=True)
        for role in roles:
            for code in perm_codes:
                perm = created_permissions.get(code)
                if perm:
                    role.permissions.add(perm)


def remove_compta_permissions(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    codes = [f"{m}.{a}.{r}" for m, a, r in COMPTA_PERMISSIONS]
    Permission.objects.filter(code__in=codes).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("authentification", "0005_permission_add_validate_close_actions"),
    ]

    operations = [
        migrations.RunPython(
            create_compta_permissions,
            reverse_code=remove_compta_permissions,
        ),
    ]

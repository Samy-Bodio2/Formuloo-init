"""
Migration de données : création des rôles système manquants.

Constat : aucune migration précédente ne créait réellement les rôles
système (SUPER_ADMIN, ADMIN_PME, RH_MANAGER, MANAGER, COMPTABLE,
COMMERCIAL, EMPLOYE). Ils sont documentés dans Role et dans
services/hr/rh/permissions.py comme s'ils existaient, mais la table
`roles` était vide en pratique :
  - RegisterView assigne RH_MANAGER au premier utilisateur d'une PME
    via Role.objects.filter(code="RH_MANAGER").first() → toujours None
  - Toutes les permissions IsRHManager/IsManagerOrRH/etc. du service RH
    échouaient systématiquement (roles=[] dans le JWT pour tout le monde)
  - Les migrations 0006 et 0007 assignaient déjà des permissions Compta
    à ces rôles via Role.objects.filter(code=role_code, is_system=True)
    — sans effet, puisque le filtre ne retournait jamais rien.

Cette migration crée les 7 rôles système (tenant=None, is_system=True)
et réapplique les assignations de permissions Compta de 0006/0007,
maintenant qu'elles peuvent réellement prendre effet.
"""

from django.db import migrations


SYSTEM_ROLES = [
    ("SUPER_ADMIN", "Super Administrateur"),
    ("ADMIN_PME", "Administrateur PME"),
    ("RH_MANAGER", "Responsable RH"),
    ("MANAGER", "Manager d'équipe"),
    ("COMPTABLE", "Comptable"),
    ("COMMERCIAL", "Commercial"),
    ("EMPLOYE", "Employé"),
]

# Reprise exacte des assignations de permissions Compta
# des migrations 0006_compta_permissions_and_roles et
# 0007_compta_init_plan_permission, jamais appliquées
# faute de rôles existants au moment de leur exécution.
ROLE_PERMISSIONS = {
    "SUPER_ADMIN": [
        "compta.read.ecritures", "compta.write.ecritures",
        "compta.validate.ecritures", "compta.delete.ecritures",
        "compta.read.comptes", "compta.write.comptes", "compta.delete.comptes",
        "compta.read.journaux", "compta.write.journaux",
        "compta.read.exercices", "compta.write.exercices", "compta.close.exercices",
        "compta.read.factures", "compta.write.factures", "compta.delete.factures",
        "compta.read.paiements", "compta.write.paiements",
        "compta.read.etats",
        "compta.init.plan",
    ],
    "ADMIN_PME": [
        "compta.read.ecritures", "compta.write.ecritures",
        "compta.validate.ecritures", "compta.delete.ecritures",
        "compta.read.comptes", "compta.write.comptes", "compta.delete.comptes",
        "compta.read.journaux", "compta.write.journaux",
        "compta.read.exercices", "compta.write.exercices", "compta.close.exercices",
        "compta.read.factures", "compta.write.factures", "compta.delete.factures",
        "compta.read.paiements", "compta.write.paiements",
        "compta.read.etats",
        "compta.init.plan",
    ],
    "COMPTABLE": [
        "compta.read.ecritures", "compta.write.ecritures", "compta.validate.ecritures",
        "compta.read.comptes", "compta.write.comptes",
        "compta.read.journaux", "compta.write.journaux",
        "compta.read.exercices", "compta.write.exercices",
        "compta.read.factures", "compta.write.factures", "compta.delete.factures",
        "compta.read.paiements", "compta.write.paiements",
        "compta.read.etats",
    ],
    "RH_MANAGER": [
        "compta.read.ecritures",
        "compta.read.etats",
    ],
}


def seed_system_roles(apps, schema_editor):
    Role = apps.get_model("authentification", "Role")
    Permission = apps.get_model("authentification", "Permission")

    roles = {}
    for code, name in SYSTEM_ROLES:
        role, _ = Role.objects.get_or_create(
            code=code,
            tenant=None,
            defaults={"name": name, "is_system": True},
        )
        roles[code] = role

    for role_code, perm_codes in ROLE_PERMISSIONS.items():
        role = roles.get(role_code)
        if not role:
            continue
        for code in perm_codes:
            perm = Permission.objects.filter(code=code).first()
            if perm:
                role.permissions.add(perm)


def remove_system_roles(apps, schema_editor):
    Role = apps.get_model("authentification", "Role")
    codes = [code for code, _ in SYSTEM_ROLES]
    Role.objects.filter(code__in=codes, is_system=True, tenant__isnull=True).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("authentification", "0008_onetimetoken_code"),
    ]

    operations = [
        migrations.RunPython(
            seed_system_roles,
            reverse_code=remove_system_roles,
        ),
    ]

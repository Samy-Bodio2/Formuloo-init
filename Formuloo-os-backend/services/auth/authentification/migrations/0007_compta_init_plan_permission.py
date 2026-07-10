"""
Migration 0007 : ajout de la permission compta.init.plan

Permet à SUPER_ADMIN et ADMIN_PME d'initialiser le plan comptable
SYSCOHADA d'un tenant (endpoint POST /api/v1/compta/initialiser/).
"""

from django.db import migrations

NEW_PERMISSIONS = [
    ("compta", "init", "plan"),
]

ROLE_PERMISSIONS = {
    "SUPER_ADMIN": ["compta.init.plan"],
    "ADMIN_PME":   ["compta.init.plan"],
}


def add_permission(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    Role = apps.get_model("authentification", "Role")

    created = {}
    for module, action, resource in NEW_PERMISSIONS:
        code = f"{module}.{action}.{resource}"
        perm, _ = Permission.objects.get_or_create(
            module=module, action=action, resource=resource,
            defaults={"code": code},
        )
        created[code] = perm

    for role_code, perm_codes in ROLE_PERMISSIONS.items():
        for role in Role.objects.filter(code=role_code, is_system=True):
            for code in perm_codes:
                perm = created.get(code)
                if perm:
                    role.permissions.add(perm)


def remove_permission(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    codes = [f"{m}.{a}.{r}" for m, a, r in NEW_PERMISSIONS]
    Permission.objects.filter(code__in=codes).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("authentification", "0006_compta_permissions_and_roles"),
    ]

    operations = [
        migrations.RunPython(add_permission, reverse_code=remove_permission),
    ]

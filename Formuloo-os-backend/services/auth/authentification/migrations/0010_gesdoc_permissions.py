"""
Migration de données : permissions du module GesDoc (Gestion Documentaire).

Ajoute 3 permissions gesdoc.* et les assigne aux rôles système :
  SUPER_ADMIN / ADMIN_PME → read.documents + write.documents + read.audit
  COMPTABLE               → read.documents + write.documents

Cohérent avec la matrice Compta (migration 0006) et les vérifications
de permissions dans services/gesdoc/gestiondoc/permissions.py.
"""

from django.db import migrations

# (module, action, resource) — code généré automatiquement par Permission.save()
GESDOC_PERMISSIONS = [
    ("gesdoc", "read",  "documents"),
    ("gesdoc", "write", "documents"),
    ("gesdoc", "read",  "audit"),
]

ROLE_PERMISSIONS = {
    "SUPER_ADMIN": ["gesdoc.read.documents", "gesdoc.write.documents", "gesdoc.read.audit"],
    "ADMIN_PME":   ["gesdoc.read.documents", "gesdoc.write.documents", "gesdoc.read.audit"],
    "COMPTABLE":   ["gesdoc.read.documents", "gesdoc.write.documents"],
}


def add_gesdoc_permissions(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    Role = apps.get_model("authentification", "Role")

    perms = {}
    for module, action, resource in GESDOC_PERMISSIONS:
        code = f"{module}.{action}.{resource}"
        perm, _ = Permission.objects.get_or_create(
            module=module,
            action=action,
            resource=resource,
            defaults={"code": code},
        )
        perms[code] = perm

    for role_code, perm_codes in ROLE_PERMISSIONS.items():
        role = Role.objects.filter(code=role_code, is_system=True, tenant__isnull=True).first()
        if not role:
            continue
        for code in perm_codes:
            perm = perms.get(code)
            if perm:
                role.permissions.add(perm)


def remove_gesdoc_permissions(apps, schema_editor):
    Permission = apps.get_model("authentification", "Permission")
    codes = [f"{m}.{a}.{r}" for m, a, r in GESDOC_PERMISSIONS]
    Permission.objects.filter(code__in=codes).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("authentification", "0009_seed_system_roles"),
    ]

    operations = [
        migrations.RunPython(
            add_gesdoc_permissions,
            reverse_code=remove_gesdoc_permissions,
        ),
    ]

"""
Configuration Celery — Service GesDoc
Formuloo OS — ERP cloud pour PME africaines

Contrainte VM Docker Toolbox : pas de threads OS disponibles.
Le worker doit être lancé avec --pool=solo --concurrency=1
(voir services/gesdoc/entrypoint.sh du worker).
"""

import os

from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")

app = Celery("gesdoc")
app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()

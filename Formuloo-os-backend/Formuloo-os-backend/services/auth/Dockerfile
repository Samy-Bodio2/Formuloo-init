# ============================================================
# Dockerfile — Service Auth
# Formuloo OS — ERP cloud pour PME africaines
# ============================================================

# Image de base Python officielle
FROM python:3.11.9-slim

# Métadonnées
LABEL maintainer="CHOWUINOU TEGUIA <backend@formuloo.com>"
LABEL description="Service Auth — Formuloo OS"
LABEL version="1.0.0"

# Variables d'environnement
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

# Dossier de travail
WORKDIR /app

# Copier et installer les dépendances Python
COPY requirements.txt .
RUN pip install --no-cache-dir --progress-bar off -r requirements.txt \
    && pip install --no-cache-dir --progress-bar off gunicorn==21.2.0

# Copier le code source
COPY . .

# Port exposé
EXPOSE 8000

# Commande de démarrage
CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]

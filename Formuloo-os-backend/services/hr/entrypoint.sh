#!/bin/sh
# ──────────────────────────────────────────────────────────
# entrypoint.sh — Service RH — Formuloo OS
# Script exécuté au démarrage du conteneur Docker
#
# Étapes :
# 1. Attendre que PostgreSQL soit prêt
# 2. Appliquer les migrations
# 3. Collecter les fichiers statiques
# 4. Lancer Gunicorn
# ──────────────────────────────────────────────────────────

echo "Demarrage du Service RH - Formuloo OS"
echo "========================================"

# ── 1. Attendre PostgreSQL ────────────────────────────────
echo "Attente de PostgreSQL ($DB_HOST:$DB_PORT)..."
while ! python -c "import socket; socket.create_connection(('${DB_HOST}', ${DB_PORT}), timeout=1).close()" 2>/dev/null; do
    echo "   PostgreSQL pas encore pret -- attente 2s..."
    sleep 2
done
echo "PostgreSQL pret !"

# ── 2. Appliquer les migrations ───────────────────────────
# Crée/met à jour les tables en base de données
# --noinput → pas d'interaction utilisateur
echo "Application des migrations..."
python manage.py migrate --noinput
echo "Migrations appliquees !"

echo "Collecte des fichiers statiques..."
python manage.py collectstatic --noinput
echo "Fichiers statiques collectes !"

echo "Lancement de Gunicorn sur le port 8001..."
echo "========================================"
exec gunicorn config.wsgi:application \
    --bind 0.0.0.0:8001 \
    --workers 2 \
    --worker-class sync \
    --timeout 120 \
    --preload \
    --access-logfile - \
    --error-logfile -

#!/bin/sh
echo "Demarrage du Service GesDoc - Formuloo OS"
echo "==========================================="

echo "Attente de PostgreSQL ($DB_HOST:$DB_PORT)..."
while ! python -c "import socket; socket.create_connection(('${DB_HOST}', ${DB_PORT}), timeout=1).close()" 2>/dev/null; do
    echo "   PostgreSQL pas encore pret -- attente 2s..."
    sleep 2
done
echo "PostgreSQL pret !"

echo "Application des migrations..."
python manage.py migrate --noinput
echo "Migrations appliquees !"

echo "Collecte des fichiers statiques..."
python manage.py collectstatic --noinput
echo "Fichiers statiques collectes !"

echo "Lancement de Gunicorn sur le port 8003..."
echo "==========================================="
exec gunicorn config.wsgi:application \
    --bind 0.0.0.0:8003 \
    --workers 1 \
    --worker-class sync \
    --timeout 120 \
    --preload \
    --access-logfile - \
    --error-logfile -

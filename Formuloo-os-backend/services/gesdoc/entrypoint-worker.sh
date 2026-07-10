#!/bin/sh
echo "Demarrage du Worker Celery GesDoc - Formuloo OS"
echo "==========================================="

echo "Attente de PostgreSQL ($DB_HOST:$DB_PORT)..."
while ! python -c "import socket; socket.create_connection(('${DB_HOST}', ${DB_PORT}), timeout=1).close()" 2>/dev/null; do
    echo "   PostgreSQL pas encore pret -- attente 2s..."
    sleep 2
done
echo "PostgreSQL pret !"

# --pool=solo --concurrency=1 : pas de threads/forks (contrainte VM
# Docker Toolbox, memoire feedback_docker_toolbox).
echo "Lancement du worker Celery (pool=solo)..."
echo "==========================================="
exec celery -A config worker \
    --pool=solo \
    --concurrency=1 \
    --loglevel=info

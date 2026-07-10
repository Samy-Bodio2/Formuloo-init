#!/bin/sh
# Suivi en direct des logs backend pendant les tests KMP.
# Usage : ./scripts/dev-watch.sh
cd "$(dirname "$0")/.."
docker compose logs -f --tail=50 gateway auth hr compta

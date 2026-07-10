"""
Pagination par curseur — Formuloo OS Service GesDoc

Le contrat (contracts/gesdoc/v1/gesdoc.yaml) attend une pagination par
curseur opaque (`cursor` en query param, `next_cursor` en réponse) sur
GET /documents/ et GET /documents/audit-log/, plutôt que la pagination
par page utilisée dans Compta/HR. Implémentation légère (pas de classe
DRF Pagination) car les vues du service sont de simples APIView,
comme dans les autres services.
"""

from django.core import signing
from django.db.models import Q

CURSOR_SALT = "gestiondoc.pagination.cursor"


def encode_cursor(time_field_value, pk):
    return signing.dumps((time_field_value.isoformat(), str(pk)), salt=CURSOR_SALT)


def decode_cursor(cursor):
    """Retourne (datetime_iso, pk) ou None si le curseur est invalide/absent."""
    if not cursor:
        return None
    try:
        return signing.loads(cursor, salt=CURSOR_SALT)
    except (signing.BadSignature, ValueError, TypeError):
        return None


def paginate_by_cursor(queryset, cursor, page_size, time_field="created_at"):
    """
    Pagine un queryset ordonné par (-time_field, -id) à l'aide d'un curseur
    opaque. Retourne (résultats, next_cursor).
    """
    decoded = decode_cursor(cursor)
    if decoded is not None:
        from datetime import datetime

        last_time_iso, last_pk = decoded
        try:
            last_time = datetime.fromisoformat(last_time_iso)
            lt_filter = {f"{time_field}__lt": last_time}
            eq_filter = {time_field: last_time, "pk__lt": last_pk}
            queryset = queryset.filter(Q(**lt_filter) | Q(**eq_filter))
        except ValueError:
            pass

    results = list(queryset.order_by(f"-{time_field}", "-pk")[: page_size + 1])
    has_next = len(results) > page_size
    results = results[:page_size]

    next_cursor = None
    if has_next and results:
        last = results[-1]
        next_cursor = encode_cursor(getattr(last, time_field), last.pk)

    return results, next_cursor

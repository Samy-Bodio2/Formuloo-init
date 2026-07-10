FROM python:3.11-slim

LABEL maintainer="Formuloo OS"
LABEL service="compta"
LABEL version="2.0.0"

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=config.settings

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --progress-bar off -r requirements.txt \
    && pip install --no-cache-dir --progress-bar off gunicorn==21.2.0

COPY . .

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8002

ENTRYPOINT ["/entrypoint.sh"]

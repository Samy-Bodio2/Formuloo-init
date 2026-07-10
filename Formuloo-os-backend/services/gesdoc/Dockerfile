FROM formuloo-gesdoc-torch-base:2.3.1-cpu

LABEL maintainer="Formuloo OS"
LABEL service="gesdoc"
LABEL version="1.0.0"

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=config.settings \
    OMP_NUM_THREADS=1 \
    MKL_NUM_THREADS=1

WORKDIR /app

# Dépendances système : Tesseract5 (fra), Poppler (pdf2image), libGL (OpenCV)
# apt-get est idempotent : déjà installés dans l'image de base, vérification rapide.
RUN apt-get update && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-fra \
        poppler-utils \
        libgl1 \
        libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# torch==2.3.1+cpu et torchvision==0.18.1+cpu sont déjà présents dans l'image de
# base (formuloo-gesdoc-torch-base:2.3.1-cpu, construite par `docker commit` du
# conteneur worker validé). Cette étape installe uniquement le reste des dépendances.
# --extra-index-url garantit que toute dépendance transitive de easyocr (torchaudio…)
# est également résolue depuis le wheel CPU et non depuis PyPI (version CUDA).
RUN pip install --no-cache-dir --progress-bar off \
        -r requirements.txt \
        --extra-index-url https://download.pytorch.org/whl/cpu

COPY . .

COPY entrypoint.sh /entrypoint.sh
COPY entrypoint-worker.sh /entrypoint-worker.sh
RUN chmod +x /entrypoint.sh /entrypoint-worker.sh

EXPOSE 8003

ENTRYPOINT ["/entrypoint.sh"]

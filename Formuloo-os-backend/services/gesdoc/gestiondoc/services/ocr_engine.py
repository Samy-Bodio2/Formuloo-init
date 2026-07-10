"""
Pipeline OCR — Formuloo OS Service GesDoc

Étapes : prétraitement OpenCV (deskew/denoise/binarisation Otsu) →
Tesseract5 (moteur principal, lang=fra) → fallback EasyOCR si le score
de confiance Tesseract est sous OCR_FALLBACK_CONFIDENCE_THRESHOLD →
extraction des champs structurés par regex uniquement (pas de
CamemBERT/spaCy — trop lourd pour la VM Docker Toolbox à 1 CPU/2 Go
RAM, voir feedback_docker_toolbox).

Toutes les librairies lourdes (cv2, pytesseract, easyocr, torch — cette
dernière une dépendance d'EasyOCR) sont importées à l'intérieur des
fonctions (lazy import) : le reste du service (API, migrations, tests
avec mocks) doit pouvoir tourner sans que ces paquets soient installés.
Le lecteur EasyOCR est chargé en singleton pour ne pas alourdir chaque appel.
"""

import os
import re
from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation

from django.conf import settings

_easyocr_reader = None


@dataclass
class OCRExtraction:
    raw_text: str
    fields: dict = field(default_factory=dict)
    engine: str = ""
    confidence: int = 0


def _load_image_for_ocr(file_path: str):
    """Charge le fichier (image ou 1ère page de PDF) en tableau OpenCV BGR."""
    import cv2
    import numpy as np

    absolute_path = os.path.join(settings.MEDIA_ROOT, file_path)

    if absolute_path.lower().endswith(".pdf"):
        from pdf2image import convert_from_path

        pages = convert_from_path(absolute_path, dpi=300, first_page=1, last_page=1)
        image = cv2.cvtColor(np.array(pages[0]), cv2.COLOR_RGB2BGR)
    else:
        image = cv2.imread(absolute_path)

    return image


def preprocess_image(image):
    """Deskew + débruitage + binarisation Otsu (+8% précision Tesseract observé)."""
    import cv2
    import numpy as np

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    denoised = cv2.fastNlMeansDenoising(gray, h=10)

    # Deskew via la boîte englobante des pixels non-blancs
    coords = np.column_stack(np.where(denoised < 250))
    angle = 0.0
    if coords.size:
        rect_angle = cv2.minAreaRect(coords)[-1]
        angle = -(90 + rect_angle) if rect_angle < -45 else -rect_angle
        (h, w) = denoised.shape
        matrix = cv2.getRotationMatrix2D((w // 2, h // 2), angle, 1.0)
        denoised = cv2.warpAffine(
            denoised, matrix, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE,
        )

    _, binarized = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return binarized


def run_tesseract(preprocessed_image) -> OCRExtraction:
    import pytesseract

    pytesseract.pytesseract.tesseract_cmd = settings.TESSERACT_CMD
    lang = settings.TESSERACT_LANG

    data = pytesseract.image_to_data(
        preprocessed_image, lang=lang, output_type=pytesseract.Output.DICT,
    )
    words = [w for w in data["text"] if w.strip()]
    confidences = [int(c) for c, w in zip(data["conf"], data["text"]) if w.strip() and int(c) >= 0]
    raw_text = " ".join(words)
    confidence = round(sum(confidences) / len(confidences)) if confidences else 0

    return OCRExtraction(raw_text=raw_text, engine="tesseract5", confidence=confidence)


def run_easyocr(image_input) -> OCRExtraction:
    """
    image_input : tableau numpy BGR (déjà chargé) ou chemin absolu vers un
    fichier image (JPEG/PNG). On n'accepte plus de chemin .pdf car EasyOCR
    passe par imageio qui ne sait pas ouvrir les PDF — le pipeline passe
    maintenant le tableau numpy issu de _load_image_for_ocr.
    """
    global _easyocr_reader
    if _easyocr_reader is None:
        import easyocr

        _easyocr_reader = easyocr.Reader(["fr"], gpu=False)

    results = _easyocr_reader.readtext(image_input)
    if not results:
        return OCRExtraction(raw_text="", engine="easyocr", confidence=0)

    texts = [r[1] for r in results]
    scores = [r[2] for r in results]
    confidence = round((sum(scores) / len(scores)) * 100)
    return OCRExtraction(raw_text=" ".join(texts), engine="easyocr", confidence=confidence)


# ── Extraction des champs structurés ──────────────────────────

_FIELD_PATTERNS = {
    "document_number": re.compile(r"\b([A-Z]{2,5}[-/]?\d{4}[-/]?\d{2,6})\b"),
    "date": re.compile(r"\b(\d{2}[/\-.]\d{2}[/\-.]\d{4})\b"),
    "amount_ttc": re.compile(r"(?:TTC|Total)\D{0,10}([\d\s]+[.,]\d{2})", re.IGNORECASE),
    "amount_ht": re.compile(r"(?:HT|hors taxe)\D{0,10}([\d\s]+[.,]\d{2})", re.IGNORECASE),
    "tva_rate": re.compile(r"TVA\D{0,10}(\d{1,2}[.,]?\d{0,2})\s*%", re.IGNORECASE),
}

# Nom de société suivi d'une forme juridique courante (Cameroun/OHADA + int'l)
# — repère le fournisseur sans NLP, ex. "CAMTEL BUSINESS SA".
_SUPPLIER_PATTERN = re.compile(
    r"\b([A-Z][A-Za-zÀ-ÿ&.\-]*(?:\s+[A-Z][A-Za-zÀ-ÿ&.\-]*){0,4}\s+"
    r"(?:SA|SARL|SUARL|SAS|EURL|ETS|PLC|LTD|INC|GMBH|LLC))\b"
)


def _to_decimal(raw: str) -> Decimal | None:
    try:
        return Decimal(raw.replace(" ", "").replace(",", "."))
    except (InvalidOperation, AttributeError):
        return None


def _extract_with_regex(raw_text: str) -> dict:
    fields = {}
    for name, pattern in _FIELD_PATTERNS.items():
        match = pattern.search(raw_text)
        if not match:
            continue
        value = match.group(1).strip()
        if name in ("amount_ttc", "amount_ht", "tva_rate"):
            decimal_value = _to_decimal(value)
            if decimal_value is None:
                continue
            fields[name] = {"value": float(decimal_value), "confidence": 75}
        else:
            fields[name] = {"value": value, "confidence": 80}
    return fields


def _extract_supplier(raw_text: str) -> dict | None:
    """
    Repère un nom de société suivi d'une forme juridique (SA, SARL, ...).
    Repli : les 4 premiers mots du texte OCR, à faible confiance — le
    fournisseur figure presque toujours en tête d'une facture/bon de commande.
    """
    match = _SUPPLIER_PATTERN.search(raw_text)
    if match:
        return {"value": match.group(1).strip(), "confidence": 70}

    words = raw_text.split()
    if words:
        return {"value": " ".join(words[:4]), "confidence": 40}

    return None


def extract_fields(raw_text: str) -> dict:
    fields = _extract_with_regex(raw_text)
    supplier = _extract_supplier(raw_text)
    if supplier:
        fields["supplier"] = supplier
    return fields


def run_ocr_pipeline(file_path: str) -> OCRExtraction:
    """
    Point d'entrée du pipeline complet : prétraitement → Tesseract5 →
    fallback EasyOCR si confiance insuffisante → extraction des champs.
    """
    image = _load_image_for_ocr(file_path)
    preprocessed = preprocess_image(image)
    result = run_tesseract(preprocessed)

    if result.confidence < settings.OCR_FALLBACK_CONFIDENCE_THRESHOLD:
        # On passe le tableau numpy (déjà converti depuis PDF si besoin) —
        # EasyOCR ne sait pas ouvrir les PDF via imageio.
        fallback = run_easyocr(preprocessed)
        if fallback.confidence > result.confidence:
            result = fallback

    result.fields = extract_fields(result.raw_text)
    return result

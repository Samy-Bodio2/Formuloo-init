# Module GesDoc — Gestion Documentaire OCR + Blockchain

Documentation du nouveau module **GesDoc** ajouté à Formuloo OS : dépôt
d'une pièce comptable → OCR → validation humaine → certification
blockchain → écriture comptable automatique dans Compta → vérification
d'intégrité. Ce document sert de point d'entrée pour le frontend et
pour quiconque reprend ce travail : arborescence complète, ce qui a été
ajouté/modifié, et l'état réel d'avancement (testé vs non testé).

---

## 1. Résumé de ce qui a été fait

- **Contrat OpenAPI complet** (`contracts/gesdoc/v1/gesdoc.yaml`) : les 11
  endpoints du pipeline (upload → OCR → validation → blockchain →
  comptabilité → intégrité → dashboard → audit) + une route de service
  de fichiers signés.
- **Service Django `gesdoc`** entièrement implémenté : modèles, vues,
  serializers, permissions RBAC, pipeline OCR (Tesseract5 + fallback
  EasyOCR + extraction par regex), ancrage blockchain (web3.py), génération
  PDF de certificat, vérification d'intégrité, tâches Celery.
- **Smart contract `DocumentRegistry.sol`** (Solidity, projet Truffle) —
  **réellement déployé et vérifié sur Ethereum Sepolia** :
  `0x6E6C69D94e26c5dC22d0487A60EccB9492974234`
  ([voir sur Etherscan](https://sepolia.etherscan.io/address/0x6E6C69D94e26c5dC22d0487A60EccB9492974234)).
  4/4 tests Truffle passent (register, doublon rejeté, `isRegistered`, `getRecord`).
- **Intégration automatique GesDoc → Compta** : dès qu'un document est
  certifié, GesDoc appelle un nouvel endpoint interne de Compta pour
  créer automatiquement l'écriture comptable SYSCOHADA correspondante
  (même pattern que l'intégration HR → Compta existante pour la paie).
- **Infra** : `docker-compose.yml`, `gateway/nginx.conf`, `.env` / `.env.example`
  mis à jour pour brancher le service (port 8003), son worker Celery, sa
  base Postgres dédiée, et le routage `/api/v1/documents/`.
- **Tests** : 36/36 pytest côté GesDoc (78 % coverage), 18/18 pytest côté
  Compta pour le nouvel endpoint interne (177/177 sur toute la suite
  Compta, aucune régression). `ruff` clean, `manage.py check` clean sur
  les deux services.

### Ce qui n'a PAS encore été vérifié en conditions réelles

- **Le pipeline OCR n'a jamais tourné réellement** (Tesseract/EasyOCR ne
  sont installés que dans l'image Docker, jamais construite avec
  succès sur cette machine — voir section 6).
- **L'appel HTTP réel GesDoc → Compta** (le vrai `requests.post` vers
  `_internal/ecritures-achat/`) n'a été exécuté qu'avec des mocks en
  test, jamais entre deux vrais conteneurs.
- **Le build Docker complet de `gesdoc`** est bloqué par un problème
  d'infrastructure sur la VM Docker Toolbox (voir section 6) — ce
  travail était en cours au moment de la rédaction de ce document.

---

## 2. Arborescence — `services/gesdoc/`

```
services/gesdoc/
├── Dockerfile                          # Image du service (Tesseract, Poppler, libGL, torch CPU, EasyOCR)
├── entrypoint.sh                       # Démarrage web (migrate + gunicorn :8003)
├── entrypoint-worker.sh                # Démarrage worker Celery (--pool=solo --concurrency=1)
├── manage.py
├── pytest.ini
├── requirements.txt
├── .env / .env.example
│
├── config/                             # Configuration Django du projet
│   ├── __init__.py                     # Charge l'app Celery
│   ├── celery.py                       # App Celery ("gesdoc")
│   ├── settings.py                     # JWT, DB, Redis, Celery, OCR, Blockchain, CORS
│   ├── settings_test.py                # SQLite mémoire, Celery eager, cache dummy
│   ├── urls.py                         # /api/v1/documents/, /api/schema/
│   └── wsgi.py
│
├── gestiondoc/                         # Application Django principale
│   ├── apps.py
│   ├── authentication.py               # GesdocJWTAuthentication (JWT stateless, même pattern que Compta/HR)
│   ├── middleware.py                   # HttpMethodMiddleware, TenantMiddleware
│   ├── exceptions.py                   # Handler d'erreurs global (codes uniformes)
│   ├── permissions.py                  # RBAC : gesdoc.read/write.documents, gesdoc.read.audit
│   ├── pagination.py                   # Pagination par curseur (upload/audit-log)
│   ├── utils.py                        # get_document_or_404
│   ├── tasks.py                        # task_ocr_pipeline, task_blockchain_anchor (+ auto-liaison Compta)
│   ├── urls.py                         # Toutes les routes /documents/...
│   │
│   ├── models/
│   │   ├── document.py                 # AccountingDocument (machine à états, 10 statuts)
│   │   └── audit_log.py                # DocumentAuditLog (journal immuable)
│   ├── migrations/
│   │   ├── 0001_initial.py
│   │   └── 0002_alter_documentauditlog_action.py   # ajout action "journal_linked"
│   │
│   ├── serializers/
│   │   ├── document.py                 # UploadSerializer (validation multipart)
│   │   ├── validate.py                 # ValidateOCRSerializer (cohérence HT/TVA/TTC)
│   │   └── accounting.py               # LinkJournalEntrySerializer
│   │
│   ├── services/                       # Logique métier (découplée des vues)
│   │   ├── storage.py                  # Stockage local + URLs signées (équivalent pre-signed S3)
│   │   ├── ocr_engine.py               # Pipeline OCR : OpenCV → Tesseract5 → EasyOCR → regex
│   │   ├── blockchain_service.py       # web3.py : ancrage/lecture DocumentRegistry sur Sepolia
│   │   ├── compta_client.py            # NOUVEAU — appel interne vers Compta (auto-création écriture)
│   │   ├── syscohada.py                 # Suggestion d'écriture SYSCOHADA (accounting-prefill)
│   │   ├── integrity.py                # Recalcul hash + comparaison on-chain
│   │   └── pdf.py                      # Génération du certificat PDF (ReportLab)
│   │
│   └── views/                          # Un fichier par groupe fonctionnel
│       ├── upload.py                   # POST /documents/upload/
│       ├── ocr.py                      # GET .../ocr-status/, .../ocr-result/
│       ├── validate.py                 # POST .../validate-ocr/
│       ├── blockchain.py               # GET .../blockchain-status/, .../blockchain-proof/
│       ├── accounting.py               # GET .../accounting-prefill/, POST .../link-journal-entry/
│       ├── integrity.py                # POST .../verify-integrity/
│       ├── list.py                     # GET /documents/ (dashboard + stats)
│       ├── audit.py                    # GET /documents/audit-log/ (réservé admin)
│       └── files.py                    # GET /documents/files/{token}/ (fichier signé)
│
├── blockchain/                         # Projet Truffle — smart contract (indépendant de Django)
│   ├── contracts/DocumentRegistry.sol  # Le contrat (registerDocument/getRecord/isRegistered)
│   ├── migrations/1_deploy_document_registry.js
│   ├── test/document_registry.test.js # 4 tests (passent sur Ganache)
│   ├── truffle-config.js               # Réseaux "development" (local) et "sepolia" (Infura)
│   ├── package.json
│   └── .env / .env.example             # INFURA_PROJECT_ID, SEPOLIA_PRIVATE_KEY
│
└── tests/                              # Suite pytest (36 tests, 78% coverage)
    ├── conftest.py                     # Fixtures : tenants, clients JWT, documents, mocks OCR/blockchain/Compta
    ├── test_upload.py
    ├── test_ocr.py
    ├── test_validate.py                # inclut le test de l'auto-liaison Compta
    ├── test_blockchain.py
    ├── test_accounting.py
    ├── test_integrity.py
    ├── test_list.py
    ├── test_audit.py
    └── test_files.py
```

---

## 3. Contrat OpenAPI — `contracts/gesdoc/v1/gesdoc.yaml`

Le contrat que le frontend doit consommer. Points clés :

- **Préfixe des routes : `/api/v1/documents/`** (pas `/api/v1/gesdoc/`,
  contrairement aux autres services — c'est ce que le frontend attend
  d'après les maquettes).
- Auth : JWT Bearer partout, sauf `GET /documents/files/{token}/`
  (le token signé fait office d'autorisation).
- Tags : `Upload`, `OCR`, `Validation`, `Blockchain`, `Comptabilite`,
  `Integrite`, `Dashboard`, `Audit`, `Fichiers`.
- Machine à états du document (champ `status`) :
  `pending_ocr → preprocessing → extracting → analyzing → extracted →
  validated → pending_chain → certified` (+ `tampered`/`failed` depuis
  n'importe quel état).
- `POST /documents/{id}/link-journal-entry/` : la description précise
  que la liaison est **automatique** (Système) depuis la certification ;
  l'appel manuel reste un filet de rattrapage (Comptable).

Swagger UI une fois le service lancé : `http://localhost:8003/api/schema/swagger-ui/`
(ou via la gateway : `http://localhost/api/schema/gesdoc/`).

---

## 4. Modifications côté Compta

GesDoc ne duplique pas le module Auth ni Compta : il valide les JWT
émis par Auth (même pattern stateless que Compta/HR) et, pour la
comptabilisation automatique, appelle un endpoint **interne** de Compta
— exactement le mécanisme déjà utilisé par HR pour la paie
(`rh/services/compta_client.py` → `_internal/ecritures-paie/`).

| Fichier | Changement |
|---|---|
| `services/compta/comptabilite/views/internal.py` | Ajout de `EcritureAchatInternalView` — `POST /_internal/ecritures-achat/`. Crée l'écriture OHADA (60xx Achats débit / 4451 TVA déductible débit / 4011 Fournisseurs crédit) à partir des données envoyées par GesDoc. |
| `services/compta/comptabilite/urls.py` | Enregistre la route `_internal/ecritures-achat/` (déjà bloquée en externe par la règle nginx générique `_internal/`, aucun changement nginx nécessaire côté Compta). |
| `services/compta/tests/test_achats.py` | Ajout de `TestEndpointInterneAchat` (4 tests : sans token, cas nominal, sans exercice ouvert, comptes manquants). |

Aucun changement de modèle ni de migration côté Compta — l'écriture
créée est une `Ecriture`/`LigneEcriture` standard, identique à celles
créées manuellement dans l'UI.

---

## 5. Infra — fichiers modifiés à la racine

| Fichier | Changement |
|---|---|
| `docker-compose.yml` | Ajout des services `gesdoc` (port 8003), `gesdoc_worker` (Celery, `--pool=solo`), `db_gesdoc` (Postgres, port hôte 5437), volume partagé `gesdoc_media`. |
| `gateway/nginx.conf` | Route `/api/v1/documents/` → `gesdoc_service`, `/api/schema/gesdoc/`, `client_max_body_size` relevé à 25M (uploads jusqu'à 20 Mo), healthcheck mis à jour. |
| `.env.example` / `.env` | Variables `GESDOC_SECRET_KEY`, `GESDOC_DB_NAME`, `BLOCKCHAIN_NETWORK`, `INFURA_PROJECT_ID`, `SEPOLIA_PRIVATE_KEY`, `CONTRACT_ADDRESS`. |
| `.gitignore` | Ajout de `.ruff_cache/` et `node_modules/` (projet Truffle). |

---

## 6. État du déploiement réel (Docker)

- Le smart contract est **déployé pour de vrai** sur Sepolia (voir
  section 1) — ancrage blockchain vérifié indépendamment via un script
  web3.py, en dehors de Django (le toolchain Node/Truffle habituel a
  échoué sur cette VM — voir ci-dessous).
- Le **build Docker du service `gesdoc`** est bloqué : la VM Docker
  Toolbox (VirtualBox, 1 CPU / ~2 Go RAM) échoue sur `apt-get update`
  avec des erreurs `NO_PUBKEY` (clés de signature Debian introuvables)
  sur les deux bases testées (`python:3.11-slim` / trixie et
  `python:3.11-slim-bookworm`). Ce n'est pas un souci d'horloge (écart
  vérifié : 4 secondes) ni de disque (11,8 Go libres sur 17,8 Go) —
  probablement une instabilité du chemin réseau de cette VM vers les
  miroirs Debian, cohérente avec les instabilités déjà rencontrées côté
  Infura (timeouts, rate-limit) pendant cette même session.
- **Aucun test end-to-end réel n'a donc encore été fait** (upload d'un
  vrai fichier → OCR réel → certification → écriture Compta réelle).
  Tout le reste (logique métier, endpoints, permissions, migrations,
  contrat) a été vérifié par des tests unitaires avec les dépendances
  lourdes mockées.

**Prochaine étape** pour finaliser : résoudre le blocage `apt-get`
(contourner via un miroir Debian alternatif ou `--allow-unauthenticated`
en dev), construire l'image, lancer `gesdoc` + `gesdoc_worker` +
`db_gesdoc`, et faire un test réel de bout en bout.

---

## 7. Démarrer le service en local (une fois le build résolu)

```bash
docker-compose up -d db_gesdoc redis gesdoc gesdoc_worker gateway
```

- API : `http://localhost/api/v1/documents/` (via la gateway) ou
  `http://localhost:8003/api/v1/documents/` (direct, pour debug).
- Swagger : `http://localhost:8003/api/schema/swagger-ui/`.

### Lancer les tests

```bash
cd services/gesdoc
python -m pytest                    # suite GesDoc (36 tests)

cd ../compta
python -m pytest tests/test_achats.py -k Interne   # nouvel endpoint interne
```

### Redéployer le smart contract (si nécessaire)

```bash
cd services/gesdoc/blockchain
npm install
npm run migrate:sepolia   # nécessite INFURA_PROJECT_ID + SEPOLIA_PRIVATE_KEY dans .env, wallet financé en ETH de test
```

# Formuloo OS — Backend API REST

> **Conception et validation d'une stratégie de tests automatisés pour les ERP modernes : cas de Formuloo OS**

[![GitLab CI/CD](https://img.shields.io/badge/CI%2FCD-GitLab-orange)](https://gitlab.formuloo.com)
[![Python](https://img.shields.io/badge/Python-3.11.9-blue)](https://python.org)
[![Django REST](https://img.shields.io/badge/Django%20REST-3.14-green)](https://django-rest-framework.org)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-purple)](https://swagger.io)
[![Docker](https://img.shields.io/badge/Docker-19.03-blue)](https://docker.com)
[![Tests](https://img.shields.io/badge/Tests-73%2F73-brightgreen)](https://pytest.org)
[![Coverage](https://img.shields.io/badge/Coverage-92%25-brightgreen)](https://pytest-cov.readthedocs.io)

---

## Table des matières

- [Présentation](#présentation)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Lancement](#lancement)
- [Structure du projet](#structure-du-projet)
- [Endpoints disponibles](#endpoints-disponibles)
- [Authentification](#authentification)
- [Tests](#tests)
- [Documentation API](#documentation-api)
- [Contrats OpenAPI](#contrats-openapi)
- [Variables d'environnement](#variables-denvironnement)
- [Auteur](#auteur)

---

## Présentation

**Formuloo OS** est une suite ERP (Enterprise Resource Planning) cloud intégrée,
développée par Formuloo, spécifiquement conçue pour les PME africaines.
Elle couvre 6 domaines fonctionnels :

| Module | Description | Statut |
|--------|-------------|--------|
| **Auth** | Authentification SSO, utilisateurs, rôles RBAC, clés API, audit | ✅ Sprint 1 |
| **HR** | Gestion des employés, congés, paies et départements | ⏳ Sprint 2 |
| **Compta** | Factures, paiements, bilans financiers SYSCOHADA | ⏳ Sprint 2 |
| **CRM** | Clients, prospects, opportunités et pipeline de ventes | ⏳ Sprint 3 |
| **Stock** | Produits, inventaire, mouvements et fournisseurs | ⏳ Sprint 3 |
| **Analytics** | KPIs, tableaux de bord et rapports exportables | ⏳ Sprint 4 |

### Approche Contract-First

Ce projet adopte l'approche **Contract-First** :
les contrats OpenAPI 3 sont définis et validés par les équipes
Backend et Frontend **avant** toute implémentation.

```
Contrat OpenAPI validé
        ↓
Frontend → mock Prism     Backend → Django REST
        ↓                          ↓
        ←——— Intégration ———————————
        ↓
Schemathesis valide la conformité en CI/CD
```

---

## Architecture

Formuloo OS adopte une **architecture microservices purs** :
chaque service est indépendant avec sa propre base de données PostgreSQL.

```
Clients (Frontend Angular / Clients tiers)
                    ↓
              API Gateway
        (Routage · /api/v1/ · Rate limiting)
                    ↓
    ┌───────────────────────────────────┐
    │           Keycloak SSO            │
    │     (JWT · OAuth2/OIDC · RBAC)   │
    └───────────────────────────────────┘
                    ↓ Validation JWT
    ┌──────┬──────┬──────┬──────┬──────┐
    │ Auth │  HR  │Compta│ CRM  │Stock │ Analytics
    │:8000 │:8001 │:8002 │:8003 │:8004 │ :8005
    └──┬───┴──┬───┴──┬───┴──┬───┴──┬───┘
       │      │      │      │      │
    db_auth db_hr db_compta db_crm db_stock db_analytics
                    ↓
              Redis (Cache · Sessions · Blacklist JWT)
                    ↓
         Docker · GitLab CI/CD
```

### Principes architecturaux (ADR)

| ADR | Décision | Justification |
|-----|---------|---------------|
| ADR-001 | Architecture Microservices purs | Scalabilité indépendante par service |
| ADR-002 | Authentification SSO via Keycloak + JWT | Sécurité centralisée, RBAC granulaire |
| ADR-003 | Versionnement par URL (/api/v1/) | Rétrocompatibilité garantie |
| ADR-004 | PostgreSQL + Redis par service | Isolation des données, performance |
| ADR-005 | Simplification modèle Organisation Sprint 1 | plan et timezone ajoutés Sprint 4/6 |

---

## Stack technique

| Technologie | Version | Rôle |
|-------------|---------|------|
| Python | 3.11.9 | Langage de développement |
| Django | 4.2.13 LTS | Framework web |
| Django REST Framework | 3.14.0 | Construction des API REST |
| SimpleJWT | 5.3.1 | Authentification JWT |
| PostgreSQL | 15-alpine | Base de données (une par service) |
| Redis | 7-alpine | Cache, sessions, blacklist JWT |
| Keycloak | Latest | Serveur SSO (OAuth2/OIDC) |
| drf-spectacular | 0.27.2 | Génération OpenAPI 3 |
| pytest | 8.2.2 | Framework de tests |
| pytest-cov | 7.1.0 | Couverture de code |
| Factory Boy | 3.3.0 | Génération de données de test |
| Faker | 25.2.0 | Données fictives réalistes |
| ruff | 0.4.4 | Linting PEP8 |
| black | 24.4.2 | Formatage automatique |
| Docker | 19.03 | Conteneurisation |
| GitLab CI/CD | — | Pipeline automatisé |

---

## Prérequis

- **Docker** >= 19.03 → [Installer Docker](https://docs.docker.com/get-docker/)
- **Docker Compose** >= 2.0
- **Git** >= 2.0
- **Python** >= 3.11 (pour le développement local sans Docker)

---

## Installation

### 1. Cloner le dépôt

```bash
git clone git@gitlab.formuloo.com:formuloo-backend/formuloo-os.git
cd formuloo-os
git checkout letissia-chowuinou
```

### 2. Aller dans le service Auth

```bash
cd services/auth
```

### 3. Créer l'environnement virtuel

```bash
py -3.11 -m venv venv
source venv/Scripts/activate  # Windows
# ou
source venv/bin/activate       # Linux/Mac
```

### 4. Installer les dépendances

```bash
pip install -r requirements.txt
```

### 5. Configurer les variables d'environnement

```bash
cp .env.example .env
# Éditer .env selon votre environnement
```

---

## Lancement

### Avec Docker Compose (recommandé)

```bash
# Lancer Auth + PostgreSQL + Redis
docker-compose up

# En arrière-plan
docker-compose up -d

# Vérifier les services
docker-compose ps

# Arrêter
docker-compose down
```

### En local (développement)

```bash
# Activer le venv
source venv/Scripts/activate

# Appliquer les migrations
python manage.py migrate

# Lancer le serveur
python manage.py runserver
```

---

## Structure du projet

```
services/auth/
├── config/
│   ├── settings.py          # Configuration Django
│   └── urls.py              # Routage principal
├── authentification/
│   ├── models/
│   │   ├── organisation.py  # Modèle Organisation (tenant)
│   │   ├── user.py          # Modèle User
│   │   ├── permission.py    # Modèle Permission (RBAC)
│   │   ├── role.py          # Modèle Role (RBAC)
│   │   ├── api_key.py       # Modèle APIKey
│   │   ├── audit_log.py     # Modèle AuditLog (immuable)
│   │   └── refresh_token.py # Modèle RefreshToken
│   ├── serializers/
│   │   ├── auth.py          # Login, logout, me, mdp
│   │   ├── organisation.py  # Organisation CRUD
│   │   ├── user.py          # User CRUD
│   │   ├── role.py          # Role + Permission
│   │   ├── api_key.py       # APIKey
│   │   ├── audit_log.py     # AuditLog
│   │   └── refresh_token.py # RefreshToken
│   ├── views/
│   │   ├── auth.py          # Login, logout, me, mdp
│   │   ├── organisation.py  # Organisation CRUD
│   │   ├── user.py          # User CRUD + activer/désactiver
│   │   ├── role.py          # Role CRUD + Permissions
│   │   ├── api_key.py       # APIKey CRUD
│   │   ├── audit_log.py     # AuditLog (lecture seule)
│   │   └── refresh_token.py # Sessions actives
│   └── urls.py              # 34 endpoints
├── tests/
│   ├── conftest.py          # Fixtures partagées
│   ├── test_auth.py         # 15 tests auth
│   ├── test_organisations.py # 10 tests organisations
│   ├── test_utilisateurs.py  # 14 tests utilisateurs
│   ├── test_roles.py         # 16 tests rôles
│   ├── test_api_keys.py      # 10 tests clés API
│   └── test_audit_logs.py    # 9 tests audit
├── Dockerfile               # Image Docker du service
├── docker-compose.yml       # Orchestration services
├── .gitlab-ci.yml           # Pipeline CI/CD
├── pytest.ini               # Configuration tests
├── requirements.txt         # Dépendances Python
└── .env                     # Variables d'environnement
```

---

## Endpoints disponibles

### Auth Service — Port 8000 (✅ Implémenté)

#### Authentification
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/v1/auth/login/` | Connexion → JWT | Public |
| POST | `/api/v1/auth/logout/` | Déconnexion | JWT |
| POST | `/api/v1/auth/refresh/` | Renouveler token | RefreshToken |
| GET | `/api/v1/auth/me/` | Profil utilisateur | JWT |
| PATCH | `/api/v1/auth/me/` | Modifier profil | JWT |
| POST | `/api/v1/auth/me/changer-mot-de-passe/` | Changer mdp | JWT |
| GET | `/api/v1/auth/refresh-tokens/` | Sessions actives | JWT |
| DELETE | `/api/v1/auth/refresh-tokens/{id}/` | Révoquer session | JWT |

#### Organisations
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/v1/auth/organisations/` | Liste tenants | SUPER_ADMIN |
| POST | `/api/v1/auth/organisations/` | Créer tenant | SUPER_ADMIN |
| GET | `/api/v1/auth/organisations/{id}/` | Détail | SUPER_ADMIN |
| PUT | `/api/v1/auth/organisations/{id}/` | Modifier | SUPER_ADMIN |
| DELETE | `/api/v1/auth/organisations/{id}/` | Supprimer | SUPER_ADMIN |

#### Utilisateurs
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/v1/auth/utilisateurs/` | Liste | JWT |
| POST | `/api/v1/auth/utilisateurs/` | Créer | JWT |
| GET | `/api/v1/auth/utilisateurs/{id}/` | Détail | JWT |
| PUT | `/api/v1/auth/utilisateurs/{id}/` | Modifier | JWT |
| DELETE | `/api/v1/auth/utilisateurs/{id}/` | Soft delete | JWT |
| POST | `/api/v1/auth/utilisateurs/{id}/activer/` | Activer | JWT |
| POST | `/api/v1/auth/utilisateurs/{id}/desactiver/` | Désactiver | JWT |
| GET | `/api/v1/auth/utilisateurs/{id}/roles/` | Rôles | JWT |
| POST | `/api/v1/auth/utilisateurs/{id}/roles/` | Assigner rôle | JWT |

#### Rôles & Permissions
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/v1/auth/roles/` | Liste rôles | JWT |
| POST | `/api/v1/auth/roles/` | Créer rôle | JWT |
| GET | `/api/v1/auth/roles/{id}/` | Détail rôle | JWT |
| PUT | `/api/v1/auth/roles/{id}/` | Modifier rôle | JWT |
| DELETE | `/api/v1/auth/roles/{id}/` | Supprimer rôle | JWT |
| GET | `/api/v1/auth/permissions/` | Liste permissions | JWT |
| GET | `/api/v1/auth/permissions/{id}/` | Détail permission | JWT |

#### Clés API & Audit
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/v1/auth/api-keys/` | Liste clés | JWT |
| POST | `/api/v1/auth/api-keys/` | Créer clé | JWT |
| GET | `/api/v1/auth/api-keys/{id}/` | Détail clé | JWT |
| DELETE | `/api/v1/auth/api-keys/{id}/` | Révoquer clé | JWT |
| GET | `/api/v1/auth/audit-logs/` | Journal audit | JWT |
| GET | `/api/v1/auth/audit-logs/{id}/` | Détail log | JWT |

---

## Authentification

Tous les endpoints (sauf `/auth/login/`) requièrent un **JWT Bearer Token**.

### Obtenir un token

```bash
curl -X POST http://localhost:8000/api/v1/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@pme-alpha.com", "password": "motdepasse123"}'
```

R�ponse :
```json
{
  "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "email": "admin@pme-alpha.com",
    "roles": ["ADMIN_PME"],
    "tenant_id": "uuid-pme-alpha"
  }
}
```

### Utiliser le token

```bash
curl http://localhost:8000/api/v1/auth/utilisateurs/ \
  -H "Authorization: Bearer <votre_access_token>"
```

### Renouveler le token (après 15 minutes)

```bash
curl -X POST http://localhost:8000/api/v1/auth/refresh/ \
  -H "Content-Type: application/json" \
  -d '{"refresh": "<votre_refresh_token>"}'
```

---

## Tests

### Lancer tous les tests

```bash
# Tests avec couverture de code
pytest

# Résultats attendus
# 73 passed — Coverage: 92%
```

### Résultats actuels

```
tests/test_api_keys.py       10/10 ✅
tests/test_audit_logs.py      9/9  ✅
tests/test_auth.py           15/15 ✅
tests/test_organisations.py  10/10 ✅
tests/test_roles.py          16/16 ✅
tests/test_utilisateurs.py   14/14 ✅
─────────────────────────────────────
Total : 73/73 ✅ — Couverture : 92%
```

### Tests de conformité OpenAPI (Schemathesis)

```bash
schemathesis run http://localhost:8000/api/schema/ \
  --checks all \
  --auth-type=bearer \
  --auth=<votre_token>
```

---

## Documentation API

La documentation interactive Swagger UI est accessible sur :

| Service | URL Swagger UI |
|---------|----------------|
| Auth | http://localhost:8000/api/schema/swagger-ui/ |
| HR | http://localhost:8001/api/schema/swagger-ui/ |
| Compta | http://localhost:8002/api/schema/swagger-ui/ |
| CRM | http://localhost:8003/api/schema/swagger-ui/ |
| Stock | http://localhost:8004/api/schema/swagger-ui/ |
| Analytics | http://localhost:8005/api/schema/swagger-ui/ |

---

## Contrats OpenAPI

Les contrats OpenAPI sont versionnés dans :

```
contracts/
├── auth/v1/auth.yaml       ✅ v2.0.0 — 34 endpoints
├── hr/v1/hr.yaml           ⏳ Sprint 2
├── compta/v1/compta.yaml   ⏳ Sprint 2
├── crm/v1/crm.yaml         ⏳ Sprint 3
├── stock/v1/stock.yaml     ⏳ Sprint 3
└── analytics/v1/analytics.yaml ⏳ Sprint 4
```

> ⚠️ **Toute modification de contrat doit être validée par les deux équipes
> avant implémentation. Voir GOVERNANCE.md.**

---

## Variables d'environnement

```env
# Django
SECRET_KEY=django-insecure-formuloo-os-dev-2024-auth-service
DEBUG=True
ALLOWED_HOSTS=localhost,127.0.0.1

# PostgreSQL
DB_NAME=db_auth
DB_USER=taskuser
DB_PASSWORD=taskpassword
DB_HOST=192.168.99.100
DB_PORT=5433

# Redis
REDIS_URL=redis://192.168.99.100:6379/0

# JWT
ACCESS_TOKEN_LIFETIME=15
REFRESH_TOKEN_LIFETIME=7

# Keycloak
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=formuloo
KEYCLOAK_CLIENT_ID=formuloo-backend
KEYCLOAK_CLIENT_SECRET=your-secret-here
```

---

## Pipeline CI/CD GitLab

Le fichier `.gitlab-ci.yml` automatise :

```
1. lint   → ruff + black (vérification style PEP8)
2. test   → pytest + couverture ≥ 80%
3. build  → docker build (construction image)
4. deploy → déploiement staging automatique
```

---

## Auteur

**CHOWUINOU TEGUIA Letissia**
Master 2 Systèmes d'Information / Génie Logiciel
Entreprise d'accueil : **Formuloo**

---


# GOVERNANCE.md — Gouvernance des contrats OpenAPI
## Formuloo OS — Backend API REST

**Projet :** Formuloo OS  
**Auteur :** CHOWUINOU TEGUIA  
**Version :** 1.0.0  
**Mise à jour :** Juin 2026 — Alignement avec l'implémentation réelle du module Auth

---

## 1. Objectif

Ce document définit les règles et conventions que toutes les équipes (Backend et Frontend) doivent respecter lors de la conception, l'implémentation et l'évolution des contrats OpenAPI 3 de Formuloo OS. Il constitue le socle de la collaboration entre les équipes et garantit la stabilité de l'API publique.

---

## 2. Principes fondamentaux

- **Contract-First** : le contrat OpenAPI est défini et validé AVANT toute implémentation.
- **Un contrat par domaine** : chaque microservice possède son propre fichier YAML versionné.
- **Aucune dérive silencieuse** : toute modification du contrat passe par le processus de validation défini en section 7.
- **Rétrocompatibilité** : une version publiée ne casse jamais ses consommateurs existants.
- **Isolation multi-tenant** : chaque endpoint filtre automatiquement les données par tenant_id extrait du JWT.
- **Traçabilité** : chaque action importante génère automatiquement un AuditLog immuable.

---

## 3. Structure du dépôt de contrats

```
formuloo-contracts/
├── README.md
├── GOVERNANCE.md          
├── auth/
│   └── v1/
│       └── auth.yaml       ← Module Auth (v1.0.0 — complet)
├── hr/
│   └── v1/
│       └── hr.yaml
├── compta/
│   └── v1/
│       └── compta.yaml
├── crm/
│   └── v1/
│       └── crm.yaml
├── stock/
│   └── v1/
│       └── stock.yaml
└── analytics/
    └── v1/
        └── analytics.yaml
```

---

## 4. Conventions de nommage des endpoints

### 4.1 Structure de l'URL

```
/api/{version}/{domaine}/{ressource}/
/api/{version}/{domaine}/{ressource}/{id}/
/api/{version}/{domaine}/{ressource}/{id}/{action}/
```

### 4.2 Règles

| Règle | Correct ✅ | Incorrect ❌ |
|-------|-----------|------------|
| Version toujours présente | `/api/v1/hr/employes/` | `/api/hr/employes/` |
| Domaine en minuscule | `/api/v1/hr/` | `/api/v1/HR/` |
| Ressource au pluriel | `/api/v1/hr/employes/` | `/api/v1/hr/employe/` |
| Séparateur tiret | `/api/v1/hr/fiches-paie/` | `/api/v1/hr/fichesPaie/` |
| Pas de verbe dans l'URL | `POST /api/v1/hr/employes/` | `/api/v1/hr/creerEmploye/` |
| Actions spéciales avec verbe | `POST /api/v1/auth/utilisateurs/{id}/activer/` | `/api/v1/auth/activerUtilisateur/{id}/` |

### 4.3 Exemples complets par domaine

```
# Module Auth (implémenté — Sprint 1)
POST   /api/v1/auth/login/
POST   /api/v1/auth/logout/
POST   /api/v1/auth/refresh/
GET    /api/v1/auth/me/
PATCH  /api/v1/auth/me/
POST   /api/v1/auth/me/changer-mot-de-passe/
GET    /api/v1/auth/refresh-tokens/
DELETE /api/v1/auth/refresh-tokens/{id}/
GET    /api/v1/auth/organisations/
POST   /api/v1/auth/organisations/
GET    /api/v1/auth/organisations/{id}/
PUT    /api/v1/auth/organisations/{id}/
DELETE /api/v1/auth/organisations/{id}/
GET    /api/v1/auth/utilisateurs/
POST   /api/v1/auth/utilisateurs/
GET    /api/v1/auth/utilisateurs/{id}/
PUT    /api/v1/auth/utilisateurs/{id}/
DELETE /api/v1/auth/utilisateurs/{id}/
POST   /api/v1/auth/utilisateurs/{id}/activer/
POST   /api/v1/auth/utilisateurs/{id}/desactiver/
GET    /api/v1/auth/utilisateurs/{id}/roles/
POST   /api/v1/auth/utilisateurs/{id}/roles/
GET    /api/v1/auth/roles/
POST   /api/v1/auth/roles/
GET    /api/v1/auth/roles/{id}/
PUT    /api/v1/auth/roles/{id}/
DELETE /api/v1/auth/roles/{id}/
GET    /api/v1/auth/permissions/
GET    /api/v1/auth/permissions/{id}/
GET    /api/v1/auth/api-keys/
POST   /api/v1/auth/api-keys/
GET    /api/v1/auth/api-keys/{id}/
DELETE /api/v1/auth/api-keys/{id}/
GET    /api/v1/auth/audit-logs/
GET    /api/v1/auth/audit-logs/{id}/

# Module RH (Sprint 2 — à venir)
GET    /api/v1/hr/employes/
POST   /api/v1/hr/employes/
GET    /api/v1/hr/employes/{id}/
PUT    /api/v1/hr/employes/{id}/
DELETE /api/v1/hr/employes/{id}/
GET    /api/v1/hr/employes/{id}/conges/
GET    /api/v1/hr/conges/
POST   /api/v1/hr/paies/

# Module Comptabilité (Sprint 2 — à venir)
GET    /api/v1/compta/factures/
POST   /api/v1/compta/factures/
GET    /api/v1/compta/paiements/
GET    /api/v1/compta/bilans/

# Module CRM (Sprint 3 — à venir)
GET    /api/v1/crm/clients/
GET    /api/v1/crm/prospects/
GET    /api/v1/crm/opportunites/

# Module Stock (Sprint 3 — à venir)
GET    /api/v1/stock/produits/
POST   /api/v1/stock/mouvements/
GET    /api/v1/stock/fournisseurs/

# Module Analytics (Sprint 4 — à venir)
GET    /api/v1/analytics/tableaux-bord/
GET    /api/v1/analytics/rh/
GET    /api/v1/analytics/finance/
GET    /api/v1/analytics/rapports/
```

---

## 5. Standards de réponse JSON

### 5.1 Codes HTTP à utiliser

| Code | Signification | Quand l'utiliser |
|------|--------------|-----------------|
| 200 | OK | GET, PUT, PATCH réussi |
| 201 | Created | POST réussi (ressource créée) |
| 204 | No Content | DELETE réussi, logout, changer-mot-de-passe |
| 400 | Bad Request | Données invalides envoyées |
| 401 | Unauthorized | Token JWT absent, expiré ou révoqué |
| 403 | Forbidden | Token valide mais permissions insuffisantes |
| 404 | Not Found | Ressource introuvable ou appartenant à un autre tenant |
| 409 | Conflict | Conflit (ex: email déjà existant dans le tenant) |
| 422 | Unprocessable Entity | Validation métier échouée |
| 429 | Too Many Requests | Rate limiting déclenché |
| 500 | Internal Server Error | Erreur interne du serveur |

### 5.2 Format de réponse — liste paginée

```json
{
  "count": 42,
  "next": "/api/v1/auth/utilisateurs/?page=2",
  "previous": null,
  "results": [
    {
      "id": "4388ffa0-c5fa-4136-9270-0abadc873747",
      "email": "employe@pme-alpha.com",
      "first_name": "Jean",
      "last_name": "Dupont",
      "roles": ["EMPLOYE"],
      "is_active": true,
      "created_at": "2026-06-08T19:51:14Z"
    }
  ]
}
```

### 5.3 Format de réponse — objet unique

```json
{
  "id": "4388ffa0-c5fa-4136-9270-0abadc873747",
  "email": "admin@pme-alpha.com",
  "first_name": "Jean",
  "last_name": "Dupont",
  "avatar_url": null,
  "roles": ["ADMIN_PME"],
  "tenant_id": "uuid-pme-alpha",
  "is_active": true,
  "is_verified": false,
  "created_at": "2026-06-08T19:51:14Z",
  "updated_at": "2026-06-08T19:51:14Z"
}
```

### 5.4 Format de réponse — erreur

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Les données fournies sont invalides.",
    "details": [
      {
        "field": "email",
        "message": "Ce champ est obligatoire."
      },
      {
        "field": "password",
        "message": "Ce champ doit contenir au moins 8 caractères."
      }
    ]
  }
}
```

### 5.5 Format de réponse — message de succès

```json
{
  "message": "Utilisateur activé avec succès."
}
```

### 5.6 Format de réponse — création de clé API
> ⚠️ La clé brute est retournée UNE SEULE FOIS lors de la création.

```json
{
  "id": "uuid-api-key",
  "name": "Intégration Mobile Money",
  "key": "fk_live_abc123def456...",
  "scopes": ["compta.read.factures"],
  "rate_limit": 100,
  "created_at": "2026-06-08T21:00:00Z",
  "message": "Conservez cette clé — elle ne sera plus affichée."
}
```

### 5.7 Règles de formatage

- **Dates** : format ISO 8601 → `"2026-06-08T19:51:14Z"`
- **Identifiants** : UUID v4 (format `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`)
- **Pagination** : paramètres `page` et `page_size` (défaut : 20, max : 100)
- **Tri** : paramètre `ordering` → `?ordering=last_name` ou `?ordering=-created_at`
- **Recherche** : paramètre `search` → `?search=Dupont`
- **Filtres** : paramètres nommés → `?is_active=true`
- **Devise** : paramètre `devise` → `?devise=XAF` (XAF, EUR, USD)
- **Noms de champs** : snake_case → `first_name`, `last_name`, `tenant_id`

---

## 6. Modèle multi-tenant

### 6.1 Principe d'isolation

Chaque entité de la base de données possède un champ `tenant_id` (UUID) qui identifie l'organisation cliente à laquelle elle appartient.

```
Organisation A (tenant_id = uuid-A)
├── Utilisateurs de A
├── Rôles de A
├── Données RH de A
└── Données Comptabilité de A

Organisation B (tenant_id = uuid-B)
├── Utilisateurs de B
├── Rôles de B  
└── ...
```

### 6.2 Extraction automatique du tenant

Le `tenant_id` est extrait automatiquement du token JWT à chaque requête via le `TenantMiddleware` Django. Aucun endpoint ne requiert que le `tenant_id` soit passé manuellement, sauf lors de la création d'un utilisateur par le SUPER_ADMIN.

### 6.3 Règle d'isolation absolue

> ⚠️ Un endpoint retourne toujours 404 (et jamais 403) quand une ressource existe mais appartient à un autre tenant. Cela évite de révéler l'existence de données d'autres organisations.

---

## 7. Sécurité et authentification

### 7.1 Tokens JWT

| Token | Durée de vie | Utilisation |
|-------|-------------|-------------|
| Access Token | 15 minutes | Accéder aux endpoints protégés |
| Refresh Token | 7 jours | Obtenir un nouvel Access Token |

### 7.2 Contenu du JWT

```json
{
  "user_id": "uuid-utilisateur",
  "tenant_id": "uuid-organisation",
  "roles": ["ADMIN_PME"],
  "exp": 1780950061,
  "iat": 1780949161
}
```

### 7.3 Rôles système disponibles

| Code | Description | Périmètre |
|------|------------|----------|
| `SUPER_ADMIN` | Accès total à la plateforme | Toutes les organisations |
| `ADMIN_PME` | Accès total à son organisation | Son tenant uniquement |
| `RH_MANAGER` | Accès au module RH | Son tenant uniquement |
| `COMPTABLE` | Accès au module Comptabilité | Son tenant uniquement |
| `COMMERCIAL` | Accès au module CRM | Son tenant uniquement |
| `EMPLOYE` | Accès à ses propres données | Ses données uniquement |

### 7.4 Format des permissions RBAC

```
module.action.ressource

Exemples :
hr.read.employes
hr.write.employes
hr.delete.employes
compta.read.factures
crm.write.clients
```

---

## 8. Journal d'audit (AuditLog)

### 8.1 Principe

Chaque action importante génère automatiquement un AuditLog **immuable**. Ces journaux ne peuvent jamais être modifiés ou supprimés.

### 8.2 Actions tracées

| Action | Déclencheur |
|--------|------------|
| `LOGIN` | Connexion réussie |
| `LOGOUT` | Déconnexion |
| `CHANGE_PASSWORD` | Changement de mot de passe |
| `UPDATE_PROFILE` | Modification du profil |
| `CREATE_USER` | Création d'un utilisateur |
| `UPDATE_USER` | Modification d'un utilisateur |
| `DELETE_USER` | Désactivation d'un utilisateur |
| `ACTIVATE_USER` | Activation d'un compte |
| `DEACTIVATE_USER` | Désactivation d'un compte |
| `ASSIGN_ROLE` | Assignation d'un rôle |
| `CREATE_ROLE` | Création d'un rôle |
| `UPDATE_ROLE` | Modification d'un rôle |
| `DELETE_ROLE` | Suppression d'un rôle |
| `CREATE_ORGANISATION` | Création d'une organisation |
| `UPDATE_ORGANISATION` | Modification d'une organisation |
| `DELETE_ORGANISATION` | Suppression d'une organisation |
| `CREATE_APIKEY` | Création d'une clé API |
| `REVOKE_APIKEY` | Révocation d'une clé API |

---

## 9. Stratégie de versionnement

### 9.1 Principe

Le versionnement se fait dans l'URL : `/api/v1/`, `/api/v2/`, etc.

### 9.2 Quand créer une nouvelle version ?

| Type de changement | Action requise |
|-------------------|---------------|
| Ajout d'un champ optionnel | Aucune — rétrocompatible ✅ |
| Ajout d'un nouvel endpoint | Aucune — rétrocompatible ✅ |
| Renommage d'un champ | Nouvelle version (v2) ⚠️ |
| Suppression d'un champ | Nouvelle version (v2) ⚠️ |
| Changement de type d'un champ | Nouvelle version (v2) ⚠️ |
| Changement de structure de réponse | Nouvelle version (v2) ⚠️ |

### 9.3 Politique de dépréciation

1. Annoncer la dépréciation avec le header `Deprecation: true`
2. Maintenir l'ancienne version pendant **minimum 3 mois**
3. Documenter la migration dans le `CHANGELOG.md`
4. Supprimer uniquement après accord des deux équipes et du tuteur

---

## 10. Processus de validation d'un contrat

### 10.1 Création d'un nouveau contrat

```
1. Backend rédige le fichier YAML dans une branche feature
   └── branche : letissia-chowuinou/feature/contrat-hr-v1

2. Backend ouvre une Merge Request sur formuloo-contracts
   └── MR title : [CONTRAT] Initialisation contrat HR v1

3. Frontend examine et commente (délai : 2 jours ouvrés)
   └── Points vérifiés : schémas, endpoints, codes d'erreur

4. Les deux équipes valident → MR mergée sur main

5. Backend implémente les endpoints Django
   └── Branche : letissia-chowuinou/feature/hr-api

6. drf-spectacular génère la spec OpenAPI depuis le code

7. Schemathesis valide la conformité en CI/CD GitLab
   └── Si dérive détectée → pipeline rouge → correction obligatoire
```

### 10.2 Modification d'un contrat existant

```
1. L'équipe demandeuse ouvre une issue sur formuloo-contracts
   └── Issue title : [EVOLUTION] Ajout champ téléphone sur Employé

2. Discussion synchrone (réunion ou chat) entre Backend et Frontend

3. Si accord → MR + validation comme en 10.1
4. Si désaccord → escalade au tuteur technique
```

### 10.3 Règle absolue

> ⚠️ **Aucune modification de contrat n'est implémentée sans Merge Request validée par les deux équipes.**

---

## 11. Outils de validation

| Outil | Version | Rôle | Quand |
|-------|---------|------|-------|
| `drf-spectacular` | 0.27.2 | Génère la spec OpenAPI depuis le code Django | À chaque push |
| `Schemathesis` | Latest | Teste la conformité API vs contrat OpenAPI | Pipeline CI/CD GitLab |
| `Prism` | Latest | Mock server pour le Frontend Angular | Développement local |
| `Swagger UI` | Via drf-spectacular | Documentation interactive | `/api/schema/swagger-ui/` |
| `pytest` | 8.2.2 | Tests unitaires et d'intégration | Pipeline CI/CD |
| `ruff` | 0.4.4 | Linting Python (PEP8) | Pipeline CI/CD |
| `black` | 24.4.2 | Formatage automatique du code | Pipeline CI/CD |

---

## 12. Stack technique de référence

| Composant | Technologie | Version |
|-----------|------------|---------|
| Langage | Python | 3.11.9 |
| Framework | Django | 4.2.13 LTS |
| API REST | Django REST Framework | 3.14.0 |
| Auth JWT | djangorestframework-simplejwt | 5.3.1 |
| SSO | Keycloak | Latest |
| Base de données | PostgreSQL | 15-alpine |
| Cache / Blacklist | Redis | 8.0.0 |
| Documentation API | drf-spectacular | 0.27.2 |
| Tests | pytest + pytest-django | 8.2.2 |
| Données de test | Factory Boy + Faker | 3.3.0 / 25.2.0 |
| Conteneurisation | Docker | 19.03 |
| CI/CD | GitLab CI/CD | — |

---

## 13. Décisions architecturales (ADR)

Les décisions architecturales importantes sont documentées dans des ADR (Architecture Decision Records) déposés dans le dépôt :

| ADR | Décision | Statut |
|-----|---------|--------|
| ADR-001 | Architecture Microservices purs | ✅ Accepté |
| ADR-002 | Authentification SSO via Keycloak + JWT | ✅ Accepté |
| ADR-003 | Versionnement par URL (/api/v1/) | ✅ Accepté |
| ADR-004 | PostgreSQL + Redis par service | ✅ Accepté |
| ADR-005 | Simplification modèle Organisation au Sprint 1 | ✅ Accepté |

---

## 14. Contacts et responsabilités

| Rôle | Responsabilité |
|------|---------------|
| **Backend** (CHOWUINOU TEGUIA) | Implémentation, publication des contrats, conformité OpenAPI |
| **Frontend** | Consommation, génération du client TypeScript (ng-openapi-gen) |
| **Tuteur technique** | Arbitrage en cas de désaccord entre les équipes |

---

## 15. Glossaire

| Terme | Définition |
|-------|-----------|
| **Contract-First** | Approche où le contrat est défini avant le code |
| **OpenAPI 3** | Standard de description des API REST (fichier YAML/JSON) |
| **Schemathesis** | Outil de test de conformité API vs contrat OpenAPI |
| **drf-spectacular** | Bibliothèque Django qui génère la spec OpenAPI automatiquement |
| **Prism** | Mock server qui simule une API à partir d'un contrat OpenAPI |
| **Tenant** | Une PME cliente utilisant Formuloo OS |
| **JWT** | Token d'authentification signé émis par Keycloak |
| **RBAC** | Contrôle d'accès basé sur les rôles (Role-Based Access Control) |
| **AuditLog** | Journal immuable des actions utilisateurs |
| **Soft delete** | Désactivation d'un enregistrement sans suppression physique |
| **Rate Limiting** | Limitation du nombre de requêtes par période |
| **Multi-tenant** | Architecture où plusieurs organisations partagent la même infrastructure |
| **SSO** | Authentification unique (Single Sign-On) via Keycloak |
| **UUID** | Identifiant unique universel utilisé comme clé primaire |

---

*Document validé par les équipes Backend et Frontend — Formuloo OS 2024-2025*  
*CHOWUINOU TEGUIA · Master 2 Systèmes d'Information / Génie Logiciel · IUC / ISTDI / 3iAC*
# ADR-002 : Strategie offline-first avec SQLDelight

## Statut

Accepte (ticket FOR16A26-985 — Vision & architecture multiplatform)

## Contexte

Les PME africaines ciblees par Formuloo OS operent frequemment dans des zones
a connectivite reseau limitee ou intermittente. L'application doit donc rester
utilisable hors-ligne (consultation et saisie), puis synchroniser les donnees
des qu'une connexion redevient disponible.

## Decision

- **SQLDelight** (`core:database`) sert de source de verite locale pour
  l'ensemble des entites metier (RH, CRM, stock, projets, ...). Chaque module
  `feature/*` qui a besoin de persistance ajoute ses propres fichiers `.sq`
  dans `core:database`.
- Les drivers SQLDelight sont fournis via l'interface `DatabaseDriverFactory`
  (`core:database`, commonMain), avec une implementation par plateforme :
  - Android : `AndroidSqliteDriver`
  - iOS : `NativeSqliteDriver`
  - Desktop (JVM) : `JdbcSqliteDriver`
  Ces implementations sont injectees via le `platformModule` Koin de
  `composeApp` (cf. ADR-001), evitant tout `expect`/`actual class` avec
  constructeurs incompatibles entre plateformes.
- Une table technique `syncMetadata` (entite, date de derniere
  synchronisation) permet a chaque repository de determiner quand
  resynchroniser une entite avec l'API Django.
- **Flux de donnees** : un repository expose les donnees locales via
  `Flow` (SQLDelight `coroutines-extensions`), declenche un appel reseau
  (`core:network`, wrappe par `safeApiCall` -> `NetworkResult`) en arriere-plan,
  et met a jour la base locale en cas de succes. L'UI ne lit jamais directement
  le reseau : elle observe la base locale.

## Consequences

- Toute ecriture utilisateur (creation/modification) est d'abord persistee en
  local, puis synchronisee — l'UI reste reactive meme hors-ligne.
- La gestion de conflits (donnees modifiees localement et a distance) est hors
  perimetre de ce ticket fondateur ; elle sera traitee module par module au fur
  et a mesure de l'implementation des fonctionnalites CRUD.
- Toute requete SQLDelight doit s'executer hors du thread principal
  (cf. regle "pas d'I/O sur le thread Main", README).

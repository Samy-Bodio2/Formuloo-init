# ADR-001 : Clean Architecture multiplateforme (KMP) + MVVM

## Statut

Accepte (ticket FOR16A26-985 — Vision & architecture multiplatform)

## Contexte

Formuloo OS est un ERP destine aux PME africaines, livre sur Android, iOS et
Desktop (JVM) a partir d'une base de code Kotlin Multiplatform unique, et
consomme une API Django REST securisee par Keycloak/JWT. Pour eviter la
duplication de la logique metier entre plateformes et garder une base de code
testable et evolutive sur le long terme, une architecture commune doit etre
definie des le depart et appliquee a tous les modules `feature/*`.

## Decision

### Decoupage en modules Gradle

- `core/*` : briques techniques transverses, sans dependance vers `feature/*`
  (sens unique `feature -> core`).
  - `core:common` — types partages (`UiState`, `NetworkResult`, extensions).
  - `core:network` — client Ktor multiplateforme + intercepteur d'auth.
  - `core:database` — cache local SQLDelight (voir ADR-002).
  - `core:auth` — `TokenRepository`, `AuthState` (voir ADR-003).
  - `core:designsystem` — theme, couleurs, typographie Compose Multiplatform.
  - `core:navigation` — routes typees (`Route`) et `AppNavHost` generique.
- `feature/*` : un module par domaine metier (`auth`, `hr`, `crm`, `stock`,
  `projects`, `analytics`), 100% Kotlin Multiplatform (Compose UI + ViewModel).
- `composeApp` : point d'entree Android + Desktop, agrege les modules
  `feature/*` et `core/*`, declare le graphe de navigation et les modules Koin.
- `iosApp` : projet Xcode consommant les frameworks produits par les modules
  KMP.

### Couches au sein d'un module `feature`

Chaque feature suit une variante allegee de Clean Architecture / MVVM :

1. **Presentation** : composables Compose Multiplatform (ex. `LoginScreen`)
   + `ViewModel` (`androidx.lifecycle.ViewModel` multiplateforme).
2. **State** : le `ViewModel` expose un unique `StateFlow<UiState<T>>`
   (`UiState` defini dans `core:common`) consomme via
   `collectAsStateWithLifecycle()`.
3. **Domaine / Donnees** : acces aux donnees via `core:network` (API distante)
   et `core:database` (cache local), wrappes par `NetworkResult` /
   `safeApiCall` (`core:common`).

### Injection de dependances

Koin est utilise sur toutes les plateformes :

- chaque module (`core:*`, `feature:*`) expose un `Module` Koin
  (`coreAuthModule`, `featureHrModule`, ...).
- `composeApp/AppModule.kt` agrege l'ensemble des modules dans `appModules`
  et declare un `expect val platformModule : Module` pour les dependances
  specifiques a la plateforme (ex. `DatabaseDriverFactory`).

## Consequences

- Les regles de dependance (`feature -> core`, jamais l'inverse) doivent etre
  respectees pour eviter les cycles entre modules.
- Toute nouvelle fonctionnalite ajoute son module `feature/*` avec son propre
  module Koin, sans modifier les modules `core/*`.
- Le code metier (ViewModel, repositories) est 100% commun ; seul le code
  d'acces aux ressources systeme (driver SQLDelight, moteur HTTP, settings)
  utilise `expect`/`actual`.

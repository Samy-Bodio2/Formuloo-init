# ADR-003 : Authentification via Ktor multiplateforme + stockage securise

## Statut

Accepte (ticket FOR16A26-985 — Vision & architecture multiplatform)

## Contexte

L'API Django REST de Formuloo OS est securisee par Keycloak (OAuth2 / JWT).
Le client multiplateforme doit pouvoir s'authentifier, conserver les jetons de
maniere securisee, les joindre automatiquement aux requetes, et les
rafraichir en cas d'expiration — sur Android, iOS et Desktop.

## Decision

- **Client HTTP** : `core:network` construit un `HttpClient` Ktor
  (`createHttpClient`) avec un moteur par plateforme, choisi via
  `expect fun platformEngineFactory(): HttpClientEngineFactory<*>` :
  - Android : `Android` (`io.ktor:ktor-client-android`)
  - iOS : `Darwin` (`io.ktor:ktor-client-darwin`)
  - Desktop (JVM) : `Java` (`io.ktor:ktor-client-java`)
- Le client installe les plugins `ContentNegotiation` (JSON,
  `kotlinx.serialization`), `Logging`, `HttpTimeout`, et **`Auth`** (provider
  `bearer`) :
  - `loadTokens` lit le couple access/refresh token depuis `TokenRepository`.
  - `refreshTokens` est le point d'extension pour appeler le endpoint de
    rafraichissement Keycloak (implementation a completer lors de la mise en
    oeuvre de `feature:auth`).
- **Stockage des jetons** : `core:auth` expose `TokenRepository`, base sur
  `multiplatform-settings-no-arg` (`com.russhwolf.settings.Settings()`).
  Cette dependance fournit deja ses propres implementations `expect`/`actual`
  par plateforme (Keystore/EncryptedSharedPreferences sur Android, Keychain
  via NSUserDefaults sur iOS, fichier de preferences sur Desktop), ce qui evite
  d'ecrire du code `expect`/`actual` supplementaire dans `core:auth`.
- **Etat d'authentification** : `AuthState` (`Authenticated`,
  `Unauthenticated`, `Unknown`) est expose par `feature:auth`'s
  `AuthViewModel` via `StateFlow<UiState<AuthState>>`, et pilote la navigation
  initiale (`Route.Login` -> `Route.Home`).

## Consequences

- Aucune logique d'authentification specifique a une plateforme ne doit fuiter
  dans les modules `feature/*` : seule l'usine de moteur HTTP
  (`platformEngineFactory`) est `expect`/`actual`.
- Le rafraichissement de jeton reste centralise dans `core:network`, ce qui
  garantit un comportement uniforme (retry transparent) pour tous les appels
  API, quel que soit le module appelant.
- L'URL de base de l'API (`ApiConfig.BASE_URL`) est centralisee dans
  `core:network` ; son passage en configuration par environnement
  (dev/staging/prod) est a traiter dans un ticket ulterieur.

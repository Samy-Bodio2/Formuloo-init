# Formuloo OS

ERP multiplateforme pour PME africaines, construit en **Kotlin Multiplatform**
(Android, iOS, Desktop/JVM) avec **Compose Multiplatform**, consommant une API
**Django REST** securisee par **Keycloak / JWT**.

## Stack technique

| Domaine            | Choix                                            |
|--------------------|--------------------------------------------------|
| Langage / UI       | Kotlin 2.4.0, Compose Multiplatform 1.11.1       |
| Build              | Gradle 9.1.0 + AGP 9.0.1 (`build-logic` convention plugins) |
| DI                 | Koin 4.1.1                                        |
| Reseau             | Ktor Client 3.4.0 (Android / Darwin / Java engines) |
| Persistance locale | SQLDelight 2.3.2 (offline-first)                 |
| Stockage securise  | multiplatform-settings 1.3.0                     |
| Navigation         | Navigation Compose 2.9.2 (routes typees)         |
| Async              | Coroutines / Flow 1.11.0                         |

La matrice de compatibilite Gradle / AGP / Kotlin / Compose retenue est
documentee en tete de [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Structure des modules

```
formuloo-os/
├── build-logic/                  # Convention plugins Gradle
│   └── convention/               # formuloo.kmp.library, formuloo.kmp.feature,
│                                  # formuloo.android.application
├── composeApp/                   # Point d'entree Android + Desktop
├── iosApp/                       # Projet Xcode (iOS)
├── core/
│   ├── common/                   # UiState, NetworkResult, extensions
│   ├── network/                  # Client Ktor multiplateforme + auth
│   ├── database/                 # SQLDelight (offline-first)
│   ├── auth/                     # TokenRepository, AuthState
│   ├── designsystem/             # Theme / couleurs / typographie Compose
│   └── navigation/                # Routes typees + AppNavHost
├── feature/
│   ├── auth/                      # Ecran de connexion
│   ├── hr/                         # Ressources humaines
│   ├── crm/                         # CRM
│   ├── stock/                        # Gestion de stock
│   ├── projects/                      # Gestion de projets
│   └── analytics/                      # Tableaux de bord
├── docs/adr/                           # Architecture Decision Records
└── gradle/libs.versions.toml           # Catalogue de versions
```

Regle de dependance : `feature/* -> core/*`, jamais l'inverse.

## Lancer l'application

### Android

```bash
./gradlew :composeApp:assembleDebug
```

ou via le run widget de l'IDE (configuration `composeApp`).

### Desktop (JVM)

```bash
./gradlew :composeApp:run
```

### iOS

Ouvrir [`iosApp/iosApp.xcodeproj`](iosApp/iosApp.xcodeproj) dans Xcode et
lancer depuis l'IDE.

## Regles du projet

1. **Pas d'I/O sur le thread Main** : tout acces reseau (`core:network`) ou
   base de donnees (`core:database`) s'execute via des coroutines sur un
   dispatcher adapte (`Dispatchers.IO`/`Default`).
2. **Coroutines / Flow partout** : les repositories exposent des `Flow`, les
   `ViewModel` exposent un `StateFlow<UiState<T>>` (`core:common`).
3. **`expect`/`actual` reserve au code plateforme** : moteur HTTP
   (`core:network`), driver SQLDelight (`core:database`). La logique metier
   (ViewModel, repositories, ecrans) reste 100% commune dans `commonMain`.
4. **Wrapper standard pour le reseau** : tout appel API passe par
   `safeApiCall` -> `NetworkResult`, converti en `UiState` via
   `NetworkResult.asUiState()`.
5. **DI Koin par module** : chaque module `core:*`/`feature:*` expose son
   propre `Module` Koin, agrege dans `composeApp/AppModule.kt`.

## Architecture Decision Records

- [ADR-001 — Clean Architecture multiplateforme + MVVM](docs/adr/ADR-001-clean-architecture-kmp.md)
- [ADR-002 — Offline-first avec SQLDelight](docs/adr/ADR-002-offline-first-sqldelight.md)
- [ADR-003 — Authentification Ktor multiplateforme](docs/adr/ADR-003-auth-ktor-multiplatform.md)

---

En savoir plus sur [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).

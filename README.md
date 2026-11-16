# ONFAA TUNNEL

ONFAA TUNNEL est un client VPN Android natif visant à fournir une connexion VPN réelle basée sur le cœur officiel **XTLS/Xray-core** (upstream : https://github.com/XTLS/Xray-core) via **libXray** (après vérification de compatibilité/API officielle).

> **État actuel : Phase 2 — Audit technique upstream (en cours / partiellement réalisé).**  
> Les fondations Android (Phase 1) sont validées en CI. Un audit des sources officielles **XTLS/Xray-core** et **XTLS/libXray** (versions/tags/commits, API, stratégie de build Android) est documenté. **Aucun moteur VPN, aucune connexion réelle, aucune dépendance libXray intégrée à l’app et aucune preuve de concept de build/compilation engagée dans le code à ce stade.**

## Objectifs

- Connexion VPN réelle sur Android (pas de simulation).
- Support des protocoles **VLESS** et **VMess**.
- Importation et gestion de configurations personnelles.
- Gestion des serveurs et sélection active.
- Statistiques réseau réelles (débit, trafic, latence selon capacités).
- Réglages DNS, routage, journaux et diagnostics.
- Interface complète avec thèmes personnalisables.

## Technologies (Phase 1)

- **Android natif**, **Kotlin** (plugin Compose), **AndroidX**, **Jetpack Compose** (Material 3), **Navigation Compose**
- **Gradle** avec catalogue de versions (`gradle/libs.versions.toml`), wrapper Gradle commité
- **Room** (entité/DAO/base préparés), **DataStore Preferences** (dépôt de préférences préparé)
- **Coroutines & Flow**, **JUnit 4** pour tests unitaires
- **Android `VpnService`** : préparation manifeste + classe de base `OnfaaTunnelVpnService` (aucun tunnel)

Versions épinglées et choix documentés dans [`docs/DECISIONS.md`](docs/DECISIONS.md) et [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md).

## État du dépôt

- Branche principale : `main`
- Package Android : `com.onfaa.tunnel` (`applicationId` identique)
- Suivi : [`STATUS.md`](STATUS.md), [`CHANGELOG.md`](CHANGELOG.md), [`docs/ROADMAP.md`](docs/ROADMAP.md)
- Séparation des responsabilités : [`docs/ANDROID_XRAY_SEPARATION.md`](docs/ANDROID_XRAY_SEPARATION.md)
- Audit upstream (Phase 2) : [`docs/XRAY_UPSTREAM_AUDIT.md`](docs/XRAY_UPSTREAM_AUDIT.md)
- Référence API libXray vérifiée : [`docs/LIBXRAY_API_REFERENCE.md`](docs/LIBXRAY_API_REFERENCE.md)
- Preuve de concept Android libXray (Phase 2B) : [`docs/LIBXRAY_ANDROID_POC.md`](docs/LIBXRAY_ANDROID_POC.md)

## Structure du projet

```
.
├── app/                  # Module Android (sources, ressources, tests, manifeste)
├── gradle/               # Wrapper + libs.versions.toml
├── docs/                 # Documentation projet, architecture, build, sécurité, design
├── .github/workflows/    # CI Android (validation et build debug)
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew, gradlew.bat
```

## Compilation (GitHub Actions uniquement)

La validation et la compilation sont réalisées sur **GitHub Actions** via `.github/workflows/android.yml` (JDK/SDK, licences, validation wrapper, tests unitaires, `assembleDebug`, publication de l’APK debug en artefact). Voir [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md).

## Sécurité

Ne jamais commiter de configurations personnelles, UUID, clés, secrets ou tokens. Voir [`docs/SECURITY.md`](docs/SECURITY.md).

## Contribution

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md).

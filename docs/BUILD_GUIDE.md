# Guide de build — ONFAA TUNNEL

## Principe directeur

La compilation et la validation du projet sont réalisées **exclusivement sur GitHub Actions**. Aucun binaire officiel ne doit être considéré comme produit par un build local non documenté comme référence. Aucun contournement masquant un échec (`|| true`, stub de bibliothèque moteur, etc.) n’est autorisé.

## État Phase 1 (workflow de validation en place)

Workflow : `.github/workflows/android.yml` (déclencheurs : `push`/`pull_request` sur `main`, `workflow_dispatch`).

Étapes principales :
1. **Checkout** (`actions/checkout@v4`)
2. **JDK 17** (Temurin, `actions/setup-java@v4`)
3. **Android SDK** (`android-actions/setup-android@v3`)
4. **Licences SDK** : `yes | sdkmanager --licenses`
5. **Gradle** : `gradle/actions/setup-gradle@v4`
6. **Validation du wrapper** : `gradle/actions/wrapper-validation@v4`
7. **Tests unitaires** : `./gradlew :app:testDebugUnitTest --stacktrace`
8. **Compilation debug** : `./gradlew :app:assembleDebug --stacktrace`
9. **Artefact APK** : upload `app/build/outputs/apk/debug/*.apk` (`if-no-files-found: error`)

## Résultats vérifiés (Phase 1)

- **Run réussi :** https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43954661801 (commit `22551de`, `completed/success`)
- **Artefact :** `onfaa-tunnel-debug-apk` (produit par le run ci-dessus)
- **Runs antérieurs (corrections CI)** : #43953850169 (échec setup Android SDK), #43954402981 (échec installation/licences SDK explicites) — corrigés avant validation finale.

> Les résultats réels doivent continuer à être consignés dans `STATUS.md` après chaque exécution pertinente.

## Outils et versions épinglés (Phase 1)

| Élément | Version / valeur |
| --- | --- |
| Gradle | 8.10.2 (wrapper) |
| Android Gradle Plugin (AGP) | 8.7.3 |
| Kotlin | 2.0.21 (plugin Compose) |
| KSP | 2.0.21-1.0.28 |
| JDK (CI) | 17 (Temurin) |
| `compileSdk` / `targetSdk` | 35 |
| `minSdk` | 26 |
| Compose BOM | 2024.12.01 |
| Navigation Compose | 2.8.5 |
| Room | 2.6.1 |
| DataStore Preferences | 1.1.1 |
| Coroutines | 1.9.0 |

Source de vérité : `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts`.

## Build local (indicatif, non officiel)

Un build local peut être tenté uniquement dans un environnement disposant d’un JDK 17 et d’un Android SDK compatibles (`./gradlew :app:testDebugUnitTest :app:assembleDebug`). L’absence de SDK dans certains environnements (ex. mobile) n’invalide pas la configuration si la CI GitHub Actions réussit.

## Secrets et signature

- Aucun secret, keystore, mot de passe ou token ne doit être commité.
- La signature release, si introduite ultérieurement, devra utiliser les secrets GitHub protégés et être documentée sans exposer de valeurs.

## Reproductibilité

- Versions épinglées dans le catalogue et le wrapper.
- Toute future dépendance binaire (notamment liée à Xray/libXray) devra être traçable (origine upstream officielle, version, checksums si applicable).

## Références

- Roadmap : [`ROADMAP.md`](ROADMAP.md)
- Décisions : [`DECISIONS.md`](DECISIONS.md)
- Séparation des composants : [`ANDROID_XRAY_SEPARATION.md`](ANDROID_XRAY_SEPARATION.md)

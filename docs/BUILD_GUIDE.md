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

## Intégration libXray (Phase 2B — réalisée et validée)

- **Sources officielles** : `XTLS/libXray` v26.11.9 (commit `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`), dépendance Xray-core v26.11.9 (`676eaf66…`) — voir `docs/XRAY_UPSTREAM_AUDIT.md`.
- **Build officiel Android** : `python3 build/main.py android` dans `.github/workflows/libxray-poc.yml`.
- **Outils épinglés** : Go `1.26.1` (`GOTOOLCHAIN=auto`), JDK 17, gomobile `v0.0.0-20260217195705-b56b3793a9c4`, NDK r30 package `ndk;30.0.16248370` via `sdkmanager`, `LIBXRAY_ANDROID_API=21`.
- **Flux** : clone + vérification du commit → build → inspection (SHA-256, ABIs, ELF, 16 KiB, `javap`) → intégration `app/libs/libXray.aar` (non commité) → tests unitaires, APK debug, APK androidTest → tests instrumentés réels sur émulateur x86_64 API 35 → artefacts.
- **Résultats** : `docs/LIBXRAY_ANDROID_POC.md` (SHA-256, ABIs, signature `libXray.LibXray#invoke`, tests).
- **Interdits** : téléchargement non vérifié, modification silencieuse upstream, `|| true`, intégration de binaire non tracé.

## Intégration libXray et tunnel réel (Phase 3 — validée en CI, émulateur x86_64)

Workflow : `.github/workflows/libxray-poc.yml` (déclencheurs : `push` sur `main` pour les sources/CI/E2E, `workflow_dispatch`).

Étapes principales :
1. Build de l’AAR officiel libXray depuis le commit épinglé, inspection (SHA-256, ABIs, ELF, 16 KiB, `javap`) puis intégration `app/libs/libXray.aar` (non commité).
2. Build du binaire **Xray-core officiel** depuis le commit épinglé (`v26.11.9`, `676eaf66…`) sur le runner, vérification du commit, `xray version`, validation de la configuration serveur (`xray -test`).
3. Démarrage de la fixture E2E : inbounds VLESS/VMess loopback, sortie `freedom` (avec `finalRules` `allow` pour la cible de `redirect` `127.0.0.1:18080`, sinon Xray 26.x la blackhole), serveur token HTTP sur `192.0.2.10:18080` (alias loopback TEST-NET), DNS `127.0.0.1:15353` en UDP et TCP pour `onfaa.test` ; le tunnel utilise `tcp://10.0.2.2:15353` (forwarding UDP de l’alias hôte non garanti).
4. Tests unitaires `:app:testDebugUnitTest`, APK debug, APK androidTest.
5. Tests instrumentés réels sur émulateur x86_64 API 35 avec arguments :
   ```bash
   ./gradlew :app:connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e=true \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.uuid="<uuid-de-test>" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.host="10.0.2.2" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.vlessPort="18443" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.vmessPort="18444" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.dns="tcp://10.0.2.2:15353" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.probeUrl="http://onfaa.test:18080/token" \
     -Pandroid.testInstrumentationRunnerArguments.onfaa.e2e.token="<token-de-test>"
   ```
6. Vérification des preuves : XML `VpnTunnelE2ETest` (**4 tests**, 0 échec/erreur/ignoré), journal d’accès Xray contenant `192.0.2.10:18080` **et au moins une ligne par protocole**, absence de `target blocked by a finalrule` dans le journal serveur, collecte des journaux, arrêt des serveurs, suppression de l’alias loopback.
7. Publication des artefacts : `poc-report` (AAR, preuves, rapports, APK de test) et APK debug fonctionnel.

**Sécurité des builds** : le trafic HTTP en clair n’est autorisé que dans le variant **debug** (`app/src/debug/res/xml/network_security_config.xml`) pour la fixture CI ; le variant release garde `cleartextTrafficPermitted=false`.

**Résultats** : run [#44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595) `success` (commit `5f51831`, 25/25 étapes) — 11/11 tests unitaires, 21/21 instrumentés, E2E VLESS et VMess, DNS et journal d’accès par protocole. Détail : `docs/PHASE_3_REPORT.md` §9. Les limites ARM64 physique et IP publique restent non revendiquées.

## Phase 4 — persistance, import étendu, cycle de vie (CI verte)

- **Aucun changement de chaîne de build** : Room 2.6.1, DataStore 1.1.1 et l’AAR libXray épinglé restent les seules dépendances ; `org.json:json:20240303` est ajouté en `testImplementation` uniquement (parsing JSON des tests unitaires JVM).
- **Version** : `versionCode 4`, `versionName 0.4.0-phase4`.
- **Workflows** : `.github/workflows/android.yml` (compilation, tests unitaires, `assembleDebug`, `assembleDebugAndroidTest`, APK) et `.github/workflows/libxray-poc.yml` (E2E réel + tous les instrumentés).
- **Portes E2E inchangées** : `E2E_XML_VALIDATED`, `ACCESS_LOG_PER_PROTOCOL_OK`, `--suite VpnTunnelE2ETest --min-tests 4` — un test non exécuté échoue.
- **Résultats** : Android CI [#44011899010](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899010) `success` (commit `230008c`) — 23/23 unitaires, APK debug et APK de tests ; E2E [#44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095) `success` — 27/27 instrumentés. Artefacts `onfaa-tunnel-debug-apk` et `libxray-poc-report`. Détail : `docs/PHASE_4_REPORT.md`.

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

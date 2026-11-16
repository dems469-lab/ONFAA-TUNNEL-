# Changelog — ONFAA TUNNEL

Tous les changements notables de ce projet seront documentés dans ce fichier.

Le format s’inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet visera un versionnage sémantique lorsque des livrables applicatifs existeront.

## [Unreleased]

### Phase 2 — audit et intégration libXray officielle (TERMINÉE, validée en CI)
- Audit des sources officielles XTLS/Xray-core v26.11.9 (commit `676eaf66…`, MPL-2.0) et XTLS/libXray v26.11.9 (commit `5a0f7d8…`, MIT), alignement de dépendance vérifié.
- Ajout de `docs/XRAY_UPSTREAM_AUDIT.md` et `docs/LIBXRAY_API_REFERENCE.md`.
- **Phase 2B** : workflow `libxray-poc.yml` (build officiel `python3 build/main.py android` depuis commit épinglé, vérification commit, NDK r30 `30.0.16248370`, inspection AAR/ABIs/ELF/16 KiB/`javap`, intégration `:app`, tests unitaires et instrumentés sur émulateur, artefacts), `XrayBridge` (API v3, appels hors UI, chargement natif explicite), tests instrumentés réels, script `scripts/inspect_libxray_aar.sh`, doc `docs/LIBXRAY_ANDROID_POC.md`.
- **Preuves** : run final [#43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249) `success` (commit `f116442`) ; AAR SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917` (53 200 280 octets), ABIs `arm64-v8a/armeabi-v7a/x86/x86_64`, ELF OK, tous `LOAD` alignés 16 KiB, signature JVM `libXray.LibXray#invoke(String):String` ; `testDebugUnitTest`/`assembleDebug`/`assembleDebugAndroidTest` réussis ; **6/6 tests instrumentés réels réussis sur émulateur x86_64 API 35 (0 ignoré)**.
- **Limite résiduelle** : exécution native prouvée sur x86_64 émulé ; pas encore sur appareil physique arm64/armv7.

### À venir
- **Phase 3** : connexion VPN réelle (`VpnService` + orchestration Xray/libXray), sans simulation ; validation sur appareil physique.

## [0.1.0-phase1] — 2026-11-16 (publié et validé en CI)

### Ajouté
- Fondations Android natives : module `:app`, Gradle Kotlin DSL, catalogue de versions épinglées, wrapper Gradle, package `com.onfaa.tunnel`.
- Application Compose Material 3 : activité principale, navigation, 5 écrans (Accueil, Configurations, Statistiques, Journaux, Paramètres) ; bouton de connexion désactivé/non opérationnel, sans état ni statistique fictifs.
- Préparation `VpnService` : manifeste/permissions et `OnfaaTunnelVpnService` (aucun tunnel, aucun moteur).
- Préparation données : modèles domaine, Room (entité/DAO/base) et dépôt DataStore de préférences.
- Tests unitaires JUnit de base et squelette instrumenté.
- Workflow GitHub Actions `android.yml` (tests unitaires, `assembleDebug`, artefact APK debug).
- Documentation : `docs/ANDROID_XRAY_SEPARATION.md`, mises à jour README/STATUS/ARCHITECTURE/BUILD_GUIDE/TEST_PLAN/DECISIONS/ROADMAP.

### Validation
- CI réussie : https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43954661801 (commit `22551dee3d00b7f33413552ad0cb60ccceb27618`), artefact `onfaa-tunnel-debug-apk` publié.

## [0.0.0-phase0] — 2026-11-16 (publié)

### Ajouté
- Structure initiale du projet (répertoires `docs/`, `app/`, `gradle/`, `.github/`).
- Documentation fondamentale : contexte, architecture cible, roadmap (phases 0–9), décisions, plan de tests, guide de build, sécurité, spécification design.
- Fichiers racine : `README.md`, `STATUS.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE`, `.gitignore`.
- Préparation documentaire CI/CD (`.github/workflows/README.md`) sans workflow de compilation Android.

### Notes
- Commit de Phase 0 publié et vérifié sur `main` : `96cab902f0f96ed5a60dc40e03016b00500fd32e`.
- Référence du commit initial distant préexistant : `3be425c984deae6c2c0d5bc8777c870c1afbed07` (*Initial commit*, README minimal).
- Une mise à jour documentaire de statut peut suivre dans un commit distinct pour refléter la validation finale.

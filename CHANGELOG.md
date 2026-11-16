# Changelog — ONFAA TUNNEL

Tous les changements notables de ce projet seront documentés dans ce fichier.

Le format s’inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet visera un versionnage sémantique lorsque des livrables applicatifs existeront.

## [Unreleased]

### Phase 2 (audit réalisé ; Phase 2B PoC préparée, validation en attente)
- Audit des sources officielles XTLS/Xray-core v26.11.9 (commit `676eaf66…`, MPL-2.0) et XTLS/libXray v26.11.9 (commit `5a0f7d8…`, MIT), alignement de dépendance vérifié.
- Ajout de `docs/XRAY_UPSTREAM_AUDIT.md` et `docs/LIBXRAY_API_REFERENCE.md`.
- **Phase 2B (préparation)** : workflow `libxray-poc.yml` (build officiel depuis source épinglée, vérification commit, inspection AAR/ABIs/ELF/JVM, intégration `:app`, tests/build), `XrayBridge` (Invoke apiVersion 3, appels hors UI), tests instrumentés réels, script `scripts/inspect_libxray_aar.sh`, doc `docs/LIBXRAY_ANDROID_POC.md`.
- **En attente de preuve réelle** : résultats CI (SHA-256 AAR, ABIs, signatures JVM, compilation/tests, APK) et exécution instrumentée sur appareil/émulateur.

### À venir
- PoC reproductible et vérifiée (artefact/source libXray épinglé, inspection ABIs, intégration minimale compilable si applicable).
- Phases suivantes : voir [`docs/ROADMAP.md`](docs/ROADMAP.md).

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

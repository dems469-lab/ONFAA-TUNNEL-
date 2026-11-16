# Changelog — ONFAA TUNNEL

Tous les changements notables de ce projet seront documentés dans ce fichier.

Le format s’inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet visera un versionnage sémantique lorsque des livrables applicatifs existeront.

## [Unreleased]

### À venir
- Phase 1 : fondations Android (structure Gradle/module, configuration de base, préparation `VpnService`).
- Phases suivantes : voir [`docs/ROADMAP.md`](docs/ROADMAP.md).

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

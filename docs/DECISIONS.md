# Décisions — ONFAA TUNNEL

Ce document consigne les décisions prises, les hypothèses de travail et les choix restant à confirmer. Il doit être mis à jour à chaque étape structurante.

## Décisions validées (Phase 0)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-16 | Dépôt GitHub `https://github.com/dems469-lab/ONFAA-TUNNEL-`, branche principale `main`, source centrale et permanente | Continuité multi-environnement et traçabilité |
| 2026-11-16 | Client **Android natif** en **Kotlin** | Alignement plateforme et écosystème |
| 2026-11-16 | Utilisation d’**Android `VpnService`** | Interface système requise pour un VPN Android |
| 2026-11-16 | Moteur **XTLS/Xray-core officiel** intégré via **libXray** | Exigence fonctionnelle du projet |
| 2026-11-16 | **Aucun remplacement par sing-box** ; **aucune simulation de connexion** | Intégrité technique et fonctionnelle |
| 2026-11-16 | Persistance cible : **Room** + **DataStore** ; asynchronisme : **Coroutines & Flow** | Architecture cible cohérente Android moderne |
| 2026-11-16 | Compilation cible **exclusivement via GitHub Actions** (mise en œuvre Phase 9) | Reproductibilité et centralisation |
| 2026-11-16 | Phase 0 = structure + documentation + préparation documentaire CI ; **pas** de workflow de build Android incomplet | Éviter les artefacts trompeurs |
| 2026-11-16 | Licence **MIT** ajoutée au dépôt (choix initial de projet, révisable si le propriétaire en décide autrement) | Clarifier les conditions de réutilisation dès l’initialisation |

## Décisions validées (Phase 1)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-16 | Package Android / `applicationId` : **`com.onfaa.tunnel`** (namespace identique) | Identité applicative cohérente et stable dès les fondations |
| 2026-11-16 | Module unique **`:app`** (pas de multi-modules Gradle en Phase 1) | Réduire la complexité initiale ; réévaluation possible si le projet grandit |
| 2026-11-16 | UI native avec **Jetpack Compose (Material 3)** et **Navigation Compose** | Interface moderne, déclarative, adaptée aux 5 écrans cibles |
| 2026-11-16 | Organisation source par couches/features : `core/`, `data/`, `domain/`, `feature/*`, `ui/theme`, `vpn` | Séparation claire entre UI, données, domaine et préparation système VPN |
| 2026-11-16 | Versions épinglées (catalogue) : AGP **8.7.3**, Kotlin **2.0.21**, KSP **2.0.21-1.0.28**, Gradle **8.10.2**, compile/target SDK **35**, minSdk **26**, JDK **17**, Compose BOM **2024.12.01**, Navigation **2.8.5**, Room **2.6.1**, DataStore **1.1.1**, Coroutines **1.9.0**, JUnit **4.13.2** | Reproductibilité et compatibilité maîtrisée (pas de `latest` flottant) |
| 2026-11-16 | Workflow CI `android.yml` : setup-java/SDK, validation wrapper, `:app:testDebugUnitTest`, `:app:assembleDebug`, upload artefact APK (`if-no-files-found: error`) | Validation compilable/testable sur GitHub Actions sans masquer les échecs |
| 2026-11-16 | `OnfaaTunnelVpnService` enregistré mais **sans tunnel** (arrêt immédiat en `onStartCommand`, aucune logique réseau/moteur) | Préparation `VpnService` conforme sans simulation de connexion |

## Décisions / constats validés (Phase 2 — audit)

| Date | Élément | Valeur / constat vérifié | Référence |
| --- | --- | --- | --- |
| 2026-11-16 | Xray-core upstream (release examinée) | `v26.11.9`, commit `676eaf66f8f05d4d558ee7a74b4736623d3feeb9`, licence MPL-2.0, `go 1.27` | `docs/XRAY_UPSTREAM_AUDIT.md` |
| 2026-11-16 | libXray upstream (release examinée) | `v26.11.9`, commit tag `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`, licence MIT, `go 1.27.3` | `docs/XRAY_UPSTREAM_AUDIT.md` |
| 2026-11-16 | Alignement moteur | libXray dépend de `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0` (correspondance v26.11.9) | `go.mod`, build script libXray |
| 2026-11-16 | API libXray (contrat) | `Invoke` JSON, `apiVersion=3`, méthodes/cycle de vie/wrappers Android/CGo vérifiés dans le code (pas de signatures inventées) | `docs/LIBXRAY_API_REFERENCE.md` |
| 2026-11-16 | Build Android officiel libXray | `python3 build/main.py android` (gomobile), défaut `LIBXRAY_ANDROID_API=21`, workflow officiel NDK r30, artefact release `libxray-android.zip` sha256 publié | `docs/XRAY_UPSTREAM_AUDIT.md` |

## Choix techniques à confirmer / à compléter

| Sujet | Questions ouvertes | Phase cible |
| --- | --- | --- |
| **Artefact d’intégration retenu** | Build from source en CI vs téléchargement release vérifié (SHA256) ; politique de stockage (pas de binaire non vérifié commité) | Phase 2 (PoC) / 3 |
| **ABIs finales** | Preuve des ABIs incluses dans l’AAR v26.11.9 (inspection `jni/`), support arm64-v8a/armeabi-v7a/x86_64 selon besoins | Phase 2 (PoC) |
| **Bindings JVM gomobile** | Noms de classes/méthodes exacts exposés à Kotlin pour `Invoke` et API Android | Phase 2 (PoC) / 3 |
| **Gomobile/Go/NDK projet** | Épinglage définitif pour builds reproductibles ONFAA (aligné upstream) | Phase 2 (PoC) / 9 |
| **Formats d’import** | Liens/URI (vmess://, vless://, etc.), JSON, QR, presse-papiers ; règles de validation | Phase 4 |
| **Modèle de données** | Schéma Room (profils, groupes, journaux), stratégie de migration | Phases 1–4 |
| **Statistiques** | Sources (API Android, compteurs moteur/stats Xray), granularité, rétention | Phases 3, 6 |
| **DNS/Routage** | Ensemble d’options exposées, correspondance avec capacités Xray/libXray | Phases 6–7 |
| **CI/CD détaillée** | Jobs dédiés libXray, caching, secrets GitHub (si nécessaires), signature d’artefacts | Phase 9 |
| **Stratégie de tests automatisés** | Unitaires, instrumentation, tests d’intégration réseau (environnement contrôlé) | Phases 1, 3, 8 |

## Règles de décision

- Toute hypothèse impactant la sécurité, la confidentialité ou le comportement réseau doit être explicitement documentée.
- Les signatures et comportements de `libXray` ne doivent **jamais** être inventés : consigner « inconnu » jusqu’à confirmation.
- Les changements de décision doivent être datés et motivés dans ce fichier.

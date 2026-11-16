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

## Choix techniques à confirmer

| Sujet | Questions ouvertes | Phase cible |
| --- | --- | --- |
| **API libXray** | Signatures, types, cycle de vie, format de configuration attendu, gestion des callbacks/états | Phase 2 (obligatoire avant intégration réelle) |
| **Artefacts d’intégration** | Forme de distribution (AAR, bibliothèques natives, build depuis source), versions/compatibilité ABI | Phase 2 |
| **Formats d’import** | Liens/URI (vmess://, vless://, etc.), JSON, QR, presse-papiers ; règles de validation | Phase 4 |
| **Modèle de données** | Schéma Room (profils, groupes, journaux), stratégie de migration | Phases 1–4 |
| **Statistiques** | Sources (API Android, compteurs moteur), granularité, rétention | Phases 3, 6 |
| **DNS/Routage** | Ensemble d’options exposées, correspondance avec capacités Xray | Phases 6–7 |
| **CI/CD détaillée** | Matrice SDK/NDK, caching, secrets GitHub (si nécessaires plus tard), signature d’artefacts | Phase 9 |
| **Stratégie de tests automatisés** | Unitaires, instrumentation, tests d’intégration réseau (environnement contrôlé) | Phases 1, 3, 8 |

## Règles de décision

- Toute hypothèse impactant la sécurité, la confidentialité ou le comportement réseau doit être explicitement documentée.
- Les signatures et comportements de `libXray` ne doivent **jamais** être inventés : consigner « inconnu » jusqu’à confirmation.
- Les changements de décision doivent être datés et motivés dans ce fichier.

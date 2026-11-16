# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 1 — Fondations Android natives
- **État :** **terminée (validée par compilation et tests réussis sur GitHub Actions, artefact APK debug publié)**

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit Phase 1 publié et validé en CI :** `22551dee3d00b7f33413552ad0cb60ccceb27618` — *CI: rely on preinstalled Android SDK for Phase 1 validation*
- **Commits Phase 1 publiés (distants, vérifiés)** :
  - `287b5df4216788bd25368efe6f827823915e3a3d` — *Phase 1: add Android foundations, CI validation workflow, and documentation* (CI run #1 : échec à l’étape « Set up Android SDK »)
  - `554a590729196e8d310cb32480e5eddc254f49f0` — *Fix Android CI workflow to use runner SDK without setup-android failure* (CI run #2 : échec à l’étape « Accept Android SDK licenses and install required components »)
  - `22551dee3d00b7f33413552ad0cb60ccceb27618` — *CI: rely on preinstalled Android SDK for Phase 1 validation* (CI run #3 : **succès**)

## Validation GitHub Actions (réelle)

- **Run réussi :** [Android CI #43954661801](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43954661801) — statut `completed`, conclusion `success` (commit `22551de`, branche `main`).
- **Étapes clés réussies :** checkout, JDK 17, setup Gradle, validation wrapper, `:app:testDebugUnitTest`, `:app:assembleDebug`, upload artefact.
- **Artefact APK :** `onfaa-tunnel-debug-apk` (disponible sur la page du run, non expiré au moment de la vérification API).

## Travaux terminés (Phase 1)

- Structure Android/Gradle complète : module `:app`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, wrapper Gradle commité.
- Application native Kotlin/Compose : activité principale, thème Material 3, navigation Compose, 5 écrans (Accueil, Configurations, Statistiques, Journaux, Paramètres).
- Bouton de connexion **désactivé** et explicitement non opérationnel ; **aucun état de connexion, aucun tunnel, aucune statistique fictive**.
- Préparation VPN : manifeste/permissions et `com.onfaa.tunnel.vpn.OnfaaTunnelVpnService` (aucun établissement de tunnel).
- Préparation données : modèles domaine, Room (entité/DAO/base) et dépôt DataStore de préférences.
- Tests unitaires JUnit de base (`app/src/test`) et squelette instrumenté (`app/src/androidTest`).
- CI Android opérationnelle sur GitHub Actions avec artefact debug produit.
- Documentation mise à jour (architecture, build, tests, décisions, roadmap, séparation app/VpnService/moteur upstream).

## Travaux non commencés

- Intégration du moteur **XTLS/Xray-core** upstream et de **libXray** (Phase 2+), connexion VPN réelle (Phase 3).
- Import/parsing réel des configurations VLESS/VMess et UI de gestion complète (Phase 4+).
- Statistiques/journaux/diagnostics réels, réglages DNS/routage effectifs (Phases 6–7).
- Renforcement qualité/sécurité et CI/CD de livraison avancée (Phases 8–9).

## Blocages

- Aucun blocage bloquant la clôture de la Phase 1. Limite connue : aucune exécution locale complète du build Android dans cet environnement (SDK absent) ; la preuve de compilation repose sur GitHub Actions.

## Prochaine tâche

- **Phase 2 — Étude et intégration Xray** : analyser XTLS/Xray-core upstream et les options d’intégration **libXray** (compatibilité, API officielle, artefacts), sans intégrer de binaire/fork approximatif et sans inventer de signatures ; documenter les choix dans `docs/DECISIONS.md`.

## Instructions de reprise pour une autre IA

1. Cloner `https://github.com/dems469-lab/ONFAA-TUNNEL-.git` et se placer sur `main`.
2. Lire `STATUS.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md`, `docs/BUILD_GUIDE.md`, `docs/ANDROID_XRAY_SEPARATION.md`.
3. Confirmer la validation Phase 1 via le run GitHub Actions réussi et l’artefact APK.
4. Respecter : XTLS/Xray-core officiel upstream uniquement (intégration future via libXray vérifié), pas de sing-box, pas de faux moteur/librairie, pas de simulation de connexion/stats, pas de secrets.
5. À chaque étape validée : mettre à jour la documentation, commiter clairement, pousser sur `main`, vérifier la présence distante et les résultats CI, puis actualiser `STATUS.md`.

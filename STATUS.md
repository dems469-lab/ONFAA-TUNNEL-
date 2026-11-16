# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 0 — Initialisation GitHub
- **État :** terminée (structure et documentation publiées et vérifiées sur GitHub)

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit publié (distant, vérifié) :** `96cab902f0f96ed5a60dc40e03016b00500fd32e` — *Phase 0: initialize project structure and documentation*
- **Commit initial préexistant (distant) :** `3be425c984deae6c2c0d5bc8777c870c1afbed07` — *Initial commit* (README minimal)

> Mise à jour documentaire finale de statut effectuée localement après vérification distante ; elle doit être commitée et poussée pour que le dépôt reflète cet état.

## Travaux terminés

- Inspection du dépôt distant (accès lecture confirmé, branche `main` présente).
- Alignement de l’espace de travail local sur `origin/main`.
- Création de la structure de répertoires et fichiers de Phase 0.
- Rédaction des documents fondamentaux (`README`, contexte, architecture cible, roadmap, décisions, plan de tests, guide de build, sécurité, spécification design).
- Préparation documentaire des bases CI/CD (`.github/workflows/README.md`) sans workflow de compilation Android.
- Commit, push sur `origin/main` et vérification distante de la présence des fichiers (arborescence complète de Phase 0 visible sur le dépôt).

## Travaux non commencés

- Fondations Android (Gradle, modules, manifeste, `VpnService`) — Phase 1.
- Étude et intégration **XTLS/Xray-core** / **libXray** — Phase 2.
- Toute connexion VPN réelle, import de configurations, UI applicative, diagnostics avancés, paramètres, qualité/sécurité applicative, et CI/CD de build — Phases 3 à 9.

## Blocages

- Aucun blocage technique identifié pour la Phase 0. L’accès en lecture et en écriture au dépôt distant a été vérifié par un push réussi.

## Prochaine tâche

- **Phase 1 — Fondations Android :** définir la structure Gradle/module, la configuration minimale du projet Kotlin, le manifeste et la préparation de `VpnService` (sans moteur réel), en maintenant la documentation et les règles de publication GitHub.

## Instructions de reprise pour une autre IA

1. Cloner le dépôt : `https://github.com/dems469-lab/ONFAA-TUNNEL-.git` et se placer sur `main`.
2. Lire `STATUS.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md` et `docs/PROJECT_CONTEXT.md`.
3. Confirmer que la Phase 0 est publiée (dernier commit de Phase 0 présent sur `origin/main`) avant d’entamer la Phase 1.
4. Respecter les règles : pas de simulation de connexion, pas de sing-box, pas de secrets/UUID/tokens dans le dépôt.
5. À chaque phase validée : mettre à jour la documentation, commiter clairement, pousser sur `main`, vérifier la présence distante, puis actualiser `STATUS.md`.
6. Toute incertitude sur l’API `libXray` doit être consignée dans `docs/DECISIONS.md` et résolue en Phase 2 par étude des sources officielles, sans inventer de signatures.

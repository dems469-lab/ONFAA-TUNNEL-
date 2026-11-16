# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 0 — Initialisation GitHub
- **État :** en cours (préparation locale de la structure et de la documentation ; publication distante à vérifier après push)

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit publié (distant, vérifié avant cette mission) :** `3be425c984deae6c2c0d5bc8777c870c1afbed07` — *Initial commit* (README minimal)

> Ce champ doit être mis à jour avec le hash du commit de Phase 0 **après** un push réussi et une vérification réelle sur GitHub.

## Travaux terminés (locaux, en préparation de publication)

- Inspection du dépôt distant (accès lecture confirmé, branche `main` présente).
- Alignement de l’espace de travail local sur `origin/main`.
- Création de la structure de répertoires et fichiers de Phase 0.
- Rédaction des documents fondamentaux (`README`, contexte, architecture cible, roadmap, décisions, plan de tests, guide de build, sécurité, spécification design).
- Préparation documentaire des bases CI/CD (`.github/workflows/README.md`) sans workflow de compilation Android.

## Travaux non commencés

- Fondations Android (Gradle, modules, manifeste, `VpnService`) — Phase 1.
- Étude et intégration **XTLS/Xray-core** / **libXray** — Phase 2.
- Toute connexion VPN réelle, import de configurations, UI applicative, diagnostics avancés, paramètres, qualité/sécurité applicative, et CI/CD de build — Phases 3 à 9.

## Blocages

- **Publication GitHub non encore exécutée/vérifiée dans cette session au moment de la rédaction initiale de ce fichier.** L’authentification push et la visibilité distante des fichiers doivent être confirmées lors de l’étape de publication. Si le push échoue (droits, authentification), la mission doit s’arrêter et demander une configuration sécurisée (sans exposer de token).

## Prochaine tâche

1. Vérifier `git status` et le diff.
2. Créer un commit explicite de la Phase 0.
3. Publier sur `origin/main`.
4. Vérifier la présence réelle des fichiers sur GitHub (ex. `git ls-remote`, consultation des chemins/arborescence).
5. Mettre à jour `STATUS.md` avec le hash du commit publié et le résultat de vérification.

## Instructions de reprise pour une autre IA

1. Cloner le dépôt : `https://github.com/dems469-lab/ONFAA-TUNNEL-.git` et se placer sur `main`.
2. Lire `STATUS.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md` et `docs/PROJECT_CONTEXT.md`.
3. Ne pas commencer le moteur VPN tant que la Phase 0 n’est pas validée (commit publié et vérifié sur GitHub).
4. Respecter les règles : pas de simulation de connexion, pas de sing-box, pas de secrets/UUID/tokens dans le dépôt.
5. À chaque phase validée : mettre à jour la documentation, commiter clairement, pousser sur `main`, vérifier la présence distante, puis actualiser `STATUS.md`.
6. Toute incertitude sur l’API `libXray` doit être consignée dans `docs/DECISIONS.md` et résolue en Phase 2 par étude des sources officielles, sans inventer de signatures.

# Roadmap — ONFAA TUNNEL

La roadmap est organisée en dix phases. Chaque phase doit être documentée, validée, commitée, poussée sur `main` et vérifiée sur GitHub avant d’être considérée comme terminée.

## Phase 0 — Initialisation GitHub
- **Objectif :** établir le dépôt comme source centrale, créer la structure du projet, rédiger la documentation fondamentale, préparer les bases documentaires CI/CD, publier et vérifier sur GitHub.
- **Livrables :** arborescence, docs racine et `docs/`, règles de contribution/sécurité, `STATUS.md` à jour, commit visible sur `main`.
- **Note :** pas de développement du moteur VPN, pas de workflow de build Android exécutable.

## Phase 1 — Fondations Android
- **Objectif :** poser les bases du projet Android natif (Kotlin).
- **Livrables attendus :** structure Gradle/module(s), configuration minimale, manifeste, squelette applicatif, préparation de l’intégration `VpnService` (sans moteur réel), outillage de base.
- **Critères :** projet cohérent, documenté, buildable en CI lorsque la CI sera prête (cible Phase 9).

## Phase 2 — Étude et intégration Xray
- **Objectif :** étudier XTLS/Xray-core officiel et définir l’intégration **libXray**.
- **Livrables attendus :** analyse documentée des artefacts/API disponibles, choix d’intégration (sans inventer de signatures), premiers éléments d’intégration technique selon faisabilité, mise à jour des décisions.
- **Contrainte :** aucun remplacement par sing-box.

## Phase 3 — Connexion VPN réelle
- **Objectif :** établir une connexion VPN réelle via `VpnService` et le moteur Xray intégré.
- **Livrables attendus :** cycle de vie de connexion/arrêt, gestion d’erreurs, état fiable, preuves de fonctionnement réel (tests manuels/procédure documentée).

## Phase 4 — Importation des configurations
- **Objectif :** importer et gérer des configurations personnelles (VLESS, VMess).
- **Livrables attendus :** mécanismes d’import, validation, persistance sécurisée locale, sélection de profil actif.

## Phase 5 — Interface complète
- **Objectif :** implémenter l’UI complète selon la spécification.
- **Livrables attendus :** cinq écrans fonctionnels, navigation, application des huit thèmes, cohérence avec les états réels.

## Phase 6 — Réseau et diagnostics
- **Objectif :** exposer statistiques réseau réelles, DNS, routage, journaux et diagnostics.
- **Livrables attendus :** métriques, affichage, outils de diagnostic, gestion des logs conforme aux règles de sécurité.

## Phase 7 — Paramètres avancés
- **Objectif :** enrichir les options utilisateur (réseau, comportement, préférences avancées) en cohérence avec le moteur et Android.
- **Livrables attendus :** écrans/options, persistance DataStore, documentation des impacts.

## Phase 8 — Qualité et sécurité
- **Objectif :** renforcer la robustesse, les tests, et la posture sécurité/confidentialité.
- **Livrables attendus :** couverture de tests accrue, revues sécurité, durcissement du stockage et des exports, gestion des cas limites.

## Phase 9 — GitHub Actions et livraison
- **Objectif :** mettre en place la CI/CD de compilation et de livraison sur GitHub Actions.
- **Livrables attendus :** workflows de build/test/lint (selon maturité), artefacts, documentation de build à jour, processus de release si applicable.
- **Précondition :** fondations Android et intégration Xray suffisamment définies (pas de workflow trompeur avant).

## Suivi

- L’état courant et la prochaine tâche sont maintenus dans [`../STATUS.md`](../STATUS.md).
- Les décisions sont tracées dans [`DECISIONS.md`](DECISIONS.md).
- Les changements sont consignés dans [`../CHANGELOG.md`](../CHANGELOG.md).

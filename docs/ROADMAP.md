# Roadmap — ONFAA TUNNEL

La roadmap est organisée en dix phases. Chaque phase doit être documentée, validée, commitée, poussée sur `main` et vérifiée sur GitHub avant d’être considérée comme terminée.

## Phase 0 — Initialisation GitHub
- **Objectif :** établir le dépôt comme source centrale, créer la structure du projet, rédiger la documentation fondamentale, préparer les bases documentaires CI/CD, publier et vérifier sur GitHub.
- **Livrables :** arborescence, docs racine et `docs/`, règles de contribution/sécurité, `STATUS.md` à jour, commit visible sur `main`.
- **Note :** pas de développement du moteur VPN, pas de workflow de build Android exécutable.

## Phase 1 — Fondations Android
- **Objectif :** poser les bases du projet Android natif (Kotlin), compilable et testable sur GitHub Actions.
- **Livrables attendus :** structure Gradle/module(s), configuration minimale reproductible, manifeste, application lançable (Compose, navigation, écrans de base), préparation `VpnService` (sans moteur réel), modèles/données préparés (Room/DataStore), tests unitaires de base, workflow CI de validation.
- **Critères :** projet cohérent, documenté, build et tests réussis sur GitHub Actions avec artefact APK debug disponible ; aucun moteur VPN/faux moteur, aucune simulation de connexion/statistiques.
- **État :** implémentation préparée en dépôt, validation CI à confirmer après publication (voir `STATUS.md`).

## Phase 2 — Étude et intégration Xray
- **Objectif :** étudier XTLS/Xray-core officiel et définir l’intégration **libXray**.
- **Livrables attendus :** analyse documentée des artefacts/API disponibles, versions/tags/commits épinglés, matrice de compatibilité, stratégie de build Android reproductible, preuve de concept si réalisable proprement, mise à jour des décisions.
- **État :** **Phase 2 terminée et validée en CI** — audit documenté (`docs/XRAY_UPSTREAM_AUDIT.md` / `docs/LIBXRAY_API_REFERENCE.md`), AAR officiel construit depuis le commit épinglé, inspecté (SHA-256, ABIs, ELF, 16 KiB, JVM) et intégré ; tests instrumentés réels réussis sur émulateur (`docs/LIBXRAY_ANDROID_POC.md`, run `43965698249`).
- **Contrainte :** aucun remplacement par sing-box, aucune signature API inventée.

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

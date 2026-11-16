# Contribuer à ONFAA TUNNEL

## Principes

- **GitHub est la source centrale et permanente** du projet. Tout travail validé doit être publié sur la branche `main`.
- Distinguer systématiquement les opérations **locales** des opérations **réellement publiées et vérifiées** sur GitHub.
- Ne jamais prétendre qu’une opération (commit, push, vérification distante) a été effectuée si elle ne l’a pas été.

## Flux de travail par phase

À chaque phase validée :

1. Mettre à jour la documentation pertinente (`STATUS.md`, `CHANGELOG.md`, docs concernées).
2. Vérifier les fichiers et l’arborescence.
3. Vérifier les changements Git (`git status`, `git diff`).
4. Créer un commit explicite (message clair, phase identifiée).
5. Publier sur `main`.
6. Vérifier la présence des fichiers sur GitHub (distant).
7. Mettre à jour `STATUS.md` (dernier commit publié, état, blocages, prochaine tâche).

## Règles de développement

- **Moteur :** XTLS/Xray-core officiel via libXray uniquement. **Aucun remplacement par sing-box.**
- **Pas de simulation de connexion VPN.** Les fonctionnalités réseau doivent refléter un comportement réel lorsque implémentées.
- Ne pas inventer les signatures de l’API `libXray` ; documenter les inconnues dans `docs/DECISIONS.md` et les résoudre par étude des sources officielles.
- Ne pas créer de fichiers source vides uniquement pour donner l’impression d’avancement.

## Sécurité et confidentialité

- Ne jamais commiter : tokens, secrets, clés privées, UUID personnels, configurations utilisateurs réelles, fichiers `.env` sensibles.
- En cas de besoin d’authentification impossible, **demander une configuration sécurisée et s’arrêter** (sans inscrire de token dans un fichier ou une commande enregistrée).
- Voir [`docs/SECURITY.md`](docs/SECURITY.md).

## Documentation

- Maintenir la cohérence entre `README.md`, `STATUS.md`, `docs/ROADMAP.md` et `docs/DECISIONS.md`.
- Enregistrer les choix techniques validés et ouverts dans `docs/DECISIONS.md`.

## Build et CI

- La compilation cible sera réalisée exclusivement sur **GitHub Actions** (Phase 9). Ne pas introduire de workflows de build Android incomplets ou trompeurs avant que les fondations et l’intégration Xray ne soient définies.

## Reprise par un autre agent

Consulter `STATUS.md` pour les instructions de reprise, l’état courant et les blocages connus.

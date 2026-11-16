# GitHub Actions — Préparation (Phase 0)

Ce dossier accueillera les workflows CI/CD du projet.

## État Phase 0

- **Aucun workflow de compilation Android n’est créé en Phase 0.**
- Cette absence est volontaire : un workflow incomplet ou non exécutable serait trompeur tant que les fondations Android (Phase 1) et l’intégration Xray/libXray (Phase 2) ne sont pas suffisamment définies.

## Cible (Phase 9)

Les workflows futurs devront permettre :

- la compilation exclusivement via GitHub Actions ;
- l’exécution des tests et vérifications de qualité pertinents ;
- la production d’artefacts traçables et reproductibles.

Les exigences détaillées sont décrites dans [`../../docs/BUILD_GUIDE.md`](../../docs/BUILD_GUIDE.md) et la [`../../docs/ROADMAP.md`](../../docs/ROADMAP.md).

## Règles

- Ne jamais stocker de secrets ou tokens dans les fichiers de workflow.
- Utiliser les mécanismes sécurisés de GitHub (secrets/variables protégées) lorsque nécessaire, sans exposer de valeurs dans le dépôt.

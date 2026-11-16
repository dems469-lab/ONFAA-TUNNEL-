# Guide de build — ONFAA TUNNEL

## Principe directeur

La compilation du projet sera réalisée **exclusivement sur GitHub Actions**. Aucun binaire officiel ne doit être considéré comme produit par un build local non documenté comme référence, et aucun workflow de compilation Android incomplet ou trompeur ne doit être ajouté avant que les prérequis techniques ne soient réunis.

## État actuel (Phase 0)

- Le projet ne contient pas encore de module Android/Gradle buildable.
- Aucun workflow de compilation n’est fourni dans `.github/workflows/` à ce stade (voir le README de ce dossier).
- Les futures étapes de build seront définies lorsque les fondations Android (Phase 1) et l’intégration Xray/libXray (Phase 2) seront suffisamment spécifiées.

## Cible future (à détailler en Phase 9)

La CI GitHub Actions devra, selon la maturité du projet :

1. **Préparer l’environnement** : JDK compatible, Android SDK/NDK si requis par `libXray` ou le code natif, mise en cache des dépendances.
2. **Restaurer les dépendances** : Gradle wrapper, artefacts `libXray`/Xray-core selon la stratégie validée.
3. **Compiler** : assemblage des variantes définies (debug/release selon politique).
4. **Tester** : exécution des tests unitaires et, si possible, instrumentés dans un environnement adapté.
5. **Analyser** : lint/vérifications statiques pertinentes.
6. **Publier des artefacts** : APK/AAB ou autres livrables, uniquement via des mécanismes GitHub Actions contrôlés.

## Secrets et signature

- Aucun secret, keystore, mot de passe ou token ne doit être commité.
- Si la signature de release est requise, les matériaux sensibles devront être fournis via les mécanismes sécurisés de GitHub (secrets/variables protégées) et documentés sans exposer de valeurs.

## Reproductibilité

- Les versions d’outils (JDK, Gradle, SDK, NDK) devront être épinglées et documentées.
- Toute dépendance binaire (notamment liée à Xray/libXray) devra être traçable (version, origine officielle, checksum si applicable).

## Références

- Roadmap : [`ROADMAP.md`](ROADMAP.md) (Phases 1, 2, 9)
- Décisions : [`DECISIONS.md`](DECISIONS.md)
- CI préparatoire : [`../.github/workflows/README.md`](../.github/workflows/README.md)

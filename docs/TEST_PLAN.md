# Plan de tests — ONFAA TUNNEL

Ce plan définit les catégories de tests nécessaires au fil des phases. Les outils et l’automatisation seront précisés lorsque les fondations Android et la CI seront en place.

## Principes

- Prioriser la **réalité fonctionnelle** (connexion VPN réelle) sur des simulations trompeuses.
- Protéger les données sensibles : les jeux de test ne doivent pas contenir d’UUID, clés ou configurations personnelles réelles.
- Distinguer tests locaux, tests en CI (Phase 9), et validations manuelles sur appareil.

## Tests par domaine

### Phase 0 — Initialisation
- **Vérifications dépôt :** arborescence conforme, présence des documents requis, cohérence des liens internes.
- **Vérifications Git :** état propre après commit, historique lisible, push effectif, fichiers visibles sur GitHub.
- **Revue documentaire :** absence d’affirmations mensongères (ex. build/connexion déjà implémentés).

### Phase 1 — Fondations Android
- **CI (obligatoire pour validation)** : exécution de `:app:testDebugUnitTest` et `:app:assembleDebug` via `.github/workflows/android.yml`, avec artefact APK debug publié.
- **Tests unitaires locaux/intégrés au module** : logique pure de base (ex. modèles domaine/préférences) — présents dans `app/src/test`.
- **Tests instrumentés** : squelette présent dans `app/src/androidTest` (exécution selon disponibilité d’un environnement Android en CI/futur).
- **Vérifications structurelles** : configuration Gradle/wrapper, manifeste, navigation Compose, écrans présents, bouton de connexion désactivé/non opérationnel, absence de moteur simulé ou de statistiques fictives.
- **Résultats** : consignés dans `STATUS.md` et `CHANGELOG.md` après exécution réelle de la CI.

### Phase 2 — Étude et intégration Xray
- Validation de la chaîne d’intégration `libXray` (chargement, appels minimaux) dans un environnement contrôlé.
- Tests de non-régression sur la configuration/build après ajout des dépendances natives.
- Documentation des prérequis runtime (ABI, versions Android) confirmés par tests.

### Phase 3 — Connexion VPN réelle
- **Tests fonctionnels manuels** sur appareil réel : démarrage/arrêt du tunnel, maintien de session, comportement à la perte réseau, reconnexion si implémentée.
- Vérification que le trafic passe par le tunnel lorsque connecté (procédure documentée, sans exposer de données sensibles).
- Tests de robustesse : configuration invalide, refus de permission VPN, arrêt forcé du service.
- Critère : aucune simulation ; résultats observables et reproductibles.

### Phase 4 — Importation des configurations
- Tests de parsing/validation pour VLESS et VMess (cas valides, invalides, limites).
- Tests de persistance (Room) : création, lecture, mise à jour, suppression.
- Tests de sécurité : absence de fuite de secrets dans les logs/exports par défaut.

### Phase 5 — Interface complète
- Tests UI/instrumentation (navigation, états connexion, erreurs).
- Vérification de l’application des 8 thèmes sur les 5 écrans.
- Tests d’accessibilité de base (contraste, tailles, focus) selon priorités définies.

### Phase 6 — Réseau et diagnostics
- Validation des statistiques affichées contre des observations réelles (plages/cohérence).
- Tests des réglages DNS/routage (effets attendus documentés).
- Tests des journaux : niveaux, rotation/rétention si applicable, export sans données sensibles.

### Phase 7 — Paramètres avancés
- Tests de persistance DataStore et application immédiate/différée des préférences.
- Tests de compatibilité entre options (matrice de cas critiques).

### Phase 8 — Qualité et sécurité
- Revue et tests ciblés : stockage, permissions, exposition de données, durcissement.
- Campagne de non-régression sur les parcours critiques (connexion, import, bascule de serveur).
- Analyse statique/lint/sécurité selon outillage disponible en CI.

### Phase 9 — GitHub Actions et livraison
- Exécution automatisée des jobs CI (build, tests, lint) sur PR/push selon politique.
- Vérification de la reproductibilité des artefacts et de la documentation de build.
- Smoke tests sur artefacts générés (installation/démarrage) lorsque pertinent.

## Traçabilité

- Les cas de test critiques doivent être liés aux phases de la roadmap et aux décisions associées.
- Les résultats de validation de phase doivent être résumés dans `STATUS.md` et/ou le changelog selon l’ampleur.

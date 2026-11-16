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
- **Audit (réalisé)** : vérification des tags/commits/licences/dépendances et de l’API source pour Xray-core et libXray ; documentation dans `docs/XRAY_UPSTREAM_AUDIT.md` et `docs/LIBXRAY_API_REFERENCE.md`.
- **PoC CI (réalisée, run [#43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249))** : build officiel depuis le commit épinglé, SHA-256, ABIs, ELF, alignement 16 KiB, signature JVM, intégration `:app`, APK debug/androidTest.
- **Tests unitaires JVM** : `:app:testDebugUnitTest` — succès.
- **Tests instrumentés réels (émulateur x86_64 API 35)** : `:app:connectedDebugAndroidTest` — **6 tests, 0 échec, 0 ignoré** :
  - chargement effectif du runtime natif (`libgojni.so`) et disponibilité de `invoke` ;
  - appel réel `xrayVersion` (JSON valide, `success=true`, version non vide) ;
  - requête invalide (`apiVersion` seul) → erreur structurée sans crash ;
  - méthode inconnue → erreur structurée ;
  - `testXray` avec configuration invalide → échec géré sans crash.
- **Limite** : pas d’exécution sur appareil physique arm64/armv7 ; pas de tunnel VPN en Phase 2.
- Documentation : `docs/LIBXRAY_ANDROID_POC.md`.

### Phase 3 — Connexion VPN réelle
- **Tests unitaires JVM (`:app:testDebugUnitTest`)** :
  - assainissement des erreurs (UUID, liens, valeurs longues, message vide) ;
  - rejet des liens vides, des schémas non supportés et du VMess base64 historique avec message précis.
- **Tests instrumentés réels (AAR officiel requis, émulateur x86_64 API 35)** :
  - conversion réelle d’un lien VLESS et d’un lien VMess par libXray ;
  - configuration TUN complète acceptée par `testXray` (`env.xray.tun.fd`, inbound `tun`, outbound proxy, DNS) ;
  - `runXray` avec descripteur invalide → échec propre, état moteur arrêté ;
  - `stopXray` idempotent ; contrôleurs de protection et DNS appelables.
- **Tests E2E réels (serveur Xray-core officiel construit depuis le commit épinglé sur le runner)** :
  - avant connexion : requête directe vers `onfaa.test`/`192.0.2.10:18080` en échec ;
  - tunnel VLESS : état `Connected` seulement après sonde réelle ;
  - tunnel VMess : idem ;
  - DNS : `onfaa.test` → `192.0.2.10` via le DNS du tunnel ;
  - routage : requête HTTP 200 + token par nom et par IP TEST-NET, adresse uniquement joignable derrière le serveur Xray local ;
  - arrêt : état `Idle`, `getXrayState=false`, trafic direct de nouveau en échec ;
  - serveur injoignable : jamais `Connected`, erreur réelle ;
  - permission VPN : consentement requis (`VpnService.prepare`), connexion sans consentement refusée proprement.
- **Fixture déterministe** : inbounds VLESS `127.0.0.1:18443` et VMess `127.0.0.1:18444`, sortie `freedom`, serveur token `192.0.2.10:18080` (alias loopback TEST-NET), DNS `127.0.0.1:15353` en **UDP et TCP** répondant `onfaa.test` ; l’émulateur joint l’hôte via `10.0.2.2`. Le tunnel interroge le DNS en **TCP** (`tcp://10.0.2.2:15353`) car le forwarding UDP de l’alias hôte de l’émulateur n’est pas garanti, alors que TCP l’est.
- **Preuve** : journal d’accès du serveur Xray contenant la destination `192.0.2.10:18080`, XML `VpnTunnelE2ETest` avec 3 tests, 0 échec, 0 ignoré ; artefacts APK + rapport.
- **Limite CI** : le changement d’IP publique n’est pas démontrable (même sortie réseau) ; protocole manuel dans `docs/PHASE_3_REPORT.md`.
- **Limite matérielle** : appareil physique ARM64 non disponible ; protocole fourni, statut en attente.
- Documentation : `docs/PHASE_3_REPORT.md`.

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

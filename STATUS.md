# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 1 — Fondations Android natives
- **État :** **en cours (implémentation locale préparée, publication/validation CI en attente)** — la Phase 1 ne sera déclarée terminée qu’après compilation Android réussie sur GitHub Actions, tests vérifiés et artefact APK produit.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit publié (distant, vérifié avant début Phase 1)** : `47afb1eb2d80e870ce72819d946954e37a4f91c0` — *Phase 0: finalize status and changelog after remote verification*
- **Commits Phase 1** : aucun publié au moment de cette mise à jour (à compléter après push et vérification distante)

## Travaux terminés (locaux, Phase 1 — non encore publiés/vérifiés à distance)

- Structure Android/Gradle complète : `app/`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, catalogue `gradle/libs.versions.toml`, wrapper Gradle (scripts + `gradle-wrapper.jar` + properties).
- Application native Kotlin/Compose lançable (structure) : `MainActivity`, thème Material 3, navigation Compose, 5 écrans (Accueil, Configurations, Statistiques, Journaux, Paramètres).
- Bouton de connexion **désactivé** et clairement indiqué comme non opérationnel (moteur Xray/libXray non intégré) ; **aucun état de connexion ni statistique fictive**.
- Préparation VPN : permissions/manifeste et classe `com.onfaa.tunnel.vpn.OnfaaTunnelVpnService` (hérite de `VpnService`, ne crée aucun tunnel).
- Préparation données : modèles domaine, Room (entité/DAO/base) et dépôt DataStore de préférences.
- Tests unitaires JUnit de base (`app/src/test`) et squelette instrumenté (`app/src/androidTest`).
- Workflow GitHub Actions `.github/workflows/android.yml` (JDK 17, SDK, licences, validation wrapper, tests unitaires, `assembleDebug`, upload artefact APK).
- Documentation mise à jour/ajoutée (README, docs d’architecture/build/décisions/roadmap/tests, séparation app/VpnService/moteur).

## Travaux non commencés

- Intégration du moteur **XTLS/Xray-core** upstream et de **libXray** (Phase 2+), connexion VPN réelle (Phase 3).
- Import/parsing réel des configurations VLESS/VMess et UI de gestion complète (Phase 4+).
- Statistiques/journaux/diagnostics réels, réglages DNS/routage effectifs (Phases 6–7).
- CI/CD de livraison avancée (Phase 9, au-delà de la validation Phase 1).

## Blocages

- **Validation distante en attente** : compilation, tests et artefact APK doivent être confirmés par une exécution réussie de GitHub Actions après publication. Aucun SDK Android n’est disponible localement dans cet environnement pour une preuve de build complète.

## Prochaine tâche

1. Vérifier les changements (`git status`, diff), absence de secrets.
2. Créer un commit explicite Phase 1 et pousser sur `main`.
3. Vérifier le commit distant et l’exécution GitHub Actions (succès/échec, logs, artefact APK).
4. Mettre à jour `STATUS.md`, `docs/BUILD_GUIDE.md` et `CHANGELOG.md` avec les résultats réels (liens/run, APK) puis republier si nécessaire.

## Instructions de reprise pour une autre IA

1. Cloner `https://github.com/dems469-lab/ONFAA-TUNNEL-.git` et se placer sur `main`.
2. Lire `STATUS.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md`, `docs/BUILD_GUIDE.md`, `docs/ANDROID_XRAY_SEPARATION.md`.
3. Ne pas déclarer la Phase 1 terminée sans succès vérifié de la CI Android et artefact APK disponible.
4. Respecter : XTLS/Xray-core officiel upstream uniquement (intégration future via libXray vérifié), pas de sing-box, pas de faux moteur/librairie, pas de simulation de connexion/stats, pas de secrets.
5. Après chaque action de publication : mettre à jour `STATUS.md` avec le dernier commit publié, l’état CI et les blocages réels.

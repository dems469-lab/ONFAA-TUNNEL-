# Changelog — ONFAA TUNNEL

Tous les changements notables de ce projet seront documentés dans ce fichier.

Le format s’inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet visera un versionnage sémantique lorsque des livrables applicatifs existeront.

## [Unreleased]

### Phase 5 — lot 3 : les réglages « moteur » sont branchés au cœur Xray (livré et validé en CI)
- **Mapping `EngineSettings`** (`core/xray/EngineSettings`) : `UserPreferences` → options réellement exécutables par Xray-core, avec validation et replis sûrs ; plancher de niveau de log jamais abaissé (debug ≥ `info` pour les preuves E2E).
- **Application réelle par le service** (`OnfaaTunnelVpnService`) : lecture des préférences à chaque connexion, ligne de journal « Options moteur appliquées… » ; DNS/DoH (config `dns` + résolveur bootstrap UDP pour `setDNS`, override `EXTRA_DNS_SERVER` toujours prioritaire), MTU (TUN Android + inbound tun), mux (`streamSettings.mux`, absent si le profil en porte déjà), fragmentation TLS (outbound `freedom` + `fragment` `tlshello` via `dialerProxy`), ad blocker (règle de routage → `blackhole`, 22 domaines embarqués), mode de routage `all`/`bypass_private`/`proxy_only`, hotspot (inbound SOCKS `0.0.0.0:10808`), timeout de connexion (fenêtre de vérification), délai + reconnexion automatique (cycle de vie ; `false` = erreur et arrêt propre immédiats).
- **Sans équivalent moteur (annoncé honnêtement)** : mode gaming, comportement en arrière-plan — persistés, sans effet, notice à l'appui.
- **Tests** : `EngineSettingsTest` (7 tests JVM) ; `XrayConfigBuilderTest` étendu (mux, fragment + `dialerProxy`, blackhole, `proxy_only`, inbound hotspot, rejets MTU/routage). Les réglages par défaut reproduisent la configuration validée par les E2E du lot 1.
- **UI** : notices des sous-écrans Paramètres mises à jour (appliqué « à la prochaine connexion » au lieu de « non raccordé ») ; port SOCKS hotspot documenté.
- **Version** : `versionCode 6`, `versionName 0.6.0-engine-options`.
- **Docs** : `README.md` (bandeau + § « Fonctionnement du VPN »), `STATUS.md`.
- **Validation CI (commit `2f1d84e`) :** [Android CI #44225296961](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296961) `success` ; [E2E #44225296989](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296989) `success` — 34/34 instrumentés, tunnels réels `vless=3 vmess=3 trojan=3 shadowsocks=9`, artefact installable `onfaa-tunnel-apk` publié. Itérations corrigées en route : `0a52025` → `1a4b699` (lecture du Flow `preferences`), `1a4b699` → `2f1d84e` (test `proxy_only` ciblant la règle IP des réseaux privés).

### Correctif — l'APK publié par Android CI ne contenait pas le moteur

- **Cause réelle du « VPN non fonctionnel »** : `android.yml` publiait sous le nom `onfaa-tunnel-debug-apk` un APK construit **sans** l'AAR libXray (gitignoré, produit uniquement par le workflow E2E). Vérification sur l'artefact du run #44020633036 : aucun `lib/*/libgojni.so`, aucune classe `libXray.*`. À l'exécution, `XrayBridge.isAvailable()` renvoyait `false` et toute connexion échouait sur « Moteur libXray indisponible dans cette compilation ».
- **Publication** : le workflow E2E publie désormais un artefact dédié `onfaa-tunnel-apk`, après vérification que `libgojni.so` est présent pour `arm64-v8a`, `armeabi-v7a` et `x86_64`, avec le SHA-256 dans le résumé du run. L'artefact Android CI est renommé `onfaa-tunnel-debug-apk-no-engine` et le workflow émet un avertissement explicite.
- **Application** : l'écran d'accueil détecte l'absence du moteur, affiche un bandeau explicite et désactive « Connexion » au lieu d'échouer après la demande de permission.
- **Version** : `versionCode 5`, `versionName 0.5.0-phase5`.
- **Docs** : `README.md` et `docs/BUILD_GUIDE.md` indiquent désormais quel artefact installer.

### Phase 5 — lot 2 en cours : wiring des transports, SNI et modes d’inject sur le moteur Xray
- **Modes d’inject** : `InjectMode` (`DIRECT`, `DIRECT_SNI`, `PROXY`, `PROXY_SNI`, `DNSTT`) ajouté au modèle domaine ; `TunnelProfile` porte désormais le mode et le SNI résolus. `XrayConfigBuilder` applique le SNI (mode SNI-aware) et rejette `DNSTT` avec un message clair — ce mode nécessite un second moteur et n’est pas exécutable par Xray-core.
- **Transports** : `XrayConfigBuilder.SUPPORTED_TRANSPORTS` (`tcp`, `ws`, `grpc`) ; override `transport` validé au build.
- **Import** : le profil importé hérite du mode (`PROXY` / `PROXY_SNI`) et du SNI du serveur résolus par le moteur.
- **Tests** : `XrayConfigBuilderTest` (4 tests JVM) couvre SNI + transport, fallback serveur, mode `PROXY` et rejet `DNSTT`/transport inconnu.

### Phase 5 — lot 1 : Trojan, Shadowsocks et SOCKS5 via le moteur Xray (livré, CI verte)
- **Protocoles** : `TunnelProtocol` gagne `TROJAN`, `SHADOWSOCKS` et `SOCKS` (libellés UI `Trojan`, `Shadowsocks`, `SOCKS5`) ; le moteur reste XTLS/Xray-core v26.11.9 via libXray v26.11.9, aucun ajout de sing-box ni de faux moteur. Aucune migration Room : le nom de l’énumération est stocké tel quel.
- **Import** : liens `trojan://`, `ss://` (SIP002 base64) et `socks://` (userinfo base64) convertis par `convertShareLinksToXrayJson`, plus les sorties JSON `trojan`, `shadowsocks` et `socks` (forme plate ou `settings.servers[0]`, une seule entrée). Validation adaptée au protocole : UUID (VLESS/VMess), mot de passe (Trojan), méthode + mot de passe (Shadowsocks), aucun identifiant requis (SOCKS5). Messages d’erreur précis et assainis.
- **UI** : libellés de protocole dans Configs/Accueil/Stats, aide d’import et libellé du champ mis à jour.
- **Tests** : **27 tests unitaires** (dont 4 nouveaux cas d’import) ; 5 tests instrumentés de conversion/validation réelle (`ProtocolImportInstrumentedTest` : liens trojan/ss/socks, Shadowsocks `servers[]`, SOCKS5 sans authentification) ; 2 scénarios E2E réels (Trojan, Shadowsocks) et garde-fou `--min-tests 6`.
- **Fixture E2E** : inbounds `trojan-in` (port 18445, **TLS** avec certificat auto-signé épinglé par `pcs`, car libXray force TLS et Xray 26 a supprimé `allowInsecure`) et `ss-in` (port 18446, `aes-256-gcm`) ; journal d’accès vérifié par protocole.
- **Validation** : [Android CI #44019183046](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183046) `success` (commit `5e2f0d4`, 27 tests unitaires, APK debug + APK de tests) ; [E2E #44019183061](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183061) `success` — 30 étapes, **34/34 instrumentés**, `VpnTunnelE2ETest` 6/6, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3 trojan=3 shadowsocks=9`.
- **Limites** : SOCKS5 prouvé en conversion + `testXray` mais sans tunnel E2E (pas de serveur SOCKS5 dans la fixture) ; Trojan prouvé avec certificat auto-signé épinglé (pas de chaîne publique) ; Shadowsocks prouvé en AEAD `aes-256-gcm` seulement ; Hysteria 2 reste refusé (non exécutable par Xray-core). Détail : `docs/PHASE_5_PROTOCOLS_REPORT.md`.

### Phase 4 — persistance réelle, import étendu, cycle de vie durci (livrée, CI verte)
- **Persistance Room v2** : table `server_configs` réellement écrite (`ProfileEntity`, `ProfileDao`, `OnfaaDatabase` v2) ; ajout, renommage, suppression, favoris et sélection ; les profils survivent au redémarrage de l’application et de l’appareil.
- **Migration `1 → 2`** : table recréée explicitement puis recopiée (pas d’`ALTER TABLE ... DEFAULT`) ; lignes v1 conservées, marquées `incomplete` car la v1 ne stockait aucune donnée d’authentification ; connexion refusée avec message tant que le lien n’est pas réimporté. Test instrumenté avec une base v1 réelle.
- **Sécurité** : outbound Xray complet (UUID, clés) chiffré en **AES-256-GCM** par une clé non exportable de l’**Android Keystore** (alias `onfaa_server_payload_v1`, `SecretCipher`) ; échec de chiffrement = pas d’enregistrement, aucun repli en clair. Seuls les champs d’affichage restent lisibles.
- **Sélection persistée** dans DataStore (`onfaa_preferences`, `selected_config_id`) ; premier import sélectionné automatiquement ; repli déterministe si le profil actif disparaît.
- **Import étendu** : liens `vless://`/`vmess://`, presse-papiers, fichier (`OpenDocument`, ≤ 256 Ko) et configuration **Xray JSON** (ou outbound unique) ; `settings.vnext[0].users[0]` aplati ; structure validée puis `testXray` avant persistance ; erreurs lisibles et assainies ; nom personnalisable.
- **Cycle de vie VPN durci** : `AwaitingPermission`, `Preparing`, `Connecting`, `Verifying`, `Connected`, `Reconnecting(attempt, profil)`, `Disconnecting`, `Error`. Double démarrage refusé (mutex + drapeaux). `Connected` uniquement après `getXrayState` et sonde HTTP réelle. Surveillance toutes les 20 s ; échec → `Reconnecting`, reprise toutes les 5 s pendant 90 s, puis `Error` et arrêt propre. Profil incomplet refusé explicitement.
- **Journaux** : tampon borné de 500 événements + fichier tournant `filesDir/logs/onfaa.log` (256 Ko, copie `.1`, 200 dernières lignes relues au démarrage, thread dédié) ; filtres par niveau, recherche, copie, export `CreateDocument`, effacement confirmé ; secrets réassainis à l’écriture et à l’export.
- **Statistiques** : durée réelle depuis `sinceEpochMs`, octets/débits `TrafficStats` de l’UID applicatif, état et profil actif ; l’UI précise que ce ne sont **pas** des compteurs noyau du TUN et affiche « indisponible » si la plateforme ne fournit rien.
- **Tests** : `ConfigImporterJsonTest` (8), `LogFormatterTest` (4) en unitaire ; `ProfileStorageInstrumentedTest` (3 : migration, CRUD chiffré, altération GCM) et `LogFileStoreInstrumentedTest` (3 : persistance, rotation, borne/effacement) en instrumenté. Total : **23 unitaires**, **27 instrumentés**.
- **Validation** : [Android CI #44011899010](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899010) `success` (commit `230008c`) — compilation, 23/23 unitaires, APK debug et APK de tests instrumentés ; [E2E #44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095) `success` — **27/27 instrumentés** (45,579 s), E2E VLESS et VMess, `vless=3 vmess=3` (détails `docs/PHASE_4_REPORT.md` §9).
- **Limites** : appareil physique ARM64 non exécuté ; changement d’IP publique non démontrable en CI ; perte de clé Keystore = profils à réimporter ; reconnexion bornée à 90 s ; un seul fichier de logs tournant.

### Phase 5 — UI/UX, thèmes et références visuelles (implémentation livrée, compilation non vérifiée localement)
- **Références** : 5 images analysées individuellement et classées sans modification dans `docs/design/reference/` (`logs`, `appearance`, `servers`, `settings`) ; **5 classées, 0 non identifiée**. Les images livrées en `.png` sont des JPEG ; hashes et dimensions consignés. Aucune référence d’accueil n’a été fournie : l’accueil est documenté comme reconstruit.
- **Thèmes** : 7 palettes (ONFAA, Aurora, Ember, Graphite, Orchid, Paper, Matcha) × 3 modes (Système, Clair, Sombre), persistés avec DataStore et appliqués à toute l’application. « Dark/Light » est traité comme le mode d’affichage, pas comme une huitième palette.
- **Design system** : tokens de couleurs, typographie, dimensions, formes, composants réutilisables (fonds, logo, en-têtes, cartes, boutons, chips, badges, lignes de réglage, barre inférieure, orbe, graphique de trafic, journaux, diagnostics, aperçus de thème). Charte complète dans `docs/design/DESIGN_SPEC.md` (lien racine `DESIGN_SPEC.md`).
- **Navigation** : 4 onglets (Connexion, Configs, Paramètres, Journaux), sous-écrans de paramètres et Apparence. L’incohérence des références (3 ou 4 onglets) est documentée.
- **Écrans** : accueil réel (consentement VPN, états, durée, trafic `TrafficStats`), Configs (recherche, filtres, tri, favoris, latence TCP réelle, import lien/fichier, détail, suppression), Paramètres (groupes, recherche, sous-pages), Apparence, Journaux (filtres, recherche, copie, effacement, export texte, diagnostic réel).
- **Honnêteté des données** : aucune connexion, statistique ou fuite simulée ; la mention « aucune fuite » de la maquette est remplacée par « non testées » ; les options moteur non raccordées sont persistées mais signalées comme UI-only.
- **Limites** : compilation/tests UI non exécutés localement (pas de JDK/SDK Android) ; validation CI à faire ; accueil sans référence ; QR import non implémenté ; textes français codés en dur.

### Phase 3 — connexion VPN réelle via libXray/Xray-core (TERMINÉE, validée en CI sur émulateur x86_64)
- `OnfaaTunnelVpnService` réel : TUN IPv4/IPv6, routes par défaut, DNS, foreground service, consentement VPN, cycle de vie complet (connexion, vérification, arrêt, révocation, destruction).
- Pont moteur étendu (`XrayBridge`) : conversion réelle des liens, `testXray`, `runXray`, `stopXray`, `getXrayState`, protection des sockets via `VpnService.protect`, `setDNS`/`resetDNS`, appels hors thread UI.
- Contrat TUN réel Xray-core v26.11.9 : descripteur passé dans `env["xray.tun.fd"]` ; routage DNS/privé/proxy construit par `XrayConfigBuilder`.
- Import VLESS/VMess réel via `convertShareLinksToXrayJson`, validation par `testXray`, erreurs précises et assainies ; refus explicite du VMess base64 historique non supporté par l’upstream.
- UI branchée sur l’état réel : connexion/déconnexion, import/validation, journaux réels, aucune statistique fictive.
- Tests : unitaires (assainissement, validation d’import), instrumentés moteur (conversion, config TUN, fd invalide, idempotence) et E2E réel (serveur Xray-core officiel local, VLESS, VMess, DNS, routage, arrêt, serveur injoignable, permission VPN).
- Workflow `libxray-poc.yml` étendu : build du serveur Xray-core épinglé, services E2E déterministes (token HTTP sur TEST-NET + DNS `onfaa.test`), arguments d’instrumentation, vérification des preuves et du journal d’accès, artefacts.
- **Défauts réels corrigés pendant la validation** :
  - contrôleur de protection des sockets enregistré **une seule fois par processus** derrière un délégué `AtomicReference` : Xray empile les contrôleurs (`RegisterDialerController`) sans pouvoir les retirer, et l’ancien `clearSocketProtector()` laissait un contrôleur `{ false }` actif qui rejetait tous les dials du processus (`failed to apply external controller`) — VLESS et VMess, y compris après reconnexion ;
  - fixture serveur : `finalRules` explicite `allow` pour la cible de `redirect` `127.0.0.1:18080`, car Xray 26.x applique `defaultBlockPrivateRule` aux inbounds VLESS/VMess (sinon `target blocked by a finalrule` / blackhole) ;
  - fixture VLESS clair déplacée sur adresse LAN (`xray` refuse VLESS sans TLS vers une IP publique) et ajout d’un test de conversion VLESS public **avec TLS** ;
  - porte de preuve durcie : échec si le serveur journalise un blackhole, et exigence d’au moins une ligne d’accès par protocole.
- **Limites** : appareil physique ARM64 non disponible ; changement d’IP publique non démontrable en CI. Le trafic est mesuré via `android.net.TrafficStats` (UID applicatif), pas par un compteur noyau du TUN.
- **Validation** : run [#44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595) `success` (commit `5f51831`, 25/25 étapes) — **11/11 tests unitaires**, **21/21 tests instrumentés** (0 échec/erreur/ignoré), E2E VLESS et VMess avec sonde HTTP 200, DNS `onfaa.test` → `192.0.2.10`, journal d’accès `3 × vless-in -> token-redirect` + `3 × vmess-in -> token-redirect`, aucun blackhole serveur. APK debug `arm64-v8a/armeabi-v7a/x86/x86_64` publié.

### Phase 2 — audit et intégration libXray officielle (TERMINÉE, validée en CI)
- Audit des sources officielles XTLS/Xray-core v26.11.9 (commit `676eaf66…`, MPL-2.0) et XTLS/libXray v26.11.9 (commit `5a0f7d8…`, MIT), alignement de dépendance vérifié.
- Ajout de `docs/XRAY_UPSTREAM_AUDIT.md` et `docs/LIBXRAY_API_REFERENCE.md`.
- **Phase 2B** : workflow `libxray-poc.yml` (build officiel `python3 build/main.py android` depuis commit épinglé, vérification commit, NDK r30 `30.0.16248370`, inspection AAR/ABIs/ELF/16 KiB/`javap`, intégration `:app`, tests unitaires et instrumentés sur émulateur, artefacts), `XrayBridge` (API v3, appels hors UI, chargement natif explicite), tests instrumentés réels, script `scripts/inspect_libxray_aar.sh`, doc `docs/LIBXRAY_ANDROID_POC.md`.
- **Preuves** : run final [#43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249) `success` (commit `f116442`) ; AAR SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917` (53 200 280 octets), ABIs `arm64-v8a/armeabi-v7a/x86/x86_64`, ELF OK, tous `LOAD` alignés 16 KiB, signature JVM `libXray.LibXray#invoke(String):String` ; `testDebugUnitTest`/`assembleDebug`/`assembleDebugAndroidTest` réussis ; **6/6 tests instrumentés réels réussis sur émulateur x86_64 API 35 (0 ignoré)**.
- **Limite résiduelle** : exécution native prouvée sur x86_64 émulé ; pas encore sur appareil physique arm64/armv7.

### À venir
- **Phase 5** : refonte visuelle (travail parallèle, hors de ce changelog Phase 4).
- **Phase 6+** : options moteur raccordées, QR import, validation ARM64 physique.

## [0.1.0-phase1] — 2026-11-16 (publié et validé en CI)

### Ajouté
- Fondations Android natives : module `:app`, Gradle Kotlin DSL, catalogue de versions épinglées, wrapper Gradle, package `com.onfaa.tunnel`.
- Application Compose Material 3 : activité principale, navigation, 5 écrans (Accueil, Configurations, Statistiques, Journaux, Paramètres) ; bouton de connexion désactivé/non opérationnel, sans état ni statistique fictifs.
- Préparation `VpnService` : manifeste/permissions et `OnfaaTunnelVpnService` (aucun tunnel, aucun moteur).
- Préparation données : modèles domaine, Room (entité/DAO/base) et dépôt DataStore de préférences.
- Tests unitaires JUnit de base et squelette instrumenté.
- Workflow GitHub Actions `android.yml` (tests unitaires, `assembleDebug`, artefact APK debug).
- Documentation : `docs/ANDROID_XRAY_SEPARATION.md`, mises à jour README/STATUS/ARCHITECTURE/BUILD_GUIDE/TEST_PLAN/DECISIONS/ROADMAP.

### Validation
- CI réussie : https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43954661801 (commit `22551dee3d00b7f33413552ad0cb60ccceb27618`), artefact `onfaa-tunnel-debug-apk` publié.

## [0.0.0-phase0] — 2026-11-16 (publié)

### Ajouté
- Structure initiale du projet (répertoires `docs/`, `app/`, `gradle/`, `.github/`).
- Documentation fondamentale : contexte, architecture cible, roadmap (phases 0–9), décisions, plan de tests, guide de build, sécurité, spécification design.
- Fichiers racine : `README.md`, `STATUS.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE`, `.gitignore`.
- Préparation documentaire CI/CD (`.github/workflows/README.md`) sans workflow de compilation Android.

### Notes
- Commit de Phase 0 publié et vérifié sur `main` : `96cab902f0f96ed5a60dc40e03016b00500fd32e`.
- Référence du commit initial distant préexistant : `3be425c984deae6c2c0d5bc8777c870c1afbed07` (*Initial commit*, README minimal).
- Une mise à jour documentaire de statut peut suivre dans un commit distinct pour refléter la validation finale.

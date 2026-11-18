# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 5 — lot 4 (filtrage par application, auto-connexion réelle, abonnements) — **code livré, validation CI en cours**
- **Non revendiqué (lot 4) :** validation CI verte et exécution E2E de ce lot (en attente des runs) ; effet réel observé sur un appareil physique ; liste d'applications vérifiée sur un grand nombre d'appareils.
- **État (lot 4) :** trois fonctionnalités réelles livrées. **Filtrage par application :** préférences `appFilterMode` (`off`/`allow`/`block`) + `appFilterPackages`, parsing pur et validé dans `core/vpn/AppFilter` (grammaire des noms de packages, déduplication, plafond 200), application réelle par `VpnService.Builder` (`addAllowedApplication`/`addDisallowedApplication`) dans `buildTun`, sélecteur d'applications (`AppFilterSettingsScreen`, requête `MAIN`/`LAUNCHER` déclarée dans `<queries>`, sans `QUERY_ALL_PACKAGES`). **Auto-connexion :** la préférence `autoConnect`, précédemment inerte, est appliquée au lancement de l'application (`HomeScreen`, une seule tentative par session, uniquement depuis `Idle` et avec un profil complet) et au démarrage du téléphone (`BootReceiver` + `RECEIVE_BOOT_COMPLETED`, uniquement si le consentement VPN a déjà été accordé — jamais de boîte de dialogue depuis l'arrière-plan). **Abonnements :** `SubscriptionParser` pur (base64 v2ray standard/URL-safe ou liste brute, lignes non reconnues comptées), `SubscriptionImporter` (HttpURLConnection, bornes 15 s/20 s et 2 Mo, chaque lien passe par `ConfigImporter` → validation réelle du moteur), `SubscriptionStore` (DataStore, sources enregistrées, rafraîchissement et suppression), dialog « Abonnement » dans l'écran Configs. Le filtrage et l'auto-connexion ne touchent pas la configuration Xray : par défaut (`off`), le comportement reste exactement celui validé par les E2E. Version : `versionCode 7`, `versionName 0.7.0-lot4-features`.
- **Non revendiqué (lot 3) :** appareil physique ARM64 ; changement d'IP publique ; tunnel SOCKS5 de bout en bout ; effet réel du mode gaming ; filtrage par application *(réellement livré dans le lot 4)*.
- **Lot 3 (options moteur) :** livré et validé en CI (runs `#44225296961` et `#44225296989`, 34/34 instrumentés) — détail dans la section « Implémentation Phase 5 — lot 3 » ci-dessous.
- **Lot 1 (protocoles) :** livré et validé — l'application importe et exécute **VLESS, VMess, Trojan, Shadowsocks et SOCKS5** avec le même moteur XTLS/Xray-core v26.11.9 (libXray v26.11.9), sans sing-box ni simulation. Le run Android CI [#44019183046](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183046) (commit `5e2f0d4`) est **vert** : 27 tests unitaires ; le run E2E [#44019183061](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183061) est **vert** : **34/34 tests instrumentés**, `VpnTunnelE2ETest` 6/6 avec tunnels réels, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3 trojan=3 shadowsocks=9`. Détail : `docs/PHASE_5_PROTOCOLS_REPORT.md`.
- **Phase 3 :** terminée et validée en CI (tunnel réel VLESS/VMess, 21/21 instrumentés, run [#44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595)) ; preuves conservées ci-dessous.
- **Phase 2B :** terminée et validée (AAR officiel, API native, 6/6 tests instrumentés) ; preuves conservées ci-dessous.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Commit Phase 5 (lot 1 protocoles) :** `5e2f0d4`
- **Runs CI Phase 5 (lot 1) :** [Android CI #44019183046](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183046) — `success`, 27 tests unitaires ; [E2E #44019183061](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183061) — `success`, 34/34 instrumentés, `vless=3 vmess=3 trojan=3 shadowsocks=9`
- **Commit Phase 5 (lot 3 moteur) :** `2f1d84e`
- **Runs CI Phase 5 (lot 3) :** [Android CI #44225296961](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296961) — `success`, tests unitaires + `assembleDebug` + compilation des tests instrumentés ; [E2E #44225296989](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296989) — `success`, 34/34 instrumentés, `vless=3 vmess=3 trojan=3 shadowsocks=9`, artefact installable `onfaa-tunnel-apk` (71 079 614 octets). Itérations précédentes : `0a52025` (échec compile, corrigé par `1a4b699`), `1a4b699` (échec test unitaire trop large, corrigé par `2f1d84e`).
- **Commit Phase 4 (code) :** `230008c`
- **Runs CI Phase 4 :** [Android CI #44011899010](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899010) — `success` ; [libXray Android build and real VPN E2E #44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095) — `success`, 27/27 instrumentés
- **Commit Phase 3 (code) :** `5f518313082fc095eeb118553b6fd969318cddfd`
- **Run CI Phase 3 réussi :** [libXray Android build and real VPN E2E #44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595) — `success`, 25/25 étapes
- **Run de confirmation (CI allégée, commit `aef515e`) :** [#44006718908](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44006718908) — `success`, 21/21 instrumentés, `vless=3 vmess=3`
- **Dernier commit Phase 2B validé :** `f116442021b4e80508e3ef2448d21980f2207534`
- **Run Phase 2B réussi :** [#43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249)

## Implémentation Phase 5 — lot 4 (filtrage par application, auto-connexion, abonnements)

### Filtrage par application (VpnService, réel)

- `domain/model/UserPreferences` : `appFilterMode` (`off` par défaut / `allow` / `block`) et `appFilterPackages` (liste `\n`/`,`). `data/preferences/UserPreferencesRepository` : clés DataStore + setters.
- `core/vpn/AppFilter` : mapping pur (mode inconnu → `off`, grammaire des noms de packages, dédoublonnage, plafond `MAX_PACKAGES = 200`, sérialisation). Testable JVM.
- `OnfaaTunnelVpnService.buildTun(mtu, appFilter)` : `addAllowedApplication` / `addDisallowedApplication` sur le vrai `VpnService.Builder`, avant `establish()` ; rien n'est ajouté quand le mode est `off` (comportement par défaut identique aux runs verts). Ligne de journal « filtrage apps … » sans les noms de packages secrets (seul mode + nombre).
- UI : `AppFilterSettingsScreen` (mode via `ChoiceDialog`, liste des applications lançables via `queryIntentActivities(MAIN/LAUNCHER)` + `<queries>` au manifeste, sélection persistée à la volée) ; entrée dans l'écran Routage (l'ancien avis « pas de fausse application » est remplacé par la fonction réelle) et dans la recherche des Paramètres.

### Auto-connexion réelle

- `HomeScreen` : effet de lancement unique (`rememberSaveable` anti-rebond) — tentative uniquement depuis `Idle`, avec profil sélectionné complet et moteur disponible ; la boîte de consentement Android reste possible (jamais contournée).
- `vpn/BootReceiver` + permission `RECEIVE_BOOT_COMPLETED` : au `BOOT_COMPLETED`, lecture asynchrone des préférences ; connexion lancée seulement si `autoConnect`, un profil est sélectionné, l'état est `Idle`/`Error` et `VpnService.prepare` renvoie `null` (consentement déjà accordé) ; chaque refus est journalisé.

### Abonnements

- `data/subscription/SubscriptionParser` : base64 v2ray (standard ou URL-safe, padding optionnel) ou liste brute ; seuls les schémas `vless/vmess/trojan/ss/socks` sont retenus, le reste est compté comme ignoré ; base64 illisible → `SubscriptionException` lisible.
- `data/subscription/SubscriptionImporter` : `HttpURLConnection` (http/https uniquement, timeouts 15/20 s, plafond 2 Mo), chaque lien est importé par `ConfigImporter.importLink` (structure + `testXray` réel) ; le rapport compte importés/refusés/ignorés, zéro profil inventé ; la source est enregistrée dans `SubscriptionStore` (DataStore, clé par URL, plafond 20 sources).
- UI : `ConfigActionButton` « Abonnement (URL) » → `SubscriptionDialog` (URL + nom, import, liste des sources avec rafraîchissement et suppression).

### Tests du lot 4

- Unitaires JVM : `AppFilterTest` (8 : défauts, repli de mode, parsing `\n`/`,`, dédoublonnage, entrées invalides non fatales, grammaire, plafond, aller-retour `allow`), `SubscriptionParserTest` (6 : liste brute, base64, URL-safe sans padding, vide, base64 invalide, schémas filtrés).
- Instrumentés (nécessitent l'AAR, saignés par `assumeTrue`) : `VpnTunnelE2ETest` + `allowOnlySelfTunnelStillForwardsRealTraffic` (l'app seule autorisée : la fixture — joignable uniquement à travers le tunnel — reste joignable, donc `addAllowedApplication` est réellement appliqué) et `blockSelfBypassesTheTunnelForThisApp` (l'app exclue : tunnel connecté mais socket de l'app contournant le VPN, la fixture devient injoignable) ; `SubscriptionImportInstrumentedTest` (4 : serveur HTTP local réel, base64 + validation moteur, liste brute, HTTP 404 lisible, abonnement sans lien exploitable rejeté).

## Implémentation Phase 5 — lot 3 (branchement des réglages au cœur, livré et validé)

### Mapping préférences → moteur (`EngineSettings`)

- `core/xray/EngineSettings` : mapping pur et validé (`init` avec rejets explicites) de `UserPreferences` vers les options réellement exécutables par Xray-core ; valeurs inconnues retombées sur des valeurs sûres ; plancher de niveau de log (compilations debug ≥ `info`) jamais abaissé par la préférence.
- Résolveur bootstrap : `bootstrapResolverEndpoint()` donne toujours un `host:53` UDP pour `setDNS`, même quand le DNS choisi est en DoH (URL valable seulement dans la config `dns`).

### Application par le service VPN

- Le service lit `UserPreferencesRepository` (`first()`) à chaque connexion et journalise une ligne « Options moteur appliquées… » (sans secret).
- DNS : serveur choisi (DoH `https://host/dns-query` ou UDP) dans la config `dns` + résolveur bootstrap ; l'override `EXTRA_DNS_SERVER` (E2E) reste prioritaire.
- MTU : `buildTun(mtu)` et inbound tun ; mux : `streamSettings.mux {enabled, concurrency=8}` seulement si le profil n'en porte pas déjà ; fragmentation : `dialerProxy` → outbound `tls-fragment` (`freedom` + `fragment` tlshello) ; ad blocker : première règle de routage → outbound `blackhole` (`AD_DOMAINS`, 22 domaines embarqués) ; routage : `proxy_only` supprime la règle directe des réseaux privés ; hotspot : inbound `socks` réel sur `0.0.0.0:10808` (`noauth`, UDP).
- Cycle de vie : timeout de connexion = fenêtre totale de vérification (`connectionTimeoutSec`, 5–120 s) ; `autoReconnect=false` → erreur et arrêt propre immédiats au lieu de la fenêtre de reprise ; `reconnectDelaySec` (1–60 s) pilote les tentatives de reprise.

### Honnêteté et UI

- Notices de Paramètres mises à jour : fragmentation, ad blocker, DNS, routage, MTU/mux, hotspot, niveau de log, reconnexion sont annoncés comme **appliqués à la prochaine connexion** ; gaming et arrière-plan restent explicitement sans effet moteur ; le hotspot précise que les appareils clients doivent configurer le proxy SOCKS manuellement.
- `UserPreferences.kt` documente le branchement ; rien n'est simulé, aucune revendication sans preuve CI.

### Tests du lot 3

- Unitaires (pur JVM, sans moteur) : `EngineSettingsTest` (7 : défauts identiques au comportement validé, DoH/bootstrap, UDP, plancher de log, replis sûrs, transport des options, rejets) et `XrayConfigBuilderTest` étendu (mux émis/absent, fragment + `dialerProxy`, blackhole + règle, `proxy_only` sans la règle directe mais DNS conservé, inbound SOCKS hotspot, rejets MTU/routage).
- Par construction, les réglages par défaut produisent une configuration identique aux runs E2E verts du lot 1 (MTU 1500, DNS via override E2E, options désactivées).

### Validation CI du lot 3 (preuves)

- **Commit :** `2f1d84e`. [Android CI #44225296961](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296961) — `success` (tests unitaires, `assembleDebug`, compilation des tests instrumentés).
- [E2E #44225296989](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296989) — `success` : **34/34 tests instrumentés** sur émulateur x86_64 API 35 avec le moteur réel, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3 trojan=3 shadowsocks=9`, étape « Verify E2E evidence » passée, artefact **`onfaa-tunnel-apk`** publié (71 079 614 octets) avec l'AAR libXray v26.11.9 vérifié (SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`).
- Tracé des itérations : `0a52025` (échec compilation : `.first()` appelé sur le dépôt au lieu du Flow `preferences`) → corrigé par `1a4b699` ; `1a4b699` (échec : le test `proxy_only` affirmait l'absence de toute règle directe alors que la règle `dns-direct` légitime porte `direct`) → corrigé par `2f1d84e` (ciblage explicite de la règle IP des réseaux privés).

## Implémentation Phase 5 — lot 1 (livrée)

### Protocoles (même moteur Xray-core)

- **VLESS, VMess, Trojan, Shadowsocks, SOCKS5** via libXray (`convertShareLinksToXrayJson`, `testXray`, `runXray`) ; aucun protocole exécuté hors du moteur officiel.
- **Liens** : `vless://`, `vmess://`, `trojan://`, `ss://` (SIP002 base64), `socks://` (userinfo base64). **JSON Xray** : sortie `vless`, `vmess`, `trojan`, `shadowsocks`, `socks` ; `settings.servers[0]` accepté pour une entrée unique.
- **Validation** : structure puis `testXray` ; identifiants vérifiés selon le protocole (UUID, mot de passe Trojan, méthode + mot de passe Shadowsocks, aucun pour SOCKS5).
- **Sécurité inchangée** : l’outbound complet (UUID, mots de passe, clés) est chiffré AES-256-GCM (Android Keystore) à l’enregistrement ; jamais journalisé ; erreurs assainies. Aucune migration Room : l’énumération est stockée par son nom.

### Tests et preuves

- Unitaires : **27** (dont 4 nouveaux cas d’import : Trojan sans mot de passe, Shadowsocks sans méthode, sans mot de passe, plusieurs `servers`).
- Instrumentés : **34** (dont `ProtocolImportInstrumentedTest` 5 : liens trojan/ss/socks réels, Shadowsocks `servers[]`, SOCKS5 sans authentification).
- E2E : `VpnTunnelE2ETest` **6/6** avec tunnels réels VLESS, VMess, **Trojan** (TLS, certificat auto-signé épinglé par `pcs`) et **Shadowsocks** (`aes-256-gcm`) ; DNS et routage réels ; arrêt propre ; journal d’accès `vless=3 vmess=3 trojan=3 shadowsocks=9`.
- Limites : SOCKS5 sans tunnel E2E ; pas de chaîne TLS publique pour Trojan ; Shadowsocks 2022 non revendiqué ; Hysteria 2 refusé (non exécutable par Xray-core). Détail : `docs/PHASE_5_PROTOCOLS_REPORT.md`.

## Implémentation Phase 4 (livrée)

### Persistance et sécurité

- **Room v2** (`onfaa_profiles.db`, table `server_configs`) : ajout, renommage, suppression, favoris, sélection ; les profils survivent au redémarrage du processus et de l’appareil.
- **Migration `1 → 2`** testée : la table est recréée explicitement puis recopiée ; les lignes v1 sont conservées, marquées `incomplete` (aucune donnée d’authentification en v1) et ne peuvent pas être connectées tant que le lien n’est pas réimporté.
- **Chiffrement** : l’outbound Xray complet (UUID, clés) est chiffré en AES-256-GCM par une clé non exportable de l’Android Keystore (alias `onfaa_server_payload_v1`). Échec de chiffrement = pas d’enregistrement (aucun repli en clair).
- **Sélection** persistée dans DataStore (`onfaa_preferences`, clé `selected_config_id`) ; premier import sélectionné automatiquement ; repli déterministe si le profil actif disparaît.

### Import

- Liens `vless://` / `vmess://`, presse-papiers, fichier (`OpenDocument`, ≤ 256 Ko) et **configuration Xray JSON** (ou un outbound unique) ; `settings.vnext[0].users[0]` aplati.
- Validation en deux temps : structure puis `testXray` (moteur officiel) ; rien n’est persisté si le moteur refuse.
- Erreurs lisibles en français, assainies (UUID/liens masqués) ; nom personnalisable à l’import.

### Cycle de vie VPN

- États : `Idle`, `AwaitingPermission`, `Preparing`, `Connecting`, `Verifying`, `Connected`, `Reconnecting(attempt, profil)`, `Disconnecting`, `Error`.
- Double démarrage refusé (mutex + drapeaux + état) ; profil absent ou incomplet refusé avec message.
- `Connected` uniquement après `getXrayState` **et** sonde HTTP réelle ; surveillance toutes les 20 s, reprise toutes les 5 s pendant 90 s maximum, puis `Error` et arrêt propre.
- Arrêt : `stopXray`, `resetDNS`, fermeture du TUN, jobs annulés, `Idle`/`Error`.

### Journaux et statistiques

- Tampon mémoire de 500 événements + fichier tournant `filesDir/logs/onfaa.log` (256 Ko, copie `.1`, 200 dernières lignes relues au démarrage) ; écriture sur thread dédié.
- Filtres par niveau, recherche, copie, export `CreateDocument`, effacement confirmé ; secrets réassainis à l’export.
- Durée réelle depuis `sinceEpochMs`, octets/débits `TrafficStats` de l’UID applicatif, état et profil actif ; l’UI précise explicitement qu’il ne s’agit **pas** d’un compteur noyau du TUN.

### Tests Phase 4

- Unitaires : **23/23** (dont `ConfigImporterJsonTest` 8, `LogFormatterTest` 4).
- Instrumentés : **27** au total (dont `ProfileStorageInstrumentedTest` 3 : migration v1→v2, CRUD chiffré, altération GCM ; `LogFileStoreInstrumentedTest` 3 : redémarrage, rotation, borne/effacement).

## Implémentation Phase 3 (livrée)

### Moteur et pont

- `XrayBridge` étendu : `convertShareLink`, `testXray`, `runXray`, `stopXray`, `getXrayState`, `registerSocketProtector`, `setDNS`, `resetDNS`.
- Appels natifs exécutés hors du thread UI (`Dispatchers.Default`).
- Runtime Go unique conservé : `libgojni.so` fourni par l’AAR officiel, aucune autre runtime Go.
- `DialerController` implémenté par un `java.lang.reflect.Proxy` réel (méthode `protectFd(long)` vérifiée dans l’AAR).

### TUN Android et DNS

- `OnfaaTunnelVpnService` établit un TUN réel : IPv4 `10.10.0.2/24`, IPv6 `fd00::2/64`, routes par défaut, DNS `10.10.0.1` / `fd00::1`, MTU 1500.
- Descripteur TUN transmis au moteur conforme au contrat Xray-core v26.11.9 : clé racine `env["xray.tun.fd"]` (libXray n’expose plus `SetTunFd`).
- Sockets sortants du moteur protégés via `VpnService.protect(fd)` ; résolveur Go de libXray configuré par `setDNS` puis restauré par `resetDNS`.
- Routage Xray : UDP/TCP port 53 → `dns-out` ; DNS interne → sortie directe ; réseaux privés → direct ; reste → proxy VLESS/VMess.

### Cycle de vie et états

- États réels : `Idle`, `Connecting`, `Verifying`, `Connected`, `Disconnecting`, `Error`.
- `Connected` n’est publié **qu’après** : `runXray` réussi, `getXrayState` vrai, et **sonde HTTP réelle** réussie à travers le tunnel.
- Arrêt : `stopXray`, `resetDNS`, neutralisation du protector, fermeture du descripteur TUN, `stopForeground`, `stopSelf`.
- Gestion de la destruction, de la annulation et de la révocation VPN (`onRevoke`).
- Notification de service au premier plan avec action « Déconnecter ».

### Import VLESS/VMess

- Conversion réelle par `convertShareLinksToXrayJson` de libXray ; l’outbound produit est embarqué dans une configuration TUN complète puis validée par `testXray`.
- Erreurs précises et assainies (UUID, liens et secrets masqués) ; aucune configuration complète journalisée.
- Refus explicite des liens VMess base64 historiques (non acceptés par libXray v26.11.9) avec message indiquant le format URL attendu.
- Profils **persistés dans Room** avec outbound chiffré depuis la Phase 4 ; la Phase 3 les gardait en mémoire.

### UI

- `HomeScreen` : connexion/déconnexion réelles, demande de consentement VPN, état réel, erreurs réelles, durée de session et trafic réel `TrafficStats`.
- `ConfigsScreen` : import réel, validation par le moteur, sélection/suppression, favoris, latence TCP réelle, tri et détail.
- `LogsScreen` : événements réels du cycle de vie, assainis, filtres/recherche/copie/partage et diagnostic basé uniquement sur l’état réel.
- `StatsScreen` : conservé dans le code mais retiré de la navigation principale ; les métriques réelles sont intégrées à l’accueil.
- **Phase 5 UI** : 4 onglets (Connexion, Configs, Paramètres, Journaux), sous-écrans de paramètres, Apparence, 7 palettes × 3 modes, composants réutilisables et charte `docs/design/DESIGN_SPEC.md`. Les 5 références fournies sont classées dans `docs/design/reference/` (0 non identifiée) ; **aucune référence d’accueil n’a été fournie**, donc l’accueil est documenté comme reconstruit.
- Les options de paramètres non raccordées au moteur sont persistées mais explicitement signalées comme UI-only ; aucun effet n’est simulé.

## Preuves Phase 3 (run CI réussi `44004893595`)

Prouvé sur émulateur **x86_64 API 35**, avec l’AAR officiel et le serveur
**Xray-core 26.11.9** officiel construits depuis les commits épinglés :

1. **Échec direct avant connexion** ✔ — requête vers `onfaa.test`/`192.0.2.10:18080` en échec hors tunnel (assertion `assertDirectTrafficFails`).
2. **Tunnel VLESS réel** ✔ — `Connected` seulement après sonde HTTP `200 — onfaa-e2e-token-phase3` ; `tun-in >> proxy` puis `proxy/vless/outbound: tunneling request`.
3. **Tunnel VMess réel** ✔ — même chaîne avec `proxy/vmess/outbound` et l’inbound VMess.
4. **DNS réel** ✔ — `onfaa.test` → `192.0.2.10` via le DNS du tunnel (`tun-in -> dns-out`, résolveur `TCPL//10.0.2.2:15353`).
5. **Routage réel** ✔ — HTTP 200 + token par IP et par nom vers une adresse TEST-NET uniquement joignable derrière le serveur Xray local ; journal d’accès `3 × vless-in -> token-redirect` et `3 × vmess-in -> token-redirect`.
6. **Arrêt propre** ✔ — état `Idle`, `getXrayState=false`, trafic direct de nouveau en échec.
7. **Serveur injoignable** ✔ — 6/6 sondes en échec, jamais `Connected`, erreur réelle publiée.
8. **Permission VPN** ✔ — consentement requis, connexion refusée proprement sans consentement, moteur arrêté.

Totaux du run : **11/11 tests unitaires**, **21/21 tests instrumentés**
(0 échec, 0 erreur, 0 ignoré), portes de preuve `E2E_XML_VALIDATED`,
`ACCESS_LOG_PROVES_TUNNEL_DESTINATION`, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3`,
aucun `SERVER_BLOCKED_TUNNEL_TARGET`. Détail et extraits : `docs/PHASE_3_REPORT.md` §9.

## Preuves Phase 2B (conservées)

- AAR officiel `libXray.aar` construit depuis `5a0f7d8d…` : **53 200 280 octets**, SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`.
- ABIs : `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` ; ELF conformes ; **tous les segments `LOAD` alignés 16 KiB**.
- JVM : `libXray.LibXray#invoke(java.lang.String): java.lang.String`, chargement de `libgojni.so`.
- Run `43965698249` : `success`, **6/6 tests instrumentés réels, 0 ignoré**, sur émulateur x86_64 API 35.

## Limites résiduelles (Phase 3)

- **Appareil physique ARM64 : non disponible.** L’exécution native du moteur est prouvée sur x86_64 émulé ; les binaires ARM sont présents et structurellement vérifiés mais pas exécutés sur téléphone physique. Protocole de test physique fourni dans `docs/PHASE_4_REPORT.md` §10, statut **en attente**.
- **Changement d’IP publique : non démontrable en CI** (le runner et l’émulateur partagent la même sortie réseau). Protocole manuel documenté ; aucune affirmation d’IP publique changée n’est faite.
- **Perte de clé Keystore** : les profils existants deviennent illisibles, sont marqués `incomplete` et doivent être réimportés (comportement affiché, aucun repli en clair).
- **Reconnexion bornée** à 90 s, sans stratégie exponentielle ni reprise prolongée en arrière-plan.
- **Journaux** : un seul fichier tournant ; pas d’archive multi-fichiers.
- Liens VMess base64 historiques refusés (libXray v26.11.9 ne les convertit pas) ; les liens VMess URL `vmess://UUID@hôte:port?...` sont supportés.
- Trafic mesuré par `android.net.TrafficStats` pour l’UID applicatif (sockets du moteur inclus), pas par un compteur noyau du TUN ; les valeurs sont réelles ou « indisponible ».
- Options moteur de la Phase 5 (fragmentation, ad block, gaming, hotspot, routage choisi, DNS choisi, MTU, mux, niveau de log, reconnexion automatique) persistées mais **non raccordées** ; l’UI l’indique explicitement.
- Aucune détection de fuite DNS/IP n’est implémentée : l’UI affiche « non testées » et ne reprend pas l’affirmation « aucune fuite » de la maquette.
- Le run CI complet dépend de GitHub Actions (pas de JDK/Go/Android SDK local sur le téléphone) ; **la compilation des changements UI Phase 5 n’a pas encore été vérifiée localement**.

## Blocages / risques

- **Aucun blocage CI** : le run `44004893595` est vert. Les deux seules limites ouvertes sont matérielles/environnementales (ARM64 physique, IP publique) et documentées ci-dessus.
- Si l’émulateur ne peut pas joindre `10.0.2.2`, le test E2E échouera explicitement (aucun succès simulé).
- Un seul runtime Go par processus : ne pas ajouter d’autre bibliothèque Go.
- Contrôleur de protection de sockets : Xray empile les contrôleurs sans pouvoir les retirer, donc **un seul enregistrement par processus** (délégué `AtomicReference`). Ne jamais enregistrer un contrôleur qui refuse.

## Prochaine tâche

1. Lire `docs/PHASE_4_REPORT.md` §9 : run E2E #44011899095 vert, 27/27 instrumentés, `vless=3 vmess=3`, artefacts `libxray-poc-report` et `onfaa-tunnel-debug-apk`.
2. Exécuter le protocole ARM64 sur un appareil physique dès qu’il est disponible (`docs/PHASE_4_REPORT.md` §10) ; ne pas revendiquer avant.
3. Faire valider en CI la compilation des changements UI Phase 5 (thèmes, navigation, sous-écrans) avant toute revendication de livraison.

## Instructions de reprise pour une autre IA

1. Lire `STATUS.md`, `docs/PHASE_3_REPORT.md`, `docs/LIBXRAY_API_REFERENCE.md`, `docs/LIBXRAY_ANDROID_POC.md`, `docs/DECISIONS.md`.
2. Ne jamais remplacer Xray-core/libXray par un mock, un fork ou sing-box ; ne jamais publier « Connecté » sans moteur actif et sonde réelle réussie.
3. Toute modification de l’intégration doit repasser le workflow `libxray-poc.yml` et conserver la traçabilité des commits/run/artefacts.

# Architecture — ONFAA TUNNEL

> Ce document décrit l’**architecture cible** et l’état des fondations implémentées. Les détails d’intégration `libXray` (signatures, types, cycle de vie exact) ont été confirmés en Phase 2 : voir [`LIBXRAY_ANDROID_POC.md`](LIBXRAY_ANDROID_POC.md). Voir aussi [`ANDROID_XRAY_SEPARATION.md`](ANDROID_XRAY_SEPARATION.md).

## État Phase 1 (implémenté/préparé)

- **Module** : `:app` unique, package `com.onfaa.tunnel`.
- **UI** : `MainActivity` + Jetpack Compose (Material 3) + Navigation Compose avec 5 écrans (`feature/home`, `configs`, `stats`, `logs`, `settings`). Bouton connexion désactivé et non opérationnel ; aucun état/statistique fictif.
- **VPN (préparation)** : `OnfaaTunnelVpnService` (extends `VpnService`) déclaré dans le manifeste avec permissions préparatoires ; **aucun tunnel, aucun moteur**.
- **Données (préparation)** : modèles domaine (`domain/model`), Room (`data/local/db` : entité, DAO, `OnfaaDatabase`), DataStore (`data/preferences/UserPreferencesRepository`), constantes (`core/AppConstants`).
- **Tests** : unitaires JUnit dans `app/src/test`, squelette instrumenté dans `app/src/androidTest`.
- **Moteur Xray/libXray** : **non intégré**.

## État Phase 2 (intégration libXray validée en CI)

- Audit upstream réalisé et documenté : `docs/XRAY_UPSTREAM_AUDIT.md`, `docs/LIBXRAY_API_REFERENCE.md`.
- AAR **libXray v26.11.9** (`5a0f7d8…`) construit depuis source épinglée en CI, SHA-256 vérifié, ABIs `arm64-v8a/armeabi-v7a/x86/x86_64`, alignement 16 KiB, dépendances système uniquement.
- **Binding réel** : `libXray.LibXray#invoke(String):String` (statique native), encapsulé dans `core/xray/XrayBridge` (API v3, hors thread UI, chargement natif explicite, runtime Go unique).
- **Preuves** : compilation `:app` avec AAR, tests unitaires et **tests instrumentés réels réussis sur émulateur x86_64 API 35** (`invoke`, `xrayVersion`, erreurs) — voir `docs/LIBXRAY_ANDROID_POC.md`.
- **Toujours absent** : établissement du tunnel VPN (`VpnService`/TUN), routage, états de connexion, statistiques (Phase 3+).

## État Phase 3 (tunnel réel validé en CI, émulateur x86_64)

- **Service VPN réel** : `OnfaaTunnelVpnService` établit un TUN Android (IPv4 `10.10.0.2/24`, IPv6 `fd00::2/64`, routes par défaut, DNS `10.10.0.1`/`fd00::1`, MTU 1500), se déclare au premier plan et pilote le moteur.
- **Chaîne réelle** : `MainActivity`/Compose → `TunnelConfigRepository`/`TunnelStatusRepository` → `OnfaaTunnelVpnService` → `XrayBridge` → `libXray.LibXray#invoke` / `setDNS` / `registerDialerController` → **Xray-core officiel**.
- **Contrat TUN** : le descripteur du `VpnService` est transmis au moteur via `env["xray.tun.fd"]` (Xray-core v26.11.9 ; libXray n’expose plus `SetTunFd`). Le service Android possède adresses/routes/DNS ; l’inbound `tun` lit le descripteur.
- **Protection des sockets** : les sockets du moteur sont protégés par `VpnService.protect(fd)` via un `DialerController` réel, pour éviter la boucle VPN. `setDNS`/`resetDNS` configurent le résolveur Go utilisé par libXray. Xray-core **empile** les contrôleurs de dial (`transport/internet.RegisterDialerController` n’a pas de désinscription et tout dial doit satisfaire tous les contrôleurs) : le pont enregistre donc **un seul** contrôleur par processus, derrière un délégué `AtomicReference` que le service remplace à la connexion et vide à l’arrêt. Un second enregistrement, même temporairement refusant, casserait tous les dials du processus.
- **Configuration** : lien VLESS/VMess/Trojan/Shadowsocks/SOCKS5 → `convertShareLinksToXrayJson` (libXray) → outbound réel → configuration TUN complète (`XrayConfigBuilder`) → `testXray` → `runXray`.
- **DNS/routage** : port 53 issu du TUN → `dns-out` ; requêtes DNS internes → sortie directe ; réseaux privés → direct ; trafic restant → proxy.
- **États** : `Idle`/`Connecting`/`Verifying`/`Connected`/`Disconnecting`/`Error`. `Connected` exige `runXray` réussi, `getXrayState=true` et une **sonde HTTP réelle** à travers le tunnel.
- **Arrêt** : `stopXray` → `resetDNS` → neutralisation du protector → fermeture du TUN → arrêt du foreground → `stopSelf` ; gestion de `onRevoke`/`onDestroy`.
- **Import** : validation réelle par le moteur, erreurs précises, secrets masqués ; profils en mémoire uniquement à ce stade (persistance chiffrée livrée en Phase 4).
- **Validation CI** : run `44004893595` — 21/21 tests instrumentés, E2E VLESS et VMess avec sonde HTTP 200 à travers le TUN, DNS via `dns-out`, journal d’accès serveur par protocole. Fixture : Xray-core officiel épinglé, inbounds loopback VLESS/VMess, token sur `192.0.2.10:18080` via `freedom` `redirect` (avec `finalRules` `allow` explicite, car Xray 26.x bloque par défaut les destinations privées pour ces inbounds).
- **Limite** : validation native prouvée sur émulateur x86_64 ; appareil physique ARM64 à exécuter.

## État Phase 4 (persistance, import étendu, cycle de vie durci)

- **Persistance Room réelle** : table `server_configs` en version 2 (`ProfileEntity`/`ProfileDao`/`OnfaaDatabase`). Colonnes : `id`, `name`, `protocol`, `endpoint`, `security`, `payloadCipher`, `isFavorite`, `notes`, `createdAt`, `updatedAt`. `TunnelConfigRepository` est un singleton initialisé par `OnfaaTunnelApp` ; il expose des `StateFlow` (profils, sélection, favoris) alimentés par Room et DataStore.
- **Migration `1 → 2`** : la table est recréée explicitement (pas d’`ALTER TABLE ... DEFAULT`) puis les lignes sont copiées ; les lignes v1 survivent avec `security='none'` et `payloadCipher=''`. Elles sont affichées comme **incomplètes** et ne peuvent pas être connectées tant que le lien n’est pas réimporté (v1 ne stockait aucun identifiant). Test instrumenté dédié (`ProfileStorageInstrumentedTest`).
- **Protection des données sensibles** : l’outbound Xray (UUID, clés) est chiffré en **AES-256-GCM** par `SecretCipher`, avec une clé non exportable générée dans le **Android Keystore** (alias `onfaa_server_payload_v1`). Seul le texte chiffré (`base64(iv||ciphertext)`) est écrit dans Room ; un échec de chiffrement annule l’enregistrement, jamais de repli en clair. Les champs d’affichage (nom, protocole, hôte, sécurité) restent en clair car nécessaires à l’UI.
- **Sélection persistée** : identifiant du profil actif dans DataStore (`onfaa_preferences`, clé `selected_config_id`), favoris dans Room.
- **Import étendu** : `ConfigImporter` accepte un lien `vless://`/`vmess://`/`trojan://`/`ss://`/`socks://`, un **objet outbound** ou une **configuration Xray JSON** (une seule sortie parmi VLESS, VMess, Trojan, Shadowsocks, SOCKS5 ; `settings.vnext[0].users[0]` et `settings.servers[0]` aplatis), du **presse-papiers** et un **fichier** (via `ActivityResultContracts.OpenDocument`, limite 256 Ko). La structure est validée avant l’appel natif, puis `testXray` valide la configuration ; rien n’est enregistré si le moteur refuse.
- **Cycle de vie VPN** : états `AwaitingPermission`, `Preparing`, `Connecting`, `Verifying`, `Connected`, `Reconnecting(attempt)`, `Disconnecting`, `Error` en plus de `Idle`. Une surveillance post-connexion rejoue une **sonde réelle** toutes les 20 s ; en cas d’échec l’état passe immédiatement à `Reconnecting` (jamais `Connected`), une fenêtre de reconnexion bornée (90 s, sonde toutes les 5 s) est ouverte, et `Connected` n’est republié qu’après une sonde réussie. Au-delà, erreur réelle et arrêt propre. Double démarrage refusé (mutex + état + `engineRunning`). Profil incomplet refusé avec message explicite.
- **Journaux** : tampon mémoire borné (500 événements) **doublé d’un fichier tournant** `filesDir/logs/onfaa.log` (256 Ko, copie `.1`, relecture des 200 dernières lignes au démarrage). Écriture sur un thread dédié ; chaque message est assaini à l’écriture puis à l’export (UUID, liens, valeurs base64). Filtres par niveau, recherche texte, copie, export via `CreateDocument`, effacement confirmé.
- **Statistiques** : durée de connexion (horloge réelle depuis `sinceEpochMs`), octets/débits `android.net.TrafficStats` de l’UID applicatif, état du tunnel, profil actif. L’UI indique explicitement que ce sont des compteurs UID (pas un compteur noyau du TUN) et affiche « indisponible » si la plateforme renvoie `UNSUPPORTED`.
- **Limite inchangée** : validation native sur émulateur x86_64 ; appareil physique ARM64 à exécuter.

## État Phase 5 — lot 1 (protocoles Trojan, Shadowsocks, SOCKS5)

- **Moteur inchangé** : XTLS/Xray-core v26.11.9 via libXray v26.11.9 ; les nouveaux protocoles passent par le même chemin `convertShareLinksToXrayJson` → `XrayConfigBuilder` → `testXray` → `runXray`.
- **Modèle** : `TunnelProtocol` = VLESS, VMESS, TROJAN, SHADOWSOCKS, SOCKS ; le nom est stocké dans Room (aucune migration), l’outbound complet reste chiffré AES-256-GCM.
- **Validation par protocole** : UUID (VLESS/VMess), mot de passe (Trojan), méthode + mot de passe (Shadowsocks), aucun identifiant (SOCKS5) ; `settings.servers[0]` accepté s’il n’y a qu’une entrée.
- **Preuves** : E2E `44019183061` — tunnels réels VLESS, VMess, Trojan (TLS auto-signé épinglé via `pcs`) et Shadowsocks (`aes-256-gcm`), `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3 trojan=3 shadowsocks=9`. SOCKS5 : conversion et `testXray` réels, sans tunnel E2E. Détail : `docs/PHASE_5_PROTOCOLS_REPORT.md`.

## Vue d’ensemble

Application Android native en **Kotlin**, structurée autour de :

1. **UI** (écrans, thèmes, état affiché)
2. **Domaine / cas d’usage** (connexion, gestion des profils, import, diagnostics)
3. **Données** (Room pour entités, DataStore pour préférences)
4. **Service VPN** (`VpnService`) comme point d’entrée système du tunnel
5. **Moteur réseau** : **XTLS/Xray-core officiel** embarqué/intégré via **libXray**

## Composants principaux

### 1. Couche Android / Système

- **`VpnService`** : établissement de l’interface TUN, gestion du cycle de vie du service au premier plan/notification selon exigences Android, transfert du trafic vers le moteur conformément à l’intégration retenue.
- **Permissions et politique réseau** : déclarées et gérées selon les besoins réels (VPN, réseau, notifications si nécessaire, etc.) lors des phases d’implémentation.

### 2. Moteur Xray

- **XTLS/Xray-core officiel** : cœur de traitement des protocoles (VLESS, VMess, Trojan, Shadowsocks, SOCKS5) et de la logique proxy/tunnel.
- **libXray** : couche de liaison utilisée par l’application Android pour démarrer/arrêter/configurer le cœur (API exacte à confirmer ; **aucune signature inventée**).
- **Contrainte** : pas de substitution par sing-box ou autre moteur.

### 3. Persistance (Phase 4)

- **Room** : base `onfaa_profiles.db`, table `server_configs` v2. Les profils survivent au redémarrage du processus et de l’appareil ; l’outbound moteur est chiffré (AES-256-GCM + Android Keystore), jamais en clair.
- **DataStore** : préférences utilisateur (`onfaa_preferences`) : thème, `selected_config_id`, options UI. La sélection de profil est appliquée au démarrage.
- **Fichier de journaux** : `filesDir/logs/onfaa.log` (+ `.1`), borné et assaini, pour diagnostiquer après un crash.

### 4. Asynchronisme et état

- **Coroutines** : opérations I/O, démarrage/arrêt, tâches longues.
- **Flow** : diffusion d’état (connexion, statistiques, logs, préférences) vers l’UI et les composants observateurs.

### 5. Modules fonctionnels cibles

- **Gestion des configurations** : import, parsing/validation, mapping vers la représentation consommée par Xray (détails en phases 2–4).
- **Gestion des serveurs** : sélection du profil actif, persistance, cohérence avec l’état du tunnel.
- **Statistiques & diagnostics** : collecte de métriques réelles disponibles, affichage, export contrôlé des logs.
- **DNS & routage** : paramètres exposés à l’utilisateur en cohérence avec les capacités du moteur et d’Android.

## Flux réel (Phase 4)

1. L’utilisateur importe un lien, un fichier, le presse-papiers ou un JSON Xray ; `ConfigImporter` convertit/normalise, valide la structure puis la configuration complète via `testXray`. Le profil validé est chiffré (`SecretCipher`) et enregistré dans Room ; le premier profil importé devient le profil actif (persisté dans DataStore).
2. L’utilisateur sélectionne un profil (persisté) et demande la connexion ; l’UI publie `AwaitingPermission`, obtient le consentement `VpnService.prepare` si nécessaire puis démarre le service au premier plan.
3. Le service publie `Preparing`, résout le profil sélectionné (refus si incomplet), valide à nouveau la configuration, établit le TUN, publie `Connecting`, enregistre le protector de sockets, configure `setDNS`, construit la configuration complète et appelle `runXray`.
4. Le service vérifie `getXrayState`, publie `Verifying`, puis effectue une requête HTTP réelle à travers le tunnel ; seulement alors il publie `Connected` et démarre l’échantillonnage `TrafficStats`.
5. Une sonde périodique (20 s) revalide le tunnel. En cas d’échec : `Reconnecting(attempt)`, sondes de reprise toutes les 5 s pendant 90 s maximum ; retour à `Connected` seulement après une sonde réussie, sinon `Error` et arrêt propre.
6. À l’arrêt (bouton, notification, révocation ou destruction), le service arrête le moteur, restaure le DNS, ferme le TUN, publie `Idle`/`Error` et conserve les journaux assainis sur disque.

## Décisions prises (Phases 1–4)

- Import : liens `vless://`/`vmess://`/`trojan://`/`ss://`/`socks://`, outbound unique ou configuration Xray JSON, presse-papiers, fichier ; validation obligatoire par `testXray`.
- Intégration libXray : AAR officiel épinglé, résolu par réflexion (`libXray.LibXray#invoke`), un seul runtime Go.
- Statistiques : `android.net.TrafficStats` de l’UID applicatif, présentées comme telles (« pas un compteur noyau du TUN »).
- Modèle Room v2 et migration `1 → 2` testée ; données sensibles chiffrées, sélection en DataStore.
- Architecture UI : Compose Material 3, Navigation Compose ; la refonte visuelle complète reste en Phase 5.

## Points encore ouverts

- Exécution sur appareil physique ARM64 (protocole fourni, non exécuté).
- Démonstration d’un changement d’IP publique (non démontrable en CI).
- Rotation de clé du Keystore : la perte de clé rend les profils existants illisibles ; ils sont alors marqués « incomplets » et doivent être réimportés (comportement assumé et affiché).

Ces points doivent être tracés dans [`DECISIONS.md`](DECISIONS.md) au fil de l’avancement.

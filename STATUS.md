# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 3 — Connexion VPN réelle (`VpnService` ↔ libXray ↔ Xray-core) — **terminée et validée en CI (émulateur x86_64)**
- **État :** le tunnel réel fonctionne de bout en bout en CI. Le run [#44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595) (commit `5f51831`) est **vert : 25/25 étapes**, 21/21 tests instrumentés, VLESS et VMess avec sonde HTTP 200 réelle, DNS `onfaa.test` → `192.0.2.10` via le tunnel, journal d’accès du serveur Xray officiel. Deux limites restent **non revendiquées** : appareil physique ARM64 et changement d’IP publique.
- **Phase 2B :** terminée et validée (AAR officiel, API native, 6/6 tests instrumentés) ; preuves conservées ci-dessous.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Commit Phase 3 (code) :** `5f518313082fc095eeb118553b6fd969318cddfd`
- **Run CI Phase 3 réussi :** [libXray Android build and real VPN E2E #44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595) — `success`, 25/25 étapes
- **Dernier commit Phase 2B validé :** `f116442021b4e80508e3ef2448d21980f2207534`
- **Run Phase 2B réussi :** [#43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249)

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
- Profils conservés **en mémoire uniquement** pour cette phase (pas d’écriture en clair sur disque) ; persistance chiffrée différée et documentée comme limite.

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

- **Appareil physique ARM64 : non disponible.** L’exécution native du moteur est prouvée sur x86_64 émulé ; les binaires ARM sont présents et structurellement vérifiés mais pas exécutés sur téléphone physique. Protocole de test physique fourni dans `docs/PHASE_3_REPORT.md`, statut **en attente**.
- **Changement d’IP publique : non démontrable en CI** (le runner et l’émulateur partagent la même sortie réseau). Protocole manuel documenté ; aucune affirmation d’IP publique changée n’est faite.
- Profils en mémoire uniquement (pas de persistance chiffrée).
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

1. Exécuter le protocole ARM64 sur un appareil physique dès qu’il est disponible (protocole `docs/PHASE_3_REPORT.md` §11) ; ne pas revendiquer avant.
2. Faire valider en CI la compilation des changements UI Phase 5 (thèmes, navigation, sous-écrans, journaux) avant toute revendication de livraison.
3. Passer à la persistance chiffrée des profils (phase suivante) sans régresser l’intégration réelle validée.

## Instructions de reprise pour une autre IA

1. Lire `STATUS.md`, `docs/PHASE_3_REPORT.md`, `docs/LIBXRAY_API_REFERENCE.md`, `docs/LIBXRAY_ANDROID_POC.md`, `docs/DECISIONS.md`.
2. Ne jamais remplacer Xray-core/libXray par un mock, un fork ou sing-box ; ne jamais publier « Connecté » sans moteur actif et sonde réelle réussie.
3. Toute modification de l’intégration doit repasser le workflow `libxray-poc.yml` et conserver la traçabilité des commits/run/artefacts.

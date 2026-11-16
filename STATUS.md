# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 3 — Connexion VPN réelle (`VpnService` ↔ libXray ↔ Xray-core) — **implémentation livrée, validation CI en cours**
- **État :** l’intégration réelle est codée et poussée sur `main` (`94ea8ba`). Le workflow Phase 3 construit l’AAR officiel, démarre un serveur **Xray-core officiel** épinglé sur le runner, exécute les tests instrumentés E2E (VLESS, VMess, DNS, routage, arrêt propre) puis vérifie les preuves. **Aucun résultat E2E ne doit être annoncé avant la réussite de ce run.**
- **Phase 2B :** terminée et validée (AAR officiel, API native, 6/6 tests instrumentés) ; preuves conservées ci-dessous.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Commit Phase 3 (code) :** `94ea8ba745467490b3e4f9c831392806258bafe7`
- **Run CI Phase 3 :** [libXray Android build and real VPN E2E #43978101438](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43978101438)
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

- `HomeScreen` : connexion/déconnexion réelles, demande de consentement VPN, état réel, erreurs réelles.
- `ConfigsScreen` : import réel, validation par le moteur, sélection/suppression, note « mémoire uniquement ».
- `LogsScreen` : événements réels du cycle de vie, assainis.
- `StatsScreen` : informations de connexion réelles, **aucun compteur simulé**.

## Vérifications Phase 3 (à confirmer par le run CI)

Le workflow Phase 3 doit prouver, sur émulateur x86_64 API 35 :

1. **Échec direct avant connexion** : requête vers `onfaa.test`/`192.0.2.10:18080` impossible.
2. **Tunnel VLESS réel** : `runXray` actif, sonde HTTP 200 à travers le tunnel.
3. **Tunnel VMess réel** : idem avec le second inbound.
4. **DNS réel** : `onfaa.test` résolu en `192.0.2.10` via le DNS du tunnel.
5. **Routage réel** : requête par nom et par IP vers une adresse TEST-NET uniquement joignable derrière le serveur Xray local.
6. **Arrêt propre** : état `Idle`, `getXrayState=false`, trafic direct de nouveau en échec.
7. **Serveur injoignable** : jamais d’état `Connected` ; erreur réelle publiée.
8. **Permission VPN** : consentement requis, connexion refusée proprement sans consentement.

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
- Compteurs de trafic non collectés (aucune statistique simulée).
- Le run CI complet dépend de GitHub Actions (pas de JDK/Go/Android SDK local sur le téléphone).

## Blocages / risques

- Validation finale conditionnée à la réussite du run `43978101438` (build AAR + serveur Xray + émulateur).
- Si l’émulateur ne peut pas joindre `10.0.2.2`, le test E2E échouera explicitement (aucun succès simulé).
- Un seul runtime Go par processus : ne pas ajouter d’autre bibliothèque Go.

## Prochaine tâche

1. Suivre le run `43978101438`, corriger toute erreur de compilation/test, puis consigner les preuves réelles (statut, journal d’accès Xray, résultats instrumentés, APK).
2. Exécuter le protocole ARM64 sur un appareil physique dès qu’il est disponible.
3. Passer à la persistance chiffrée des profils (phase suivante) sans régresser l’intégration réelle.

## Instructions de reprise pour une autre IA

1. Lire `STATUS.md`, `docs/PHASE_3_REPORT.md`, `docs/LIBXRAY_API_REFERENCE.md`, `docs/LIBXRAY_ANDROID_POC.md`, `docs/DECISIONS.md`.
2. Ne jamais remplacer Xray-core/libXray par un mock, un fork ou sing-box ; ne jamais publier « Connecté » sans moteur actif et sonde réelle réussie.
3. Toute modification de l’intégration doit repasser le workflow `libxray-poc.yml` et conserver la traçabilité des commits/run/artefacts.

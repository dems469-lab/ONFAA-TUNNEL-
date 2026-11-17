# Rapport Phase 3 — Tunnel VPN réel (VpnService ↔ libXray ↔ Xray-core)

> **Statut de validation : validé en CI (émulateur x86_64).** Le run
> [#44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595)
> (commit `5f51831`) est entièrement vert : AAR officiel + serveur Xray-core officiel +
> 21/21 tests instrumentés + E2E VLESS et VMess avec sonde HTTP réelle, DNS et journal
> d’accès. Les résultats de la §9 proviennent uniquement des artefacts de ce run.
> Deux limites restent ouvertes et ne sont pas revendiquées : appareil physique ARM64
> (§11) et changement d’IP publique (§12).

## 1. Objectif

Faire fonctionner un vrai tunnel Android : `VpnService` fournit un descripteur TUN,
libXray (AAR officiel construit depuis les sources XTLS) pilote Xray-core, et le trafic
réel de l’application traverse un serveur Xray-core officiel. Protocoles ciblés : VLESS
et VMess. Aucun mock, aucun fork, aucun sing-box, aucun compteur simulé.

## 2. Versions épinglées

| Composant | Version | Commit |
| --- | --- | --- |
| Xray-core | `v26.11.9` | `676eaf66f8f05d4d558ee7a74b4736623d3feeb9` |
| libXray | `v26.11.9` | `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3` |
| Go (CI) | `1.26.1` | — |
| gomobile (CI) | `v0.0.0-20260217195705-b56b3793a9c4` | — |
| NDK (CI) | r30 `30.0.16248370` | — |
| JDK (CI) | 17 | — |

L’AAR n’est pas téléchargé : il est construit depuis le commit libXray épinglé par
`.github/workflows/libxray-poc.yml`, puis vérifié (SHA-256, ABIs, ELF, alignement
16 KiB, signatures `javap`) avant intégration. L’AAR, les APK et les rapports restent
des artefacts CI non commités.

## 3. Architecture réelle

```text
Applications Android
        │  (paquets IP)
        ▼
VpnService TUN (fd)  ──────────────►  Xray-core (libXray, même processus)
  adresses 10.10.0.2/24, fd00::2/64      inbound "tun" (lit env["xray.tun.fd"])
  routes 0.0.0.0/0, ::/0                 routage : port 53 → dns-out
  DNS 10.10.0.1, fd00::1                 privé → direct ; reste → proxy
        ▲                                      │
        │  paquets IP                          │ sockets protégés
        │                                      ▼
   sonde HTTP réelle                    serveur Xray-core officiel
   (non protégée, donc via le TUN)      VLESS/VMess → freedom
                                               │
                                               ▼
                                      fixture token 192.0.2.10:18080
                                      (alias loopback TEST-NET)
```

Le moteur tourne dans le processus de l’application. Les sockets du moteur sont
protégés par `VpnService.protect(fd)` via un vrai `DialerController`, afin d’éviter
la boucle de routage. La sonde de connectivité, elle, n’est **pas** protégée : elle
est donc réellement routée dans le TUN, ce qui prouve le tunnel.

## 4. Contrat TUN réel (Xray-core v26.11.9)

Vérifié dans les sources upstream :

- inbound `tun` ; le descripteur est lu depuis la variable d’environnement
  `xray.tun.fd` (`common/platform.TunFdKey`), elle-même alimentée par la clé racine
  `env` de la configuration Xray ;
- libXray v26.11.9 n’expose plus `SetTunFd` ; `NewTun` duplit le descripteur fourni
  et ne ferme pas celui de l’application ;
- `testXray` construit les handlers sans démarrer la lecture du TUN ; `runXray`
  appelle `Start()` ;
- Android : `VpnService.Builder` possède les adresses, routes et DNS ; Xray ne fait
  que lire/écrire les paquets.

## 5. API libXray réellement utilisées

Signatures vérifiées dans l’AAR officiel (aucune supposition) :

```text
libXray.LibXray#invoke(String): String
libXray.LibXray#registerDialerController(libXray.DialerController): void
libXray.LibXray#registerListenerController(libXray.DialerController): void
libXray.LibXray#setDNS(libXray.DialerController, String): void
libXray.LibXray#resetDNS(): void
libXray.DialerController#protectFd(long): boolean
```

Méthodes `invoke` utilisées : `convertShareLinksToXrayJson`, `testXray`, `runXray`,
`stopXray`, `getXrayState`, `xrayVersion`. Le pont Java/Kotlin résout la classe par
réflexion : le build normal compile sans AAR, et l’absence de l’AAR produit une erreur
explicite (« Moteur libXray indisponible »), jamais un faux succès.

## 6. Pipeline de connexion

1. Import : lien `vless://` ou `vmess://` → `convertShareLinksToXrayJson` → un seul
   outbound réel → profil (nom, protocole, endpoint, sécurité) ;
2. Construction d’une configuration TUN complète :
   - `env["xray.tun.fd"]` ;
   - inbound `tun` (MTU 1500) ;
   - outbound converti (tag `proxy`) ;
   - outbound `dns` (tag `dns-out`) et `freedom` (tag `direct`) ;
   - DNS avec `queryStrategy: UseIP` ;
   - routage : port 53 → `dns-out` ; tag DNS → direct ; réseaux privés → direct ;
3. `testXray` (descripteur inerte) → validation réelle par le moteur ;
4. `VpnService.Builder.establish()` → descripteur TUN réel ;
5. `registerDialerController`/`registerListenerController` → `protect(fd)` ;
6. `setDNS(protector, résolveur)` → résolveur Go de libXray ;
7. `runXray` avec le descripteur réel ;
8. `getXrayState` doit être vrai ;
9. sonde HTTP réelle à travers le TUN ; `Connected` seulement si la sonde réussit ;
10. arrêt : `stopXray`, `resetDNS`, neutralisation du protector, fermeture du TUN,
    `stopForeground`, `stopSelf`.

## 7. Fichiers modifiés (Phase 3)

- `app/src/main/java/com/onfaa/tunnel/core/xray/XrayBridge.kt`
- `app/src/main/java/com/onfaa/tunnel/core/xray/XrayConfigBuilder.kt`
- `app/src/main/java/com/onfaa/tunnel/core/xray/XrayErrorSanitizer.kt`
- `app/src/main/java/com/onfaa/tunnel/core/xray/ConfigImporter.kt`
- `app/src/main/java/com/onfaa/tunnel/vpn/OnfaaTunnelVpnService.kt`
- `app/src/main/java/com/onfaa/tunnel/vpn/ConnectivityProbe.kt`
- `app/src/main/java/com/onfaa/tunnel/vpn/TunnelStatus.kt`
- `app/src/main/java/com/onfaa/tunnel/data/config/TunnelConfigRepository.kt`
- `app/src/main/java/com/onfaa/tunnel/domain/model/TunnelProfile.kt`
- `app/src/main/java/com/onfaa/tunnel/feature/home/HomeScreen.kt`
- `app/src/main/java/com/onfaa/tunnel/feature/configs/ConfigsScreen.kt`
- `app/src/main/java/com/onfaa/tunnel/feature/logs/LogsScreen.kt`
- `app/src/main/java/com/onfaa/tunnel/feature/stats/StatsScreen.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/debug/res/xml/network_security_config.xml`
- `app/build.gradle.kts` (version `3` / `0.3.0-phase3`)
- tests unitaires et instrumentés (voir §8)
- `.github/workflows/libxray-poc.yml`, `.github/workflows/android.yml`
- `scripts/e2e/onfaa_e2e_services.py`
- documentation : `STATUS.md`, `CHANGELOG.md`, `docs/ROADMAP.md`,
  `docs/ARCHITECTURE.md`, `docs/TEST_PLAN.md`, `docs/BUILD_GUIDE.md`,
  `docs/DECISIONS.md`, ce rapport.

## 8. Tests exécutés

### Unitaires (JVM, sans AAR)

- `XrayErrorSanitizerTest` : masquage UUID / liens / secrets base64, troncature,
  messages vides ; un secret base64 long est masqué (pas affiché).
- `ConfigImporterValidationTest` : lien vide, schéma non supporté, VMess base64
  historique refusé avec message précis.
- `ServerConfigTest` : modèle existant.

### Instrumentés avec l’AAR officiel (x86_64, API 35)

- `XrayBridgeInstrumentedTest` : chargement de `libgojni.so`, `xrayVersion`,
  réponses d’erreur structurées, `testXray` refusant une configuration invalide.
- `XrayConfigInstrumentedTest` : conversion VLESS/VMess réelle, conversion d’un lien
  VLESS **public avec TLS**, configuration TUN
  acceptée par le moteur, descripteur invalide refusé proprement, `stopXray`
  idempotent, contrôleurs de protection et DNS appelables.
- `VpnPermissionInstrumentedTest` : consentement requis (`VpnService.prepare`),
  connexion sans consentement refusée avec message, moteur resté arrêté.
- `VpnTunnelE2ETest` : quatre cas réels — trois scénarios de tunnel et un contrôle
  de joignabilité de la fixture (`emulatorReachesHostFixtureThroughHostAlias`).

### E2E réel

Fixture déterministe sur le runner :

- serveur **Xray-core officiel** construit depuis le commit épinglé ;
- inbounds VLESS `127.0.0.1:18443` et VMess `127.0.0.1:18444`, sortie `freedom`
  (la cible de `redirect` `127.0.0.1:18080` est autorisée par un `finalRules`
  `allow` explicite : Xray 26.x bloque par défaut les destinations privées pour
  ces inbounds) ;
- token HTTP `192.0.2.10:18080` (adresse TEST-NET ajoutée en alias loopback) ;
- DNS `onfaa.test` → `192.0.2.10`, servi en UDP **et** TCP ;
- émulateur joint l’hôte via `10.0.2.2` ; le tunnel utilise le DNS-over-TCP
  `tcp://10.0.2.2:15353` (le forwarding UDP de l’alias hôte n’est pas garanti,
  TCP l’est) ;
- preuve de routage : `192.0.2.10` n’est pas joignable directement depuis
  l’émulateur ; seul le serveur Xray local le joint.

Scénarios :

1. VLESS : échec direct avant connexion, connexion, `Connected` après sonde,
   résolution DNS `onfaa.test` → `192.0.2.10`, HTTP 200 + token par nom et par IP,
   déconnexion, `Idle`, moteur arrêté, trafic direct de nouveau en échec.
2. VMess : mêmes assertions avec l’inbound VMess.
3. Serveur injoignable (`10.0.2.2:9`) : jamais `Connected`, erreur réelle, moteur
   arrêté.

## 9. Résultats (run final validé)

- **Run :** [libXray Android build and real VPN E2E #44004893595](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44004893595)
  — `success`, **25/25 étapes vertes** (2026-11-17 02:42:01Z → 02:49:55Z).
- **Commit validé :** `5f518313082fc095eeb118553b6fd969318cddfd` (`main`).
- **Artefact :** `libxray-poc-report` (122 253 621 octets) avec `libXray.aar`,
  `app-debug.apk` (155 889 248 octets, `libgojni.so` pour les 4 ABIs),
  `app-debug-androidTest.apk`, XML de tests, `logcat` par test, `server.json`,
  `xray-server.log`, `access.log`, `services.log`, `verify.log`.

| Élément | Résultat |
| --- | --- |
| Build AAR officiel + inspection | **OK** — `libXray.aar` 53 200 280 octets, SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`, ABIs `arm64-v8a/armeabi-v7a/x86/x86_64`, ELF conformes, tous les segments `LOAD` alignés 16 KiB |
| Build serveur Xray-core officiel | **OK** — `Xray 26.11.9 (Xray, Penetrates Everything.) 676eaf6 (go1.27.0 linux/amd64)` |
| Tests unitaires | **11/11**, 0 échec (`ConfigImporterValidationTest` 3, `XrayErrorSanitizerTest` 6, `ServerConfigTest` 2) |
| Tests instrumentés réels | **21/21**, 0 échec, 0 erreur, 0 ignoré (48,867 s, émulateur x86_64 API 35) |
| E2E VLESS | **OK** — `Connected` seulement après sonde HTTP 200 `onfaa-e2e-token-phase3` (1ʳᵉ tentative) |
| E2E VMess | **OK** — idem avec l’inbound VMess |
| DNS `onfaa.test` → `192.0.2.10` | **OK** — détour `dns-out`, puis `TCPL//10.0.2.2:15353` → `192.0.2.10` |
| Journal d’accès Xray `192.0.2.10:18080` | **OK** — 3 lignes `vless-in -> token-redirect` et 3 lignes `vmess-in -> token-redirect` |
| APK debug | **OK** — `app-debug.apk` publié, `libgojni.so` présent pour les 4 ABIs |
| Arrêt propre | **OK** — `Idle`, `getXrayState=false`, trafic direct de nouveau en échec |
| Serveur injoignable | **OK** — 6/6 sondes en échec, jamais `Connected` |
| Portes de preuve CI | `E2E_XML_VALIDATED`, `ACCESS_LOG_PROVES_TUNNEL_DESTINATION`, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3`, aucune occurrence de `SERVER_BLOCKED_TUNNEL_TARGET` |

### 9.1 Preuves brutes

Sonde réelle et routage (VLESS, `logcat` du test) :

```text
I OnfaaTunnel: Engine mode: VLESS -> 10.0.2.2:18443 (log=info)
I OnfaaTunnel: TUN established
I GoLog: proxy/tun: read Android Tun Fd 130 / onfaa0 created / onfaa0 up
I GoLog: [Warning] core: Xray 26.11.9 started
I OnfaaTunnel: VPN network is the active network
I GoLog: from tcp:10.10.0.2:60764 accepted tcp:192.0.2.10:18080 [tun-in >> proxy]
I OnfaaTunnel: VpnService.protect granted for an engine socket
I GoLog: proxy/vless/outbound: tunneling request to tcp:192.0.2.10:18080 via 10.0.2.2:18443
I OnfaaTunnel: Connectivity probe attempt 1/6: ok=true detail=HTTP 200 — onfaa-e2e-token-phase3
I GoLog: from udp:10.10.0.2:6117 accepted udp:10.10.0.1:53 [tun-in -> dns-out]
I GoLog: app/dns: TCPL//10.0.2.2:15353 querying DNS for: onfaa.test.
I GoLog: app/dns: TCPL//10.0.2.2:15353 got answer: onfaa.test. TypeA -> [192.0.2.10]
```

Même chaîne en VMess (`tun-in >> proxy` puis `proxy/vmess/outbound: tunneling
request … via 10.0.2.2:18444`, sonde 1/6 `ok=true`).

Journal d’accès du serveur Xray officiel (destinations réellement proxifiées) :

```text
02:48:37 accepted tcp:192.0.2.10:18080 [vless-in -> token-redirect]
02:48:40 accepted tcp:192.0.2.10:18080 [vless-in -> token-redirect]
02:48:40 accepted tcp:192.0.2.10:18080 [vless-in -> token-redirect]
02:49:12 accepted tcp:192.0.2.10:18080 [vmess-in -> token-redirect]
02:49:15 accepted tcp:192.0.2.10:18080 [vmess-in -> token-redirect]
02:49:15 accepted tcp:192.0.2.10:18080 [vmess-in -> token-redirect]
```

Répondeur de fixture (le trafic tunnelé a bien atteint le token, et le DNS du
tunnel a bien été résolu par le répondeur) :

```text
02:48:37 HTTP 200 /token from 127.0.0.1:42672
02:48:38 DNS/TCP A onfaa.test -> 192.0.2.10 from 127.0.0.1:49516
02:48:40 HTTP 200 /token from 127.0.0.1:60182
02:48:40 HTTP 200 /token from 127.0.0.1:60194
02:49:12 HTTP 200 /token from 127.0.0.1:40898
02:49:13 DNS/TCP A onfaa.test -> 192.0.2.10 from 127.0.0.1:38244
02:49:15 HTTP 200 /token from 127.0.0.1:40910
02:49:15 HTTP 200 /token from 127.0.0.1:40920
```

Le `logcat` montre aussi l’avertissement bénin `app/dns: … TypeAAAA -> []`
(réponse AAAA vide du répondeur de test, résolution A réussie) : il ne bloque
aucune requête.

### 9.2 Défauts réels trouvés et corrigés pendant la validation

Ces trois défauts ont été diagnostiqués sur des runs réels et corrigés à la
cause racine, jamais contournés par un mock ou un test désactivé.

1. **Contrôleur de protection de sockets enregistré deux fois** (run
   `43995354053`) : chaque dial du moteur échouait avec
   `failed to apply external controller > protect Android VPN socket failed`
   alors que le callback de l’application n’était jamais appelé, et le serveur ne
   voyait aucune connexion. Cause : `transport/internet.RegisterDialerController`
   **empile** le contrôleur dans `internet.Controllers` et n’expose aucune
   désinscription ; `clearSocketProtector()` ajoutait un contrôleur `{ false }`
   qui, une fois installé par un test de contrôleur, rejetait tous les dials du
   processus (VLESS et VMess identiquement), y compris en production après une
   reconnexion. Correctif : contrôleur unique enregistré une seule fois par
   processus derrière un délégué `AtomicReference` remplacé par le service ;
   `clearSocketProtector()` ne fait que vider le délégué.
2. **Cible de redirection bloquée par le serveur** (run `43999172035`) : le
   tunnel fonctionnait (`tun-in >> proxy`, journal d’accès `vless-in ->
   token-redirect`) mais la sonde expirait. Cause :
   `[Warning] proxy/freedom: target blocked by a finalrule:
   tcp:127.0.0.1:18080, blackholing connection for 54s` — Xray 26.x applique
   `defaultBlockPrivateRule` aux inbounds VLESS/VMess, et la cible de
   `redirect` (`127.0.0.1`) est privée. Correctif : `finalRules` explicite
   `allow tcp 127.0.0.1:$E2E_HTTP_PORT` sur l’outbound `token-redirect`, plus une
   porte de preuve qui échoue si le serveur journalise un blackhole et qui exige
   au moins une ligne d’accès par protocole.
3. **Fixture VLESS publique sans TLS** (run `43995354053`, 3 échecs de
   `XrayConfigInstrumentedTest`) : Xray refuse un outbound VLESS sans
   TLS/chiffrement vers une adresse publique (`requiresTransportSecurity`), d’où
   `no valid outbound found`. Ce n’était pas un défaut de l’application : la
   fixture utilisait `192.0.2.1` avec `security=none`. Correctif : fixture
   VLESS clair sur adresse LAN (`192.168.1.10`) et nouveau test de conversion
   d’un lien VLESS **public avec TLS** (`convertsPublicVlessTlsShareLinkWithEngine`).

Un run antérieur (`43978101438`) avait échoué **avant les tests** (course au
démarrage du serveur token) ; le workflow attend désormais explicitement les
ports Xray, le token HTTP et une requête DNS/TCP réelle avant de continuer.

## 10. Limites honnêtes

- **Appareil physique ARM64 non disponible** : le moteur natif est prouvé sur
  x86_64 émulé ; les binaires ARM sont présents et structurellement vérifiés, mais
  pas exécutés sur un téléphone physique. Voir le protocole §11.
- **Changement d’IP publique non démontrable en CI** : le runner et l’émulateur
  partagent la même sortie réseau. Aucune affirmation d’IP publique changée n’est
  faite. Voir le protocole §12.
- **Profils en mémoire uniquement** : pas de persistance en clair ; la persistance
  chiffrée est reportée à la phase suivante.
- **VMess base64 historique refusé** : libXray v26.11.9 ne le convertit pas ; seul
  le format URL `vmess://UUID@hôte:port?...` est accepté, avec message explicite.
- **Statistiques de trafic** : la Phase 3 n’en affichait aucune. Depuis la Phase 5,
  l’accueil affiche des valeurs réelles issues de `android.net.TrafficStats`
  (UID applicatif, sockets du moteur inclus), jamais un compteur simulé ; aucune
  métrique noyau du TUN n’est exposée.
- **HTTP en clair uniquement en debug** : la fixture CI est en clair ; le build
  release garde `cleartextTrafficPermitted=false`.
- **Dépendance CI** : pas de JDK/Go/SDK Android local sur le téléphone ; la
  validation passe par GitHub Actions.

## 11. Protocole de validation sur appareil physique ARM64

À exécuter quand un appareil est disponible :

1. Installer l’APK debug du run CI (`onfaa-tunnel-debug-apk`, ABI `arm64-v8a`).
2. Vérifier `Build.SUPPORTED_ABIS` et que `libgojni.so` est chargé (les tests
   instrumentés réels peuvent être lancés sur l’appareil).
3. Importer un lien VLESS personnel valide, puis un lien VMess URL valide.
4. Se connecter : l’état doit passer par `Connecting`/`Verifying` puis `Connected`
   seulement après la sonde réelle.
5. Vérifier la résolution DNS et l’accès HTTP/HTTPS à travers le tunnel.
6. Comparer l’IP publique avant/après avec un service de confiance (§12).
7. Se déconnecter : état `Idle`, `getXrayState=false`, trafic direct rétabli.
8. Refaire le scénario avec un serveur injoignable : erreur, jamais `Connected`.
9. Consigner : modèle, version Android, ABI, commit, hash APK, captures, logs
   assainis (aucun UUID, lien ou secret).

## 12. Protocole de vérification du changement d’IP publique (manuel)

1. Hors tunnel, relever l’IP publique via deux services indépendants.
2. Activer le tunnel avec un serveur Xray dont la sortie est connue.
3. Relever à nouveau l’IP publique via les mêmes services.
4. Vérifier que l’IP observée correspond à la sortie du serveur (et non à celle du
   fournisseur d’accès mobile).
5. Se déconnecter et vérifier le retour à l’IP d’origine.
6. Consigner les résultats ; ne pas publier d’identifiants de serveur.

En CI, ce protocole n’est pas exécutable de façon probante (même sortie réseau) ;
il reste donc manuel et n’est pas revendiqué.

## 13. Reproductibilité

- Workflow : `.github/workflows/libxray-poc.yml` (push sur `main` pour les chemins
  concernés, ou `workflow_dispatch`).
- Run de référence validé : `44004893595`, commit `5f51831`, `success` (25/25
  étapes), artefact `libxray-poc-report`.
- Build local : voir `docs/BUILD_GUIDE.md` (AAR + serveur officiels construits
  depuis les commits épinglés, jamais téléchargés non vérifiés).
- Tests E2E : arguments d’instrumentation documentés dans `docs/BUILD_GUIDE.md`.

## 14. Sécurité

- Aucun secret réel, keystore, token utilisateur ou UUID de production dans le
  dépôt ; les valeurs E2E sont des constantes de test explicites.
- Les erreurs affichées/journalisées passent par `XrayErrorSanitizer` (UUID, liens,
  secrets base64 masqués) ; aucune configuration complète n’est journalisée.
- Le protector est neutralisé à l’arrêt ; le résolveur Go est restauré.
- La configuration release interdit le trafic en clair ; seule la variante debug
  autorise la fixture CI.

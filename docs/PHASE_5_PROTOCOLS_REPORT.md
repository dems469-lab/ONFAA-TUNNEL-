# Phase 5 — Lot 1 : protocoles Trojan, Shadowsocks et SOCKS5

**Statut : livré, validé en CI (émulateur x86_64 API 35).**
Commit de code : `5e2f0d4`. Plan de parité : [`PHASE_5_PARITY_PLAN.md`](PHASE_5_PARITY_PLAN.md) (lot 1 = L1).

## 1. Objectif

Étendre l’import et l’exécution aux protocoles proposés par les clients grand public
inspirés de SNI Tunnel, **sans remplacer le moteur** : le cœur reste
XTLS/Xray-core v26.11.9 (`676eaf66…`) via libXray v26.11.9 (`5a0f7d8d…`).
Aucun protocole n’est revendiqué sans preuve réelle de tunnel et journal d’accès.

## 2. Périmètre livré

| Protocole | Lien | JSON Xray | Preuve réelle |
|---|---|---|---|
| VLESS | `vless://` | oui | E2E existant (Phase 3/4) |
| VMess | `vmess://` | oui | E2E existant (Phase 3/4) |
| Trojan | `trojan://` | oui | **nouvel E2E** (TLS, cert auto-signé épinglé) |
| Shadowsocks | `ss://` (SIP002 base64) | oui (`settings.servers[0]` accepté) | **nouvel E2E** (AEAD `aes-256-gcm`) |
| SOCKS5 | `socks://` (userinfo base64) | oui (forme plate `user`/`pass`) | conversion + `testXray` réels, pas de tunnel E2E (voir §7) |

Hors périmètre (inchangé) : Hysteria 2, SSH, partage cloud, thèmes, édition manuelle
complète — voir le plan de parité.

## 3. Modifications

- `domain/model/ServerConfig.kt` : `TunnelProtocol` = `VLESS`, `VMESS`, `TROJAN`,
  `SHADOWSOCKS`, `SOCKS`, avec libellé d’affichage (`VMess`, `Shadowsocks`, `SOCKS5`).
  La base Room stocke le nom de l’énumération : **aucune migration** n’est nécessaire,
  les profils existants restent lisibles.
- `core/xray/ConfigImporter.kt` :
  - schémas de liens acceptés : `vless`, `vmess`, `trojan`, `ss`, `socks` ;
  - sorties JSON acceptées : `vless`, `vmess`, `trojan`, `shadowsocks`, `socks` ;
  - validation adaptée au protocole (UUID pour VLESS/VMess, mot de passe Trojan,
    méthode + mot de passe Shadowsocks, aucun identifiant requis pour SOCKS5) ;
  - `settings.servers[0]` aplati pour les JSON de style v2ray (une seule entrée
    acceptée, sinon message explicite) ;
  - messages d’erreur mis à jour et toujours assainis avant affichage.
- `core/xray/XrayConfigBuilder.kt` : jeu de protocoles supportés explicite ; la
  configuration TUN, le DNS, le routage et le chiffrement au repos sont inchangés.
- UI : libellés de protocole dans Configs/Accueil/Stats, textes d’aide et libellé du
  champ d’import (« Lien ou JSON Xray »).
- Tests : 4 nouveaux cas unitaires d’import (Trojan sans mot de passe, Shadowsocks
  sans méthode, Shadowsocks sans mot de passe, plusieurs `servers`), 5 tests
  instrumentés de conversion/validation réelle (`ProtocolImportInstrumentedTest`),
  2 scénarios E2E de tunnel (Trojan, Shadowsocks).

## 4. Preuves CI (réelles)

### 4.1 Android CI — [#44019183046](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183046)

- `success`, 1 min 15 s, commit `5e2f0d4`.
- `:app:testDebugUnitTest` vert : **27 tests unitaires** (23 avant ce lot).
- APK debug + APK de tests instrumentés compilés ; artefact
  `onfaa-tunnel-debug-apk` (id `14300007325`).

### 4.2 E2E Xray-core officiel — [#44019183061](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183061)

- `success`, 30 étapes, 0 échec, 7 min 16 s, commit `5e2f0d4`.
- **34/34 tests instrumentés** (0 échec, 0 erreur, 0 ignoré, 61,351 s).
- `VpnTunnelE2ETest` : **6/6** (`emulatorReachesHostFixtureThroughHostAlias`,
  VLESS, VMess, **Trojan**, **Shadowsocks**, serveur injoignable), vérifié par le
  garde-fou `--min-tests 6`.
- Portes de preuve :

```
E2E_XML_VALIDATED
ACCESS_LOG_PROVES_TUNNEL_DESTINATION
ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3 trojan=3 shadowsocks=9
```

- Aucun `SERVER_BLOCKED_TUNNEL_TARGET` ; artefact `libxray-poc-report`
  (id `14299544855`).

Chaque scénario de tunnel prouve, pour son protocole : échec du trafic direct avant
connexion, `Connected` seulement après sonde HTTP réelle, résolution `onfaa.test` →
`192.0.2.10` via le DNS du tunnel, requête HTTP 200 + token par IP et par nom vers une
adresse TEST-NET uniquement joignable derrière le serveur Xray local, arrêt propre
(`Idle`, `getXrayState=false`), retour à l’échec direct.

## 5. Fixture E2E

| Inbound | Port hôte | Configuration |
|---|---|---|
| `vless-in` | 18443 | VLESS sans TLS (adresse LAN privée) |
| `vmess-in` | 18444 | VMess, UUID de test |
| `trojan-in` | 18445 | Trojan **TLS** avec certificat auto-signé `CN=onfaa.test` |
| `ss-in` | 18446 | Shadowsocks `aes-256-gcm`, mot de passe de test |

Le convertisseur libXray force TLS pour `trojan://` (même si le lien demande
`security=none`) et Xray 26 a supprimé `allowInsecure`. Le certificat auto-signé est
donc épinglé par son SHA-256 hexadécimal via le paramètre `pcs` du lien, mécanisme
officiel équivalent (`pinnedPeerCertSha256`). Les secrets de la fixture sont des
identifiants de test dédiés, jamais des secrets utilisateur.

## 6. Conversion vérifiée dans libXray

- `share/parse_share.go` : schémas `socks`, `ss`, `trojan` (+ `hysteria2`/`hy2`, que
  Xray-core ne sait pas exécuter et qui restent refusés par l’application).
- `ss://` : userinfo `method:password` en base64 (SIP002) ou en clair.
- `socks://` : userinfo base64 `utilisateur:motdepasse` (optionnel).
- `share/stream.go` : TLS/REALITY/pinning (`pcs`, `vcn`, `sni`, `fp`, `alpn`) ;
  `allowInsecure` n’est pas exposé.
- Sortie : `{"outbounds":[ … ]}` avec une seule sortie aplatie.

## 7. Limites assumées

- **SOCKS5** : conversion et validation moteur réelles prouvées, mais **aucun tunnel
  E2E** (la fixture n’embarque pas de serveur SOCKS5). Non revendiqué comme
  « tunnel SOCKS5 prouvé ».
- **Trojan** : preuve avec certificat auto-signé épinglé ; chaîne de certification
  publique non testée (dépend du serveur de l’utilisateur).
- **Shadowsocks** : preuve AEAD `aes-256-gcm` uniquement ; les méthodes 2022 ne sont
  pas revendiquées (le moteur les accepte si le lien les décrit).
- Les liens Hysteria 2 restent refusés : Xray-core ne les exécute pas.
- Limites antérieures inchangées : appareil physique ARM64 non exécuté, changement
  d’IP publique non démontrable en CI.

## 8. Prochaine étape

Lot 3 du plan (réseau/transport : WebSocket, gRPC, REALITY/utls, fragmentation) puis
lot 2 (QR, abonnements), lot 4 (automatisation), lot 5 (édition manuelle avancée).

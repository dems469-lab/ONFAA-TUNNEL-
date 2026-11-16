# Rapport Phase 3 — Tunnel VPN réel (VpnService ↔ libXray ↔ Xray-core)

> **Statut de validation : en cours.** Ce rapport décrit l’implémentation réelle et les
> preuves attendues. Aucun résultat E2E n’est déclaré réussi tant que le run CI complet
> (AAR officiel + serveur Xray-core officiel + émulateur) n’a pas produit ses artefacts.
> La section « Résultats » est mise à jour uniquement avec des preuves vérifiables.

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
- `XrayConfigInstrumentedTest` : conversion VLESS/VMess réelle, configuration TUN
  acceptée par le moteur, descripteur invalide refusé proprement, `stopXray`
  idempotent, contrôleurs de protection et DNS appelables.
- `VpnPermissionInstrumentedTest` : consentement requis (`VpnService.prepare`),
  connexion sans consentement refusée avec message, moteur resté arrêté.
- `VpnTunnelE2ETest` : trois scénarios réels décrits ci-dessous.

### E2E réel

Fixture déterministe sur le runner :

- serveur **Xray-core officiel** construit depuis le commit épinglé ;
- inbounds VLESS `127.0.0.1:18443` et VMess `127.0.0.1:18444`, sortie `freedom` ;
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

## 9. Résultats

| Élément | Résultat |
| --- | --- |
| Build AAR officiel + inspection | à confirmer par le run final |
| Build serveur Xray-core officiel | à confirmer par le run final |
| Tests unitaires | à confirmer par le run final |
| Tests instrumentés réels | à confirmer par le run final |
| E2E VLESS | à confirmer par le run final |
| E2E VMess | à confirmer par le run final |
| DNS `onfaa.test` → `192.0.2.10` | à confirmer par le run final |
| Journal d’accès Xray `192.0.2.10:18080` | à confirmer par le run final |
| APK debug | à confirmer par le run final |
| Commit / run / artefacts | à compléter après le run |

Un run antérieur (`43978101438`) a échoué **avant les tests** : le serveur Xray
démarrait correctement, mais le serveur token Python n’était pas encore prêt au
moment du `curl` (course au démarrage). Le workflow attend désormais explicitement
les ports Xray, le token HTTP et une requête DNS/TCP réelle avant de continuer.
Ces corrections ne constituent pas une preuve E2E.

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
- **Aucune statistique de trafic** : aucune source réelle branchée, donc aucun
  compteur affiché.
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

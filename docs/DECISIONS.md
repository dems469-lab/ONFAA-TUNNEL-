# Décisions — ONFAA TUNNEL

Ce document consigne les décisions prises, les hypothèses de travail et les choix restant à confirmer. Il doit être mis à jour à chaque étape structurante.

## Décisions validées (Phase 0)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-16 | Dépôt GitHub `https://github.com/dems469-lab/ONFAA-TUNNEL-`, branche principale `main`, source centrale et permanente | Continuité multi-environnement et traçabilité |
| 2026-11-16 | Client **Android natif** en **Kotlin** | Alignement plateforme et écosystème |
| 2026-11-16 | Utilisation d’**Android `VpnService`** | Interface système requise pour un VPN Android |
| 2026-11-16 | Moteur **XTLS/Xray-core officiel** intégré via **libXray** | Exigence fonctionnelle du projet |
| 2026-11-16 | **Aucun remplacement par sing-box** ; **aucune simulation de connexion** | Intégrité technique et fonctionnelle |
| 2026-11-16 | Persistance cible : **Room** + **DataStore** ; asynchronisme : **Coroutines & Flow** | Architecture cible cohérente Android moderne |
| 2026-11-16 | Compilation cible **exclusivement via GitHub Actions** (mise en œuvre Phase 9) | Reproductibilité et centralisation |
| 2026-11-16 | Phase 0 = structure + documentation + préparation documentaire CI ; **pas** de workflow de build Android incomplet | Éviter les artefacts trompeurs |
| 2026-11-16 | Licence **MIT** ajoutée au dépôt (choix initial de projet, révisable si le propriétaire en décide autrement) | Clarifier les conditions de réutilisation dès l’initialisation |

## Décisions validées (Phase 1)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-16 | Package Android / `applicationId` : **`com.onfaa.tunnel`** (namespace identique) | Identité applicative cohérente et stable dès les fondations |
| 2026-11-16 | Module unique **`:app`** (pas de multi-modules Gradle en Phase 1) | Réduire la complexité initiale ; réévaluation possible si le projet grandit |
| 2026-11-16 | UI native avec **Jetpack Compose (Material 3)** et **Navigation Compose** | Interface moderne, déclarative, adaptée aux 5 écrans cibles |
| 2026-11-16 | Organisation source par couches/features : `core/`, `data/`, `domain/`, `feature/*`, `ui/theme`, `vpn` | Séparation claire entre UI, données, domaine et préparation système VPN |
| 2026-11-16 | Versions épinglées (catalogue) : AGP **8.7.3**, Kotlin **2.0.21**, KSP **2.0.21-1.0.28**, Gradle **8.10.2**, compile/target SDK **35**, minSdk **26**, JDK **17**, Compose BOM **2024.12.01**, Navigation **2.8.5**, Room **2.6.1**, DataStore **1.1.1**, Coroutines **1.9.0**, JUnit **4.13.2** | Reproductibilité et compatibilité maîtrisée (pas de `latest` flottant) |
| 2026-11-16 | Workflow CI `android.yml` : setup-java/SDK, validation wrapper, `:app:testDebugUnitTest`, `:app:assembleDebug`, upload artefact APK (`if-no-files-found: error`) | Validation compilable/testable sur GitHub Actions sans masquer les échecs |
| 2026-11-16 | `OnfaaTunnelVpnService` enregistré mais **sans tunnel** (arrêt immédiat en `onStartCommand`, aucune logique réseau/moteur) | Préparation `VpnService` conforme sans simulation de connexion |

## Décisions / constats validés (Phase 2 — audit)

| Date | Élément | Valeur / constat vérifié | Référence |
| --- | --- | --- | --- |
| 2026-11-16 | Xray-core upstream (release examinée) | `v26.11.9`, commit `676eaf66f8f05d4d558ee7a74b4736623d3feeb9`, licence MPL-2.0, `go 1.27` | `docs/XRAY_UPSTREAM_AUDIT.md` |
| 2026-11-16 | libXray upstream (release examinée) | `v26.11.9`, commit tag `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`, licence MIT, `go 1.27.3` | `docs/XRAY_UPSTREAM_AUDIT.md` |
| 2026-11-16 | Alignement moteur | libXray dépend de `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0` (correspondance v26.11.9) | `go.mod`, build script libXray |
| 2026-11-16 | API libXray (contrat) | `Invoke` JSON, `apiVersion=3`, méthodes/cycle de vie/wrappers Android/CGo vérifiés dans le code (pas de signatures inventées) | `docs/LIBXRAY_API_REFERENCE.md` |
| 2026-11-16 | Build Android officiel libXray | `python3 build/main.py android` (gomobile), `LIBXRAY_ANDROID_API=21`, NDK r30 **package exact `ndk;30.0.16248370`** installé via `sdkmanager`, Go `1.26.1`, gomobile `v0.0.0-20260217195705-b56b3793a9c4` | `.github/workflows/libxray-poc.yml` |
| 2026-11-16 | Artefact PoC (vérifié) | AAR construit depuis source épinglée, SHA-256 `1655655c…d943d917`, 53 200 280 octets, ABIs `arm64-v8a/armeabi-v7a/x86/x86_64`, tous `LOAD` 16 KiB | `docs/LIBXRAY_ANDROID_POC.md` |
| 2026-11-16 | Binding JVM réel | `libXray.LibXray` méthode **`public static native String invoke(String)`** (minuscule), runtime Go chargé par initialisation de classe | `javap` CI |
| 2026-11-16 | Validation runtime | Tests instrumentés réels exécutés sur émulateur **x86_64 API 35** en CI : 6/6 succès, 0 ignoré | run `43965698249` |

## Décisions validées (Phase 3 — tunnel réel)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-16 | Contrat TUN : descripteur `VpnService` transmis dans `env["xray.tun.fd"]` de la configuration Xray, inbound `tun` | Xray-core v26.11.9 lit ce contrat (`common/platform.TunFdKey`) ; libXray v26.11.9 n’expose plus `SetTunFd` |
| 2026-11-16 | TUN Android : `10.10.0.2/24`, `fd00::2/64`, routes `0.0.0.0/0` et `::/0`, DNS `10.10.0.1`/`fd00::1`, MTU 1500 | Interface complète possédée par Android ; le moteur ne fait que lire le descripteur |
| 2026-11-16 | Protection des sockets via `VpnService.protect(fd)` exposée par un vrai `DialerController` (`protectFd(long)`) enregistré par `registerDialerController` | Éviter la boucle de routage du moteur ; contrat réel vérifié dans l’AAR |
| 2026-11-16 | Résolveur Go configuré par `setDNS(protector, serveur)` et restauré par `resetDNS()` | libXray résout les domaines via ce résolveur ; restauration obligatoire à l’arrêt |
| 2026-11-16 | Pipeline : lien → `convertShareLinksToXrayJson` → outbound réel → configuration TUN complète → `testXray` → `runXray` | Une seule chaîne officielle, aucune configuration inventée |
| 2026-11-16 | `Connected` publié uniquement après `runXray` réussi + `getXrayState=true` + sonde HTTP réelle réussie | Interdiction de toute connexion simulée ou affichée sans preuve |
| 2026-11-16 | Profils en mémoire uniquement (pas de fichier en clair) | Ne pas exposer d’identifiants ; persistance chiffrée reportée à la phase suivante |
| 2026-11-16 | VMess base64 historique refusé avec message explicite ; seul le format URL `vmess://UUID@hôte:port?...` est accepté | Le convertisseur libXray v26.11.9 ne gère pas le format historique ; ne pas prétendre le supporter |
| 2026-11-16 | E2E : serveur Xray-core officiel construit depuis le commit épinglé sur le runner ; émulateur via `10.0.2.2` ; fixture token HTTP sur TEST-NET `192.0.2.10:18080` + DNS `onfaa.test` | Prouver tunnel, DNS et routage réels sans dépendance Internet externe |
| 2026-11-16 | DNS du tunnel en **TCP** vers `10.0.2.2:15353` en E2E (fixture servant UDP et TCP) | Le forwarding UDP de l’alias hôte `10.0.2.2` de l’émulateur n’est pas garanti ; TCP l’est. Xray-core accepte l’adresse `tcp://hôte:port` |
| 2026-11-16 | Changement d’IP publique non revendiqué en CI (même sortie réseau) ; protocole manuel documenté | Ne pas affirmer un résultat non mesurable dans l’environnement |
| 2026-11-16 | Trafic HTTP en clair autorisé uniquement en variant `debug` pour la fixture ; release en TLS strict | La fixture E2E est hors production ; le build release reste durci |
| 2026-11-16 | Aucune statistique de trafic affichée tant qu’aucune source réelle n’est branchée | Pas de compteurs simulés |

## Décisions validées (Phase 3 — validation CI)

| Date | Décision | Raison / portée |
| --- | --- | --- |
| 2026-11-17 | Contrôleur de protection des sockets enregistré **une seule fois par processus**, derrière un délégué `AtomicReference` ; `clearSocketProtector()` ne fait que vider le délégué | `transport/internet.RegisterDialerController` **empile** dans `internet.Controllers` et tout dial doit satisfaire **tous** les contrôleurs ; il n’existe pas de désinscription. Un contrôleur `{ false }` laissé par un test rejetait tous les dials du processus (run `43995354053`, `failed to apply external controller`), VLESS et VMess identiquement, y compris après reconnexion en production |
| 2026-11-17 | Fixture E2E : `finalRules` `allow` (`tcp`, `127.0.0.1:$E2E_HTTP_PORT`) sur l’outbound `freedom` avec `redirect` | Xray 26.x applique `defaultBlockPrivateRule` aux inbounds VLESS/VMess ; la cible `127.0.0.1` du `redirect` était blackholée (`target blocked by a finalrule`, run `43999172035`) alors que le tunnel et le journal d’accès étaient corrects |
| 2026-11-17 | Fixture VLESS en clair sur adresse LAN (`192.168.1.10`) et test dédié `convertsPublicVlessTlsShareLinkWithEngine` | Xray refuse un outbound VLESS sans TLS vers une IP publique (`requiresTransportSecurity`) ; ce n’était pas un défaut de l’application (run `43995354053`) |
| 2026-11-17 | Portes de preuve CI durcies : échec si `target blocked by a finalrule` dans le journal serveur, ≥1 ligne d’accès par protocole, 4 tests E2E exigés | Un journal d’accès peut prouver la destination **sans** prouver la réponse HTTP ; ne jamais déclarer un tunnel réussi sur une sonde expirée |
| 2026-11-17 | Validation Phase 3 prononcée sur le run `44004893595` (commit `5f51831`) : 21/21 instrumentés, VLESS+VMess, DNS, arrêt propre | Preuves réelles uniquement. **Non revendiqué** : appareil physique ARM64 et changement d’IP publique (protocoles manuels documentés) |

## Choix techniques à confirmer / à compléter

| Sujet | Questions ouvertes | Phase cible |
| --- | --- | --- |
| **Stockage de l’AAR** | Choix retenu : build from source en CI, AAR non commité (gitignore), injection vérifiée dans `app/libs` | Phase 2 (fait) / 9 |
| **Exécution arm** | Valider `xrayVersion`/`invoke`/tunnel sur appareil physique `arm64-v8a` (émulateur x86_64 seulement en CI) — **en attente**, protocole dans `docs/PHASE_3_REPORT.md` | Phase 3 |
| **Formats d’import** | Liens `vless://` et `vmess://` URL implémentés et validés en Phase 3 ; restent JSON, QR, presse-papiers et persistance | Phase 4 |
| **Modèle de données** | Schéma Room (profils, groupes, journaux), persistance chiffrée, stratégie de migration | Phases 4+ |
| **Statistiques** | Aucune source réelle branchée : ne pas afficher de compteurs tant que l’API moteur n’est pas intégrée | Phases 6+ |
| **DNS/Routage** | Base réelle Phase 3 : DNS via tunnel, privé direct, reste proxy ; options utilisateur à exposer | Phases 6–7 |
| **CI/CD détaillée** | Jobs dédiés libXray, caching, secrets GitHub (si nécessaires), signature d’artefacts | Phase 9 |
| **Stratégie de tests automatisés** | Unitaires, instrumentation, tests d’intégration réseau (environnement contrôlé) | Phases 1, 3, 8 |

## Règles de décision

- Toute hypothèse impactant la sécurité, la confidentialité ou le comportement réseau doit être explicitement documentée.
- Les signatures et comportements de `libXray` ne doivent **jamais** être inventés : consigner « inconnu » jusqu’à confirmation.
- Les changements de décision doivent être datés et motivés dans ce fichier.

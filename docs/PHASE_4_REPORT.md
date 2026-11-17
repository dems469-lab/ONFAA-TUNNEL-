# Rapport Phase 4 — Persistance réelle, import étendu, cycle de vie durci

> **Statut de validation : code compilé, tests unitaires et instrumentés verts en CI.**
> Le workflow Android CI
> [#44011899010](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899010)
> (commit `230008c`) est vert : compilation, 23/23 tests unitaires, APK debug et APK de
> tests instrumentés construits. Le workflow
> « libXray Android build and real VPN E2E (Phase 3) »
> [#44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095)
> rejoue l’E2E réel et les 27 tests instrumentés avec l’AAR officiel : **succès** —
> **27/27 instrumentés** (0 échec, 0 erreur, 0 ignoré, 45,579 s), E2E VLESS et VMess
> avec sonde réelle, `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3`.
> Deux limites restent **non revendiquées** : appareil physique ARM64 (§10) et
> changement d’IP publique.

## 1. Objectif

Livrer la Phase 4 du plan :

- profils persistés dans Room (ajout, modification, suppression, renommage, sélection),
  survivant au redémarrage, avec protection des données sensibles ;
- import fiable de liens VLESS/VMess, du presse-papiers, de fichiers et de
  configurations JSON compatibles Xray, avec validation et erreurs lisibles ;
- cycle de vie VPN durci (états explicites, double démarrage, perte de réseau,
  reconnexion bornée, arrêt propre, nettoyage) ;
- journaux utilisables (niveaux, filtre, copie, export, effacement, taille bornée,
  secrets masqués) ;
- statistiques uniquement mesurées (durée, octets, état, profil actif) ;
- CI qui compile, teste, publie rapports et APK sans masquer d’échec ;
- documentation à jour et ce rapport.

## 2. Audit initial

À l’audit du commit de départ (`493ef0f`) :

- `OnfaaDatabase`, `ServerConfigEntity`/`ServerConfigDao` et
  `UserPreferencesRepository` existaient **mais n’étaient jamais instanciés** :
  aucun composant ne construisait la base, aucune écriture n’avait lieu.
- `TunnelConfigRepository` gardait les profils **en mémoire** : tout disparaissait
  à la fermeture du processus ; la sélection n’était pas persistée.
- L’import ne couvrait que les liens et ne distinguait pas proprement les erreurs de
  structure des refus du moteur.
- `TunnelState` ne distinguait pas la préparation de la connexion ni la vérification
  réelle ; il n’y avait ni surveillance post-connexion ni reconnexion.
- Les journaux n’existaient qu’en mémoire, sans fichier, sans filtre, sans export.
- Les statistiques n’exposaient que des octets bruts sans durée ni distinction de
  source.

## 3. Architecture livrée

```text
UI Compose ──► TunnelConfigRepository (StateFlow) ──► Room v2 (profils)
                     │                                └► DataStore (sélection)
                     └─ SecretCipher (AES-256-GCM, Android Keystore)

HomeScreen ──► OnfaaTunnelVpnService ──► libXray (AAR officiel) ──► Xray-core
                     │
                     ├─ TunnelStatusRepository (états + journaux bornés)
                     └─ LogFileStore (fichier tournant assaini)
```

- **Un seul runtime moteur** : Xray-core `v26.11.9`
  (`676eaf66f8f05d4d558ee7a74b4736623d3feeb9`), via libXray `v26.11.9`
  (`5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`). Aucun mock, aucun sing-box,
  aucune statistique simulée, aucun état inventé.
- **Pas de nouvelle dépendance lourde** : Room 2.6, DataStore Preferences,
  Compose Material 3 et l’AAR libXray déjà épinglé.

### 3.1 Couche de données

- `ProfileEntity` (table `server_configs`, version 2) : `id`, `name`, `protocol`,
  `endpoint`, `security`, `payloadCipher`, `isFavorite`, `notes`, `createdAt`,
  `updatedAt`.
- `ProfileDao` : `observeAll`, `findById`, `insert`, `update`, `rename`,
  `delete`, `setFavorite`, `clear`.
- `OnfaaDatabase.version = 2`, `exportSchema = false`, méthode `build(context)`
  centralisée (`onfaa_profiles.db`).
- `OnfaaDataStore` : extension `Context.onfaaDataStore` (fichier
  `onfaa_preferences`), clés `selected_config_id` et préférences existantes.
- `TunnelConfigRepository` (singleton, initialisé par `OnfaaTunnelApp`) :
  combine le flux Room et le flux DataStore, publie `profiles`, `selectedId`,
  `favoriteIds`, `ready`. API conservée (`profiles`/`selected`) pour ne pas casser
  l’UI ni le service ; les écritures se font sur `Dispatchers.IO`.

Comportements :

- premier profil importé → sélectionné automatiquement et persisté ;
- sélection persistée restaurée au démarrage ; si l’identifiant n’existe plus,
  repli sur le premier profil ;
- suppression du profil actif → repli déterministe sur le profil suivant ;
- renommage refusé si vide, rogné, longueur bornée (`MAX_NAME_LENGTH`).

### 3.2 Protection des données sensibles

- L’outbound Xray complet (UUID, `id`, clés, en-têtes) est **chiffré en
  AES-256-GCM** par `SecretCipher`, clé **non exportable** générée dans le
  **Android Keystore** (alias `onfaa_server_payload_v1`).
- Format stocké : `base64(iv || ciphertext + tag)` dans `payloadCipher`.
- Si le chiffrement échoue, le profil **n’est pas enregistré** : aucun repli en
  clair.
- Champs en clair limités à ce que l’UI doit afficher : nom, protocole, hôte:port,
  type de sécurité.
- Les journaux ne contiennent jamais l’UUID, les liens complets ni la configuration :
  `XrayErrorSanitizer` + `redactSecrets` masquent les UUID et les charges base64
  longues, à l’écriture **et** à l’export.
- Si la clé Keystore disparaît (réinstallation, réinitialisation), le profil est
  marqué **incomplet**, affiché comme tel, et la connexion est refusée avec un
  message explicite (« réimportez le lien »).

### 3.3 Import

`ConfigImporter` accepte :

| Entrée | Détection | Validation |
| --- | --- | --- |
| Lien `vless://` / `vmess://` | schéma d’URL | converti par libXray, puis `testXray` |
| Objet outbound unique | `protocol` VLESS/VMess | structure + `testXray` |
| Configuration Xray JSON | sorties `outbounds` | une seule sortie VLESS/VMess exigée |
| Texte brut (presse-papiers) | tentative JSON puis lien | idem |
| Fichier (≤ 256 Ko) | `OpenDocument` | idem |

Ordre de validation : structure JSON/URL → champs obligatoires (adresse, port
1–65535, identifiant) → construction de la configuration TUN complète →
`testXray` (le moteur officiel construit ses handlers ; le TUN n’est pas démarré).
Rien n’est persisté si une étape échoue. Erreurs en français, sans secret :
« Aucun serveur VLESS/VMess trouvé », « Plusieurs sorties VLESS/VMess (2) : gardez
une seule sortie », « Identifiant (id) manquant », « Port invalide (1–65535) »…

Le JSON accepté est normalisé : `settings.vnext[0].users[0]` est aplati en
`settings.address/port/id`, `streamSettings.security` conservé (`none`, `tls`,
`reality`), `tag` réécrit. Un nom peut être fourni à l’import ; sinon le libellé du
lien (fragment décodé) ou `VLESS/VMess hôte:port` est utilisé.

### 3.4 Cycle de vie du service VPN

États exposés : `Idle`, `AwaitingPermission`, `Preparing`, `Connecting`,
`Verifying`, `Connected`, `Reconnecting(attempt, profileName)`, `Disconnecting`,
`Error`.

- **Consentement** : `AwaitingPermission` est publié par l’UI **avant** le dialogue
  système ; le démarrage effectif n’a lieu qu’après `RESULT_OK`.
- **Double démarrage** : verrou `Mutex` + drapeaux `engineRunning`/état ; une seconde
  demande pendant une connexion est ignorée avec un journal WARNING, jamais deux
  moteurs.
- **Refus explicite** : profil absent, incomplet ou configuration invalide →
  `Error` avec message, sans démarrage partiel.
- **Preuve réelle** : `Connected` n’est publié qu’après `getXrayState == true`
  **et** une sonde HTTP réelle à travers le TUN (token attendu en réponse).
- **Surveillance** : après connexion, sonde toutes les 20 s. Échec → passage
  immédiat à `Reconnecting` (jamais de faux `Connected`), sondes de reprise toutes
  les 5 s pendant **90 s** au maximum, puis `Error` et arrêt propre.
- **Arrêt** : bouton, notification, révocation ou destruction → arrêt du moteur
  (`stopXray`), restauration DNS, fermeture du TUN, `Idle`/`Error`, nettoyage des
  jobs et du fichier de logs conservé.
- **UI jamais bloquée** : tout le travail réseau/moteur est sur des coroutines
  `Dispatchers.IO`, l’état est publié via `StateFlow`.

### 3.5 Journaux

- Tampon mémoire borné à **500 événements** (`TunnelStatusRepository`), chacun avec
  horodatage `HH:mm:ss`, niveau (`DEBUG`/`INFO`/`WARNING`/`ERROR`), état du tunnel,
  source et message.
- Miroir fichier `filesDir/logs/onfaa.log` via `LogFileStore` : écriture sur un
  thread dédié, fichier limité à **256 Ko**, copie `.1` à la rotation, relecture des
  **200 dernières lignes** au démarrage (les journaux survivent à un crash).
- `LogFormatter` : format stable `epoch|niveau|état|source|message`, échappement des
  sauts de ligne, lignes corrompues ignorées à la relecture.
- UI : filtres par niveau, recherche texte, copie presse-papiers, export
  (`CreateDocument`, nom horodaté), effacement avec confirmation. L’export ajoute un
  en-tête (version, nombre d’événements) et **réassainit** les messages.

### 3.6 Statistiques

- Durée de connexion calculée depuis `sinceEpochMs` réel, rafraîchie chaque seconde
  tant que l’état est `Connected`.
- Octets reçus/émis et débits issus de `android.net.TrafficStats` pour l’UID de
  l’application — **compteurs UID Android, pas un compteur noyau du TUN** ; l’écran
  l’écrit explicitement et affiche « indisponible » si la plateforme renvoie
  `UNSUPPORTED`.
- État du tunnel et profil actif affichés ensemble ; aucune valeur estimée présentée
  comme exacte.

## 4. Migration Room `1 → 2`

- La table `server_configs` est **recréée explicitement** puis recopiée par
  `MIGRATION_1_2` (pas d’`ALTER TABLE ... DEFAULT`, qui échoue sur SQLite ancien).
- Les lignes v1 sont conservées (identifiant, nom, protocole, hôte, sécurité) et
  reçoivent `security='none'`, `payloadCipher=''`, `isFavorite=0`, `notes=''`.
- Ces lignes apparaissent comme **incomplètes** (badge) : la v1 ne stockait aucune
  donnée d’authentification. Elles ne peuvent pas être connectées ; l’utilisateur
  doit réimporter le lien. Aucun secret n’est inventé.
- Test instrumenté : `ProfileStorageInstrumentedTest.migrationFromVersion1…` crée une
  base v1 réelle (`LegacyServerConfigDatabase` déclarée dans `androidTest`), la fait
  migrer par `OnfaaDatabase`, vérifie la conservation des lignes et les nouvelles
  colonnes.

## 5. Fichiers modifiés (Phase 4)

| Fichier | Nature |
| --- | --- |
| `app/src/main/java/com/onfaa/tunnel/core/security/SecretCipher.kt` | nouveaux |
| `app/src/main/java/com/onfaa/tunnel/data/local/db/ProfileEntity.kt` | nouveaux |
| `app/src/main/java/com/onfaa/tunnel/data/local/db/ProfileDao.kt` | nouveaux |
| `app/src/main/java/com/onfaa/tunnel/data/local/db/OnfaaDatabase.kt` | réécrit (v2 + migration) |
| `app/src/main/java/com/onfaa/tunnel/data/local/db/ServerConfigEntity.kt` | supprimés |
| `app/src/main/java/com/onfaa/tunnel/data/local/db/ServerConfigDao.kt` | supprimés |
| `app/src/main/java/com/onfaa/tunnel/data/preferences/OnfaaDataStore.kt` | modifié |
| `app/src/main/java/com/onfaa/tunnel/data/config/TunnelConfigRepository.kt` | réécrit |
| `app/src/main/java/com/onfaa/tunnel/domain/model/TunnelProfile.kt` | modifié (`incomplete`) |
| `app/src/main/java/com/onfaa/tunnel/core/xray/ConfigImporter.kt` | réécrit |
| `app/src/main/java/com/onfaa/tunnel/vpn/TunnelStatus.kt` | réécrit (états + logs) |
| `app/src/main/java/com/onfaa/tunnel/vpn/LogFormatter.kt` | nouveaux |
| `app/src/main/java/com/onfaa/tunnel/vpn/LogFileStore.kt` | nouveaux |
| `app/src/main/java/com/onfaa/tunnel/vpn/OnfaaTunnelVpnService.kt` | modifié (états, reprise, garde) |
| `app/src/main/java/com/onfaa/tunnel/feature/configs/ConfigsScreen.kt` | modifié |
| `app/src/main/java/com/onfaa/tunnel/feature/logs/LogsScreen.kt` | modifié |
| `app/src/main/java/com/onfaa/tunnel/feature/stats/StatsScreen.kt` | modifié |
| `app/src/main/java/com/onfaa/tunnel/feature/home/HomeScreen.kt` | modifié |
| `app/src/main/java/com/onfaa/tunnel/OnfaaTunnelApp.kt` | modifié |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | version `0.4.0-phase4`, `org.json` pour les tests |
| `app/src/test/java/com/onfaa/tunnel/core/xray/ConfigImporterJsonTest.kt` | nouveaux (8) |
| `app/src/test/java/com/onfaa/tunnel/vpn/LogFormatterTest.kt` | nouveaux (4) |
| `app/src/androidTest/java/com/onfaa/tunnel/data/local/db/ProfileStorageInstrumentedTest.kt` | nouveaux (3) |
| `app/src/androidTest/java/com/onfaa/tunnel/vpn/LogFileStoreInstrumentedTest.kt` | nouveaux (3) |
| `docs/ARCHITECTURE.md`, `README.md`, `STATUS.md`, `CHANGELOG.md`, `docs/ROADMAP.md`, `docs/TEST_PLAN.md`, `docs/BUILD_GUIDE.md` | documentation |
| `docs/PHASE_4_REPORT.md` | ce rapport |

`SettingsScreen` et le thème n’ont pas été touchés : la refonte visuelle appartient à
la Phase 5.

## 6. Tests

### Unitaires (JVM, 23 au total)

| Classe | Nombre | Couverture |
| --- | --- | --- |
| `ConfigImporterValidationTest` | 3 | erreurs de lien historiques |
| `XrayErrorSanitizerTest` | 6 | masquage des secrets |
| `ServerConfigTest` | 2 | modèle |
| `ConfigImporterJsonTest` | 8 | JSON invalide, vide, sans sortie, protocole non supporté, plusieurs sorties, identifiant manquant, port invalide, adresse manquante |
| `LogFormatterTest` | 4 | aller-retour fichier, sauts de ligne, lignes corrompues, export assaini |

### Instrumentés (27 au total)

| Classe | Nombre | Couverture |
| --- | --- | --- |
| `ExampleInstrumentedTest` | 1 | sanity |
| `XrayBridgeInstrumentedTest` | 5 | AAR natif |
| `XrayConfigInstrumentedTest` | 9 | configurations réelles validées par le moteur |
| `VpnPermissionInstrumentedTest` | 2 | consentement |
| `VpnTunnelE2ETest` | 4 | VLESS/VMess réels, DNS, arrêt, serveur injoignable |
| `ProfileStorageInstrumentedTest` | 3 | migration v1→v2, CRUD chiffré, altération GCM |
| `LogFileStoreInstrumentedTest` | 3 | persistance après redémarrage, rotation, borne + effacement |

### Preuves exigées du workflow E2E

Le workflow reste verrouillé : `E2E_XML_VALIDATED`, `ACCESS_LOG_PER_PROTOCOL_OK` et
`--suite VpnTunnelE2ETest --min-tests 4` — un test non exécuté fait échouer le job.

## 7. CI et artefacts

- **Android CI** — run
  [#44011899010](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899010)
  (commit `230008c`) : **succès** — compilation debug, 23/23 tests unitaires,
  `assembleDebug`, `assembleDebugAndroidTest`.
- **E2E + instrumentés** — run
  [#44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095)
  (commit `230008c`) : **succès**, 30 étapes, 9 min 03 s — voir §9.
- APK debug publié comme artefact `onfaa-tunnel-debug-apk` (run Android CI
  #44011899010) et dans l’artefact `libxray-poc-report` (run E2E #44011899095,
  qui contient aussi l’APK de tests instrumentés et le rapport complet).

## 8. Limites assumées

1. **Appareil physique ARM64** : non exécuté. Toute la validation native est sur
   émulateur x86_64 API 35. Le protocole §10 est fourni, aucun résultat n’est
   revendiqué.
2. **Changement d’IP publique** : non démontrable en CI (§12 du rapport Phase 3).
3. **Statistiques** : compteurs UID Android, pas les compteurs noyau du TUN.
4. **Perte de clé Keystore** : profils illisibles → marqués incomplets, réimport
   nécessaire (comportement affiché, pas de repli en clair).
5. **Reconnexion** : reprise bornée à 90 s puis erreur ; pas de stratégie
   exponentielle ni de reprise en arrière-plan prolongée.
6. **Journaux** : un seul fichier tournant (256 Ko) ; pas d’archive multi-fichiers.
7. **Import** : un seul profil par import ; pas d’import de lot ni de QR code.

## 9. Résultats du run E2E (#44011899095)

Run
[#44011899095](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44011899095)
sur le commit `230008c` : **`success`**, 30 étapes, du 2026-11-17T03:40:54Z au
2026-11-17T03:49:57Z (9 min 03 s), AAR libXray SHA-256
`1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`.

### 9.1 Tests instrumentés

Le XML publié par le job donne exactement :

```text
<testsuite name="com.onfaa.tunnel.ExampleInstrumentedTest"
           tests="27" failures="0" errors="0" skipped="0" time="45.579" ...>
```

**27/27 tests instrumentés réussis** (0 échec, 0 erreur, 0 ignoré), incluant les
6 nouveaux tests Phase 4 (migration Room v1→v2, CRUD chiffré, altération GCM,
persistance des journaux après redémarrage, rotation, borne/effacement).

### 9.2 Preuves E2E

- `E2E_XML_VALIDATED` — les 4 tests `VpnTunnelE2ETest` sont présents dans le XML :
  `emulatorReachesHostFixtureThroughHostAlias`,
  `vlessTunnelForwardsTrafficResolvesDnsAndStopsCleanly`,
  `unreachableServerNeverReportsConnected`,
  `vmessTunnelForwardsTrafficAndStopsCleanly` — tous `PASS`.
- `ACCESS_LOG_PER_PROTOCOL_OK vless=3 vmess=3` — le journal d’accès du serveur
  Xray officiel contient au moins une entrée par protocole ; aucun
  `target blocked by a finalrule`.
- Aucune porte masquée : les scripts échouent si le XML manque, si un test n’est pas
  exécuté ou si le journal d’accès ne contient pas les deux protocoles.

### 9.3 Artefacts

- `libxray-poc-report` (id `14297207450`, ~122 Mo) : rapport, AAR construit,
  APK debug, APK de tests instrumentés, rapports de tests.
- `onfaa-tunnel-debug-apk` (id `14296603246`, ~17 Mo) : APK debug du run Android CI
  #44011899010.

**Conclusion Phase 4** : les exigences fonctionnelles sont livrées et vérifiées par
des tests réels exécutés en CI ; aucune affirmation au-delà de ces preuves n’est
faite (ARM64 physique et IP publique restent non revendiqués).

## 10. Protocole de validation sur appareil physique ARM64

À exécuter par une personne disposant d’un appareil ARM64 et d’un serveur Xray
joignable ; chaque étape doit être consignée (capture ou journal) avant d’être
revendiquée.

1. Installer l’APK debug du run CI (`onfaa-debug-apk-*`), activer le débogage USB.
2. Importer un lien VLESS TLS réel, vérifier l’affichage (nom, hôte, sécurité).
3. Connecter, accepter le consentement VPN, attendre `Connecté` — vérifier dans les
   journaux la ligne de sonde réelle (`ok=true detail=HTTP …`).
4. Naviguer vers un site externe et relever l’IP publique avant/après ; consigner
   les deux valeurs.
5. Couper le réseau (mode avion) 30 s : vérifier `Reconnexion`, puis retour réseau :
   `Connecté` seulement après sonde réussie.
6. Arrêter le tunnel : vérifier `Déconnecté` et le retour du trafic direct.
7. Fermer l’application, la relancer : le profil et la sélection sont toujours là.
8. Consulter les journaux et l’export : aucun UUID, lien ou configuration en clair.
9. Consigner la version d’Android, le modèle et les journaux dans le rapport de
   validation physique.

## 11. Reprise

- Exécuter le protocole §10 puis mettre à jour ce rapport et `STATUS.md` avec les
  preuves.
- Phase 5 (refonte visuelle) : hors périmètre de ce rapport, travail parallèle.

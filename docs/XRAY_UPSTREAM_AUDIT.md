# Audit technique upstream — Xray-core & libXray (Phase 2)

> Audit réalisé à partir des sources officielles publiques (GitHub API, fichiers bruts des dépôts aux tags examinés) le 2026-11-16. Ce document ne contient que des éléments vérifiés ou explicitement signalés comme non vérifiés.

## 1. Périmètre et règles

- **Moteur cœur** : XTLS/Xray-core officiel (`https://github.com/XTLS/Xray-core`) — aucun fork tiers, aucune version modifiée présentée comme upstream.
- **Intégration Android cible** : XTLS/libXray officiel (`https://github.com/XTLS/libXray`) après vérification de compatibilité/API.
- **Interdictions respectées** : pas de sing-box, pas de fausse bibliothèque libXray, pas de simulation de moteur/tunnel, pas d’invention de signatures API.
- **Phase 2** : audit, compatibilité, stratégie de build reproductible et documentation ; **aucune connexion VPN réelle démarrée**.

## 2. Xray-core (upstream officiel)

| Élément | Valeur vérifiée | Source |
|---|---|---|
| Dépôt | `XTLS/Xray-core` | https://github.com/XTLS/Xray-core |
| Licence | **MPL-2.0** (Mozilla Public License 2.0) | GitHub API `/license` |
| Dernière release stable examinée | **v26.11.9** — “Xray-core v26.11.9” (prerelease: false, draft: false) | Release API `releases/latest` / `releases/tags/v26.11.9` |
| Tag | `v26.11.9` | Release API |
| Commit du tag (objet commit) | **`676eaf66f8f05d4d558ee7a74b4736623d3feeb9`** | Git ref `tags/v26.11.9` |
| Date de publication release | 2026-11-09T05:31:16Z | Release API |
| Module Go | `github.com/xtls/xray-core` | `go.mod` @ v26.11.9 |
| Directive Go (`go.mod`) | `go 1.27` | `go.mod` @ v26.11.9 |

### 2.1 Dépendances Go (extrait vérifié)

`go.mod` déclare notamment : `google.golang.org/grpc v1.85.0`, `google.golang.org/protobuf v1.36.12`, `golang.org/x/net v0.62.0`, `golang.org/x/crypto v0.58.0`, `golang.zx2c4.com/wireguard`, `github.com/refraction-networking/utls`, `github.com/xtls/reality`, `github.com/apernet/quic-go`, etc. (liste complète : `https://raw.githubusercontent.com/XTLS/Xray-core/v26.11.9/go.mod`).

### 2.2 Artefacts release Android (binaire Xray-core, information de référence)

Assets Android publiés (nom, SHA256 digest, taille) :

| Asset | Digest (release) | Taille |
|---|---|---|
| `Xray-android-amd64.zip` | `sha256:48c266cdb9660dade968814e2f91d84e6aff534b7c3762ca3e385fb8aa475bd8` | 16900215 |
| `Xray-android-amd64.zip.dgst` | `sha256:2e46e09dd11ddb0c224e30613082c31c3f24f3d61904543fef63c644ce4daf8f` | 299 |
| `Xray-android-arm64-v8a.zip` | `sha256:edcefc25b9b3195b11e18624c8269a969d6aabc39c6298a9a5ab29de15d46f3b` | 15621040 |
| `Xray-android-arm64-v8a.zip.dgst` | `sha256:e6b8fb07f1eceb36641badf845bcf880cdd93d21802f662be6aa922e01933e16` | 299 |

> **Note** : ces artefacts sont des binaires Xray-core Android publiés par l’upstream. L’intégration applicative ONFAA TUNNEL vise **libXray** (AAR/bibliothèques) ; les ABIs/format exacts des `.so` dans l’AAR libXray doivent être confirmés par inspection d’un artefact libXray vérifié (voir section 5). Les noms d’assets ci-dessus indiquent au moins amd64 et arm64-v8a côté distribution Xray-core, mais ne constituent pas à eux seuls une preuve des ABIs de l’AAR libXray.

### 2.3 Exigences de compilation (général, vérifié partiellement)

- Toolchain Go compatible avec la directive `go.mod` (ici `go 1.27` pour Xray-core v26.11.9). Les exigences détaillées par plateforme sont définies par les scripts/workflows upstream (non entièrement audités fichier par fichier dans cette phase).

## 3. libXray (wrapper officiel d’intégration)

| Élément | Valeur vérifiée | Source |
|---|---|---|
| Dépôt | `XTLS/libXray` | https://github.com/XTLS/libXray |
| Licence | **MIT** | GitHub API `/license` |
| Dernière release stable examinée | **v26.11.9** — “libXray v26.11.9” (prerelease: false, draft: false) | Release API |
| Tag | `v26.11.9` (tag annoté) | Git ref/tags API |
| Objet tag | `71d9d443aa88ff939ab6ce22b82aa4b2adcc4d9f` | Git tag object |
| Commit cible du tag | **`5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`** | Git tag object → commit |
| Date release | 2026-11-09T12:39:27Z | Release API |
| Module Go | `github.com/xtls/libxray` | `go.mod` @ v26.11.9 |
| Directive Go (`go.mod`) | `go 1.27.3` | `go.mod` @ v26.11.9 |
| Dépendance Xray-core | `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0` | `go.mod` @ v26.11.9 |
| Version Xray-core épinglée par le build script | `DEFAULT_XRAY_CORE_VERSION = "v1.260327.1-0.20261109052709-676eaf66f8f0"` | `build/app/build.py` @ v26.11.9 |

### 3.1 Correspondance libXray ↔ Xray-core (vérifiée)

- `libXray` v26.11.9 dépend de la pseudo-version Xray-core **`v1.260327.1-0.20261109052709-676eaf66f8f0`**, dont le suffixe correspond au commit horodaté **`676eaf66f8f0`** (préfixe du commit taggé Xray-core v26.11.9 : `676eaf66f8f05d4d558ee7a74b4736623d3feeb9`).
- Le README de libXray indique : « pins Xray-core to release tag `v26.11.9` through its pseudo-version ».
- **Conclusion** : pour la release libXray v26.11.9 examinée, la compatibilité avec Xray-core v26.11.9 est **explicitement alignée** par la dépendance Go et la documentation du dépôt.

### 3.2 Fichiers de référence examinés (libXray v26.11.9)

- `README.md`, `go.mod`, `go.sum` (présence via arbre), `build/main.py`, `build/app/build.py`, `build/app/android.py`
- `invoke.go`, `invoke_model.go`, `android_wrapper.go` (build tag `android`), `cgo_bridge/main.go`
- `xray/xray.go`, `xray/validation.go`, `xray/ping_batch.go` (et tests associés dans l’arbre)
- `controller/controller.go`, `controller/controller_android.go`, `controller/proc_resolver_android.go`
- `dns/dns_android.go`, `dns/dns_desktop.go`, etc.
- Workflows : `.github/workflows/build.yml`, `.github/workflows/validate.yml`, `.github/workflows/libxray-test.yml`, `.github/workflows/release-go-mirror.yml`

### 3.3 Versioning (vérifié)

- Releases CalVer `v<YY>.<M>.<D>` avec miroir SemVer Go (ex. `v26.11.9` ↔ `v1.260327.x` selon politique décrite) ; README recommande aux consommateurs Go de pinner le tag miroir SemVer.

## 4. API libXray (synthèse vérifiée)

- **Entrée unique** : `Invoke(requestJSON string) string` (Go) ; exports CGo via `cgo_bridge/main.go` : `CGoInvoke(*C.char) *C.char`, `CGoFree(*C.char)`.
- **Protocole** : JSON request `{apiVersion, method, payload}` ; JSON response `{success, data, error}` ; `apiVersion` supportée = **3** (`LibXrayAPIVersion`).
- **Méthodes** (enum `LibXrayMethod` dans `invoke_model.go`) : `getFreePorts`, `decodeSubscriptionText`, `convertShareLinksToXrayJson`, `convertXrayJsonToShareLinks`, `generateAgeKeyPair`, `countGeoData`, `pingBatch`, `testXray`, `runXray`, `stopXray`, `xrayVersion`, `getXrayState`.
- **Android (fichier `android_wrapper.go`, tag `//go:build android`)** : `SetDNS(controller DialerController, server string) error`, `ResetDNS()`, `RegisterDialerController`, `RegisterListenerController`, `RegisterProcessFinder` avec interfaces `DialerController` (`ProtectFd(int) bool`) et `ProcessFinder` (`FindProcessByConnection(...) int`).
- Détail complet (champs, comportements, erreurs observables dans le code) : voir `docs/LIBXRAY_API_REFERENCE.md`.

## 5. Build Android & ABIs (stratégie officielle)

### 5.1 Script officiel

- Commande : `python3 build/main.py android` (optionnel `local`, `--no-snapshot`) — README libXray.
- Implémentation : `build/app/android.py` utilise **gomobile bind** :
  - `gomobile bind -target android -androidapi <API> -trimpath -ldflags=... -extldflags=...`
  - `LIBXRAY_ANDROID_API` : niveau API Android (défaut **21**), aussi minSdk de l’AAR selon README ; ajuste `-Wl,-z,max-page-size=16384` et packing relocs selon API.
- `build/app/build.py` : gestion snapshot `go.mod/go.sum`, `go mod init/tidy`, `go get` Xray-core à `DEFAULT_XRAY_CORE_VERSION` (sans `local`), `download_geo`, préparation gomobile (`LIBXRAY_GOMOBILE_VERSION`, défaut `latest` résolu).

### 5.2 ABIs

- **Non explicitement listées** comme `-target android/arm64,...` dans `android.py` (cible passée : `android`). Les ABIs effectivement produites dépendent du comportement de `gomobile bind` et de la toolchain utilisée au moment du build.
- **Preuve requise (non réalisée dans cette phase)** : inspection du contenu de `libxray-android.zip` / `libXray.aar` (répertoires `jni/<ABI>/libgojni.so` ou équivalents) pour un artefact **vérifié** (checksum release : `libxray-android.zip` sha256 `1b1aa12124a7112af983a6cea74c77f27aadc6b650acf3c5a57019619ba9fa48`, taille 52573017, release v26.11.9).

### 5.3 Workflow CI officiel (référence)

`.github/workflows/build.yml` (libXray, tag `v*`) : job Android sur `ubuntu-24.04`, `setup-java`, `android-actions/setup-android@v4`, NDK via `nttld/setup-ndk@v1` avec `NDK_VERSION: r30`, build `python3 build/main.py android`, packaging AAR + sources jar, publication release.

### 5.4 Alignement 16 Ko / contraintes mémoire

`android.py` ajoute `-extldflags=-Wl,-z,max-page-size=16384` (et options relr/pack selon API) — élément pertinent pour la compatibilité d’alignement des pages sur Android récents (à valider sur les binaires produits).

## 6. Compatibilité projet ONFAA TUNNEL (état actuel)

- Projet Android : AGP 8.7.3, Gradle 8.10.2, `compileSdk/targetSdk 35`, `minSdk 26`, JDK 17.
- **Consommation AAR libXray** : compatible en principe avec un module Gradle Android standard si l’AAR cible un minSdk ≤ 26 ; la version exacte de l’API gomobile (`LIBXRAY_ANDROID_API`, défaut 21) et les ABIs incluses doivent être confirmées sur l’artefact retenu.
- **NDK** : requis pour le build gomobile Android (workflow officiel : r30). L’application ONFAA TUNNEL n’embarque pas encore de code NDK propre ; l’intégration consistera à consommer l’AAR/librairies produites.

## 7. Reproductibilité et vérification (stratégie recommandée)

1. **Épingler** : tag/commit libXray (ex. `v26.11.9` / `5a0f7d8d...`), pseudo-version Xray-core correspondante, version Go (≥ `go.mod`), version gomobile (`LIBXRAY_GOMOBILE_VERSION`), `LIBXRAY_ANDROID_API`, NDK (ex. r30), JDK.
2. **Vérifier les sources** : checkout par tag/commit, contrôle des hashes git, revue des diffs avant build.
3. **Vérifier les artefacts téléchargés** (si utilisés) : checksum SHA256 publié par la release GitHub (ex. `libxray-android.zip`) avant extraction/utilisation.
4. **Build en CI** : exécuter le script officiel `build/main.py android` dans un job dédié, conserver journaux, publier AAR/jar comme artefacts de build.
5. **Contrôles post-build** : lister ABIs présentes (`unzip -l` sur AAR / inspection `jni/`), vérifier présence des symboles/classes attendus, tailles, et (si applicable) alignement ELF (`readelf -l` pour `LOAD` align 0x4000) — à automatiser en Phase 2+.
6. **Interdits** : téléchargements non vérifiés, `|| true`, modification silencieuse des sources upstream.

## 8. Preuve de concept (Phase 2)

- **Non engagée dans le code applicatif** dans cette itération : aucune dépendance libXray ajoutée à `:app`, aucun artefact binaire commité, aucun changement de comportement runtime.
- **Raisons** : garantir une intégration **vérifiée et reproductible** (checksum/téléchargement contrôlé ou build from source en CI) sans introduire de binaire non vérifié ni de complexité CI non validée dans le même commit ; l’accès aux logs CI détaillés n’est pas disponible sans authentification dans cet environnement.
- **Prochaine étape PoC propre** : ajouter un job CI qui (a) télécharge `libxray-android.zip` de la release épinglée et vérifie le SHA256, ou (b) reconstruit via script officiel sur tag épinglé, puis publie l’AAR et un rapport d’inspection (ABIs, fichiers). L’intégration Gradle/Kotlin (compilation avec API réelle) pourra suivre une fois ce pipeline vérifié.

## 9. Limites et points non résolus

- **ABIs exactes de l’AAR libXray v26.11.9** : non confirmées par inspection d’artefact dans cette phase (téléchargement volumineux non réalisé localement) ; à prouver par inspection automatisée en CI.
- **`arm64-v8a` / `armeabi-v7a` / `x86_64`** : objectifs typiques Android ; la prise en charge effective dépend de l’artefact construit (gomobile/toolchain). Ne pas déclarer support sans preuve.
- **Signatures d’appel Kotlin/JVM** générées par gomobile pour l’AAR : non extraites ici (nécessite inspection de l’AAR/classes) ; l’API contractuelle vérifiée est l’interface `Invoke` JSON + fonctions Android additionnelles dans `android_wrapper.go`.
- **Statistiques trafic** : non exposées comme méthode dédiée dans `invoke_model.go` ; à étudier via capacités Xray/stats API dans les phases ultérieures si applicable.
- **DNS/protection sockets** : primitives Android disponibles (`SetDNS`, `ResetDNS`, contrôleurs dialer/listener, `RegisterProcessFinder`) mais intégration réelle dans `VpnService` réservée aux phases ultérieures.

## 10. Références (URLs)

- Xray-core repo : https://github.com/XTLS/Xray-core
- Xray-core release v26.11.9 : https://github.com/XTLS/Xray-core/releases/tag/v26.11.9
- libXray repo : https://github.com/XTLS/libXray
- libXray release v26.11.9 : https://github.com/XTLS/libXray/releases/tag/v26.11.9
- libXray README (tag) : https://raw.githubusercontent.com/XTLS/libXray/v26.11.9/README.md
- libXray build script : https://github.com/XTLS/libXray/blob/v26.11.9/build/main.py
- libXray Android build : https://github.com/XTLS/libXray/blob/v26.11.9/build/app/android.py
- libXray CI build : https://github.com/XTLS/libXray/blob/v26.11.9/.github/workflows/build.yml
- libXray invoke API : https://github.com/XTLS/libXray/blob/v26.11.9/invoke.go , https://github.com/XTLS/libXray/blob/v26.11.9/invoke_model.go
- libXray Android wrapper : https://github.com/XTLS/libXray/blob/v26.11.9/android_wrapper.go
- libXray CGo bridge : https://github.com/XTLS/libXray/blob/v26.11.9/cgo_bridge/main.go

# Preuve de concept Android — intégration libXray officielle (Phase 2B)

> **Statut : PoC terminée et validée en CI le 2026-11-16**, y compris l’exécution des tests instrumentés réels sur émulateur Android x86_64 (API 35) : chargement du runtime natif Go, appel réel de `invoke`, `xrayVersion`, requêtes invalides et erreurs gérées sans crash. Aucune connexion VPN n’est démarrée dans cette phase, aucune statistique n’est simulée. Aucun fork ni binaire non vérifié n’est utilisé : l’AAR est construit depuis les sources officielles épinglées.

## 1. Versions upstream épinglées (inchangées)

| Composant | Tag | Commit | Licence |
|---|---|---|---|
| Xray-core (XTLS/Xray-core) | `v26.11.9` | `676eaf66f8f05d4d558ee7a74b4736623d3feeb9` | MPL-2.0 |
| libXray (XTLS/libXray) | `v26.11.9` | `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3` | MIT |

Dépendance Go épinglée par libXray : `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0`.

Le commit libXray effectivement checkouté en CI a été vérifié par `git rev-parse HEAD` :

```
5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3 build: pack Android relocations by LIBXRAY_ANDROID_API (#172)
```

## 2. Méthode de compilation (reproductible, CI)

Workflow : `.github/workflows/libxray-poc.yml`

| Outil | Version/pin |
|---|---|
| Python | 3.x (`actions/setup-python@v5`) |
| Go | **1.26.1** (`actions/setup-go@v5`, `GOTOOLCHAIN=auto`) |
| gomobile | `LIBXRAY_GOMOBILE_VERSION=v0.0.0-20260217195705-b56b3793a9c4` (résolu par le script officiel) |
| JDK | Temurin **17** |
| Android SDK | SDK préinstallé du runner `ubuntu-24.04` |
| Android NDK | package officiel `ndk;30.0.16248370` (NDK **r30** stable) installé via `sdkmanager` |
| API Android cible du binding | `LIBXRAY_ANDROID_API=21` |

Étapes réelles :

1. Checkout ONFAA TUNNEL.
2. Installation des outils épinglés ci-dessus.
3. Clone des sources officielles libXray, checkout du commit exact, vérification stricte de `git rev-parse HEAD`.
4. Build via le script officiel : `python3 build/main.py android` avec `ANDROID_NDK_HOME` pointant sur `.../ndk/30.0.16248370`.
5. Calcul SHA-256 et inspection de l’AAR, copie dans `app/libs/libXray.aar`.
6. Vérifications bloquantes : ABIs présentes, architecture ELF par ABI, alignement 16 KiB des segments `LOAD`, signature JVM `invoke(String):String`.
7. Build/tests app : `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, puis `:app:connectedDebugAndroidTest` sur émulateur x86_64 API 35.
8. Publication des rapports et artefacts.

> Les sources upstream ne sont pas modifiées. Aucun AAR précompilé non vérifié n’est utilisé : l’artefact est construit à partir du commit officiel épinglé. L’**unique runtime Go** est celui embarqué dans l’AAR libXray (`libgojni.so`) ; aucun autre runtime Go n’est ajouté au processus.

## 3. Inspections réelles de l’artefact

Exécutées par `scripts/inspect_libxray_aar.sh` en CI, résultats ci-dessous issus du run `43965698249`.

### 3.1 Identité de l’AAR

| Élément | Valeur vérifiée |
|---|---|
| Fichier produit | `libXray.aar` |
| Taille | **53 200 280 octets** |
| SHA-256 | **`1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`** |
| Copie intégrée | `app/libs/libXray.aar` (même SHA-256, revérifié après copie) |

### 3.2 ABIs réellement présentes

```
arm64-v8a
armeabi-v7a
x86
x86_64
```

Bibliothèque native unique par ABI : `jni/<ABI>/libgojni.so`.
Aucune autre ABI n’est annoncée (aucune ABI supposée).

### 3.3 Architectures ELF (vérifiées par `readelf -h`)

| ABI | Classe | Machine | Résultat |
|---|---|---|---|
| `arm64-v8a` | ELF64 | AArch64 | OK |
| `armeabi-v7a` | ELF32 | ARM | OK |
| `x86` | ELF32 | Intel 80386 | OK |
| `x86_64` | ELF64 | Advanced Micro Devices X86-64 | OK |

### 3.4 Alignement mémoire 16 KiB (vérifié par `readelf -lW`)

Tous les segments `LOAD` des quatre `libgojni.so` ont `align=0x4000` (16 KiB) : **12/12 segments OK**, marqueur final `ALL_LOAD_ALIGNED_16K`.

### 3.5 Dépendances natives (`NEEDED`)

Pour les quatre ABIs, uniquement des bibliothèques système Android :

```
liblog.so, libandroid.so, libdl.so, libc.so
```

Aucune dépendance native inattendue.

### 3.6 API JVM réellement exposée

`classes.jar` extrait de l’AAR inspecté avec `jar tf` et `javap -public`. Classe d’entrée réelle :

```
Compiled from "LibXray.java"
public abstract class libXray.LibXray {
  public static final long LibXrayAPIVersion;
  public static void touch();
  public static native java.lang.String invoke(java.lang.String);
  public static native void registerDialerController(libXray.DialerController);
  public static native void registerListenerController(libXray.DialerController);
  public static native void registerProcessFinder(libXray.ProcessFinder, long);
  public static native void resetDNS();
  public static native void setDNS(libXray.DialerController, java.lang.String) throws java.lang.Exception;
}
```

Points confirmés :

- Signature JVM réelle : **`public static native java.lang.String invoke(java.lang.String)`** (méthode **statique**, nom **`invoke`** en minuscule, classe `libXray.LibXray`).
- La fonction Go `Invoke(string) string` n’est donc pas exposée telle quelle : le binding gomobile renomme et génère une méthode statique native.
- Les modèles (`LibXrayInvokeRequest`, `XrayVersionResponse`, `TestXrayRequest`, etc.) sont présents dans le package JVM `libXray`.
- Le chargement du runtime Go est assuré par l’initialisation statique de la classe générée (helpers `go.Seq`, `touch()`), pas par un `System.loadLibrary` manuel côté app.

## 4. Intégration Android minimale

- **Dépendance** : `app/libs/*.aar` via `fileTree` dans `app/build.gradle.kts` (AAR ignoré par Git, injecté par la CI après vérification).
- **Bridge** : `com.onfaa.tunnel.core.xray.XrayBridge`
  - résout la méthode réelle `invoke(String):String` par réflexion sur la classe générée (`libXray.LibXray`) ;
  - force l’initialisation de la classe pour charger `libgojni.so` (`loadNativeLibrary()`) ;
  - construit des requêtes conformes à `apiVersion=3` : `{"apiVersion":3,"method":"…","payload":{…}}` ;
  - décode les réponses `{success,data,error}` ;
  - limite les requêtes à 16 MiB (limite officielle libXray) ;
  - exécute les appels réels hors thread UI (`Dispatchers.Default`) ;
  - **ne démarre aucun tunnel**, n’affiche aucun état « connecté », ne produit aucune statistique simulée.

## 5. Tests exécutés

### 5.1 Tests unitaires JVM

`:app:testDebugUnitTest` — **succès** (tests existants du domaine, sans natif).

### 5.2 Tests instrumentés réels (artefact réel, émulateur)

Exécutés par `:app:connectedDebugAndroidTest` sur **émulateur `emulator-5554`, API 35, x86_64**. Résultat XML :

```
tests="6" failures="0" errors="0" skipped="0"
```

| Test (classe `XrayBridgeInstrumentedTest`) | Résultat |
|---|---|
| `nativeLibraryAndInvokeApiAvailable` — chargement effectif du runtime natif + API `invoke` | **PASS** |
| `xrayVersionReturnsValidJsonAndSuccess` — appel réel `xrayVersion`, JSON valide, `success=true`, version non vide | **PASS** |
| `invalidRequestIsHandledAsErrorResponse` — requête sans méthode → erreur structurée, pas de crash | **PASS** |
| `unknownMethodReturnsStructuredError` — méthode inconnue → erreur structurée | **PASS** |
| `testXrayRejectsInvalidConfigWithoutCrash` — configuration Xray invalide → échec géré, pas de crash | **PASS** |

Le test `ExampleInstrumentedTest.useAppContext` (template) passe également. Total : **6 tests, 0 échec, 0 ignoré**.

## 6. Résultats CI et artefacts

| Élément | Résultat |
|---|---|
| Run final | [libXray Android PoC (Phase 2B) #43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249) — `completed / success` |
| Commit ONFAA validé | `f116442021b4e80508e3ef2448d21980f2207534` |
| Commit libXray vérifié | `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3` |
| Go / NDK | `go1.26.1 linux/amd64` / `30.0.16248370` |
| SHA-256 AAR | `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917` |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| ELF / 16 KiB / NEEDED | OK (voir §3) |
| Signature JVM | `libXray.LibXray#invoke(java.lang.String):java.lang.String` (static native) |
| `:app:testDebugUnitTest` | succès |
| `:app:assembleDebug` (avec AAR intégré) | succès |
| `:app:assembleDebugAndroidTest` | succès |
| Tests instrumentés émulateur | **6/6 succès, 0 ignoré** |
| Artefact CI | [`libxray-poc-report` id `14276047955`](https://api.github.com/repos/dems469-lab/ONFAA-TUNNEL-/actions/artifacts/14276047955/zip), 122 092 534 octets (AAR, APK debug, APK androidTest, rapports, `javap`, ELF, résultats de tests) |
| APK debug | produit avec l’AAR réellement intégré (`app/build/outputs/apk/debug/`) |
| APK androidTest | produit (`app/build/outputs/apk/androidTest/debug/`) |

## 7. Limitations et points non couverts

- Les tests instrumentés ont été exécutés sur **émulateur x86_64 API 35**, pas sur appareil physique arm64/armv7 : le chargement natif est prouvé pour x86_64 ; les binaires arm sont présents et vérifiés structurellement (ELF/16 KiB) mais non exécutés dans cette CI.
- Aucun tunnel VPN, aucun `VpnService`, aucun routage TUN : hors périmètre Phase 2B.
- Aucune statistique de trafic, aucun état de connexion simulé.
- La version Xray retournée est vérifiée non vide et le JSON est valide ; la valeur exacte n’est pas figée dans ce document.
- L’inspection dépend de l’artefact CI `libxray-poc-report` pour les rapports complets (`javap.txt`, `elf.txt`, etc.) ; les éléments clés sont reproduits ci-dessus et dans la page *Summary* du run.

## 8. Références

- `docs/XRAY_UPSTREAM_AUDIT.md`
- `docs/LIBXRAY_API_REFERENCE.md`
- `.github/workflows/libxray-poc.yml`
- `scripts/inspect_libxray_aar.sh`
- `app/src/main/java/com/onfaa/tunnel/core/xray/XrayBridge.kt`
- `app/src/androidTest/java/com/onfaa/tunnel/core/xray/XrayBridgeInstrumentedTest.kt`

# Preuve de concept Android — intégration libXray officielle (Phase 2B)

> **Statut** : **PoC exécutée avec succès en CI** (build officiel, inspection, intégration, compilation/tests possibles). Les preuves détaillées (SHA-256 de l’AAR, liste des ABIs, signatures JVM, segments ELF/LOAD) sont générées dans l’artefact `libxray-poc-report` du run CI (téléchargement via interface GitHub). Aucune connexion VPN réelle n’est démarrée et aucune statistique fictive n’est affichée. Les tests instrumentés réels sont **compilés** mais **non exécutés** sur appareil/émulateur dans ce workflow.

## 1. Versions upstream épinglées (inchangées)

| Composant | Tag | Commit | Licence |
|---|---|---|---|
| Xray-core (XTLS/Xray-core) | `v26.11.9` | `676eaf66f8f05d4d558ee7a74b4736623d3feeb9` | MPL-2.0 |
| libXray (XTLS/libXray) | `v26.11.9` | `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3` | MIT |

Dépendance Go épinglée par libXray : `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0`.

## 2. Méthode de compilation (reproductible, CI)

Workflow : `.github/workflows/libxray-poc.yml`

1. Checkout ONFAA TUNNEL.
2. Installation outils épinglés : Python 3.x, Go **1.26.1** (`actions/setup-go`, `GOTOOLCHAIN=auto`), JDK 17 (Temurin), Android SDK, Android NDK **r30**.
3. Clone des sources officielles libXray, checkout du commit exact et vérification (`git rev-parse HEAD == LIBXRAY_COMMIT`).
4. Build via script officiel : `python3 build/main.py android` (dans `/tmp/libXray`) avec `ANDROID_NDK_HOME` pointant vers NDK r30, `LIBXRAY_ANDROID_API=21`, `LIBXRAY_GOMOBILE_VERSION=v0.0.0-20260217195705-b56b3793a9c4`.
5. Calcul SHA-256 de l’AAR, inspection du contenu, copie dans `app/libs/libXray.aar`.
6. Vérifications : présence d’ABIs (`jni/`) et présence de signatures `invoke` dans les classes JVM générées.
7. Tests/compilation app : `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`.
8. Publication des rapports et artefacts (AAR, APKs, rapports d’inspection).

> Les sources upstream ne sont pas modifiées silencieusement (checkout du commit officiel). Aucun téléchargement d’AAR précompilé non vérifié n’est utilisé ; la construction part des sources officielles épinglées.

## 3. Intégration Android minimale

- **Dépendance** : `app/libs/*.aar` ajouté via `fileTree` dans `app/build.gradle.kts`.
- **Bridge** : `com.onfaa.tunnel.core.xray.XrayBridge`
  - Résolution de l’API réelle `Invoke(String):String` par réflexion (compatible avec le nommage réel généré par gomobile).
  - Construction de requêtes JSON conformes à `apiVersion=3`.
  - Décodage des réponses `{success,data,error}`.
  - Exécution des appels via `Dispatchers.Default` (pas d’appel bloquant UI prévu).
  - **Aucun** démarrage de tunnel VPN, aucune simulation de connexion/statistiques.
- **Contrainte runtime Go unique** : respectée côté conception (une seule bibliothèque Go/native intégrée via l’AAR libXray ; aucune autre runtime Go indépendant ajouté).

## 4. Tests de preuve (code ajouté)

**Instrumentés (appareil/émulateur requis, artefact réel)** : `app/src/androidTest/java/com/onfaa/tunnel/core/xray/XrayBridgeInstrumentedTest.kt`
- Disponibilité de l’API `Invoke` (résolution de signature).
- Appel réel `xrayVersion` (JSON valide, `success=true`, version non vide).
- Requête invalide → réponse d’erreur structurée (sans crash).
- Méthode inconnue → erreur structurée.
- `testXray` avec configuration invalide → échec géré (sans crash).

> Si l’AAR n’est pas bundlé, les tests instrumentés sont ignorés via `Assume` (à ne pas interpréter comme succès de la preuve). L’exécution sur émulateur/appareil réel n’est pas automatisée dans ce workflow ; les APKs de test sont compilés et publiés comme preuve de compilabilité.

**Unitaires (JVM, sans natif)** : tests existants conservés et exécutés en CI (`:app:testDebugUnitTest`).

## 5. Inspection AAR (script)

`scripts/inspect_libxray_aar.sh` produit dans le rapport :
- SHA-256 et taille de l’AAR, listing du contenu.
- Liste des ABIs (`jni/<ABI>/`) et fichiers natifs.
- Informations ELF / segments LOAD (`readelf`) pour analyse d’alignement (16 Ko à vérifier sur les `LOAD`).
- Extraction de `classes.jar`, `javap -public` et correspondances `invoke`.

## 6. Résultats d’exécution CI (réels, vérifiés)

| Élément | Résultat |
|---|---|
| Workflow / run URL | [libXray Android PoC (Phase 2B) #43959959254](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43959959254) — `completed / success` |
| Commit ONFAA (validé par ce run) | `f567fd773746803abb691d14492bbe257bc09814` — *Phase 2B: fix PoC workflow Android SDK setup on CI runner* |
| Commit libXray vérifié (checkout CI) | `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3` (étape “Clone and verify libXray pinned commit” réussie) |
| SHA-256 `libXray.aar` | Calculé et enregistré dans `poc-report/aar.sha256` (artefact CI `libxray-poc-report`) |
| ABIs détectées | Extraites dans `poc-report/abis.txt` ; étape “Verify AAR contains native libs and Invoke binding” réussie (ABIs présentes) |
| Inspection ELF / alignement 16 Ko | Générée dans `poc-report/elf.txt` et `poc-report/load_segments.txt` (interprétation détaillée via artefact) |
| Signatures JVM `Invoke` | `classes.jar` inspecté (`javap -public`), correspondances dans `poc-report/invoke_matches.txt` ; étape de vérification `invoke` réussie |
| `:app:testDebugUnitTest` | **Succès** |
| `:app:assembleDebug` (avec `app/libs/libXray.aar` intégré) | **Succès** |
| `:app:assembleDebugAndroidTest` (tests réels compilés) | **Succès** |
| APK debug / artefacts publiés | Inclus dans l’artefact CI [`libxray-poc-report` (14273602500)](https://api.github.com/repos/dems469-lab/ONFAA-TUNNEL-/actions/artifacts/14273602500/zip) (téléchargement via interface GitHub) |
| Tests instrumentés exécutés sur appareil/émulateur | **Non exécutés** (pas d’émulateur/appareil provisionné dans le workflow) ; code de test présent et compilé |

**Preuves clés apportées par la CI** : build officiel `python3 build/main.py android` depuis sources épinglées, vérification du commit libXray, production de l’AAR, inspection automatisée, intégration dans `:app`, compilation Android réussie avec dépendance réelle, et exécution des tests unitaires JVM du module. **Limite restante** : exécution runtime des tests instrumentés contre le binaire natif sur appareil/émulateur non réalisée.

## 7. Limitations et points à valider

- Exécution réelle des tests instrumentés (`xrayVersion`, gestion d’erreurs, chargement natif) requiert un appareil/émulateur Android avec l’APK de test et l’APK debug intégrant l’AAR.
- Les ABIs et l’alignement 16 Ko doivent être interprétés à partir du rapport ELF/LOAD produit par la CI.
- La version Go utilisée en CI (1.26.1 + `GOTOOLCHAIN=auto`) est choisie pour reproductibilité de l’environnement runner ; la compatibilité effective avec `go.mod` upstream doit être confirmée par le succès du build.
- Aucune intégration de `VpnService`/tunnel ; aucune API de stats/connexion exposée à l’UI.

## 8. Références

- `docs/XRAY_UPSTREAM_AUDIT.md`
- `docs/LIBXRAY_API_REFERENCE.md`
- `.github/workflows/libxray-poc.yml`
- `scripts/inspect_libxray_aar.sh`
- `app/src/main/java/com/onfaa/tunnel/core/xray/XrayBridge.kt`

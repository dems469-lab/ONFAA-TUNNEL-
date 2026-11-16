# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 2 — Audit et intégration upstream Xray-core/libXray — **TERMINÉE**
- **État :** **Phase 2 complète et démontrée en CI**, y compris l’exécution réelle de l’API native sur émulateur Android x86_64 (API 35) : AAR officiel construit depuis le commit épinglé, inspecté (SHA-256, ABIs, ELF, 16 KiB, `javap`), intégré à `:app`, APK compilé, et **6/6 tests instrumentés réussis sans aucun test ignoré** (`invoke` réel, `xrayVersion`, requêtes invalides, erreurs, absence de crash). Aucun tunnel VPN n’a été démarré.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit de code validé (distant)** : `f116442021b4e80508e3ef2448d21980f2207534` — *Phase 2B: force native runtime load in instrumented test*
- **Dernier run CI Android réussi connu (Phase 1)** : [#43954661801](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43954661801) (commit `22551de`, `success`, artefact `onfaa-tunnel-debug-apk`)

## Travaux réalisés (Phase 2 — audit/préparation)

- Inspection du dépôt ONFAA TUNNEL et de l’état Phase 1.
- Audit des dépôts officiels **XTLS/Xray-core** et **XTLS/libXray** (releases/tags/commits, licences, go.mod, build scripts, API invoke, wrappers Android, workflows CI).
- Sélection documentée (alignée sur les dernières releases stables examinées) :
  - **Xray-core** `v26.11.9`, commit tag **`676eaf66f8f05d4d558ee7a74b4736623d3feeb9`** (MPL-2.0), `go 1.27`
  - **libXray** `v26.11.9`, tag annoté commit **`5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`** (MIT), `go 1.27.3`, dépend `github.com/xtls/xray-core v1.260327.1-0.20261109052709-676eaf66f8f0`
- Documentation créée : `docs/XRAY_UPSTREAM_AUDIT.md`, `docs/LIBXRAY_API_REFERENCE.md` (méthodes/API vérifiées uniquement, sans signatures inventées).
- Mise à jour de la documentation projet (README, ARCHITECTURE, DECISIONS, ROADMAP, BUILD_GUIDE, TEST_PLAN, CHANGELOG).

## Vérifications effectuées (Phase 2)

- **Origine upstream** : dépôts officiels XTLS confirmés ; tags/releases/commits récupérés via GitHub API et fichiers bruts aux tags.
- **Compatibilité libXray/Xray-core** : alignement vérifié par dépendance Go/pseudo-version et documentation libXray pour v26.11.9.
- **API** : `Invoke` (apiVersion 3), méthodes listées, cycle de vie `RunXray/StopXray/TestXray/GetXrayState/XrayVersion`, exports CGo, fonctions Android (`SetDNS`, `ResetDNS`, contrôleurs, `RegisterProcessFinder`) vérifiés dans le code source.
- **Build Android officiel** : script `python3 build/main.py android` (gomobile), paramètres/NDK observés dans `build.yml` (NDK r30) ; checksum release `libxray-android.zip` relevé.

## Points vérifiés (Phase 2B) / limites

**Vérifiés par preuves CI (run `43965698249`, commit `f116442`)** :

- AAR officiel `libXray.aar` construit depuis `5a0f7d8d…` via `python3 build/main.py android` : **53 200 280 octets**, SHA-256 `1655655c5dce1d45d2c065cf8db5373661badb4f0721a21db32e82d3d943d917`.
- ABIs réellement présentes : `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (`libgojni.so` par ABI).
- Architectures ELF correctes pour les 4 ABIs ; **tous les segments `LOAD` alignés 16 KiB (`0x4000`)** ; dépendances `NEEDED` limitées à `liblog/libandroid/libdl/libc`.
- API JVM réelle : `libXray.LibXray`, `public static native java.lang.String invoke(java.lang.String)`.
- Intégration `:app` compilée : `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` en succès.
- Tests instrumentés réels sur émulateur x86_64 API 35 : **6 tests, 0 échec, 0 ignoré**, dont chargement natif, `xrayVersion`, requête invalide, méthode inconnue et `testXray` invalide.

**Limites résiduelles** :

- Exécution native prouvée sur **x86_64 émulé** uniquement ; les binaires `arm64-v8a`/`armeabi-v7a` sont présents et vérifiés structurellement mais non exécutés sur appareil physique.
- Aucun tunnel VPN, `VpnService` ou routage TUN (hors périmètre Phase 2B).
- La valeur exacte de version retournée par `xrayVersion` n’est pas figée dans la doc (seul le contrat JSON, la non-nullité et le succès sont testés).

## Blocages / risques

- Validation du build libXray dépend de GitHub Actions (pas d’environnement Go/gomobile/NDK local sur le téléphone) ; le workflow est désormais reproductible et épinglé.
- Risque d’intégration multi-runtime Go si d’autres bibliothèques Go étaient ajoutées (contrainte libXray : un seul runtime Go par processus) — aucune autre runtime Go n’est présente.
- Les tests natifs n’ont pas été exécutés sur appareil physique arm64 ; à prévoir en Phase 3.

## Phase 2B — PoC d’intégration (TERMINÉE, validée en CI)

- **Commit de code validé** : `f116442021b4e80508e3ef2448d21980f2207534` — *Phase 2B: force native runtime load in instrumented test*
- **Run CI final réussi** : [libXray Android PoC (Phase 2B) #43965698249](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43965698249) (`completed/success`, toutes les étapes).
- **Preuves** : vérification du commit libXray `5a0f7d8d…`, build officiel (Go 1.26.1, NDK r30 `30.0.16248370`, gomobile épinglé), SHA-256/ABIs/ELF/16 KiB/`javap` conformes, AAR intégré, APKs debug + androidTest produits, tests unitaires et **6/6 tests instrumentés réels réussis sur émulateur x86_64 API 35**.
- **Artefact** : `libxray-poc-report` (id `14276047955`, 122 092 534 octets).
- **Détail complet** : `docs/LIBXRAY_ANDROID_POC.md`.

## Prochaine tâche

- **Phase 3 — Connexion VPN réelle** : concevoir `VpnService` + orchestration du moteur Xray réel (permissions, cycle de vie, états, erreurs), sans simulation, en s’appuyant sur l’API vérifiée et en respectant le runtime Go unique. Prévoir des tests sur appareil physique arm64 avant toute annonce de connexion fonctionnelle.

## Instructions de reprise pour une autre IA

1. Lire `STATUS.md`, `docs/LIBXRAY_ANDROID_POC.md`, `docs/XRAY_UPSTREAM_AUDIT.md`, `docs/LIBXRAY_API_REFERENCE.md`, `docs/DECISIONS.md`, `docs/BUILD_GUIDE.md`.
2. La Phase 2 est clôturée : l’intégration réelle est prouvée. Ne pas régresser vers un mock/fork/sing-box ; conserver les versions et le SHA-256 documentés. Toute modification de l’intégration doit repasser le workflow `libxray-poc.yml`.
3. Phase 3 seulement : aucun tunnel réel ne doit être annoncé « connecté » sans preuve observable sur appareil ; conserver la traçabilité CI et vérifier le commit distant après chaque push.

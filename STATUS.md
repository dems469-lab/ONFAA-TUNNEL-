# STATUS — ONFAA TUNNEL

## État de phase

- **Phase actuelle :** 2 — Audit technique et préparation de l'intégration upstream Xray-core
- **État :** **substantiellement validée par PoC CI (build/intégration/compilation prouvés), avec une limite résiduelle** — artefact libXray officiel construit depuis sources épinglées, inspecté et intégré, compilation/tests possibles réussis en CI avec APK produit ; **tests instrumentés réels non exécutés sur appareil/émulateur** (compilés uniquement). La Phase 2 peut être considérée comme **clôturée sur le plan de l’intégration technique démontrée en CI**, sous réserve de cette limitation explicitement documentée.

## Références dépôt

- **Dépôt :** https://github.com/dems469-lab/ONFAA-TUNNEL-
- **Branche principale :** `main`
- **Dernier commit publié (distant, vérifié avant travail Phase 2)** : `992b51b85ce46a6d5404932808b46a6bde3941b8` — *Phase 1: record successful CI validation and APK artifact*
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

## Points non vérifiés / non réalisés

- **Preuve de concept non engagée** : aucun artefact libXray téléchargé/vérifié/intégré dans l’app, aucune inspection d’ABIs `.so` réalisée, aucune compilation du projet Android avec dépendance libXray, aucune vérification d’accessibilité runtime de l’API depuis `:app`.
- **ABIs effectives de l’AAR libXray** (arm64-v8a, armeabi-v7a, x86_64, etc.) : non prouvées par inspection d’un artefact v26.11.9.
- **Bindings JVM exacts** générés par gomobile pour l’AAR : non extraits.
- **Matrice Go/gomobile/NDK “finale” de projet** : stratégie documentée (références upstream) mais non figée par un build reproductible exécuté dans ce dépôt.

## Blocages / risques

- Absence d’environnement Android/Go/gomobile local pour PoC ; validation dépend de GitHub Actions.
- Téléchargement/usage d’artefacts volumineux nécessite vérification SHA256 stricte et politique claire (commit binaire vs fetch CI).
- Risque d’intégration multi-runtime Go si d’autres bibliothèques Go étaient ajoutées (voir README libXray).
- Détails ABI/alignement 16 Ko à confirmer sur binaires réels.

## Phase 2B — PoC d’intégration (validée en CI)

- **Commit PoC publié** : `f567fd773746803abb691d14492bbe257bc09814` — *Phase 2B: fix PoC workflow Android SDK setup on CI runner*
- **Run CI réussi** : [libXray Android PoC (Phase 2B) #43959959254](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/43959959254) (`completed/success`)
- **Preuves CI** : checkout+vérification commit libXray `5a0f7d8d…`, build `python3 build/main.py android` (NDK r30, Go/gomobile épinglés dans workflow), génération+inspection AAR (SHA-256, ABIs, ELF/LOAD, `javap`/`invoke`), intégration dans `:app`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, artefact `libxray-poc-report`.
- **Limite non levée** : tests instrumentés réels (chargement natif, `xrayVersion` runtime, etc.) **non exécutés** sur appareil/émulateur (APK de test compilé uniquement).

## Prochaine tâche

- **Phase 3 — Connexion VPN réelle** (préparation contrôlée) : concevoir l’orchestration `VpnService` + moteur libXray/Xray-core réel, gestion permissions/cycle de vie/états/erreurs, en s’appuyant sur l’API et les contrôles Android vérifiés (sans simulation). Préalable recommandé : exécuter les tests instrumentés sur appareil/émulateur et consigner les résultats (SHA-256/ABIs/signatures exactes depuis l’artefact CI).

## Instructions de reprise pour une autre IA

1. Lire `STATUS.md`, `docs/XRAY_UPSTREAM_AUDIT.md`, `docs/LIBXRAY_API_REFERENCE.md`, `docs/DECISIONS.md`, `docs/BUILD_GUIDE.md`.
2. Ne pas démarrer de connexion VPN réelle en Phase 2 ; ne pas intégrer de binaire/fork non vérifié ; ne pas inventer de signatures libXray.
3. Toute intégration doit épingler tag/commit/SHA256 et conserver la traçabilité CI. Status: `main...origin/main` will change after commit/push — toujours vérifier distant et CI.

# ONFAA TUNNEL

ONFAA TUNNEL est un client VPN Android natif visant à fournir une connexion VPN réelle basée sur le cœur officiel **XTLS/Xray-core** (upstream : https://github.com/XTLS/Xray-core) via **libXray** (après vérification de compatibilité/API officielle).

> **État actuel : Phase 5 — lot 4 (filtrage par application, auto-connexion réelle, abonnements) — code livré, validation CI en cours.**  
> **Lot 3 validé par des runs verts** (commit `2f1d84e`) : les réglages « moteur » sont réellement appliqués au cœur Xray à la prochaine connexion (DNS/DoH, MTU, mux, fragmentation TLS, ad blocker, mode de routage, hotspot, logs, timeout/reconnexion). Le tunnel VPN réel fonctionne de bout en bout en CI pour **VLESS, VMess, Trojan et Shadowsocks**. Preuves : [Android CI #44225296961](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296961) (**succès**) et [E2E #44225296989](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44225296989) (**succès**, 34/34 instrumentés, `vless=3 vmess=3 trojan=3 shadowsocks=9`, artefact installable **`onfaa-tunnel-apk`**).  
> **Lot 4 (nouveau) :** filtrage par application **réel** (`VpnService.addAllowedApplication`/`addDisallowedApplication` + sélecteur d'applications dans Paramètres), auto-connexion réelle au lancement de l'application **et au démarrage du téléphone** (`BOOT_COMPLETED`, consentement VPN préalable exigé), et **abonnements** (import d'une URL http/https, base64 v2ray ou liste brute, chaque lien validé par le moteur avant d'être ajouté, rafraîchissement et suppression des sources enregistrées). Le mode gaming et le comportement en arrière-plan restent sans équivalent moteur (signalé honnêtement). **Correctif 0.7.1 :** la vérification de connectivité est désormais bornée dans le temps (une résolution DNS figée ne peut plus bloquer la connexion sans message) et entièrement journalisée (réseau VPN, chaque cible testée, chaque échec), avec repli sur plusieurs cibles HTTP neutres et repli IP sans DNS. Le DNS du moteur passe désormais **par le tunnel** par défaut (au lieu d'être résolu en direct), ce qui le rend fonctionnel sur les réseaux qui bloquent les résolveurs publics, et le bootstrap utilise le résolveur du réseau de l'appareil. Version : `0.7.1-lot4-probe-fix`.  
> Rapports : [`docs/PHASE_5_PROTOCOLS_REPORT.md`](docs/PHASE_5_PROTOCOLS_REPORT.md) et [`docs/PHASE_4_REPORT.md`](docs/PHASE_4_REPORT.md). Limites **non revendiquées** : appareil physique ARM64, changement d'IP publique, tunnel SOCKS5 de bout en bout.

## Objectifs

- Connexion VPN réelle sur Android (pas de simulation).
- Support des protocoles **VLESS**, **VMess**, **Trojan**, **Shadowsocks** et **SOCKS5** (moteur Xray-core).
- Importation et gestion de configurations personnelles.
- Gestion des serveurs et sélection active.
- Statistiques réseau réelles (débit, trafic, latence selon capacités).
- Réglages DNS, routage, journaux et diagnostics.
- Interface complète avec thèmes personnalisables.

## Technologies

- **Android natif**, **Kotlin** (plugin Compose), **AndroidX**, **Jetpack Compose** (Material 3), **Navigation Compose**
- **Gradle** avec catalogue de versions (`gradle/libs.versions.toml`), wrapper Gradle commité
- **Room v2** (profils persistés, migration `1 → 2` testée) et **DataStore Preferences** (sélection, préférences)
- **Android Keystore** + AES-256-GCM (`SecretCipher`) pour l’outbound moteur
- **Coroutines & Flow**, **JUnit 4** (unitaires), **AndroidX Test** (instrumentés)
- **XTLS/Xray-core v26.11.9** via **libXray v26.11.9** (AAR officiel construit en CI), `VpnService` réel

Versions épinglées et choix documentés dans [`docs/DECISIONS.md`](docs/DECISIONS.md) et [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md).

## État du dépôt

- Branche principale : `main`
- Package Android : `com.onfaa.tunnel` (`applicationId` identique)
- Suivi : [`STATUS.md`](STATUS.md), [`CHANGELOG.md`](CHANGELOG.md), [`docs/ROADMAP.md`](docs/ROADMAP.md)
- Séparation des responsabilités : [`docs/ANDROID_XRAY_SEPARATION.md`](docs/ANDROID_XRAY_SEPARATION.md)
- Audit upstream (Phase 2) : [`docs/XRAY_UPSTREAM_AUDIT.md`](docs/XRAY_UPSTREAM_AUDIT.md)
- Référence API libXray vérifiée : [`docs/LIBXRAY_API_REFERENCE.md`](docs/LIBXRAY_API_REFERENCE.md)
- Preuve de concept Android libXray (Phase 2B) : [`docs/LIBXRAY_ANDROID_POC.md`](docs/LIBXRAY_ANDROID_POC.md)
- Rapport Phase 3 (tunnel réel) : [`docs/PHASE_3_REPORT.md`](docs/PHASE_3_REPORT.md)
- Rapport Phase 4 (persistance, import, cycle de vie) : [`docs/PHASE_4_REPORT.md`](docs/PHASE_4_REPORT.md)
- Plan de parité inspiré de SNI Tunnel : [`docs/PHASE_5_PARITY_PLAN.md`](docs/PHASE_5_PARITY_PLAN.md)
- Rapport Phase 5 — lot 1 (protocoles) : [`docs/PHASE_5_PROTOCOLS_REPORT.md`](docs/PHASE_5_PROTOCOLS_REPORT.md)

## Structure du projet

```
.
├── app/                  # Module Android (sources, ressources, tests, manifeste)
├── gradle/               # Wrapper + libs.versions.toml
├── docs/                 # Documentation projet, architecture, build, sécurité, design
├── .github/workflows/    # CI Android (validation et build debug)
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew, gradlew.bat
```

## Fonctionnement du VPN (comment l'utiliser maintenant)

1. Onglet **Actions** de ce dépôt → workflow **libXray Android build and real VPN E2E** → run vert → artefact **`onfaa-tunnel-apk`** (c'est le seul APK **avec** le moteur ; celui d'Android CI, `onfaa-tunnel-debug-apk-no-engine`, n'ouvre pas de tunnel).
2. Installer l'APK sur le téléphone (Android ≥ 8.1), importer un lien `vless://`, `vmess://`, `trojan://`, `ss://` ou `socks://`, appuyer sur **Connexion** et accorder la permission VPN.
3. Les réglages de Paramètres (DNS/DoH, MTU, mux, fragmentation TLS, ad blocker, routage, hotspot, logs, reconnexion) sont appliqués au cœur Xray **à la prochaine connexion** ; un message du journal (« Options moteur appliquées… ») le confirme à chaque démarrage du tunnel.

## Compilation (GitHub Actions uniquement)

La validation et la compilation sont réalisées sur **GitHub Actions**. Deux workflows existent :

- `.github/workflows/android.yml` : tests unitaires, `assembleDebug`, compilation des tests instrumentés. Rapide, mais **sans moteur Xray** (l'AAR libXray est gitignoré) : son artefact `onfaa-tunnel-debug-apk-no-engine` ne peut pas ouvrir de tunnel.
- `.github/workflows/libxray-poc.yml` : construit l'AAR libXray épinglé, lance le tunnel réel sur émulateur et publie l'APK installable **`onfaa-tunnel-apk`**.

**Pour installer l'application, téléchargez `onfaa-tunnel-apk`** depuis un run vert du workflow `libXray Android build and real VPN E2E` (onglet Actions → run → Artifacts). Voir [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md) §« Quel APK installer ».

## Sécurité

Ne jamais commiter de configurations personnelles, UUID, clés, secrets ou tokens. Voir [`docs/SECURITY.md`](docs/SECURITY.md).

## Contribution

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md).

# ONFAA TUNNEL

ONFAA TUNNEL est un client VPN Android natif visant à fournir une connexion VPN réelle basée sur le cœur officiel **XTLS/Xray-core** (upstream : https://github.com/XTLS/Xray-core) via **libXray** (après vérification de compatibilité/API officielle).

> **État actuel : Phase 5 — lot 1 livré (Trojan, Shadowsocks, SOCKS5) sur le tunnel réel.**  
> Le tunnel VPN réel fonctionne de bout en bout en CI pour **VLESS, VMess, Trojan et Shadowsocks** avec le même moteur officiel XTLS/Xray-core v26.11.9 ; l’import accepte aussi les liens/JSON **SOCKS5** (conversion et validation moteur réelles, sans tunnel E2E revendiqué). Les profils restent persistés en Room v2 (outbound chiffré AES-256-GCM via Android Keystore), avec sélection DataStore, import lien/presse-papiers/fichier/JSON, cycle de vie à 9 états, journaux assainis et statistiques mesurées. Code : commit `5e2f0d4` ; CI :
> [Android CI #44019183046](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183046) (**succès**, 27 tests unitaires),
> [E2E #44019183061](https://github.com/dems469-lab/ONFAA-TUNNEL-/actions/runs/44019183061) (**succès**, 34/34 instrumentés, `vless=3 vmess=3 trojan=3 shadowsocks=9`).
> Rapports : [`docs/PHASE_5_PROTOCOLS_REPORT.md`](docs/PHASE_5_PROTOCOLS_REPORT.md) et [`docs/PHASE_4_REPORT.md`](docs/PHASE_4_REPORT.md). Limites **non revendiquées** : appareil physique ARM64, changement d’IP publique, tunnel SOCKS5 de bout en bout.

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

## Compilation (GitHub Actions uniquement)

La validation et la compilation sont réalisées sur **GitHub Actions** via `.github/workflows/android.yml` (JDK/SDK, licences, validation wrapper, tests unitaires, `assembleDebug`, publication de l’APK debug en artefact). Voir [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md).

## Sécurité

Ne jamais commiter de configurations personnelles, UUID, clés, secrets ou tokens. Voir [`docs/SECURITY.md`](docs/SECURITY.md).

## Contribution

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md).

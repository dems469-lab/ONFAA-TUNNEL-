# ONFAA TUNNEL

ONFAA TUNNEL est un client VPN Android natif visant à fournir une connexion VPN réelle basée sur le cœur officiel **XTLS/Xray-core** via **libXray**.

> **État actuel : Phase 0 — Initialisation GitHub (en cours de mise en place documentaire).**  
> Aucun moteur VPN, aucune connexion réelle et aucun binaire Xray ne sont encore intégrés à ce stade.

## Objectifs

- Connexion VPN réelle sur Android (pas de simulation).
- Support des protocoles **VLESS** et **VMess**.
- Importation et gestion de configurations personnelles.
- Gestion des serveurs et sélection active.
- Statistiques réseau réelles (débit, trafic, latence selon capacités).
- Réglages DNS, routage, journaux et diagnostics.
- Interface complète avec thèmes personnalisables.

## Technologies cibles

- **Android natif**, **Kotlin**
- **Android `VpnService`**
- **XTLS/Xray-core officiel** intégré via **libXray** (aucun remplacement par sing-box)
- **Room** (persistance locale), **DataStore** (préférences)
- **Coroutines & Flow** (asynchronisme et flux réactifs)

Les signatures exactes de l’API `libXray` ne sont pas inventées dans cette phase ; elles seront confirmées lors de l’étude et de l’intégration (Phase 2).

## État du dépôt

Ce dépôt GitHub est la **source centrale et permanente** du projet. Tout développement validé doit y être publié pour permettre la reprise du travail par un autre agent ou environnement.

- Branche principale : `main`
- Documentation de suivi : [`STATUS.md`](STATUS.md)
- Historique des changements : [`CHANGELOG.md`](CHANGELOG.md)
- Feuille de route : [`docs/ROADMAP.md`](docs/ROADMAP.md)

## Structure du projet (Phase 0)

```
.
├── README.md, STATUS.md, CHANGELOG.md, CONTRIBUTING.md, LICENSE
├── docs/                 # Contexte, architecture, roadmap, décisions, tests, build, sécurité, design
├── app/                  # Futur module/application Android (structure préparée)
├── gradle/               # Futurs éléments liés à Gradle/wrapper (structure préparée)
└── .github/              # Modèles et préparation CI/CD (workflows documentaires en Phase 0)
```

## Suivre le développement

1. Consulter [`STATUS.md`](STATUS.md) pour la phase actuelle, le dernier commit publié, les blocages et les instructions de reprise.
2. Consulter [`docs/ROADMAP.md`](docs/ROADMAP.md) pour les phases 0 à 9.
3. Consulter [`docs/DECISIONS.md`](docs/DECISIONS.md) pour les choix validés et ouverts.

## Compilation

La compilation sera réalisée **exclusivement via GitHub Actions** lorsqu’elle sera mise en place (prévue Phase 9). Aucun workflow de build Android trompeur n’est fourni en Phase 0 ; voir [`docs/BUILD_GUIDE.md`](docs/BUILD_GUIDE.md) et `.github/workflows/README.md`.

## Sécurité

Ne jamais commiter de configurations personnelles, UUID, clés, secrets ou tokens. Voir [`docs/SECURITY.md`](docs/SECURITY.md).

## Contribution

Voir [`CONTRIBUTING.md`](CONTRIBUTING.md) pour les règles de travail, de documentation et de publication sur GitHub.

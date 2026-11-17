# Plan de parité — inspiration SNI Tunnel

> **Statut : proposition, aucun code modifié.** Ce document compare les fonctions
> mises en avant par SNI Tunnel (client V2Ray/Xray/SSH) à l’état réel d’ONFAA TUNNEL
> au commit `203929c`, puis propose des lots exécutables dans les contraintes du
> projet : **moteur Xray-core officiel unique**, aucun mock, chaque protocole prouvé
> par un **tunnel réel** en CI, aucune donnée envoyée à un serveur tiers.

## 1. Matrice de parité

| Fonction | SNI Tunnel | ONFAA (203929c) | Décision |
| --- | --- | --- | --- |
| VLESS / VMess | oui | oui, prouvé E2E | conservé |
| Trojan | oui | non | **lot 1** |
| Shadowsocks AEAD / 2022 | oui | non | **lot 1** |
| SOCKS5 | oui | non | **lot 1** |
| Hysteria 2 (QUIC) | oui | non | **exclu** (moteur distinct) |
| SSH (mot de passe/clé) | oui | non | **exclu** (moteur distinct) |
| Import liens | vless/vmess/trojan/ss/hy2/sni-ssh | vless/vmess | lot 1 puis 2 |
| Import QR (caméra) | oui | non | **lot 2** |
| Abonnements HTTPS + quota/expiration | oui | non | **lot 2** |
| Liste groupée par abonnement | oui | non | lot 2 |
| Édition manuelle d’une config | oui | non | lot 5 |
| Split tunneling (apps incluses/exclues) | oui | non | **lot 3** |
| DNS / MTU / IPv6 / routage raccordés | oui | partiel (UI-only) | **lot 3** |
| Tuile Réglages rapides | oui | non | lot 4 |
| Widget | oui | non | lot 4 |
| Liens d’automatisation (`snitunnel://`) | oui | non | lot 4 |
| Connect on Demand | oui | non | lot 4 (limite Android) |
| Partage hotspot via proxy HTTP | oui | non | reporté |
| Thèmes personnalisés | oui | Phase 5 UI (parallèle) | hors lot |
| Partage cloud de configs | oui | non | **exclu** (backend) |
| Configs scellées non exportables | oui | non | exclu (pas de cloud) |
| Journaux locaux assainis | oui | oui | conservé |
| Statistiques réelles | oui | oui | conservé |
| Favoris / tri / suppression | oui | oui | conservé |
| Chiffrement lié à l’appareil | oui | oui (Keystore) | conservé |
| Aucune pub / analytics / compte | oui | oui | conservé |

## 2. Lots proposés

### Lot 1 — Protocoles Xray : Trojan, Shadowsocks, SOCKS5

Objectif : trois protocoles de plus, **dans le moteur officiel déjà épinglé**, sans
toucher à l’intégration validée VLESS/VMess.

Changements prévus :

- `domain/model/ServerConfig.kt` : `TunnelProtocol` + `TROJAN`, `SHADOWSOCKS`,
  `SOCKS` ; libellé d’affichage.
- `core/xray/ConfigImporter.kt` :
  - liens acceptés `trojan://` et `ss://` (conversion libXray, puis validation
    moteur) ; `socks://` reste JSON-only (pas de format de lien standard) ;
  - validation des identifiants **par protocole** : `id` (VLESS/VMess),
    `password` (Trojan), `method` + `password` (Shadowsocks), authentification
    optionnelle (SOCKS) ;
  - normalisation des formes v2ray `settings.servers[0]` (Shadowsocks/SOCKS) en plus
    de `settings.vnext[0]` (VMess) ;
  - messages d’erreur dédiés, sans secret.
- `core/xray/XrayConfigBuilder.kt` : ensemble de protocoles supportés au lieu de la
  liste VLESS/VMess codée en dur.
- `feature/configs/ConfigsScreen.kt` : libellés et exemples d’import.
- Tests :
  - unitaires : rejets structurels (Trojan sans mot de passe, SS sans méthode,
    port invalide, protocole inconnu) ;
  - instrumentés : conversion réelle `trojan://` et `ss://` + `testXray` ;
  - E2E : deux nouveaux inbounds dans la fixture officielle (Trojan TLS avec
    certificat auto-signé généré en CI, Shadowsocks AEAD), tunnel réel avec token,
    et **journal d’accès par protocole** (`vless`, `vmess`, `trojan`,
    `shadowsocks`) comme porte de preuve.

Critère de sortie : aucun `Connected` sans sonde réelle pour chaque protocole ;
`ACCESS_LOG_PER_PROTOCOL_OK` étendu ; zéro régression VLESS/VMess.

### Lot 2 — Import QR et abonnements HTTPS

- **QR** : décodage **on-device** (ML Kit code-barres embarqué ou ZXing), caméra
  uniquement à la demande, import d’une image depuis le stockage. Aucune image
  envoyée.
- **Abonnements** : URL **HTTPS uniquement**, corps ≤ 256 Ko, redirections HTTPS,
  format base64/JSON V2Ray ; en-tête `subscription-userinfo` (upload, download,
  total, expire) affiché comme **donnée du fournisseur**, jamais recalculée ;
  rafraîchissement manuel + à l’ouverture ; URL (souvent porteuse de jeton) stockée
  chiffrée, jamais journalisée.
- **Modèle** : table `subscriptions` (migration Room `2 → 3` testée) et rattachement
  des profils ; liste groupée repliable.
- Tests : parseurs unitaires (corps d’abonnement, en-tête, URL invalide), migration
  instrumentée, import QR à partir d’une image de test.

### Lot 3 — Réseau réellement configurable

- **Split tunneling** : mode « tout », « inclure », « exclure » appliqué via
  `addAllowedApplication`/`addDisallowedApplication` ; sélection d’apps via
  `queryIntentActivities(CATEGORY_LAUNCHER)` (pas de `QUERY_ALL_PACKAGES`).
  Application au prochain démarrage du tunnel, annoncée dans l’UI.
- **DNS / MTU / IPv6** : les valeurs persistées sont réellement transmises à
  `XrayConfigBuilder` et au `VpnService` (aujourd’hui plusieurs options sont
  UI-only). Toute option non raccordée reste étiquetée comme telle.
- Tests : construction de configuration (MTU, DNS, IPv6), unitaires ; scénario
  instrumenté limité à ce qui est observable sans réseau externe.

### Lot 4 — Automatisation Android

- **Tuile Réglages rapides** : connecter/déconnecter ; si le consentement VPN
  manque, ouverture de l’app (une tuile ne peut pas afficher le dialogue système).
- **Widget** : état réel + action connexion/déconnexion, sans état simulé.
- **Liens `onfaa://`** : `connect`, `disconnect`, `profile/<id>` via une activité
  exportée qui valide l’entrée ; aucun secret dans l’URL.
- **Connect on Demand** : Android réserve le « toujours actif » à l’utilisateur ;
  l’app fournit un accès direct au réglage système et le documente honnêtement.
- Tests : gestion d’intent instrumentée, widget/tuile compilés et vérifiés en CI.

### Lot 5 — Édition manuelle et partage local

- Formulaire par protocole, soumis au **même pipeline** structure → `testXray` →
  chiffrement → Room.
- Export QR d’un lien reconstruit quand c’est possible ; sinon export bloqué avec
  message explicite (pas d’export en clair d’un secret qu’on ne sait pas
  reconstruire).

## 3. Séquence recommandée

`Lot 1` → `Lot 3` → `Lot 2` → `Lot 4` → `Lot 5`.

- Le lot 1 étend le cœur déjà validé et se prouve avec la fixture E2E existante.
- Le lot 3 corrige des options aujourd’hui décoratives : c’est un gain de véracité
  avant d’ajouter des fonctions.
- Le lot 2 ajoute de la surface réseau (abonnements) : à faire après le durcissement
  des options.
- Les lots 4 et 5 sont du confort, sans risque pour le tunnel.

## 4. Coûts et contraintes CI

- Chaque lot = 1 à 3 pushes sur `main` ; le workflow E2E (AAR + serveur + émulateur)
  dure ~9 minutes et doit rester la référence de preuve.
- Aucun test ne doit être rendu facultatif pour faire passer un lot.
- Toute nouvelle dépendance (QR) doit être justifiée, épinglée et minimale ;
  privilégier un décodeur embarqué pour ne pas dépendre des services Google.

## 5. Explicitement hors périmètre

- **Hysteria 2** et **SSH** : nécessitent un second moteur (QUIC/SSH), donc une
  révision du contrat « Xray-core officiel uniquement ». Décision à prendre
  séparément, avec un lot dédié et ses propres preuves.
- **Partage cloud de configurations** : suppose un service hébergé, une politique de
  rétention et des clés de partage ; contraire au fonctionnement local-first actuel.
- **Thèmes et palettes** : déjà couverts par la Phase 5 UI (travail parallèle).
- **Partage hotspot via proxy HTTP** : utile mais ajoute un proxy local à sécuriser ;
  à étudier après le lot 3.

## 6. Critères d’acceptation communs

1. Chaque protocole revendiqué est prouvé par un **tunnel réel** et un journal
   d’accès, pas seulement par une conversion.
2. Aucun UUID, mot de passe, lien complet ou configuration en clair dans les
   journaux, exports ou erreurs.
3. Toute migration Room est testée avec une base réelle de la version précédente.
4. Toute option affichée est soit raccordée, soit explicitement étiquetée
   « UI-only ».
5. Chaque lot fournit un rapport, des chiffres CI réels et un APK publié.

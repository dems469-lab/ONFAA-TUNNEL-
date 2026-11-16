# Architecture cible — ONFAA TUNNEL

> Ce document décrit l’**architecture cible**. Il ne décrit pas une implémentation existante. Les détails d’intégration `libXray` (signatures, types, cycle de vie exact) seront confirmés en Phase 2.

## Vue d’ensemble

Application Android native en **Kotlin**, structurée autour de :

1. **UI** (écrans, thèmes, état affiché)
2. **Domaine / cas d’usage** (connexion, gestion des profils, import, diagnostics)
3. **Données** (Room pour entités, DataStore pour préférences)
4. **Service VPN** (`VpnService`) comme point d’entrée système du tunnel
5. **Moteur réseau** : **XTLS/Xray-core officiel** embarqué/intégré via **libXray**

## Composants principaux

### 1. Couche Android / Système

- **`VpnService`** : établissement de l’interface TUN, gestion du cycle de vie du service au premier plan/notification selon exigences Android, transfert du trafic vers le moteur conformément à l’intégration retenue.
- **Permissions et politique réseau** : déclarées et gérées selon les besoins réels (VPN, réseau, notifications si nécessaire, etc.) lors des phases d’implémentation.

### 2. Moteur Xray

- **XTLS/Xray-core officiel** : cœur de traitement des protocoles (VLESS, VMess) et de la logique proxy/tunnel.
- **libXray** : couche de liaison utilisée par l’application Android pour démarrer/arrêter/configurer le cœur (API exacte à confirmer ; **aucune signature inventée**).
- **Contrainte** : pas de substitution par sing-box ou autre moteur.

### 3. Persistance

- **Room** : stockage structuré (profils/serveurs, métadonnées d’import, historiques/journaux si retenu, etc. — schéma à définir en phases ultérieures).
- **DataStore** : préférences utilisateur (thème, options DNS/routage/UI, drapeaux de diagnostic, etc.).

### 4. Asynchronisme et état

- **Coroutines** : opérations I/O, démarrage/arrêt, tâches longues.
- **Flow** : diffusion d’état (connexion, statistiques, logs, préférences) vers l’UI et les composants observateurs.

### 5. Modules fonctionnels cibles

- **Gestion des configurations** : import, parsing/validation, mapping vers la représentation consommée par Xray (détails en phases 2–4).
- **Gestion des serveurs** : sélection du profil actif, persistance, cohérence avec l’état du tunnel.
- **Statistiques & diagnostics** : collecte de métriques réelles disponibles, affichage, export contrôlé des logs.
- **DNS & routage** : paramètres exposés à l’utilisateur en cohérence avec les capacités du moteur et d’Android.

## Flux cible (simplifié)

1. L’utilisateur sélectionne un profil et demande la connexion.
2. L’application prépare la configuration (source : stockage local) et démarre le `VpnService`.
3. Le service initialise l’interface VPN et orchestre l’exécution du moteur **Xray via libXray**.
4. Le trafic est acheminé selon la configuration ; l’état et les métriques sont publiés (Flow) vers l’UI.
5. À l’arrêt, le moteur et le service sont proprement terminés ; l’état persistant est mis à jour.

## Décisions différées / à confirmer

- Format(s) d’import supportés et stratégie de validation.
- Méthode exacte d’intégration binaire/librairie de `libXray` (AAR, .so, artefacts) et API publique.
- Stratégie de collecte des statistiques (sources Android vs métriques moteur).
- Modèle de données Room détaillé et politique de migration.
- Architecture UI détaillée (navigation, state holders) — cadrée par `docs/design/DESIGN_SPEC.md`.

Ces points doivent être tracés dans [`DECISIONS.md`](DECISIONS.md) au fil de l’avancement.

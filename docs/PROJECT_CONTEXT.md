# Contexte du projet — ONFAA TUNNEL

## Présentation

**ONFAA TUNNEL** est un projet de client VPN Android natif. L’objectif est de fournir une application fiable permettant d’établir une **connexion VPN réelle** sur Android en s’appuyant sur le cœur réseau officiel **XTLS/Xray-core**, intégré via **libXray**.

## Besoins et objectifs

- Offrir un tunnel VPN fonctionnel (pas de démonstration simulée).
- Permettre l’utilisation de configurations compatibles **VLESS** et **VMess**.
- Permettre l’**importation** de configurations personnelles fournies par l’utilisateur.
- Gérer une liste de **serveurs** (ajout, édition, suppression, sélection) et l’état de connexion.
- Afficher des **statistiques réseau réelles** (ex. trafic montant/descendant, débit, indicateurs de session) selon les capacités exposées par la pile Android et le moteur.
- Fournir des réglages de **DNS**, de **routage**, des **journaux** et des outils de **diagnostics**.
- Proposer une interface complète avec **thèmes personnalisables**.

## Fonctionnalités prévues (vue d’ensemble)

| Domaine | Exemples de capacités prévues |
| --- | --- |
| Connexion | Démarrage/arrêt du VPN, état, reconnexion contrôlée, gestion des erreurs |
| Protocoles | VLESS, VMess (via Xray-core) |
| Configurations | Import (formats à préciser), validation, stockage sécurisé local |
| Serveurs | CRUD, favoris/ordre (à préciser), sélection du profil actif |
| Réseau | Statistiques temps réel, informations de session |
| DNS / Routage | Options configurables alignées avec les capacités Xray/Android |
| Observabilité | Journaux, export/partage contrôlé, diagnostics de connectivité |
| UI/UX | 5 écrans principaux, 8 thèmes |

## Contraintes et principes

- **Plateforme :** Android natif, langage **Kotlin**.
- **Interface système VPN :** `android.net.VpnService`.
- **Moteur :** **XTLS/Xray-core officiel** + **libXray**. **Interdiction** de remplacer par sing-box.
- **Persistance :** Room (données structurées), DataStore (préférences).
- **Concurrence :** Coroutines et Flow.
- **Réalité fonctionnelle :** aucune simulation de connexion ; les comportements doivent correspondre à une intégration réelle.
- **API libXray :** les signatures ne doivent pas être inventées ; elles seront déterminées en Phase 2 à partir des artefacts/sources officiels.
- **Build :** compilation cible exclusivement via **GitHub Actions** (mise en œuvre en Phase 9).
- **Confidentialité :** aucune donnée sensible (UUID, clés, tokens, configs personnelles) ne doit être commise.

## Public et usage

Application destinée à un usage personnel/contrôlé par l’utilisateur, qui fournit ses propres configurations de serveur. Le projet doit faciliter la reprise du développement (documentation, statut, décisions) via le dépôt GitHub.

## Hors périmètre immédiat (Phase 0)

- Implémentation du code Android, du moteur Xray, de l’UI finale, des workflows de build exécutables.
- Ajout d’assets graphiques de référence non fournis (les emplacements sont documentés).

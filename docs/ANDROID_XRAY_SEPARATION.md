# Séparation des responsabilités : application Android, VpnService et moteur Xray upstream

Ce document précise les frontières prévues entre les composants afin d’éviter toute confusion sur l’état réel du projet.

## 1. Application Android (UI & orchestration)

- **Rôle** : interface utilisateur native (Jetpack Compose), navigation, présentation de l’état réel, gestion des entrées utilisateur (import, sélection, paramètres).
- **Technologies** : Kotlin, AndroidX, Compose, Navigation Compose, Coroutines/Flow.
- **Données** : Room (configurations/profils) et DataStore (préférences/thème/réglages) selon le périmètre implémenté.
- **Règle** : l’UI ne doit jamais simuler une connexion VPN, des statistiques réseau ou des journaux de tunnel inexistants.

## 2. Service VPN Android (`VpnService`)

- **Rôle** : point d’intégration système pour établir l’interface TUN, gérer le cycle de vie du service, les notifications/foreground si requis, et coordonner le démarrage/arrêt du moteur.
- **État Phase 1** : classe de base préparée (`com.onfaa.tunnel.vpn.OnfaaTunnelVpnService`) enregistrée dans le manifeste ; **aucun tunnel n’est établi**, aucun trafic n’est capturé/redirigé, aucune statistique n’est produite.
- **Implémentation réelle** : prévue en Phase 3, uniquement avec un moteur Xray réel intégré.

## 3. Moteur réseau upstream (futur)

- **Cœur définitif** : **XTLS/Xray-core** — version officielle du dépôt upstream **https://github.com/XTLS/Xray-core**.
- **Intégration Android cible** : via **libXray**, après vérification de compatibilité et de l’API officielle (signatures/types/cycle de vie non inventés).
- **Interdictions** : aucun fork tiers présenté comme upstream, aucune version modifiée présentée comme upstream, aucun remplacement par sing-box, aucune fausse bibliothèque libXray, aucune simulation de moteur.
- **État Phase 1** : **aucune dépendance moteur intégrée** (ni binaire, ni wrapper approximatif).

## Flux cible (résumé)

1. L’utilisateur agit dans l’application (sélection de configuration, demande de connexion).
2. L’application demande le démarrage contrôlé du `VpnService`.
3. Le `VpnService` configure l’interface VPN et orchestre le moteur **Xray-core officiel** via **libXray** (intégration future vérifiée).
4. L’état, les logs et les statistiques proviennent de sources réelles (Android/moteur) et sont exposés à l’UI via des flux observables.
5. À l’arrêt, le moteur et le service sont arrêtés proprement sans état fictif résiduel.

## Traçabilité

- Choix et contraintes : `docs/DECISIONS.md`
- Architecture cible : `docs/ARCHITECTURE.md`
- Avancement : `STATUS.md` et `docs/ROADMAP.md`

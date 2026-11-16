# `app/` — Module applicatif (préparé)

Ce répertoire est réservé au futur module/application Android natif (Kotlin) de ONFAA TUNNEL.

## État Phase 0

- Aucun code applicatif n’est présent volontairement à ce stade.
- La structure Gradle, le manifeste, les sources Kotlin et l’intégration `VpnService` seront introduits à partir de la **Phase 1** (fondations Android), conformément à la roadmap.

## Règles

- Ne pas ajouter de fichiers Kotlin/ressources vides uniquement pour simuler de l’avancement.
- Toute implémentation devra respecter les contraintes du projet : `VpnService`, XTLS/Xray-core officiel via `libXray`, pas de simulation de connexion, pas de sing-box.

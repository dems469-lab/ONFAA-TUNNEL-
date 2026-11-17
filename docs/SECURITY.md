# Sécurité — ONFAA TUNNEL

## Objectifs

- Protéger les informations sensibles des utilisateurs (configurations VPN, identifiants de serveurs, clés, UUID).
- Éviter toute fuite de secrets dans le code source, l’historique Git, les logs ou les artefacts.
- Préparer une base saine pour l’implémentation future (stockage, export, diagnostics).

## Données sensibles (à ne jamais commiter)

- Tokens d’accès (GitHub, CI, services tiers), mots de passe, clés privées, certificats.
- UUID, mots de passe (Trojan, Shadowsocks, SOCKS5), identifiants, clés et paramètres secrets contenus dans les configurations personnelles (VLESS, VMess, Trojan, Shadowsocks, SOCKS5).
- Fichiers de configuration réels des utilisateurs, captures contenant des secrets, exports de logs non assainis.
- Fichiers keystore/JKS, `.env` contenant des secrets, tout matériel de signature.

## Règles de dépôt

- Vérifier les diffs avant commit pour détecter toute inclusion accidentelle de secrets.
- Utiliser `.gitignore` pour exclure les artefacts et fichiers sensibles courants ; compléter au besoin sans affaiblir la protection.
- En cas de suspicion de fuite dans l’historique : traiter comme incident (rotation des secrets côté utilisateur, nettoyage d’historique si nécessaire) — procédure à détailler en Phase 8.

## Authentification et opérations GitHub

- Ne jamais inscrire de token dans un fichier du dépôt ni dans une commande destinée à être enregistrée dans la documentation ou les logs partagés.
- Si l’authentification Git (push) est impossible : **demander une configuration sécurisée et s’arrêter**.

## Application (cible)

- **Stockage local :** minimiser l’exposition des configurations ; envisager des protections adaptées (ex. stockage privé app, chiffrement si justifié) — décisions à tracer.
- **Journaux et diagnostics :** prévoir des niveaux de log et des mécanismes de masquage/anonymisation avant tout partage/export.
- **Import/export :** valider les entrées, limiter les fuites involontaires, avertir l’utilisateur lors d’actions sensibles.
- **Réseau :** s’appuyer sur une intégration réelle et maîtrisée de Xray-core/libXray ; ne pas introduire de composants réseau non audités sans justification.

## Signalement

Les vulnérabilités ou comportements à risque doivent être documentés de manière responsable (sans publier d’exploit ni de secrets) et traités en priorité selon la gravité.

## Références

- [`../CONTRIBUTING.md`](../CONTRIBUTING.md)
- [`DECISIONS.md`](DECISIONS.md)
- [`TEST_PLAN.md`](TEST_PLAN.md)

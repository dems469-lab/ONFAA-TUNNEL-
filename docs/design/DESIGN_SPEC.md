# Spécification design — ONFAA TUNNEL

Ce document définit le cadre UI/UX cible : écrans principaux et thèmes. Les visuels de référence (maquettes, captures) seront ajoutés ultérieurement dans `docs/design/reference/` ; **aucune image n’est intégrée dans le dépôt à ce stade**.

## Écrans prévus (5)

| # | Écran | Rôle principal | Éléments typiques (cibles) |
| --- | --- | --- | --- |
| 1 | **Accueil / Connexion** | Point central pour l’état du tunnel et l’action connecter/déconnecter | Bouton principal, état (déconnecté/connexion/en cours/erreur), serveur actif, raccourcis vers statistiques et sélection de serveur |
| 2 | **Serveurs / Configurations** | Gestion des profils et configurations importées | Liste des serveurs, sélection du profil actif, ajout/import, édition/suppression, indicateurs de validité |
| 3 | **Statistiques / Réseau** | Visualisation des métriques réelles de session | Débit montant/descendant, trafic cumulé, durée, informations de connexion disponibles |
| 4 | **Journaux / Diagnostics** | Observabilité et dépannage | Consultation des logs (avec règles de confidentialité), outils de diagnostic, export contrôlé/assaini si implémenté |
| 5 | **Paramètres** | Configuration de l’application et du comportement réseau | DNS, routage, options avancées, préférences d’interface, sélection du thème, informations légales/version |

> La navigation exacte (barre d’onglets, tiroir, pile) sera précisée lors de l’implémentation UI (Phase 5) tout en préservant ces cinq domaines fonctionnels.

## Thèmes prévus (8)

Les thèmes doivent être applicables de manière cohérente sur l’ensemble des écrans. Les noms ci-dessous sont les identifiants fonctionnels à supporter.

| Thème | Intention visuelle (cible) |
| --- | --- |
| **ONFAA** | Identité principale du produit (thème signature) |
| **Aurora** | Palette froide/lumineuse, ambiance « aurore » |
| **Ember** | Palette chaude, accents incandescents |
| **Graphite** | Neutres sombres, contraste maîtrisé |
| **Orchid** | Accents violets/magenta, expressif |
| **Paper** | Clair, sobre, type « papier » |
| **Matcha** | Tons verts naturels, apaisants |
| **Système** | Suit le thème clair/sombre du système Android |

## Exigences transverses

- **Cohérence :** typographie, espacements, composants et états (actif, désactivé, erreur) harmonisés entre thèmes.
- **Accessibilité :** contrastes et lisibilité à considérer pour chaque thème (détails en Phase 5/8).
- **États réels :** l’UI doit refléter l’état réel du VPN/moteur (pas d’indicateurs de connexion fictifs).
- **Référence visuelle :** placer les images de référence dans `docs/design/reference/` lorsqu’elles seront fournies, et les lister dans le README du dossier.

## Évolutions

Toute modification de la liste des écrans ou des thèmes doit être consignée dans `docs/DECISIONS.md` et répercutée dans `STATUS.md`/roadmap si elle impacte le périmètre.

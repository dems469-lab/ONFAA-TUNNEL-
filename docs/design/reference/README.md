# Références visuelles — ONFAA TUNNEL

Ce dossier contient les **5 images de référence fournies**, classées sans modification
(aucun recadrage, aucune retouche, aucun renommage de contenu). Les fichiers ont été
livrés par la session sous des noms `.png`, mais leurs octets sont des **JPEG**.

## Inventaire et classification

| # | Fichier classé | Écran / état identifié | Dimensions | SHA-256 |
| --- | --- | --- | --- | --- |
| 1 | `logs/journaux_diagnostics.jpg` | Journaux + diagnostic automatique + actions, onglet **Journaux** actif, état connecté/succès | 1254 × 1254 | `3666161910d62e003c8b0da91d773146e8b12228907e8231f85551acebe28b3b` |
| 2 | `appearance/apparence_themes.jpg` | Apparence : modes Système/Clair/Sombre, 7 thèmes, thème actuel, onglet **Paramètres** actif | 1254 × 1254 | `20c6ec858855b9d77952faf05a25e13d0783edc90afb6f4eb9a8d9bde635ec91` |
| 3 | `servers/configs_serveurs.jpg` | Configs : recherche, filtres, actions, cartes serveur, latence, favori, sélection | 1254 × 1254 | `1fba9917756e85f5b23193c6211f4f1e9e11485419c462164059cb5267de0fd7` |
| 4 | `settings/parametres_vue_generale_en.jpg` | Paramètres — vue générale anglaise (Connection, Anti-censorship, Advanced Modes, Network, More) | 1212 × 1297 | `02a91528efe2af289441636e9e78bfc3b335b4ca7578cfdf5ef0cb536a7a4832` |
| 5 | `settings/parametres_detail_fr.jpg` | Paramètres — vue détaillée française avec en-tête marque, recherche et interrupteurs | 1214 × 1295 | `776c29436c5bdffe23589b189b76ad1c4d75c62ede9c3b6ee95d40ca2e74c42e` |

**Total : 5 images — 5 classées — 0 dans `unidentified/`.**
**Aucune image d’accueil/connexion n’a été fournie.**

## Dossiers

| Dossier | Contenu | Remarque |
| --- | --- | --- |
| `logs/` | 1 image | Référence la plus complète pour la barre de navigation (4 onglets). |
| `appearance/` | 1 image | Sous-écran de Paramètres ; barre à 3 onglets visible. |
| `servers/` | 1 image | Écran Configs. |
| `settings/` | 2 images | Deux variantes de la même famille d’écran, langues et densités différentes. |
| `home/` | 0 image | **Référence manquante** ; l’accueil est reconstruit et documenté comme tel. |
| `components/` | 0 image | Aucune planche de composants isolée ; les composants sont dérivés des 5 écrans. |
| `unidentified/` | 0 image | Aucune image ambiguë. |

## Règles de classification

- Chaque image a été analysée **indépendamment**, sans se fier au nom de fichier d’origine.
- Le classement repose sur le contenu réel : titre, hiérarchie, onglet actif, contrôles,
  données affichées et contexte.
- Les deux images de paramètres ont été conservées ensemble car elles montrent le même
  domaine fonctionnel à deux niveaux de détail. La version française (`..._detail_fr`)
  sert de référence principale pour la copie ; la version anglaise sert de référence
  structurelle pour les groupes Réseau et More.
- Aucun Original n’a été modifié. Les noms de fichiers sont descriptifs et les hashes
  ci-dessus permettent de vérifier l’intégrité.

## Incohérences de référence à connaître

1. **Barre de navigation** : 4 onglets dans `logs`, 3 onglets dans `servers`,
   `settings` et `appearance`. L’implémentation retient **4 onglets**
   (Connexion, Configs, Paramètres, Journaux), car c’est la seule référence qui montre
   l’ensemble des domaines et que Journaux est un écran demandé.
2. **Langue** : les références mélangent anglais et français. L’application est
   implémentée en **français**, avec la version française comme source de copie.
3. **« Aucune fuite détectée »** apparaît dans la maquette Logs. Cette affirmation est
   **interdite par le cahier des charges** (aucune détection de fuite réelle n’est
   implémentée). L’UI affiche à la place « Non testées » et explique explicitement la
   limite.
4. **Statistiques** : les valeurs de la maquette (latence, trafic, heure) sont
   illustratives. L’application n’affiche que des mesures réelles ou « non testé /
   indisponible ».
5. **Accueil** : absent des références. La fidélité de l’accueil ne peut pas être
   revendiquée ; seuls la cohérence de langage visuel et les états réels sont garantis.

## Mapping référence → implémentation

| Référence | Écran Compose | Fichier principal |
| --- | --- | --- |
| `logs/journaux_diagnostics.jpg` | Journaux | `feature/logs/LogsScreen.kt` |
| `appearance/apparence_themes.jpg` | Apparence | `feature/appearance/AppearanceScreen.kt` |
| `servers/configs_serveurs.jpg` | Configs | `feature/configs/ConfigsScreen.kt` |
| `settings/parametres_vue_generale_en.jpg` | Paramètres (structure Réseau/More) | `feature/settings/SettingsScreen.kt` |
| `settings/parametres_detail_fr.jpg` | Paramètres (vue française, sous-écrans) | `feature/settings/SettingsScreen.kt`, `SettingsSubScreens.kt` |
| _(aucune)_ | Accueil / Connexion | `feature/home/HomeScreen.kt` — **reconstruit** |

La charte complète, les tokens et les écarts sont dans
[`../DESIGN_SPEC.md`](../DESIGN_SPEC.md).

# Audit de la maquette — ONFAA TUNNEL

Version : **1.0 — Phase 5**
Date : 2026-11-17

Cet audit évalue les **5 maquettes fournies** et leur reproduction dans l'UI
Compose. Il distingue à chaque fois :

- un **défaut de la maquette** (le document de design est faux ou incomplet) ;
- un **écart d'implémentation** (le code ne suit pas la maquette) ;
- un **écart volontaire et justifié** (l'app ne peut pas dire ce qu'elle ne mesure pas).

---

## 1. Verdict global

| Dimension | Verdict | Commentaire |
| --- | --- | --- |
| Direction artistique | Solide | Identité claire, cartes « glass », cyan/bleu, hiérarchie lisible. |
| Cohérence interne de la maquette | Moyenne | 3 vs 4 onglets, mélange EN/FR, deux structures de Paramètres. |
| Couverture | Faible | 4 écrans référencés sur 15 routes ; aucune maquette d'Accueil. |
| Fidélité d'implémentation | Partielle | Structure fidèle, détails visuels déviés (titres, boutons, vignettes). |
| Accessibilité | À corriger | Contrastes mesurés insuffisants dans plusieurs palettes et états. |
| Vérité des données | Maquette non conforme / app globalement conforme | La maquette affirme « aucune fuite » ; l'app refuse de le faire. |
| UX | Moyenne | Bonnes actions primaires, mais frictions navigation / recherche / export. |

**Lecture d'ensemble.** La maquette est un bon document de *direction visuelle*,
pas un dossier de conception complet. L'implémentation en tire un système
cohérent et honnête, mais ne peut pas prétendre à une fidélité pixel : le
matériau de départ est incomplet, incohérent et parfois trompeur.

---

## 2. Ce que la maquette fait bien

- **Identité forte et cohérente** : fond bleu nuit, cartes translucides, accents
  cyan/bleu, badge protocolaire, chips pilule.
- **Hiérarchie claire** : titre de page, actions d'en-tête, contenu, actions
  globales, barre de navigation.
- **Vocabulaire visuel stable** : cartes arrondies (~20 dp), icônes en tuiles
  colorées, badges d'état, dégradés discrets.
- **Apparence** : la distinction *mode d'affichage* (Système/Clair/Sombre) et
  *palettes* (7) est visuellement évidente et pertinente.
- **Logs** : seule maquette avec 4 onglets ; structure filtres → liste →
  diagnostic → actions très lisible.
- **Configs** : recherche, filtres, deux actions globales et cartes serveur
  forment un flux de gestion compréhensible.

---

## 3. Audit de la maquette en tant que livrable de design

### 3.1 Couverture réelle

| Écran | Référence ? | États montrés | États manquants |
| --- | --- | --- | --- |
| Accueil / Connexion | **Non** | — | Tout : déconnecté, connexion, erreur, permission VPN, trafic. |
| Configs | Oui | Liste remplie, serveurs testés | Vide, import, erreur d'import, surcharge, tri actif. |
| Paramètres | 2 variantes | Vue EN + haut de vue FR | Sous-pages, états d'erreur, valeurs longues. |
| Apparence | Oui | Sélection ONFAA | Mode clair, 6 autres palettes en plein écran. |
| Journaux | Oui | Connecté, succès | Vide, erreur, filtres actifs, recherche active, longs logs. |

**Constat majeur.** Sur les 15 routes de navigation (`MainActivity.kt`), seules
4 ont une référence visuelle. L'écran le plus important — la connexion VPN —
n'a **aucune** maquette. La fidélité de l'Accueil ne peut donc être ni
revendiquée ni évaluée.

### 3.2 Incohérences internes des références

| Sujet | Références | Impact |
| --- | --- | --- |
| Navigation | 4 onglets (Logs) vs 3 (Configs, Paramètres, Apparence) | Impossible de reproduire les deux ; l'app retient 4. |
| Langue | Maquette EN + maquettes FR mélangées | Copie incohérente, y compris dans une même image. |
| Paramètres | Structure « cartes top + sections » (EN) vs « sections partout » (FR) | Deux designs différents pour le même écran. |
| Recherche | Icône en en-tête (EN) vs champ visible (FR/Configs) vs tuile (Logs) | Trois modèles d'interaction. |
| Titres | Centrés (Logs, Configs, Apparence, Paramètres EN) vs alignés à gauche (Paramètres FR) | Alignement non stable. |
| Onglets FR | « Connection / Configs / Settings » dans la maquette Paramètres FR | Mélange FR/EN dans une image présentée comme française. |
| Copy | « All » (Configs), « SÉLECTIONNE » sans accent, « Anti-censorship » | Copie non normalisée. |

### 3.3 Promesses non démontrables

| Affirmation dans la maquette | Problème |
| --- | --- |
| « Aucune fuite détectée » | Aucune détection de fuite DNS/IP n'existe. Affirmation invérifiable. |
| « Protection renforcée (Aucune fuite) » | Même problème, formulation marketing de plus. |
| « Tout fonctionne correctement » | N'est pas démontrable sans tests réels complets. |
| « Exportation des rapports (TXT, PDF) » | Le PDF n'est pas implémenté. |
| « Importer (Fichier, Lien, QR Code) » | Le QR n'est pas implémenté. |
| Latences 82 / 64 / 71 ms | Valeurs illustratives, pas une spécification de données. |
| Endpoints type « orange.bf », « onfaa.com » | Risque de confusion avec de vrais opérateurs. |

### 3.4 Accessibilité de la maquette

- Textes secondaires gris sur fond sombre : contraste visuellement limite.
- Petites tailles (~11–12 px rendus) sans garantie de contraste.
- Aucun état focus, pressé, désactivé ou erreur montré.
- Aucune annotation de cible tactile, de description d'icône ou de lecteur d'écran.
- Uniquement du mode sombre ; le mode clair n'est pas spécifié.

---

## 4. Fidélité de l'implémentation, écran par écran

### 4.1 Journaux (`LogsScreen.kt`)

**Fidèle :** titre, sous-titre, corbeille, partage, filtres, liste, diagnostic,
grille 2×3 (Copier, Effacer, Filtrer, Exporter, Rapport, Rechercher),
4 onglets.

**Écarts visuels :**
- Titre aligné à gauche au lieu d'être centré.
- Filtres sans icônes (la maquette montre une icône par filtre).
- Carte diagnostic sans icône « loupe » à côté du titre.
- Longues notes de véracité ajoutées (utiles, mais denses).

**Écarts fonctionnels :**
- « Rapport (TXT, PDF) » → partage **texte** uniquement.
- « Aucune fuite détectée » → remplacé par **« Non testées »** (correct).

**Problèmes propres à l'implémentation :**
1. Le diagnostic est **toujours** au moins « À vérifier », car le contrôle
   « Fuites DNS/IP : Non testées » est un `UNKNOWN` qui déclenche l'avertissement
   global (`LogsScreen.kt:113-124`, `427-431`). Une session saine ne peut jamais
   afficher « Connexion stable » vert.
2. Le badge du diagnostic affiche une **icône de coche** même en état
   « À corriger » ou « À vérifier » (`LogComponents.kt:177`, `188-191`).
3. Le contrôle DNS est marqué **OK** alors qu'il lit une constante de
   configuration (`1.1.1.1:53`) sans test réel (`LogsScreen.kt:422-426`).
4. « Copier » respecte les filtres, mais « Exporter » exporte **tous** les
   événements, y compris ceux masqués par le filtre
   (`LogsScreen.kt:126-139` vs `141-160`, action à `334`).

### 4.2 Configs (`ConfigsScreen.kt`)

**Fidèle :** recherche, chips Tous/Favoris/VLESS/VMess, deux actions globales,
cartes avec badge protocole, endpoint monospace, latence réelle, étoile,
badge « Sélectionnée », aide au swipe.

**Écarts visuels :**
- « Tous » au lieu de « All » (normalisation correcte).
- Boutons d'en-tête ronds au lieu de carrés arrondis.
- Carte d'aide plus verbeuse que la maquette.

**Écarts fonctionnels :**
- QR désactivé « bientôt » (`ConfigsScreen.kt:923-928`) — la maquette l'affiche
  comme disponible.

**Problèmes propres à l'implémentation :**
1. L'aide dit « ou touchez ⋮ pour ouvrir les options »
   (`ConfigsScreen.kt:424`) mais **aucune carte serveur n'a de bouton ⋮**
   (`553-641`). L'instruction décrit un contrôle inexistant.
2. Les profils sont en **mémoire uniquement** ; l'avertissement n'apparaît que
   dans la boîte d'import (`932-935`), pas dans la liste elle-même. Un
   utilisateur peut perdre ses serveurs au redémarrage sans avoir vu d'avertissement.
3. La liste n'est pas `LazyColumn` (`397-410`) : avec de nombreux profils,
   tout est composé en mémoire.
4. Les résultats de latence ne sont pas persistés ; le tri par latence repose
   sur l'état de la session uniquement.

### 4.3 Apparence (`AppearanceScreen.kt`)

**Fidèle :** sélecteur de mode, grille 3 colonnes, 7 palettes, coche de
sélection, carte « Thème actuel ».

**Écarts visuels :**
- Vignettes : dégradés abstraits + icône Material au lieu des visuels
  photographiques de la maquette (`ThemePreviewCard.kt:127-206`).
- Logo ONFAA vectoriel simplifié au lieu du logo 3D de la maquette.
- Titre aligné à gauche au lieu d'être centré.
- 4 onglets au lieu de 3 (décision assumée, documentée).

**Problèmes propres à l'implémentation :**
1. Le sélecteur de mode utilise `palette.secondary` comme fond sélectionné
   avec un texte blanc ou sombre décidé par un seuil de luminance
   (`ThemePreviewCard.kt:64-69`). Résultat : le texte sélectionné est
   **illisible** dans plusieurs palettes sombres (voir §5).
2. Les vignettes ne reflètent pas le **mode courant** : toutes les palettes
   sauf Paper sont montrées en sombre, même en mode clair
   (`ThemePreviewCard.kt:127`). L'utilisateur ne prévisualise pas ce qu'il obtient.
3. Aucune sémantique `selected` sur les vignettes et le sélecteur : un lecteur
   d'écran ne sait pas quel thème ou mode est actif.

### 4.4 Paramètres (`SettingsScreen.kt`, `SettingsSubScreens.kt`)

**Fidèle (référence FR) :** en-tête marque + retour, champ de recherche,
groupes Connexion / Anti-censorship / Modes avancés / DNS, interrupteurs,
valeurs « 30 s », « 5 s », « Toujours actif ».

**Écarts structurels :**
- Hybridation EN + FR : groupes « Réseau » et « Plus » repris de la vue EN,
  en-tête marque repris de la vue FR. Groupes de tête présentés en sections
  au lieu de cartes indépendantes.
- Groupe « Interface » (Apparence, Journaux) ajouté : absent des deux maquettes.
- Titres de section en majuscules avec tracking ; la maquette FR utilise la
  casse normale.
- Interrupteur : piste `secondary` (bleu) au lieu du cyan de la maquette
  (`SettingRows.kt:179-188`).

**Écarts de copie :**
- « Anti-censorship » et « Automation » restent en anglais
  (`SettingsScreen.kt:258`, `367`).
- « gVisor » est mentionné dans la recherche et dans la sous-page Moteur
  (`SettingsScreen.kt:95`, `SettingsSubScreens.kt:438`) mais **aucun réglage
  gVisor n'existe** dans `NetworkEngineScreen`.

**Problèmes de vérité UI :**
1. Les lignes de synthèse affichent les valeurs (DoH, MTU, Mux, fragmentation)
   comme si elles étaient actives, alors qu'elles sont **persistées mais non
   raccordées** au moteur. L'encart en bas (`SettingsScreen.kt:391-395`) liste
   « fragmentation, blocage, gaming, hotspot, routage » mais **omet** DNS, MTU,
   Mux, niveau de log et reconnexion automatique.
2. Les sous-pages sont correctement honnêtes (encarts « non raccordé »), mais
   la vue d'ensemble ne porte pas ce statut au niveau de chaque ligne.

### 4.5 Accueil (`HomeScreen.kt`)

**Aucune référence n'existe.** L'écran est une reconstruction cohérente :
en-tête marque, orbe, état réel, durée de session, carte serveur, trafic
`TrafficStats` réel, mentions honnêtes. Il ne peut pas être jugé « fidèle » ;
seule sa cohérence avec le système visuel et son honnêteté sont évaluables.

---

## 5. Accessibilité mesurée (contrastes WCAG)

Calcul sur les tokens opaques de `ThemeCatalog.kt` (formule WCAG, seuil AA
4.5:1 pour du texte normal). Les surfaces avec alpha peuvent varier légèrement.

### 5.1 Texte sélectionné du sélecteur de mode — **échec critique**

Le fond sélectionné est `palette.secondary` ; le texte est blanc ou sombre
selon un seuil de luminance.

| Palette (mode sombre) | Contraste obtenu | Verdict |
| --- | --- | --- |
| Paper | **1.80:1** | Illisible |
| Orchid | **2.64:1** | Illisible |
| Aurora | **2.98:1** | Illisible |
| Ember | **3.67:1** | Insuffisant |
| ONFAA | **3.72:1** | Insuffisant |
| Graphite | 11.37:1 | OK |
| Matcha | 14.51:1 | OK |

**Cause racine.** Le sélecteur de mode utilise `secondary` alors que les chips
utilisent `primary`/`onPrimary` (toutes > 5:1). Recommandation : utiliser
`primary` + `onPrimary` partout.

### 5.2 `textMuted` sur `surfaceElevated` — échec fréquent

| Palette | Contraste | Verdict |
| --- | --- | --- |
| ONFAA clair | 3.64:1 | Insuffisant |
| ONFAA sombre | 3.81:1 | Insuffisant |
| Graphite sombre | 3.88:1 | Insuffisant |
| Aurora clair | 3.98:1 | Insuffisant |
| Matcha clair | 4.01:1 | Insuffisant |
| Ember sombre | 4.10:1 | Insuffisant |
| Ember clair | 4.11:1 | Insuffisant |
| Aurora sombre | 4.12:1 | Insuffisant |
| Paper clair | 4.28:1 | Insuffisant |
| Graphite clair | 4.31:1 | Insuffisant |
| Paper sombre | 4.32:1 | Insuffisant |

`textMuted` est utilisé dans les libellés de navigation non sélectionnés,
placeholders, métadonnées et notes. Sur les **fonds clairs**, le texte de
navigation non sélectionnée passe sous 4.5:1 (ONFAA 3.94, Matcha 4.36,
Aurora 4.38, Ember 4.42).

### 5.3 Cibles tactiles

| Composant | Taille | Recommandation Android |
| --- | --- | --- |
| `OnfaaChip` | 38 dp | 48 dp |
| `TonalButton` compact | 42 dp | 48 dp |
| `CircleIconButton` | 44 dp | 48 dp |
| `ThemeModeSelector` | 44 dp | 48 dp |

Aucun de ces composants n'applique `minimumInteractiveComponentSize` : la
taille tactile réelle est la taille visuelle.

### 5.4 Sémantique lecteur d'écran

| Composant | Problème |
| --- | --- |
| `ThemeModeSelector` | `.clickable` sans `Role.RadioButton` ni état `selected`. |
| `ThemePreviewCard` | `.clickable` sans état `selected` ; seule la coche a une description. |
| `OnfaaChip` | `Role.Tab` mais pas d'état `selected` exposé. |
| `SettingSwitchRow` | La ligne est cliquable, mais le `Switch` a `onCheckedChange = null` et n'expose pas d'état toggleable. |
| `ChoiceDialog` | `RadioButton(onClick = null)` dans une ligne cliquable sans rôle ni état. |

---

## 6. Constatations priorisées

| # | Sévérité | Type | Constat | Preuve | Recommandation |
| --- | --- | --- | --- | --- | --- |
| A1 | **P0** | Maquette | « Aucune fuite détectée » + « Protection renforcée » : affirmation de sécurité invérifiable | `logs/journaux_diagnostics.jpg` | Corriger la maquette ; ne jamais l'utiliser telle quelle en communication. |
| A2 | P1 | Maquette | Aucune maquette d'Accueil ; 4 écrans / 15 routes couverts | `reference/README.md` | Créer les maquettes manquantes avant d'annoncer une fidélité globale. |
| B1 | P1 | Impl. | Texte du mode sélectionné illisible (1.8–3.7:1) dans 5 palettes sombres | `ThemePreviewCard.kt:64-69` | Utiliser `primary` + `onPrimary` comme les chips. |
| B2 | P1 | Impl. | `textMuted` < 4.5:1 sur 11 combinaisons ; navigation claire < 4.5:1 | `ThemeCatalog.kt` | Éclaircir `textMuted` en clair, l'assombrir en sombre ; viser ≥ 4.5:1. |
| B3 | P1 | Impl. | Diagnostic toujours « À vérifier » : `UNKNOWN` (fuites) déclenche l'avertissement global | `LogsScreen.kt:113-124`, `427-431` | Distinguer « non testé » (neutre) de « avertissement » ; permettre un état vert si seuls des contrôles non critiques sont inconnus. |
| B4 | P1 | Impl. | DNS marqué OK alors que seule la configuration est lue | `LogsScreen.kt:422-426` | État « Configuré (non testé) » ou test réel ; ajuster aussi le libellé « Contrôles réels » du rapport. |
| B5 | P1 | Impl. | Icône de coche sur les badges « À vérifier » / « À corriger » | `LogComponents.kt:177`, `188-191` | Icône Warning / Close selon l'état. |
| B6 | P1 | Impl. | L'aide Configs référence un bouton ⋮ inexistant sur les cartes | `ConfigsScreen.kt:424`, `553-641` | Ajouter le bouton ⋮ ou corriger le texte. |
| B7 | P1 | Impl. | Profils en mémoire uniquement ; avertissement caché dans l'import | `ConfigsScreen.kt:932-935` | Bannière persistante dans Configs quand la liste est non vide et non persistée. |
| C1 | P2 | Impl. | Vignettes de thème abstraites et non représentatives du mode courant | `ThemePreviewCard.kt:127` | Vignette reflétant le mode actif, ou deux demi-vignettes clair/sombre. |
| C2 | P2 | Impl. | Sélecteur de mode et vignettes sans sémantique `selected` ; switch sans état toggleable | `ThemePreviewCard.kt`, `SettingRows.kt` | Ajouter `Role`, `selected`, `toggleable` / `selectable`. |
| C3 | P2 | Impl. | Cibles tactiles 38–44 dp sans `minimumInteractiveComponentSize` | `OnfaaButtons.kt`, `OnfaaSurfaces.kt` | Garantir 48 dp tactile. |
| C4 | P2 | Impl. | « Exporter » ignore les filtres alors que « Copier » les respecte | `LogsScreen.kt:126-160` | Exporter la vue courante, ou libeller « Exporter tout ». |
| C5 | P2 | Maquette/Impl. | PDF et QR annoncés mais non implémentés | Maquettes ; `ConfigsScreen.kt:923` | Décision produit : implémenter ou retirer des écrans de communication. |
| C6 | P2 | Impl. | Vue d'ensemble Paramètres : valeurs affichées comme actives alors qu'elles sont UI-only ; encart final incomplet | `SettingsScreen.kt:391-395` | Badge « UI uniquement » par ligne concernée ; compléter la liste de l'encart. |
| C7 | P2 | Impl. | « gVisor » mentionné mais aucun réglage correspondant | `SettingsScreen.kt:95`, `SettingsSubScreens.kt:438` | Supprimer la mention ou implémenter le réglage. |
| C8 | P2 | Impl. | Listes non paresseuses (Configs, Logs) ; risque de performance avec beaucoup d'éléments | `ConfigsScreen.kt:397`, `LogsScreen.kt:280` | Migrer vers `LazyColumn` quand le volume le justifie. |
| C9 | P2 | Impl. | Chaînes FR codées en dur ; `strings.xml` contient un ancien squelette inutilisé | `strings.xml`, écrans | Extraire les ressources pour préparer l'i18n. |
| C10 | P2 | Impl. | Titres alignés à gauche vs centrés ; boutons d'en-tête ronds vs carrés arrondis ; interrupteur bleu vs cyan | Écrans vs maquettes | Décision design : aligner sur la maquette ou assumer et documenter l'écart. |
| D1 | P3 | Maquette | « All », « SÉLECTIONNE », mélange EN/FR dans les images | Maquettes | Normaliser la copie dans les futures maquettes. |
| D2 | P3 | Impl. | Filtres de logs sans icônes (la maquette en montre) | `LogsScreen.kt:201-208` | Ajouter les icônes de filtre. |
| D3 | P3 | Impl. | Densité des notes de véracité en bas des écrans | `LogsScreen.kt`, `SettingsScreen.kt` | Regrouper dans une carte « Limites » ou une fiche d'information. |

---

## 7. Décisions à valider (produit / design)

1. **Navigation** : conserver 4 onglets (recommandé) et mettre à jour les
   maquettes, ou revenir à 3 et déplacer Journaux ?
2. **Flèche retour sur les onglets racine** : la maquette la montre ; l'app la
   reproduit. UX discutable (redondant avec la barre inférieure).
3. **Langue** : français uniquement ? Corriger « Anti-censorship » et
   « Automation », ou les garder comme vocabulaire produit ?
4. **Recherche** : unifier (champ visible partout) ou garder des modèles
   distincts par écran ?
5. **Export** : PDF réel, fichier texte, ou partage texte assumé ?
6. **Persistance des profils** : chiffrée sur disque, ou mémoire uniquement
   avec bannière explicite ?
7. **QR code** : priorité ou différé ?

---

## 8. Ce qui ne doit pas être « corrigé »

- **4 onglets** : choix documenté et défendable ; la maquette à 3 onglets est
  l'incohérence, pas l'implémentation.
- **« Non testées » au lieu de « Aucune fuite détectée »** : obligatoire.
- **Aucune valeur de latence, trafic ou connexion simulée** : obligatoire.
- **QR désactivé avec mention « bientôt »** : honnête tant que le scanner
  n'existe pas.
- **Encarts « non raccordé » dans les sous-pages** : obligatoires tant que les
  options ne modifient pas Xray.

---

## 9. Plan d'action recommandé

### Avant toute diffusion de la maquette
1. Corriger les affirmations « aucune fuite » / « protection renforcée ».
2. Ajouter une mention « données illustratives » sur les captures.
3. Créer au minimum : Accueil (3 états), état vide Configs, état vide Logs,
   mode clair d'Apparence.

### Avant la prochaine release de l'app
1. Corriger les contrastes (sélecteur de mode, `textMuted`, navigation claire).
2. Corriger la sémantique d'accessibilité (chips, vignettes, switch, dialogs).
3. Corriger le modèle d'état du diagnostic (B3, B4, B5).
4. Corriger l'aide « ⋮ » et ajouter l'avertissement de persistance (B6, B7).
5. Rendre « Exporter » cohérent avec les filtres (C4).

### Plus tard
1. Extrayire les chaînes vers `strings.xml` et gérer l'i18n.
2. Passer les listes longues en `LazyColumn`.
3. Décider PDF / QR / persistance chiffrée.
4. Harmoniser les détails visuels (titres, boutons d'en-tête, cyan des
   interrupteurs) ou documenter formellement les écarts.

---

## 10. Limites de cet audit

- **Audit statique** : code + maquettes. Aucun rendu sur appareil n'a été
  produit (pas de JDK/SDK local) ; la comparaison pixel n'est pas possible.
- Les contrastes sont calculés sur les **tokens opaques** ; les surfaces avec
  alpha peuvent légèrement varier.
- Les maquettes sont des **composites promotionnels** (téléphone + panneau
  marketing), pas des fichiers de design : dimensions, typographies et
  espacements exacts ne sont pas spécifiés.
- La CI verte la plus récente (`44006718908`, commit `aef515e`) ne couvre pas
  les fichiers UI non committés présents dans le répertoire de travail au
  moment de cet audit.
- Cet audit ne constitue pas une certification WCAG ; il identifie des
  problèmes mesurables et des zones à vérifier sur appareil.

# Spécification design — ONFAA TUNNEL

Version documentaire : **Phase 5.1 — Alignement visuel sur les maquettes**
Date : 2026-11-17

> **Phase 5.1 — alignement maquette.** Passe visuelle pour coller aux cinq
> références : titres d'en-tête centrés avec retour nu (flèche sans cercle),
> boutons d'en-tête carrés arrondis (46 dp, rayon 14 dp) dont l'ajout Configs
> bleu plein, badges de protocole pleins (VLESS bleu, VMess bleu clair) et
> badges d'état pleins (SÉLECTIONNÉE, verdict diagnostic), interrupteurs cyan,
> en-têtes de groupes Paramètres en cyan avec tuile d'icône (maquette FR),
> libellés « Thèmes disponibles » sans section majuscule, vignettes de thème
> illustrées par des scènes Canvas (aurore, braises, graphite, orchidée,
> papier, matcha, tuile logo ONFAA), icône pinceau sur la carte thème actuel,
> filtres de journaux avec icônes colorées, loupe sur la carte diagnostic,
> cercle de verdict plein, bouton loupe dans l'en-tête Paramètres (focus le
> champ de recherche), « Toujours actif » en sous-titre, libellés maquette
> « Advanced Modes » et « Afficher la vitesse dans notification », export des
> journaux limité à la vue filtrée. Honnêteté des données inchangée : le
> contrôle « Fuites DNS/IP » reste « Non testées » (gris neutre) et n'est
> jamais annoncé comme une absence de fuite ; le verdict global passe au vert
> dès que tous les contrôles **testables** sont au vert, les contrôles
> non testés restent neutres et ne forcent plus l'alerte. Navigation
> 4 onglets conservée (maquette Journaux). Contrastes : l'écart relevé à
> l'audit (texte blanc sur `secondary` pour le sélecteur de mode, 1,80–3,72:1
> sur 5 palettes sombres) est conservé tel quel pour reproduire la maquette.

Cette spécification décrit l’interface Compose réellement implémentée. Elle est
volontairement séparée de la vérité réseau : **l’UI ne fabrique ni statistique, ni
connexion, ni détection de fuite**. Les états proviennent du `VpnService`, du moteur
Xray libXray, des sondes TCP réelles et de `android.net.TrafficStats`.

## 1. Périmètre et statut des références

Cinq maquettes ont été fournies. Elles ont été analysées une par une, classées sans
modification et documentées dans `docs/design/reference/`.

| Référence | Écran identifié | État de reproduction |
| --- | --- | --- |
| `logs/journaux_diagnostics.jpg` | Journaux / diagnostics | Implémenté, avec retrait de l’affirmation « aucune fuite » (non démontrable). |
| `appearance/apparence_themes.jpg` | Apparence | Implémenté : 3 modes + 7 palettes + carte thème actuel. |
| `servers/configs_serveurs.jpg` | Configs | Implémenté : recherche, filtres, tri, test de latence, détail, import. |
| `settings/parametres_vue_generale_en.jpg` | Paramètres — structure anglaise | **Non suivie** (conflit avec la FR) ; structure française retenue. |
| `settings/parametres_detail_fr.jpg` | Paramètres — vue française détaillée | Référence principale : groupes, interrupteurs, valeurs, en-tête. |
| _(aucune)_ | Accueil / Connexion | **Reconstruit** : aucune conformité à une maquette ne peut être revendiquée. |

- Total : **5 images**, **5 classées**, **0 non identifiée**.
- Aucune image réservée aux composants, aucune image d’accueil.
- Les fichiers livrés en `.png` sont en réalité des **JPEG** ; ils n’ont pas été
  convertis pour préserver les octets d’origine.

## 2. Architecture de navigation

Barre inférieure à **4 onglets** : `Connexion`, `Configs`, `Paramètres`, `Journaux`.

```
Connexion (home)
Configs (configs)
Paramètres (settings)
  ├─ Connexion            settings/connection
  ├─ Anti-censorship      settings/anti-censorship
  ├─ Modes avancés        settings/advanced-modes
  ├─ DNS                  settings/dns
  ├─ Routage              settings/routing
  ├─ Moteur réseau        settings/network-engine
  ├─ Partage hotspot      settings/hotspot
  ├─ Automation           settings/automation
  ├─ Diagnostics          settings/diagnostics
  ├─ À propos             settings/about
  └─ Apparence            appearance
Journaux (logs)
```

- L’onglet sélectionné reste `Paramètres` sur toutes les sous-pages de paramètres et
  sur Apparence.
- La flèche retour des sous-pages revient à la page précédente, ou à Paramètres si la
  pile est vide.
- Configs et Journaux affichent aussi une flèche retour vers Connexion, conformément
  aux maquettes, tout en restant accessibles par la barre inférieure.
- La référence Logs montre 4 onglets, les autres 3. Le choix de 4 onglets est assumé
  et documenté : il couvre les cinq domaines demandés.

## 3. Système de thèmes

### 3.1 Modèle retenu

La maquette Apparence montre **7 palettes** (ONFAA, Aurora, Ember, Graphite, Orchid,
Paper, Matcha) et **3 modes** (Système, Clair, Sombre). Le brief mentionne « 8 thèmes,
dont Dark/Light ». L’implémentation interprète « Dark/Light » comme le **mode
d’affichage**, pas comme une huitième palette :

```
7 palettes × 3 modes = 21 combinaisons réelles
```

Le mode `Système` suit `isSystemInDarkTheme()`. Le changement est persisté dans
DataStore et appliqué instantanément à toute l’application (fonds, cartes, textes,
navigation, boutons, états, graphiques, dialogues).

### 3.2 Tokens de couleur

Chaque palette expose les tokens suivants (`OnfaaPalette`) :

| Groupe | Tokens |
| --- | --- |
| Fond | `backgroundTop`, `backgroundBottom`, `glowPrimary`, `glowSecondary` |
| Surfaces | `surface`, `surfaceElevated`, `surfaceMuted`, `border` |
| Marque | `primary`, `onPrimary`, `secondary`, `tertiary` |
| Texte | `textPrimary`, `textSecondary`, `textMuted` |
| États | `success`, `warning`, `danger` |
| Navigation | `navBackground`, `navSelected` |
| Preview | `previewColors` |

Palette ONFAA sombre, échantillonnée sur les références :

| Rôle | Valeur de référence | Token implémenté |
| --- | --- | --- |
| Fond haut | `#021429` / `#01142B` | `#04152B` |
| Fond bas | très sombre | `#010812` |
| Carte | `#092849` / `#0E244E` | `surfaceElevated #0D2949` |
| Cyan | `#08EBEF` / `#0FD1EF` | `primary #19D8F5` |
| Bleu | `#0775F0` / `#0997ED` | `secondary #2F80FF` |
| Succès | `#08CB19` | `success #2FD46B` |
| Texte | `#DBE2E5` / `#E2E5E7` | `textPrimary #EAF4FF` |
| Texte secondaire | `#9299A3` | `textSecondary #9CB2C8` |

Les variantes claires conservent la même hiérarchie avec des surfaces claires et des
accents assombris pour le contraste. Paper est la palette la plus « papier » ; elle
reste disponible en mode sombre (papier et encre).

### 3.3 Mapping Material 3

`Theme.kt` mappe chaque palette sur `darkColorScheme` ou `lightColorScheme` :
`primary`, `onPrimary`, `background`, `surface`, `surfaceVariant`, `outline`, `error`,
etc. Les propriétés Material 3 servent de repli pour les composants standards
(`AlertDialog`, `DropdownMenu`, `OutlinedTextField`, `Switch`, `RadioButton`), tandis
que les composants ONFAA consomment directement `OnfaaTheme.colors` pour les tokens
propres au produit.

## 4. Typographie

Famille : sans-serif système (pas de police distante, pas de téléchargement).
Échelle principale :

| Style | Taille / ligne | Usage |
| --- | --- | --- |
| `displaySmall` | 34 / 40, bold | grands titres exceptionnels |
| `headlineLarge` | 28 / 34, bold | titre de page (`ScreenTitle`) |
| `headlineMedium` | 24 / 30, bold | titres secondaires |
| `headlineSmall` | 20 / 26, semibold | en-tête d’écran, état de connexion |
| `titleLarge` | 18 / 24, semibold | titres de carte/dialogue |
| `titleMedium` | 16 / 22, semibold | titres de section, valeurs |
| `titleSmall` | 14 / 20, medium | titres de lignes de réglage |
| `bodyLarge` | 16 / 23 | texte courant |
| `bodyMedium` | 14 / 20 | descriptions |
| `bodySmall` | 12 / 17 | aides, sous-titres |
| `labelLarge` | 14 / 18, semibold | boutons principaux |
| `labelMedium` | 12 / 16, medium | chips, badges, boutons secondaires |
| `labelSmall` | 11 / 14, medium, tracking 0.4 | métadonnées, heures, badges |

Le monospace système est utilisé pour les horodatages, endpoints, latences et valeurs
techniques. Les libellés de section sont en majuscules avec tracking (`SectionHeader`).

## 5. Espacement, formes et profondeur

| Token | Valeur | Usage |
| --- | --- | --- |
| `screenHorizontal` | 20 dp | marge latérale des écrans |
| `screenTop` | 12 dp | marge haute sous la barre système |
| `sectionGap` | 20 dp | séparation de sections |
| `cardPadding` | 16 dp | padding interne des cartes |
| `cardGap` | 10 dp | espace entre cartes |
| `controlHeight` | 50 dp | bouton principal |
| `topBarHeight` | 56 dp | en-tête |
| `iconButton` | 44 dp | cible tactile ronde |
| `cardRadius` | 20 dp | cartes |
| `pillRadius` | 99 dp | chips, badges |
| `bottomBarHeight` | 72 dp | navigation |

- Profondeur par **dégradés de surface + bordure fine**, pas par ombre lourde.
- Fond d’écran : dégradé vertical + deux halos radiaux (cyan/bleu en ONFAA).
- Les cartes combinent un dégradé `surfaceElevated → surface` et une bordure
  translucide.
- Les états sélectionnés utilisent une bordure `primary` plus épaisse (1,5 dp sur les
  cartes serveur, 2 dp sur la carte de thème).

## 6. Composants réutilisables

Tous dans `app/src/main/java/com/onfaa/tunnel/ui/components/`.

| Composant | Rôle et états |
| --- | --- |
| `OnfaaBackground` | Fond dégradé + halos, thémé. Contenu en `BoxScope`. |
| `OnfaaLogo` | Bouclier + éclair dessinés en Canvas, couleurs de la palette. |
| `BrandHeader` | Logo, « ONFAA TUNNEL », sous-titre, actions à droite. |
| `ScreenHeader` | Retour optionnel, titre, sous-titre, actions rondes. |
| `ScreenTitle` | Grand titre + sous-titre d’écran. |
| `SectionHeader` | Libellé capitalisé + icône + ligne dégradée + trailing. |
| `GlassCard` | Carte dégradée, bordure, padding paramétrable, clic optionnel. |
| `GradientButton` | Action principale, dégradé primary→secondary, icône, loading, disabled. |
| `TonalButton` | Action secondaire, surface élevée, bordure, compacte ou non. |
| `CircleIconButton` | Action ronde 44 dp, tint, disabled. |
| `ActionTile` | Tuile d’action 2×3 (icône + libellé), accent optionnel. |
| `OnfaaChip` | Filtre pilule, sélectionné/non sélectionné, icône optionnelle. |
| `ProtocolBadge` | `VLESS`/`VMESS` complet, ou `VL`/`VM` compact. |
| `StatusBadge` | Badge d’état capitalisé avec icône optionnelle. |
| `DividerLine` | Séparateur 1 dp avec inset. |
| `SettingRow` | Icône, titre, sous-titre, valeur, chevron ; clic optionnel. |
| `SettingSwitchRow` | Ligne complète cliquable + `Switch` ; le switch n’a pas de handler séparé. |
| `SettingsGroup` | En-tête de section + carte de lignes. |
| `SettingsNotice` | Encart d’information honnête sur les limites. |
| `InlineNotice` | Message contextuel (info/erreur), pastille + titre + texte. |
| `ConnectionOrb` | Orbe 226 dp, pulsation réelle selon l’état, clic connexion/déconnexion. |
| `TrafficChart` | Sparkline descendant/montant, uniquement alimentée par des échantillons réels. |
| `TrafficLegend` | Débits descendant/montant formatés. |
| `LogEventRow` | Heure, icône de sévérité, message, fond vert/rouge pour succès/erreur. |
| `SeverityIcon` | Icône et teinte par sévérité. |
| `DiagnosticCard` | Badge global, titre, sous-titre, liste de contrôles avec états. |
| `LogActionGrid` | Grille 2×3 : Copier, Effacer, Filtrer, Exporter, Rapport, Rechercher. |
| `ThemeModeSelector` | 3 segments Système/Clair/Sombre. |
| `ThemePreviewCard` | Vignette de palette, sélection, check. |
| `CurrentThemeCard` | Palette et mode actuels, tagline. |
| `OnfaaBottomBar` | Barre à 4 onglets, indicateur, couleurs de palette. |

## 7. Écrans

### 7.1 Connexion / Accueil — **reconstruit, sans référence**

Structure :

1. `BrandHeader` avec sous-titre « Votre liberté, notre priorité » et accès Paramètres.
2. `ConnectionOrb` + état réel (`Déconnecté`, `Connexion en cours…`,
   `Vérification du tunnel…`, `Connecté`, `Déconnexion…`, `Erreur de connexion`).
3. Durée de session réelle quand connecté.
4. Erreur réelle assainie si présente.
5. Carte serveur sélectionné (protocole, nom, endpoint) ou état vide avec appel à
   l’import.
6. Section « Trafic de la session » : octets téléchargés/envoyés, débits, badge
   « En direct / Dernière session / En attente / Indisponible », graphique réel.
7. Mention explicite de la source `TrafficStats` (UID application) et de l’absence de
   valeurs simulées.

Actions : l’orbe demande le consentement VPN puis démarre le service ; un second clic
déconnecte. La carte serveur navigue vers Configs.

### 7.2 Configs / Serveurs

Structure :

1. En-tête « Configs », sous-titre, retour, tri, ajout.
2. Recherche par nom ou endpoint.
3. Filtres : Tous, Favoris, VLESS, VMess (défilement horizontal si nécessaire).
4. Deux actions : « Tester toutes les configs », « Sélectionner le plus rapide ».
5. Liste de cartes : badge VL/VM, nom, endpoint monospace, latence TCP réelle, étoile
   favori, badge « SÉLECTIONNÉE », chevron.
6. Carte d’aide : glisser à gauche, appui long ou menu pour ouvrir les options.

Dialogues :

- **Détail** : endpoint, sécurité, latence, état, sélection, favori, test, suppression.
  Aucun UUID, clé ou JSON interne n’est affiché.
- **Import** : lien VLESS/VMess, fichier texte (max 64 Kio), QR désactivé « bientôt »,
  validation réelle par Xray avant ajout.
- **Suppression** : confirmation explicite.

Tri : import récent, nom A–Z, latence croissante, favoris d’abord. La latence est une
**connexion TCP réelle** de 3 s maximum, pas une mesure de handshake Xray ; ce n’est
pas une promesse de qualité de tunnel.

### 7.3 Paramètres — vue générale

En-tête avec retour, logo, « ONFAA TUNNEL », titre « Paramètres », recherche.

Groupes :

- **Connexion** : vitesse dans la notification, reconnexion automatique, timeout,
  délai de reconnexion, comportement en arrière-plan.
- **Anti-censorship** : TLS & SNI, fragmentation TLS (valeurs, pas de faux résultat).
- **Modes avancés** : Ad blocker, Gaming mode, accès aux réglages avancés.
- **Réseau** : DNS, Routage, Moteur réseau, Partage hotspot.
- **Interface** : Apparence, Journaux.
- **Plus** : Automation, Diagnostics, À propos.

La recherche filtre une liste de destinations et navigue vers la sous-page ; elle ne
prétend pas modifier un réglage directement.

### 7.4 Sous-pages de paramètres

| Écran | Contenu | Vérité |
| --- | --- | --- |
| Connexion | notifications, reconnexion, timeout, délai, arrière-plan | préférences persistées ; non raccordées au cycle de vie actuel |
| Anti-censorship | SNI, fragmentation TLS | préférences UI ; **moteur Xray inchangé** |
| Modes avancés | Ad blocker, Gaming mode, liens moteur/hotspot | préférences UI ; aucun filtrage simulé |
| DNS | DoH, choix du résolveur | le service utilise réellement `1.1.1.1:53` ; le choix n’est pas encore transmis |
| Routage | 3 modes | le routage réel reste celui de `XrayConfigBuilder` |
| Moteur réseau | MTU, Mux | le service conserve MTU 1500 et le mux par défaut |
| Hotspot | partage | aucun proxy n’est exposé |
| Automation | connexion auto | deep links non déclarés |
| Diagnostics | état réel, nombre d’événements, niveau de log, ouverture des journaux | niveau persisté, non appliqué au service |
| À propos | version, package, API Android, version moteur interrogée réellement | erreur moteur affichée sans invention |

Chaque option non raccordée est signalée dans l’UI par un encart ou un sous-titre. Le
but est d’éviter une fausse impression de protection.

### 7.5 Apparence

1. En-tête retour + icône palette.
2. « Mode d’affichage » : Système / Clair / Sombre.
3. « Thèmes disponibles » : grille 3 colonnes, 7 vignettes, coche sur la sélection.
4. Carte « Thème actuel » : palette, tagline, mode.
5. Note : 7 palettes × 3 modes = 21 combinaisons, appliquées à toute l’app.

Les vignettes utilisent une version sombre pour toutes les palettes sauf Paper, afin de
rester lisibles et proches de la maquette. Le changement est immédiat et persistant.

### 7.6 Journaux / Diagnostics

1. En-tête « Journaux », sous-titre, corbeille, partage.
2. Filtres Tous / Info / Avertissement / Erreur.
3. Recherche repliable.
4. Compteur d’événements visibles + limite mémoire (200).
5. Liste réelle : heure, sévérité, message assaini ; succès en vert, erreur en rouge.
6. `DiagnosticCard` réelle :
   - moteur libXray disponible/indisponible (vérification réelle) ;
   - tunnel actif/inactif/erreur (état réel) ;
   - serveur configuré (protocole) ;
   - latence TCP testée/injoignable/non testée ;
   - résolveur réellement configuré (`1.1.1.1:53`) ;
   - fuites DNS/IP : **non testées**.
7. Actions : Copier, Effacer (confirmation), Filtrer (cycle), Exporter les journaux
   (partage texte), Rapport (diagnostic + journaux, partage texte), Rechercher.
8. Mention : aucune détection de fuite implémentée, aucun UUID/clé/lien complet
   journalisé, export par partage Android sans fichier temporaire.

## 8. États et interactions

### Boutons

| État | Rendu |
| --- | --- |
| Normal | dégradé ou surface pleine, texte contrasté |
| Pressé | ripple Material, pas de changement de taille |
| Désactivé | alpha 0,45, clic bloqué, sémantique `Role.Button` |
| Chargement | spinner 18 dp, libellé de progression, clic bloqué |
| Erreur | message `InlineNotice` ou texte `danger`, jamais un faux succès |

### Cartes et listes

- Carte serveur : clic = détail ; appui long = détail ; glissement horizontal vers la
  gauche = détail (seuil 56 px) ; étoile = favori.
- Le glissement est une **ouverture du panneau d’options**, pas un dismiss destructif.
- Aucune action destructive sans confirmation (suppression, effacement des logs).
- Les états vides expliquent la prochaine action utile.

### Chargements

- Latence : spinner compact + texte « Connexions TCP réelles vers les serveurs… ».
- Import : « Validation par Xray… », bouton désactivé.
- Moteur : « Vérification… » tant que la disponibilité n’est pas connue.
- Aucun skeleton ne suggère de fausses données.

### Animations

- Orbe : pulsation lente liée à l’état (pas de pulsation « connecté » au repos).
- Thème : application immédiate, pas de transition qui masque l’état.
- Graphique : rendu des échantillons réels, pas d’animation inventée.
- Aucune animation ne crée de valeur ou de connexion fictive.

## 9. Règles de données réelles

1. **Connexion** : `Connected` n’est publié qu’après `runXray`, `getXrayState` et une
   sonde HTTP réelle réussie.
2. **Trafic** : `TrafficStats` de l’UID applicatif, échantillonné par le service. Si la
   plateforme renvoie `UNSUPPORTED`, l’UI affiche « Indisponible ». Les octets incluent
   le trafic du moteur Xray, pas un compteur noyau du TUN.
3. **Latence** : connexion TCP réelle, timeout 3 s. Elle indique la joignabilité, pas la
   qualité du tunnel.
4. **Diagnostics** : uniquement état, disponibilité, configuration et résultats réels.
   Aucune détection de fuite DNS/IP n’est implémentée ; l’UI ne dit jamais « aucune
   fuite ».
5. **Journal** : messages produits par le service, assainis par `XrayErrorSanitizer`.
   Aucun UUID, clé ou lien complet.
6. **Options non raccordées** : persistées, affichées, mais explicitement décrites comme
   UI-only. Le moteur Xray n’est pas modifié par ces options dans cette phase.
7. **Profils** : mémoire uniquement dans cette phase ; l’UI le dit dans l’import et la
   suppression.

## 10. Accessibilité et i18n

- Cibles tactiles principales : 44–50 dp.
- Contrastes vérifiés par tokens ; les textes secondaires ne portent pas d’information
  critique seuls.
- Les icônes décoratives ont `contentDescription = null` ; les actions ont un libellé.
- Textes français codés en dur dans cette phase. L’i18n (`strings.xml`) est différée ;
  les références anglaises ne sont pas encore une locale supportée.
- Les états ne reposent pas uniquement sur la couleur : texte, icône et badge.

## 11. Mapping fichiers

| Domaine | Fichiers |
| --- | --- |
| Thèmes et tokens | `ui/theme/ThemeCatalog.kt`, `ui/theme/Theme.kt` |
| Composants | `ui/components/*.kt` |
| Accueil | `feature/home/HomeScreen.kt` |
| Configs | `feature/configs/ConfigsScreen.kt`, `data/config/LatencyRepository.kt` |
| Paramètres | `feature/settings/SettingsScreen.kt`, `SettingsRoutes.kt`, `SettingsSubScreens.kt` |
| Apparence | `feature/appearance/AppearanceScreen.kt` |
| Journaux | `feature/logs/LogsScreen.kt`, `ui/components/LogComponents.kt` |
| Navigation | `MainActivity.kt`, `ui/components/OnfaaBottomBar.kt` |
| Préférences | `domain/model/UserPreferences.kt`, `data/preferences/UserPreferencesRepository.kt` |
| Trafic | `vpn/TrafficStatsRepository.kt`, `vpn/OnfaaTunnelVpnService.kt` |
| État/journal | `vpn/TunnelStatus.kt` |
| Références | `docs/design/reference/` |

## 12. Limites et points non revendiqués

- **Aucune référence d’accueil** : la fidélité de l’accueil ne peut pas être affirmée.
- **Compilation non exécutée localement** : aucun JDK ni SDK Android sur le téléphone.
  La validation doit passer par la CI. Aucun succès de build n’est revendiqué ici.
- **Options moteur non raccordées** : fragmentation, ad block, gaming, hotspot,
  routage, DNS choisi, MTU, mux, niveau de log, reconnexion automatique.
- **Pas de détection de fuite** : la maquette Logs l’affiche, l’application ne le peut
  pas sans implémentation réelle.
- **Pas de QR code** : marqué « bientôt ».
- **Persistance des profils** : mémoire uniquement.
- **Swipe** : ouvre le détail ; ce n’est pas un `SwipeToDismiss` avec révélation
  d’actions intégrée.
- **Langue** : français uniquement.
- **Barre de navigation** : 4 onglets alors que 3 apparaissent sur certaines maquettes.

## 13. Checklist de vérification UI

- [x] 5 images classées, 0 non identifiée, originaux inchangés.
- [x] Thèmes appliqués à tous les écrans et composants.
- [x] 7 palettes × 3 modes persistés.
- [x] Navigation à 4 onglets + sous-pages.
- [x] Accueil branché sur l’état réel, le consentement VPN et `TrafficStats`.
- [x] Configs : import, sélection, favoris, latence réelle, tri, détail.
- [x] Paramètres : groupes, recherche, sous-pages, préférences persistées.
- [x] Apparence : modes, grille, thème actuel.
- [x] Journaux : filtres, recherche, copie, effacement, partage, diagnostic réel.
- [x] Aucune statistique, connexion ou fuite simulée.
- [ ] Compilation et tests instrumentés à exécuter en CI.
- [ ] Validation visuelle sur appareil à effectuer.

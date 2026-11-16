# Référence API libXray (vérifiée) — libXray v26.11.9

> **Source de vérité** : dépôt officiel `XTLS/libXray`, tag `v26.11.9`, commit `5a0f7d8d1c28b34afb5727b04b26a10f6dbac7e3`. Seules les méthodes/structures observées dans le code source à ce tag sont documentées ici. Aucune signature n’est inventée.

## 1. Point d’entrée structuré (toutes plateformes)

### 1.1 `Invoke`

| Champ | Valeur |
|---|---|
| **Nom exact** | `Invoke` |
| **Emplacement** | `invoke.go` (`package libXray`) — https://github.com/XTLS/libXray/blob/v26.11.9/invoke.go |
| **Signature Go** | `func Invoke(requestJSON string) string` |
| **Paramètres** | `requestJSON` : chaîne JSON représentant `LibXrayInvokeRequest` |
| **Résultat** | Chaîne JSON représentant `invokeResponse` (`{success, data, error}`) |
| **Erreurs possibles (observées)** | dépassement taille JSON (16 MiB req/resp), JSON invalide, `apiVersion` non supportée, méthode inconnue, erreurs métier retournées par les sous-systèmes (`xray`, `share`, `geo`, `nodep`, etc.) encapsulées dans `error` |

**Contrat JSON :**
- Requête : `{"apiVersion":3,"method":"<LibXrayMethod>","payload":{...}}` (`LibXrayInvokeRequest` dans `invoke_model.go`)
- Réponse succès : `{"success":true,"data":<objet|{}|null selon cas>,"error":""}`
- Réponse échec : `{"success":false,"data":<valeur courante>, "error":"<message>"}`

### 1.2 Exports C (cgo_bridge)

| Champ | Valeur |
|---|---|
| **Noms exacts** | `CGoInvoke`, `CGoFree` |
| **Emplacement** | `cgo_bridge/main.go` — https://github.com/XTLS/libXray/blob/v26.11.9/cgo_bridge/main.go |
| **Signatures (C)** | `char* CGoInvoke(char* requestJSON);` / `void CGoFree(char* value);` |
| **Comportement** | `CGoInvoke` alloue la chaîne réponse (`CString`) ; l’appelant doit libérer toute réponse non nulle via `CGoFree` (pas d’allocateur plateforme direct). |
| **Utilisation Kotlin/Android (indicative)** | Via classes générées par **gomobile bind** dans l’AAR (noms de packages/méthodes JVM à confirmer par inspection de l’AAR produit). Pour une intégration C/FFI, utiliser les exports C du bridge. |

## 2. Constantes et modèles (invoke_model.go)

**Fichier** : `invoke_model.go` — https://github.com/XTLS/libXray/blob/v26.11.9/invoke_model.go

- `LibXrayAPIVersion = 3`
- `LibXrayMethod` (string) : `getFreePorts`, `decodeSubscriptionText`, `convertShareLinksToXrayJson`, `convertXrayJsonToShareLinks`, `generateAgeKeyPair`, `countGeoData`, `pingBatch`, `testXray`, `runXray`, `stopXray`, `xrayVersion`, `getXrayState`

**Structures de requête/réponse (champs JSON vérifiés)** :

| Méthode | Payload requête | Data réponse (succès) |
|---|---|---|
| `getFreePorts` | `GetFreePortsRequest{count, excludePorts[]}` | `GetFreePortsResponse{ports[]}` |
| `decodeSubscriptionText` | `DecodeSubscriptionTextRequest{text, age{secretKey}}` | `DecodeSubscriptionTextResponse{text}` |
| `convertShareLinksToXrayJson` | `ConvertShareLinksToXrayJsonRequest{text, age{secretKey}}` | résultat de `share.ConvertShareLinksToXrayJson` (structure définie côté package `share`) |
| `convertXrayJsonToShareLinks` | `ConvertXrayJsonToShareLinksRequest{xrayJson}` | `ConvertXrayJsonToShareLinksResponse{links}` |
| `generateAgeKeyPair` | `GenerateAgeKeyPairRequest{keyType}` (`x25519`/`hybrid`) | `GenerateAgeKeyPairResponse{secretKey, publicKey}` |
| `countGeoData` | `CountGeoDataRequest{name, geoType, datDir}` | `{}` (erreur si `datDir` manquant ou échec geo) |
| `pingBatch` | `PingBatchRequest{configs[]{xrayJson,outboundTag}, timeout, url}` | `PingBatchResponse{results[]{success, delay, error}}` |
| `testXray` | `TestXrayRequest{xrayJson}` | `{}` (validation/construction/fermeture sans Start) |
| `runXray` | `RunXrayRequest{xrayJson}` | `{}` |
| `stopXray` | (aucun payload requis) | `{}` |
| `xrayVersion` | (aucun payload requis) | `XrayVersionResponse{version}` |
| `getXrayState` | (aucun payload requis) | `GetXrayStateResponse{running}` |

## 3. Cycle de vie Xray (package `xray`)

### 3.1 `RunXray`

| Champ | Valeur |
|---|---|
| **Nom** | `RunXray` |
| **Emplacement** | `xray/xray.go` — https://github.com/XTLS/libXray/blob/v26.11.9/xray/xray.go |
| **Signature** | `func RunXray(xrayJSON string) error` |
| **Paramètres** | `xrayJSON` : configuration Xray JSON sérialisée |
| **Résultat** | `error` (`nil` si démarrage réussi) |
| **Erreurs/comportements** | `ErrAlreadyRunning` si une instance est déjà active ; erreurs de `core.LoadConfig`, `core.New`, `server.Start()` (avec fermeture en cas d’échec de Start). Initialise mémoire, stocke instance globale, libère mémoire OS. |

### 3.2 `StopXray`

| Champ | Valeur |
|---|---|
| **Nom** | `StopXray` |
| **Emplacement** | `xray/xray.go` |
| **Signature** | `func StopXray() error` |
| **Résultat** | `error` de `Close()` si instance présente |

### 3.3 `GetXrayState`

| Champ | Valeur |
|---|---|
| **Nom** | `GetXrayState` |
| **Emplacement** | `xray/xray.go` |
| **Signature** | `func GetXrayState() bool` |
| **Résultat** | `true` si instance présente et en cours d’exécution |

### 3.4 `XrayVersion`

| Champ | Valeur |
|---|---|
| **Nom** | `XrayVersion` |
| **Emplacement** | `xray/xray.go` |
| **Signature** | `func XrayVersion() string` |
| **Résultat** | version reportée par `core.Version()` |

### 3.5 `TestXray`

| Champ | Valeur |
|---|---|
| **Nom** | `TestXray` |
| **Emplacement** | `xray/validation.go` — https://github.com/XTLS/libXray/blob/v26.11.9/xray/validation.go |
| **Signature** | `func TestXray(xrayJSON string) error` |
| **Comportement** | Construit puis ferme une instance **sans** appeler `Start()` ; refuse si une instance gérée est déjà active (`testXray requires an isolated process without a managed Xray instance`). Ne teste pas la connectivité réseau ni les ressources de démarrage. |

## 4. API Android spécifique (`//go:build android`)

**Fichier** : `android_wrapper.go` — https://github.com/XTLS/libXray/blob/v26.11.9/android_wrapper.go

| Nom | Signature | Rôle (vérifié) |
|---|---|---|
| `SetDNS` | `func SetDNS(controller DialerController, server string) error` | Installe un résolveur de processus compatible VPN Android ; `server` doit être un endpoint IP (ex. `8.8.8.8:53`, `[2001:4860:4860::8888]:53`) ; protection des sockets via callback. |
| `ResetDNS` | `func ResetDNS()` | Restaure le résolveur actif avant `SetDNS`. |
| `RegisterDialerController` | `func RegisterDialerController(controller DialerController)` | Enregistre callback de protection des FDs (dialer). |
| `RegisterListenerController` | `func RegisterListenerController(controller DialerController)` | Enregistre callback de protection des FDs (listener). |
| `RegisterProcessFinder` | `func RegisterProcessFinder(finder ProcessFinder, sdkVersion int)` | Enregistre résolveur de processus pour routage per-app ; `nil` pour désenregistrer ; comportement `/proc` vs callback Java selon `sdkVersion` (voir commentaires source). |

**Interfaces** :
- `DialerController { ProtectFd(int) bool }`
- `ProcessFinder { FindProcessByConnection(network, srcIP string, srcPort int, destIP string, destPort int) int }`

## 5. Notes d’intégration Kotlin/Android (à confirmer à l’intégration)

- L’AAR gomobile expose des bindings Java/Kotlin générés ; les noms exacts de classes/méthodes côté JVM doivent être confirmés par inspection de l’AAR retenu (non effectué dans cette phase).
- **Obligation** : un seul runtime Go par processus (avertissement README libXray) — ne pas charger simultanément un autre runtime Go/cgo/gomobile indépendant.
- **Sécurité/robustesse** : ne pas logger de configurations sensibles (UUID, clés) ; valider les entrées avant appel ; gérer les erreurs `success=false`.

## 6. Hors périmètre / non exposé directement

- Pas de méthode `Invoke` dédiée aux “statistiques trafic temps réel” dans `invoke_model.go` (v26.11.9). Les capacités de stats Xray-core éventuelles relèvent de la configuration/API Xray et doivent être étudiées séparément.
- L’établissement du tunnel VPN Android (`VpnService`, TUN, routage) n’est pas réalisé par libXray seul ; libXray fournit le moteur/contrôles applicables selon intégration.
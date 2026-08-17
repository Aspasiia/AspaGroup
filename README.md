# AspaGroup

Plugin Paper (Minecraft) de gestion de groupes pour serveurs francophones : groupes de joueurs, chat de groupe, couleurs personnalisées, et revendication de chunks avec affichage des territoires.

## Pour qui ?

AspaGroup est pensé pour les **petits serveurs** entre amis qui veulent des équipes identifiables et un minimum de protection de territoire, sans installer une usine à gaz type Towny, Factions ou GriefPrevention, ni le gestionnaire de permissions et la base de données qui vont avec.

L'idée est de rester le plus simple possible :

- **Aucune dépendance** : il suffit de déposer le `.jar` dans `plugins/` et de redémarrer.
- **Aucun plugin de permissions requis** : toutes les commandes fonctionnent d'office, celles d'administration (`/group admin`) étant réservées aux op. Si vous *avez* un gestionnaire de permissions, les nœuds `aspagroup.*` restent évidemment utilisables.
- **Aucune base de données** : trois petits fichiers YAML dans le dossier du plugin, rien d'autre. Le jar pèse moins de 70 Ko.
- **Le gameplay vanilla n'est pas modifié** : le plugin ajoute des commandes, un préfixe de groupe dans le chat et la liste des joueurs, et un affichage de territoire dans l'Action Bar. Rien de plus.

> ### ⚠️ Fourni tel quel — aucun support
>
> Ce plugin est publié **tel quel** (*as-is*), **sans aucune garantie**, expresse ou implicite.
>
> Il a été développé pour les besoins spécifiques d'un serveur privé, et il est partagé uniquement au cas où il serait utile à quelqu'un d'autre.
>
> **Je ne fournis aucun support**, sous quelque forme que ce soit :
>
> - pas d'aide à l'installation, à la compilation ou à la configuration ;
> - pas de dépannage, même si le plugin ne fonctionne pas chez vous ;
> - pas de réponse garantie aux *issues*, *pull requests* ou messages privés ;
> - aucune promesse de maintenance, de correctifs ou de compatibilité avec les futures versions de Minecraft.
>
> Vous l'utilisez **à vos propres risques**. Si vous ne savez pas compiler un plugin Paper ou lire un log d'erreur, ce dépôt n'est probablement pas fait pour vous.

## Prérequis

- Java 21
- Maven 3.6+
- Un serveur Paper 1.21.x

## Compilation

```bash
mvn clean package
```

Le JAR est généré dans `target/AspaGroup-1.1.0.jar`.

## Installation

1. Copier `target/AspaGroup-1.1.0.jar` dans le dossier `plugins/` du serveur
2. Redémarrer le serveur
3. Ajuster `plugins/AspaGroup/config.yml` puis redémarrer

## Commandes

Toutes les commandes utilisent `/group` (alias `/g`).

### Commandes de base

| Commande | Description |
|---|---|
| `/group create <nom>` | Créer un groupe (16 caractères max) |
| `/group join <groupe>` | Demander à rejoindre un groupe |
| `/group leave` | Quitter son groupe |
| `/group info [groupe]` | Informations sur un groupe |
| `/group list` | Lister tous les groupes du serveur |
| `/group players [groupe]` | Membres d'un groupe, avec statut en ligne/hors ligne |
| `/group chat <message>` | Envoyer un message unique à son groupe |
| `/group togglechat` | Basculer tout son chat vers le groupe uniquement |
| `/group help` | Aide détaillée en jeu |

### Commandes du propriétaire

Réservées au propriétaire du groupe.

| Commande | Description |
|---|---|
| `/group accept <joueur>` | Accepter une demande d'adhésion |
| `/group deny <joueur>` | Refuser une demande d'adhésion |
| `/group kick <joueur>` | Expulser un membre |
| `/group transfer <joueur>` | Transférer la propriété du groupe |
| `/group rename <nom>` | Renommer le groupe |
| `/group color <couleur>` | Couleur nommée ou hex (`#ff6600`) |
| `/group description [texte]` | Voir ou modifier la description (alias `/group desc`) |

### Territoires

| Commande | Description | Permission | Défaut |
|---|---|---|---|
| `/group claim` | Revendiquer le chunk actuel (propriétaire) | `aspagroup.claim` | tous |
| `/group unclaim` | Abandonner le chunk actuel (propriétaire) | — | tous |
| `/group claimlist [groupe]` | Lister les chunks revendiqués | — | tous |
| `/group claiminfo [monde] [x] [z]` | Info sur un chunk, avec carte des territoires 15×15 | — | tous |
| `/group hud [toggle\|on\|off\|status]` | Contrôler l'affichage des territoires | `aspagroup.hud` | tous |

### Commandes administratives

Réservées aux op.

| Commande | Description |
|---|---|
| `/group admin delete <groupe>` | Supprimer un groupe |
| `/group admin info <groupe>` | Informations détaillées sur un groupe |
| `/group admin list` | Liste administrative de tous les groupes |
| `/group admin kick <joueur>` | Expulser un joueur de son groupe |
| `/group admin add <joueur> <groupe>` | Ajouter un joueur à un groupe (transfert auto) |
| `/group admin setowner <groupe> <joueur>` | Changer le propriétaire |
| `/group admin setcolor <groupe> <couleur>` | Changer la couleur d'un groupe |
| `/group admin join <groupe>` | Rejoindre un groupe en contournant les restrictions |
| `/group admin takeownership <groupe>` | Prendre la propriété d'un groupe |
| `/group admin claiminfo <monde> <x> <z>` | Info sur un chunk précis |
| `/group admin unclaim <monde> <x> <z>` | Forcer l'abandon d'un chunk |
| `/group admin claimlist [groupe]` | Lister les chunks d'un groupe |
| `/group admin claimstats` | Statistiques des revendications du serveur |

## Fonctionnalités

- **Couleurs de groupe** : 15 couleurs nommées (`RED`, `BLUE`, `GOLD`, `DARK_PURPLE`…) ou n'importe quelle couleur hex RGB, appliquée au chat et à la liste des joueurs (`[NomGroupe] Joueur: message`)
- **Mode chat de groupe** : `/group togglechat` route tous les messages tapés vers les seuls membres du groupe, sans préfixe à retaper
- **Demandes d'adhésion** : le propriétaire est notifié en direct, et les demandes expirent d'elles-mêmes après 10 minutes
- **Territoires** : affichage en Action Bar de « Zone: [NomGroupe] » ou « Terre sauvage » aux couleurs du groupe, mis à jour au franchissement de chunk, activable ou désactivable par joueur
- **Complétion contextuelle** sur toutes les sous-commandes : noms de groupes, joueurs, couleurs, demandes en attente
- **Nettoyage automatique** : groupes vides supprimés et demandes expirées purgées sans intervention

> ⚠️ Les revendications de chunks servent à **délimiter et afficher** un territoire. Le plugin **n'empêche pas** de casser, poser ou interagir avec des blocs dans le territoire d'un autre groupe : il n'y a aucune protection anti-grief.

## Permissions

Le plugin fonctionne sans gestionnaire de permissions. Trois nœuds sont réellement vérifiés dans le code :

| Permission | Effet | Défaut |
|---|---|---|
| `aspagroup.claim` | Revendiquer un chunk (`/group claim`) | tous |
| `aspagroup.hud` | Utiliser l'affichage des territoires | tous |
| `aspagroup.claim.unlimited` | Ignorer la limite de chunks par groupe | op |

Trois autres nœuds sont déclarés dans `plugin.yml` mais **ne sont pas vérifiés** : `aspagroup.create`, `aspagroup.join` et `aspagroup.admin`. Les commandes correspondantes sont ouvertes à tous, sauf `/group admin` qui teste directement le statut op. Les retirer à un joueur n'aura donc aucun effet.

## Configuration

Toutes les options sont dans `src/main/resources/config.yml` (copié dans `plugins/AspaGroup/config.yml` au premier démarrage) :

| Clé | Défaut | Description |
|---|---|---|
| `join-request-expire-minutes` | `10` | Durée de validité d'une demande d'adhésion |
| `max-group-size` | `10` | Nombre maximum de membres par groupe |
| `max-groups-per-server` | `50` | Nombre maximum de groupes sur le serveur |
| `max-claims-per-group` | `20` | Nombre maximum de chunks par groupe |
| `enable-chunk-hud` | `true` | Activer l'affichage des territoires |
| `hud-update-cooldown-ms` | `100` | Délai minimum entre deux mises à jour de l'affichage |
| `hud-refresh-interval-ticks` | `40` | Intervalle de rafraîchissement de l'affichage (40 ticks = 2 s) |
| `allow-claim-overlap` | `false` | Plusieurs groupes sur un même chunk — **non implémenté** |

La section `messages:` contient l'intégralité des textes affichés aux joueurs, tous personnalisables.

## Données persistées

Générées dans `plugins/AspaGroup/` au runtime, jamais versionnées :

- `groups.yml` — groupes, membres, propriétaires, couleurs, descriptions
- `chunks.yml` — revendications de chunks, au format `monde:chunkX:chunkZ: nomGroupe`
- `hud-preferences.yml` — préférence d'affichage des territoires, par joueur

C'est tout : trois fichiers YAML, aucune base de données.

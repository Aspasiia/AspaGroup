package com.aspa.aspagroup.commands;

import com.aspa.aspagroup.AspaGroup;
import com.aspa.aspagroup.models.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GroupCommand implements CommandExecutor, TabCompleter {
    
    private final AspaGroup plugin;
    
    public GroupCommand(AspaGroup plugin) {
        this.plugin = plugin;
    }
    
    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    private void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
    
    private void sendSuccessMessage(Player player, String message) {
        player.sendMessage(ChatColor.GREEN + message);
        playSuccessSound(player);
    }
    
    private void sendErrorMessage(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        playErrorSound(player);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par les joueurs !");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
            case "join":
                handleJoin(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "deny":
                handleDeny(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "color":
                handleColor(player, args);
                break;
            case "rename":
                handleRename(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "players":
                handlePlayers(player, args);
                break;
            case "description":
            case "desc":
                handleDescription(player, args);
                break;
            case "admin":
                handleAdmin(player, args);
                break;
            case "chat":
                handleGroupChat(player, args);
                break;
            case "claim":
                handleClaim(player);
                break;
            case "unclaim":
                handleUnclaim(player);
                break;
            case "claimlist":
                handleClaimList(player, args);
                break;
            case "claiminfo":
                handleClaimInfo(player, args);
                break;
            case "hud":
                handleHudToggle(player, args);
                break;
            case "togglechat":
                handleToggleChat(player);
                break;
            case "help":
                sendDetailedHelpMessage(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(player, "Usage : /group create <nom>");
            return;
        }
        
        String groupName = args[1];
        
        if (groupName.length() > 16) {
            sendErrorMessage(player, "Le nom du groupe ne peut pas dépasser 16 caractères !");
            return;
        }
        
        if (plugin.getGroupManager().createGroup(groupName, player.getUniqueId())) {
            sendSuccessMessage(player, "Groupe '" + groupName + "' créé avec succès !");
            plugin.getGroupManager().updatePlayerDisplay(player);
        } else {
            sendErrorMessage(player, "Impossible de créer le groupe ! Vous êtes peut-être déjà dans un groupe ou le nom est déjà pris.");
        }
    }
    
    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group join <nom_groupe>");
            return;
        }
        
        String groupName = args[1];
        Group group = plugin.getGroupManager().getGroup(groupName);
        
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Group '" + groupName + "' not found!");
            return;
        }
        
        if (plugin.getGroupManager().getPlayerGroup(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Vous êtes déjà dans un groupe !");
            return;
        }
        
        if (group.hasJoinRequest(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You already have a pending request to this group!");
            return;
        }
        
        plugin.getGroupManager().addJoinRequest(groupName, player.getUniqueId());
        sendSuccessMessage(player, "Join request sent to group '" + groupName + "'!");
        
        Player owner = Bukkit.getPlayer(group.getOwner());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.YELLOW + player.getName() + " wants to join your group! Use /group accept " + player.getName());
        }
    }
    
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group accept <joueur>");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can accept join requests!");
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        
        // Check if group is full before accepting
        int maxGroupSize = plugin.getConfig().getInt("max-group-size", 10);
        if (group.getMemberCount() >= maxGroupSize) {
            player.sendMessage(ChatColor.RED + "Le groupe '" + group.getName() + "' est plein ! (Maximum " + maxGroupSize + " membres)");
            return;
        }
        
        if (plugin.getGroupManager().acceptJoinRequest(group.getName(), player.getUniqueId(), targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " a rejoint le groupe !");
            playSuccessSound(player);
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage(ChatColor.GREEN + "Vous avez été accepté dans le groupe '" + group.getName() + "' !");
                plugin.getGroupManager().updatePlayerDisplay(onlineTarget);
                playSuccessSound(onlineTarget);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Impossible d'accepter la demande d'adhésion !");
            playErrorSound(player);
        }
    }
    
    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group deny <joueur>");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can deny join requests!");
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        
        if (plugin.getGroupManager().denyJoinRequest(group.getName(), player.getUniqueId(), targetPlayer.getUniqueId())) {
            sendSuccessMessage(player, "La demande d'adhésion de " + targetPlayer.getName() + " a été refusée !");
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage(ChatColor.RED + "Votre demande d'adhésion au groupe '" + group.getName() + "' a été refusée.");
                playErrorSound(onlineTarget);
            }
        } else {
            sendErrorMessage(player, "Impossible de refuser la demande d'adhésion !");
        }
    }
    
    private void handleLeave(Player player) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (plugin.getGroupManager().removePlayerFromGroup(player.getUniqueId())) {
            sendSuccessMessage(player, "Vous avez quitté le groupe !");
            plugin.getGroupManager().updatePlayerDisplay(player);
        } else {
            sendErrorMessage(player, "Impossible de quitter le groupe !");
        }
    }
    
    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group kick <joueur>");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can kick members!");
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        
        if (!group.isMember(targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " is not in your group!");
            return;
        }
        
        if (group.isOwner(targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot kick yourself!");
            return;
        }
        
        if (plugin.getGroupManager().removePlayerFromGroup(targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " a été expulsé du groupe !");
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage(ChatColor.RED + "Vous avez été expulsé du groupe '" + group.getName() + "' !");
                plugin.getGroupManager().updatePlayerDisplay(onlineTarget);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Impossible d'expulser le joueur !");
        }
    }
    
    private void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group color <couleur>");
            player.sendMessage(ChatColor.GRAY + "Couleurs disponibles : red, blue, green, yellow, light_purple, aqua, white, gray, dark_red, dark_blue, dark_green, gold, dark_purple, dark_aqua");
            player.sendMessage(ChatColor.GRAY + "Ou utilisez un code hex : #ff0000, #00ff00, #0000ff, etc.");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can change the group color!");
            return;
        }
        
        String colorInput = args[1];
        String hexColor = parseColor(colorInput);
        
        if (hexColor == null) {
            player.sendMessage(ChatColor.RED + "Couleur invalide ! Utilisez un nom (red, blue, etc.) ou un code hex (#ff0000)");
            return;
        }
        
        group.setColorHex(hexColor);
        plugin.getGroupManager().saveGroups();
        
        // Send confirmation with color preview
        Component colorPreview = Component.text("Couleur du groupe changée vers ", NamedTextColor.GREEN)
            .append(Component.text(colorInput, group.getTextColor()))
            .append(Component.text(" !", NamedTextColor.GREEN));
        player.sendMessage(colorPreview);
        
        // Update display for all group members using the new method
        plugin.getGroupManager().updateGroupDisplay(group.getName());
    }
    
    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group rename <nouveau_nom>");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can rename the group!");
            return;
        }
        
        String newName = args[1];
        if (newName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Group name cannot be longer than 16 characters!");
            return;
        }
        
        String oldName = group.getName();
        if (plugin.getGroupManager().renameGroup(oldName, newName, player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Groupe renommé de '" + oldName + "' vers '" + newName + "' !");
            
            // Notify all group members and update their displays
            for (UUID memberId : group.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(player)) {
                    member.sendMessage(ChatColor.YELLOW + "Votre groupe a été renommé vers '" + newName + "' !");
                }
            }
            // Update display for all group members using the new method
            plugin.getGroupManager().updateGroupDisplay(newName);
        } else {
            player.sendMessage(ChatColor.RED + "Impossible de renommer le groupe ! Le nom est peut-être déjà pris.");
        }
    }
    
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage : /group transfer <joueur>");
            return;
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the group owner can transfer ownership!");
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        
        if (plugin.getGroupManager().transferOwnership(group.getName(), player.getUniqueId(), targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Propriété transférée à " + targetPlayer.getName() + " !");
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage(ChatColor.GREEN + "Vous êtes maintenant propriétaire du groupe '" + group.getName() + "' !");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Impossible de transférer la propriété ! Le joueur doit être membre du groupe.");
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        Group group;
        
        if (args.length > 1) {
            group = plugin.getGroupManager().getGroup(args[1]);
            if (group == null) {
                player.sendMessage(ChatColor.RED + "Groupe '" + args[1] + "' introuvable !");
                return;
            }
        } else {
            group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
            if (group == null) {
                player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe ! Utilisez /group info <groupe> pour voir les infos d'un autre groupe.");
                return;
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Informations du Groupe: " + group.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Propriétaire: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(group.getOwner()).getName());
        player.sendMessage(ChatColor.YELLOW + "Membres: " + ChatColor.WHITE + group.getMemberCount());
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + group.getDescription());
        Component colorDisplay = Component.text("Couleur: ", NamedTextColor.YELLOW)
            .append(Component.text(group.getColorHex(), group.getTextColor()));
        player.sendMessage(colorDisplay);
        
        if (!group.getJoinRequests().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Demandes en attente: " + ChatColor.WHITE + group.getJoinRequests().size());
        }
    }
    
    private void handleList(Player player) {
        Collection<Group> groups = plugin.getGroupManager().getAllGroups();
        
        if (groups.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No groups exist!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== All Groups ===");
        for (Group group : groups) {
            Component groupDisplay = Component.text(group.getName(), group.getTextColor())
                .append(Component.text(" (" + group.getMemberCount() + " members)", NamedTextColor.GRAY));
            player.sendMessage(groupDisplay);
        }
    }
    
    private void handlePlayers(Player player, String[] args) {
        Group group;
        
        if (args.length > 1) {
            // Show players of specified group (admin/public feature)
            group = plugin.getGroupManager().getGroup(args[1]);
            if (group == null) {
                player.sendMessage(ChatColor.RED + "Groupe '" + args[1] + "' introuvable !");
                return;
            }
        } else {
            // Show players of player's own group
            group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
            if (group == null) {
                player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe ! Utilisez /group players <groupe> pour voir les membres d'un autre groupe.");
                return;
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Membres du Groupe: " + group.getName() + " ===");
        
        UUID ownerId = group.getOwner();
        int onlineCount = 0;
        int totalCount = group.getMemberCount();
        
        for (UUID memberId : group.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
            String memberName = member.getName();
            
            boolean isOwner = memberId.equals(ownerId);
            boolean isOnline = member.isOnline();
            
            if (isOnline) onlineCount++;
            
            String status = isOnline ? ChatColor.GREEN + "En ligne" : ChatColor.GRAY + "Hors ligne";
            String role = isOwner ? ChatColor.GOLD + " [Propriétaire]" : "";
            
            player.sendMessage(ChatColor.WHITE + "• " + memberName + " " + status + role);
        }
        
        player.sendMessage(ChatColor.YELLOW + "Total: " + ChatColor.WHITE + totalCount + " membres (" + 
                         ChatColor.GREEN + onlineCount + " en ligne" + ChatColor.WHITE + ", " + 
                         ChatColor.GRAY + (totalCount - onlineCount) + " hors ligne" + ChatColor.WHITE + ")");
    }
    
    private void handleDescription(Player player, String[] args) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (args.length == 1) {
            // Show current description
            player.sendMessage(ChatColor.GOLD + "Description du groupe " + group.getName() + ":");
            player.sendMessage(ChatColor.WHITE + group.getDescription());
            return;
        }
        
        if (!group.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le propriétaire peut changer la description !");
            return;
        }
        
        // Set new description
        String newDescription = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (newDescription.length() > 100) {
            player.sendMessage(ChatColor.RED + "La description ne peut pas dépasser 100 caractères !");
            return;
        }
        
        group.setDescription(newDescription);
        plugin.getGroupManager().saveGroups();
        player.sendMessage(ChatColor.GREEN + "Description du groupe mise à jour !");
    }
    
    private void handleAdmin(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Commande réservée aux administrateurs !");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "=== Commandes Admin AspaGroup ===");
            player.sendMessage(ChatColor.YELLOW + "/group admin delete <groupe> - Supprimer un groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin info <groupe> - Infos détaillées sur un groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin list - Liste tous les groupes avec détails");
            player.sendMessage(ChatColor.YELLOW + "/group admin kick <joueur> - Expulser un joueur de son groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin join <groupe> - Rejoindre un groupe (bypass)");
            player.sendMessage(ChatColor.YELLOW + "/group admin takeownership <groupe> - Prendre la propriété d'un groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin claiminfo <monde> <x> <z> - Info sur un chunk spécifique");
            player.sendMessage(ChatColor.YELLOW + "/group admin unclaim <monde> <x> <z> - Forcer l'abandon d'un chunk");
            player.sendMessage(ChatColor.YELLOW + "/group admin claimlist [groupe] - Liste les chunks d'un groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin claimstats - Statistiques des revendications");
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        switch (adminCommand) {
            case "delete":
                handleAdminDelete(player, args);
                break;
            case "info":
                handleAdminInfo(player, args);
                break;
            case "list":
                handleAdminList(player);
                break;
            case "kick":
                handleAdminKick(player, args);
                break;
            case "join":
                handleAdminJoin(player, args);
                break;
            case "add":
                handleAdminAdd(player, args);
                break;
            case "setowner":
                handleAdminSetOwner(player, args);
                break;
            case "setcolor":
                handleAdminSetColor(player, args);
                break;
            case "takeownership":
                handleAdminTakeOwnership(player, args);
                break;
            case "claiminfo":
                handleAdminClaimInfo(player, args);
                break;
            case "unclaim":
                handleAdminUnclaim(player, args);
                break;
            case "claimlist":
                handleAdminClaimList(player, args);
                break;
            case "claimstats":
                handleAdminClaimStats(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Commande admin inconnue !");
                break;
        }
    }
    
    private void handleAdminDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin delete <groupe>");
            return;
        }
        
        String groupName = args[2];
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        plugin.getGroupManager().deleteGroup(groupName);
        player.sendMessage(ChatColor.GREEN + "Groupe '" + groupName + "' supprimé par un administrateur !");
        plugin.getLogger().warning("ADMIN: " + player.getName() + " a supprimé le groupe '" + groupName + "'");
        
        // Notify all online members
        for (UUID memberId : group.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Votre groupe '" + groupName + "' a été supprimé par un administrateur !");
                plugin.getGroupManager().updatePlayerDisplay(member);
            }
        }
    }
    
    private void handleAdminInfo(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin info <groupe>");
            return;
        }
        
        String groupName = args[2];
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Informations Administrateur - " + group.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Propriétaire: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(group.getOwner()).getName());
        player.sendMessage(ChatColor.YELLOW + "Membres (" + group.getMemberCount() + "): ");
        for (UUID memberId : group.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
            String status = member.isOnline() ? ChatColor.GREEN + "En ligne" : ChatColor.GRAY + "Hors ligne";
            player.sendMessage(ChatColor.WHITE + "  - " + member.getName() + " " + status);
        }
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + group.getDescription());
        Component colorDisplay = Component.text("Couleur: ", NamedTextColor.YELLOW)
            .append(Component.text(group.getColorHex(), group.getTextColor()));
        player.sendMessage(colorDisplay);
        player.sendMessage(ChatColor.YELLOW + "Demandes en attente: " + ChatColor.WHITE + group.getJoinRequests().size());
    }
    
    private void handleAdminList(Player player) {
        Collection<Group> allGroups = plugin.getGroupManager().getAllGroups();
        if (allGroups.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun groupe créé.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Liste Administrative des Groupes ===");
        for (Group group : allGroups) {
            String ownerName = Bukkit.getOfflinePlayer(group.getOwner()).getName();
            Component groupDisplay = Component.text(group.getName(), group.getTextColor())
                .append(Component.text(" - ", NamedTextColor.WHITE))
                .append(Component.text(ownerName, NamedTextColor.YELLOW))
                .append(Component.text(" (" + group.getMemberCount() + " membres)", NamedTextColor.WHITE));
            player.sendMessage(groupDisplay);
        }
    }
    
    private void handleAdminKick(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin kick <joueur>");
            return;
        }
        
        String targetName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        UUID targetId;
        
        if (targetPlayer != null) {
            targetId = targetPlayer.getUniqueId();
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                player.sendMessage(ChatColor.RED + "Joueur '" + targetName + "' introuvable !");
                return;
            }
            targetId = offlineTarget.getUniqueId();
        }
        
        Group group = plugin.getGroupManager().getPlayerGroup(targetId);
        if (group == null) {
            player.sendMessage(ChatColor.RED + targetName + " n'est dans aucun groupe !");
            return;
        }
        
        plugin.getGroupManager().removePlayerFromGroup(targetId);
        player.sendMessage(ChatColor.GREEN + targetName + " expulsé du groupe '" + group.getName() + "' !");
        plugin.getLogger().warning("ADMIN: " + player.getName() + " a expulsé " + targetName + " du groupe '" + group.getName() + "'");
        
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.RED + "Vous avez été expulsé de votre groupe par un administrateur !");
            plugin.getGroupManager().updatePlayerDisplay(targetPlayer);
        }
    }

    private void handleAdminJoin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin join <groupe>");
            return;
        }
        
        String groupName = args[2];
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        // Check if admin is already in a group
        Group currentGroup = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (currentGroup != null) {
            player.sendMessage(ChatColor.RED + "Vous êtes déjà membre du groupe '" + currentGroup.getName() + "' ! Quittez d'abord votre groupe.");
            return;
        }
        
        // Check if group is full
        int maxGroupSize = plugin.getConfig().getInt("max-group-size", 10);
        if (group.getMemberCount() >= maxGroupSize) {
            player.sendMessage(ChatColor.RED + "Le groupe est plein (" + maxGroupSize + " membres max) ! Rejoindre quand même en tant qu'administrateur...");
        }
        
        // Add admin to group bypassing normal restrictions
        if (plugin.getGroupManager().addPlayerToGroupBypass(player.getUniqueId(), groupName)) {
            player.sendMessage(ChatColor.GREEN + "Vous avez rejoint le groupe '" + groupName + "' en tant qu'administrateur !");
            plugin.getGroupManager().updatePlayerDisplay(player);
            plugin.getLogger().warning("ADMIN: " + player.getName() + " a rejoint le groupe '" + groupName + "' (bypass administrateur)");
            
            // Notify group members
            for (UUID memberId : group.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(player)) {
                    member.sendMessage(ChatColor.YELLOW + "L'administrateur " + player.getName() + " a rejoint le groupe !");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la tentative de rejoindre le groupe !");
        }
    }
    
    private void handleAdminAdd(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin add <joueur> <groupe>");
            return;
        }
        
        String targetPlayerName = args[2];
        String groupName = args[3];
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        Group targetGroup = plugin.getGroupManager().getGroup(groupName);
        
        if (targetGroup == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        // Check if player is already in the target group
        Group currentGroup = plugin.getGroupManager().getPlayerGroup(targetPlayer.getUniqueId());
        if (currentGroup != null && currentGroup.getName().equalsIgnoreCase(groupName)) {
            player.sendMessage(ChatColor.RED + targetPlayerName + " est déjà membre du groupe '" + groupName + "' !");
            return;
        }
        
        // If player is in another group, remove them first (transfer)
        if (currentGroup != null) {
            plugin.getGroupManager().removePlayerFromGroup(targetPlayer.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Transfert de " + targetPlayerName + " du groupe '" + currentGroup.getName() + "' vers '" + groupName + "'...");
            plugin.getLogger().info("ADMIN TRANSFER: " + player.getName() + " a transféré " + targetPlayerName + " de '" + currentGroup.getName() + "' vers '" + groupName + "'");
            
            // Notify old group members
            for (UUID memberId : currentGroup.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(targetPlayer.getPlayer())) {
                    member.sendMessage(ChatColor.YELLOW + targetPlayerName + " a été transféré vers un autre groupe par un administrateur.");
                }
            }
        }
        
        // Add player to new group (bypass size limit)
        if (plugin.getGroupManager().addPlayerToGroupBypass(targetPlayer.getUniqueId(), groupName)) {
            sendSuccessMessage(player, "Joueur " + targetPlayerName + " ajouté au groupe '" + groupName + "' avec succès !");
            plugin.getLogger().info("ADMIN ADD: " + player.getName() + " a ajouté " + targetPlayerName + " au groupe '" + groupName + "'");
            
            // Update display if player is online
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                plugin.getGroupManager().updatePlayerDisplay(onlineTarget);
                onlineTarget.sendMessage(ChatColor.GREEN + "Vous avez été ajouté au groupe '" + groupName + "' par un administrateur !");
            }
            
            // Notify new group members
            for (UUID memberId : targetGroup.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(onlineTarget)) {
                    member.sendMessage(ChatColor.YELLOW + "L'administrateur " + player.getName() + " a ajouté " + targetPlayerName + " au groupe !");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de l'ajout de " + targetPlayerName + " au groupe !");
        }
    }
    
    private void handleAdminSetOwner(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin setowner <groupe> <nouveau_proprietaire>");
            return;
        }
        
        String groupName = args[2];
        String newOwnerName = args[3];
        
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        OfflinePlayer newOwner = Bukkit.getOfflinePlayer(newOwnerName);
        if (!group.isMember(newOwner.getUniqueId())) {
            player.sendMessage(ChatColor.RED + newOwnerName + " n'est pas membre du groupe '" + groupName + "' !");
            return;
        }
        
        UUID currentOwnerId = group.getOwner();
        String currentOwnerName = Bukkit.getOfflinePlayer(currentOwnerId).getName();
        
        if (currentOwnerId.equals(newOwner.getUniqueId())) {
            player.sendMessage(ChatColor.RED + newOwnerName + " est déjà propriétaire du groupe '" + groupName + "' !");
            return;
        }
        
        // Transfer ownership
        group.setOwner(newOwner.getUniqueId());
        plugin.getGroupManager().saveGroups();
        
        sendSuccessMessage(player, "Propriété du groupe '" + groupName + "' transférée de " + currentOwnerName + " à " + newOwnerName + " !");
        plugin.getLogger().info("ADMIN SETOWNER: " + player.getName() + " a transféré la propriété du groupe '" + groupName + "' de " + currentOwnerName + " à " + newOwnerName);
        
        // Notify the new owner if online
        Player onlineNewOwner = newOwner.getPlayer();
        if (onlineNewOwner != null && onlineNewOwner.isOnline()) {
            onlineNewOwner.sendMessage(ChatColor.GREEN + "Vous êtes maintenant propriétaire du groupe '" + groupName + "' ! (changement administrateur)");
        }
        
        // Notify the old owner if online
        Player onlineOldOwner = Bukkit.getPlayer(currentOwnerId);
        if (onlineOldOwner != null && onlineOldOwner.isOnline()) {
            onlineOldOwner.sendMessage(ChatColor.YELLOW + "La propriété du groupe '" + groupName + "' a été transférée à " + newOwnerName + " par un administrateur.");
        }
        
        // Notify all group members
        for (UUID memberId : group.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline() && 
                !member.equals(onlineNewOwner) && !member.equals(onlineOldOwner)) {
                member.sendMessage(ChatColor.YELLOW + "La propriété du groupe a été transférée à " + newOwnerName + " par un administrateur.");
            }
        }
    }
    
    private void handleAdminSetColor(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin setcolor <groupe> <couleur>");
            player.sendMessage(ChatColor.GRAY + "Couleurs disponibles : red, blue, green, yellow, light_purple, aqua, white, gray, dark_red, dark_blue, dark_green, gold, dark_purple, dark_aqua");
            player.sendMessage(ChatColor.GRAY + "Ou utilisez un code hex : #ff0000, #00ff00, #0000ff, etc.");
            return;
        }
        
        String groupName = args[2];
        String colorInput = args[3];
        
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        String hexColor = parseColor(colorInput);
        if (hexColor == null) {
            player.sendMessage(ChatColor.RED + "Couleur invalide ! Utilisez un nom (red, blue, etc.) ou un code hex (#ff0000)");
            player.sendMessage(ChatColor.YELLOW + "Couleurs disponibles : red, blue, green, yellow, light_purple, aqua, white, gray");
            player.sendMessage(ChatColor.YELLOW + "dark_red, dark_blue, dark_green, gold, dark_purple, dark_aqua");
            player.sendMessage(ChatColor.YELLOW + "Ou codes hex : #ff0000, #00ff00, #0000ff, etc.");
            return;
        }
        
        String oldColorHex = group.getColorHex();
        group.setColorHex(hexColor);
        plugin.getGroupManager().saveGroups();
        
        Component changeMessage = Component.text("Couleur du groupe '" + groupName + "' changée de ", NamedTextColor.GREEN)
            .append(Component.text(oldColorHex, TextColor.fromCSSHexString(oldColorHex)))
            .append(Component.text(" vers ", NamedTextColor.GREEN))
            .append(Component.text(colorInput, group.getTextColor()))
            .append(Component.text(" !", NamedTextColor.GREEN));
        player.sendMessage(changeMessage);
        playSuccessSound(player);
        plugin.getLogger().info("ADMIN SETCOLOR: " + player.getName() + " a changé la couleur du groupe '" + 
                              groupName + "' de " + oldColorHex + " vers " + group.getColorHex());
        
        // Update display for all online group members
        for (UUID memberId : group.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                plugin.getGroupManager().updatePlayerDisplay(member);
                Component notificationMessage = Component.text("La couleur du groupe a été changée vers ", NamedTextColor.YELLOW)
                    .append(Component.text(group.getColorHex(), group.getTextColor()))
                    .append(Component.text(" par un administrateur !", NamedTextColor.YELLOW));
                member.sendMessage(notificationMessage);
            }
        }
    }
    
    private void handleAdminTakeOwnership(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage : /group admin takeownership <groupe>");
            return;
        }
        
        String groupName = args[2];
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Groupe '" + groupName + "' introuvable !");
            return;
        }
        
        // Check if admin is already in a group
        Group currentGroup = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (currentGroup != null) {
            player.sendMessage(ChatColor.RED + "Vous ne pouvez pas prendre la propriété d'un groupe car vous êtes déjà membre du groupe '" + currentGroup.getName() + "' ! Quittez d'abord votre groupe.");
            return;
        }
        
        // Get current owner info
        String currentOwnerName = Bukkit.getOfflinePlayer(group.getOwner()).getName();
        
        // First add admin to the group
        if (!plugin.getGroupManager().addPlayerToGroup(player.getUniqueId(), groupName)) {
            player.sendMessage(ChatColor.RED + "Erreur lors de l'ajout au groupe !");
            return;
        }
        
        // Then transfer ownership
        if (plugin.getGroupManager().transferOwnership(groupName, group.getOwner(), player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Vous êtes maintenant propriétaire du groupe '" + groupName + "' !");
            plugin.getGroupManager().updatePlayerDisplay(player);
            plugin.getLogger().warning("ADMIN: " + player.getName() + " a pris la propriété du groupe '" + groupName + "' (ancien propriétaire: " + currentOwnerName + ")");
            
            // Notify all group members
            for (UUID memberId : group.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline() && !member.equals(player)) {
                    member.sendMessage(ChatColor.YELLOW + "L'administrateur " + player.getName() + " est maintenant le nouveau propriétaire du groupe !");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors du transfert de propriété !");
        }
    }

    private void handleGroupChat(Player player, String[] args) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe !");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Vous devez écrire un message ! Usage: /group chat <message>");
            return;
        }
        
        // Join all arguments from index 1 onwards to form the message
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Format the group chat message using Components
        Component formattedMessage = Component.text("[", NamedTextColor.GRAY)
            .append(Component.text("Chat Groupe", NamedTextColor.GREEN))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text("[", group.getTextColor()))
            .append(Component.text(group.getName(), group.getTextColor()))
            .append(Component.text("] ", group.getTextColor()))
            .append(Component.text(player.getName(), NamedTextColor.WHITE))
            .append(Component.text(": " + message, NamedTextColor.WHITE));
        
        // Send message to all online group members
        int recipientCount = 0;
        for (UUID memberId : group.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
                recipientCount++;
            }
        }
        
        plugin.getLogger().info("Chat groupe [" + group.getName() + "] " + player.getName() + ": " + message + " (envoyé à " + recipientCount + " membres)");
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Commandes AspaGroup ===");
        player.sendMessage(ChatColor.YELLOW + "/group help - Aide détaillée en français");
        player.sendMessage(ChatColor.YELLOW + "/group create <name> - Create a group");
        player.sendMessage(ChatColor.YELLOW + "/group join <group> - Join a group");
        player.sendMessage(ChatColor.YELLOW + "/group leave - Leave your group");
        player.sendMessage(ChatColor.YELLOW + "/group info [group] - Group information");
        player.sendMessage(ChatColor.YELLOW + "/group list - List all groups");
        player.sendMessage(ChatColor.YELLOW + "/group players [group] - List group members");
        player.sendMessage(ChatColor.YELLOW + "/group chat <message> - Group chat");
        player.sendMessage(ChatColor.YELLOW + "/group togglechat - Toggle group chat mode");
        if (player.isOp()) {
            player.sendMessage(ChatColor.RED + "/group admin - Admin commands");
        }
    }
    
    private void sendDetailedHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Guide Complet AspaGroup ===");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GREEN + "▶ Commandes de Base:");
        player.sendMessage(ChatColor.YELLOW + "/group create <nom>" + ChatColor.WHITE + " - Créer un nouveau groupe");
        player.sendMessage(ChatColor.YELLOW + "/group join <groupe>" + ChatColor.WHITE + " - Demander à rejoindre un groupe");
        player.sendMessage(ChatColor.YELLOW + "/group leave" + ChatColor.WHITE + " - Quitter votre groupe actuel");
        player.sendMessage(ChatColor.YELLOW + "/group info [groupe]" + ChatColor.WHITE + " - Voir les infos d'un groupe");
        player.sendMessage(ChatColor.YELLOW + "/group list" + ChatColor.WHITE + " - Lister tous les groupes du serveur");
        player.sendMessage(ChatColor.YELLOW + "/group players [groupe]" + ChatColor.WHITE + " - Voir les membres d'un groupe");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GREEN + "▶ Communication:");
        player.sendMessage(ChatColor.YELLOW + "/group chat <message>" + ChatColor.WHITE + " - Envoyer un message privé au groupe");
        player.sendMessage(ChatColor.GRAY + "   Les messages ne sont visibles que par les membres du groupe");
        player.sendMessage(ChatColor.YELLOW + "/group togglechat" + ChatColor.WHITE + " - Basculer en mode chat de groupe");
        player.sendMessage(ChatColor.GRAY + "   Mode activé : tous vos messages vont automatiquement au groupe");
        player.sendMessage(ChatColor.GRAY + "   Mode désactivé : vos messages vont au chat global (par défaut)");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GREEN + "▶ Commandes Propriétaire:");
        player.sendMessage(ChatColor.YELLOW + "/group accept <joueur>" + ChatColor.WHITE + " - Accepter une demande d'adhésion");
        player.sendMessage(ChatColor.YELLOW + "/group deny <joueur>" + ChatColor.WHITE + " - Refuser une demande d'adhésion");
        player.sendMessage(ChatColor.YELLOW + "/group kick <joueur>" + ChatColor.WHITE + " - Expulser un membre du groupe");
        player.sendMessage(ChatColor.YELLOW + "/group transfer <joueur>" + ChatColor.WHITE + " - Transférer la propriété du groupe");
        player.sendMessage(ChatColor.YELLOW + "/group rename <nom>" + ChatColor.WHITE + " - Renommer le groupe");
        player.sendMessage(ChatColor.YELLOW + "/group color <couleur>" + ChatColor.WHITE + " - Changer la couleur du groupe");
        player.sendMessage(ChatColor.YELLOW + "/group description [texte]" + ChatColor.WHITE + " - Voir/modifier la description");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GREEN + "▶ Territoires et Chunks:");
        player.sendMessage(ChatColor.YELLOW + "/group claim" + ChatColor.WHITE + " - Revendiquer le chunk actuel (propriétaire)");
        player.sendMessage(ChatColor.YELLOW + "/group unclaim" + ChatColor.WHITE + " - Abandonner le chunk actuel (propriétaire)");
        player.sendMessage(ChatColor.YELLOW + "/group claimlist [groupe]" + ChatColor.WHITE + " - Lister les chunks revendiqués");
        player.sendMessage(ChatColor.YELLOW + "/group claiminfo [monde] [x] [z]" + ChatColor.WHITE + " - Info sur un chunk");
        player.sendMessage(ChatColor.YELLOW + "/group hud [toggle|on|off|status]" + ChatColor.WHITE + " - Contrôler l'affichage des territoires");
        player.sendMessage(ChatColor.GRAY + "   HUD activé par défaut - affiche le territoire actuel en temps réel");
        player.sendMessage("");

        player.sendMessage(ChatColor.GREEN + "▶ Couleurs Disponibles:");
        player.sendMessage(ChatColor.RED + "RED " + ChatColor.BLUE + "BLUE " + ChatColor.GREEN + "GREEN " + 
                          ChatColor.YELLOW + "YELLOW " + ChatColor.LIGHT_PURPLE + "LIGHT_PURPLE");
        player.sendMessage(ChatColor.AQUA + "AQUA " + ChatColor.WHITE + "WHITE " + ChatColor.GRAY + "GRAY " + 
                          ChatColor.GOLD + "GOLD " + ChatColor.DARK_PURPLE + "DARK_PURPLE");
        player.sendMessage(ChatColor.YELLOW + "Ou utilisez des codes RGB hex : " + ChatColor.WHITE + "#ff0000 (rouge), #00ff00 (vert), #0000ff (bleu), etc.");
        
        if (player.isOp()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "▶ Commandes Administrateur:");
            player.sendMessage(ChatColor.YELLOW + "/group admin delete <groupe>" + ChatColor.WHITE + " - Supprimer un groupe");
            player.sendMessage(ChatColor.YELLOW + "/group admin info <groupe>" + ChatColor.WHITE + " - Infos détaillées");
            player.sendMessage(ChatColor.YELLOW + "/group admin list" + ChatColor.WHITE + " - Liste administrative");
            player.sendMessage(ChatColor.YELLOW + "/group admin kick <joueur>" + ChatColor.WHITE + " - Expulser un joueur");
            player.sendMessage(ChatColor.YELLOW + "/group admin add <joueur> <groupe>" + ChatColor.WHITE + " - Ajouter un joueur (transfert auto)");
            player.sendMessage(ChatColor.YELLOW + "/group admin setowner <groupe> <joueur>" + ChatColor.WHITE + " - Changer le propriétaire");
            player.sendMessage(ChatColor.YELLOW + "/group admin setcolor <groupe> <couleur>" + ChatColor.WHITE + " - Changer la couleur");
            player.sendMessage(ChatColor.YELLOW + "/group admin join <groupe>" + ChatColor.WHITE + " - Rejoindre un groupe (bypass)");
            player.sendMessage(ChatColor.YELLOW + "/group admin takeownership <groupe>" + ChatColor.WHITE + " - Prendre la propriété");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "join", "accept", "deny", "leave", "kick",
                "color", "rename", "transfer", "info", "list", "players",
                "description", "desc", "chat", "claim", "unclaim", "claimlist", "claiminfo", "hud", "togglechat", "help");
            if (player.isOp()) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("admin");
            }
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "join":
                case "info":
                case "players":
                    return plugin.getGroupManager().getAllGroups().stream()
                        .map(Group::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "accept":
                case "deny":
                    Group playerGroup = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
                    if (playerGroup != null && playerGroup.isOwner(player.getUniqueId())) {
                        return playerGroup.getJoinRequests().stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null && name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
                    
                case "kick":
                case "transfer":
                    Group ownerGroup = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
                    if (ownerGroup != null && ownerGroup.isOwner(player.getUniqueId())) {
                        return ownerGroup.getMembers().stream()
                            .filter(uuid -> !uuid.equals(player.getUniqueId()))
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null && name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
                    
                case "color":
                    List<String> colors = Arrays.asList("red", "blue", "green", "yellow", "light_purple", 
                        "aqua", "white", "gray", "dark_red", "dark_blue", "dark_green", "gold", 
                        "dark_purple", "dark_aqua", "#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff", "#00ffff");
                    return colors.stream()
                        .filter(color -> color.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "claimlist":
                case "claiminfo":
                    return plugin.getGroupManager().getAllGroups().stream()
                        .map(Group::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "hud":
                    return Arrays.asList("toggle", "on", "off", "status").stream()
                        .filter(cmd -> cmd.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "admin":
                    if (player.isOp()) {
                        List<String> adminCommands = Arrays.asList("delete", "info", "list", "kick", "add", "setowner", "setcolor", "join", "takeownership", "claiminfo", "unclaim", "claimlist", "claimstats");
                        return adminCommands.stream()
                            .filter(cmd -> cmd.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String adminCommand = args[1].toLowerCase();
            
            if ("admin".equals(subCommand) && player.isOp()) {
                switch (adminCommand) {
                    case "delete":
                    case "info":
                    case "join":
                    case "setowner":
                    case "setcolor":
                    case "takeownership":
                        return plugin.getGroupManager().getAllGroups().stream()
                            .map(Group::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                            
                    case "add":
                        // For "add" command, arg[2] is player name
                        return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                            
                    case "kick":
                        // For kick command, return all players from all groups
                        return plugin.getGroupManager().getAllGroups().stream()
                            .flatMap(group -> group.getMembers().stream())
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null && name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .distinct()
                            .collect(Collectors.toList());

                    case "claiminfo":
                    case "unclaim":
                        // For chunk admin commands, suggest world names
                        return Bukkit.getWorlds().stream()
                            .map(world -> world.getName())
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());

                    case "claimlist":
                        // For admin claimlist, suggest group names
                        return plugin.getGroupManager().getAllGroups().stream()
                            .map(Group::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        
        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String adminCommand = args[1].toLowerCase();
            
            if ("admin".equals(subCommand) && player.isOp()) {
                if ("add".equals(adminCommand)) {
                    // For "admin add <player> <group>", arg[3] is group name
                    return plugin.getGroupManager().getAllGroups().stream()
                        .map(Group::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
                } else if ("setowner".equals(adminCommand)) {
                    // For "admin setowner <group> <player>", arg[3] is player name from that group
                    String groupName = args[2];
                    Group group = plugin.getGroupManager().getGroup(groupName);
                    if (group != null) {
                        return group.getMembers().stream()
                            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null && name.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                } else if ("setcolor".equals(adminCommand)) {
                    // For "admin setcolor <group> <color>", arg[3] is color name
                    List<String> colors = Arrays.asList("red", "blue", "green", "yellow", "light_purple", 
                        "aqua", "white", "gray", "dark_red", "dark_blue", "dark_green", "gold", 
                        "dark_purple", "dark_aqua", "#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff", "#00ffff");
                    return colors.stream()
                        .filter(color -> color.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Parses color input (hex or named color) and returns hex string
     */
    private String parseColor(String input) {
        // Check if it's a hex color
        if (input.startsWith("#") && input.length() == 7) {
            try {
                // Validate hex format
                Integer.parseInt(input.substring(1), 16);
                return input.toLowerCase();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // Check if it's a named color
        Map<String, String> namedColors = new HashMap<>();
        namedColors.put("red", "#ff5555");
        namedColors.put("blue", "#5555ff");
        namedColors.put("green", "#55ff55");
        namedColors.put("yellow", "#ffff55");
        namedColors.put("purple", "#ff55ff");
        namedColors.put("light_purple", "#ff55ff");
        namedColors.put("aqua", "#55ffff");
        namedColors.put("white", "#ffffff");
        namedColors.put("gray", "#aaaaaa");
        namedColors.put("dark_red", "#aa0000");
        namedColors.put("dark_blue", "#0000aa");
        namedColors.put("dark_green", "#00aa00");
        namedColors.put("gold", "#ffaa00");
        namedColors.put("dark_purple", "#aa00aa");
        namedColors.put("dark_aqua", "#00aaaa");
        
        return namedColors.get(input.toLowerCase());
    }
    
    /**
     * Validates if a string is a valid hex color
     */
    private boolean isValidHexColor(String hex) {
        if (!hex.startsWith("#") || hex.length() != 7) {
            return false;
        }
        try {
            Integer.parseInt(hex.substring(1), 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ===== ADMIN CHUNK COMMANDS =====

    private void handleAdminClaimInfo(Player player, String[] args) {
        if (args.length < 5) {
            sendErrorMessage(player, "Usage: /group admin claiminfo <monde> <x> <z>");
            return;
        }

        try {
            String world = args[2];
            int chunkX = Integer.parseInt(args[3]);
            int chunkZ = Integer.parseInt(args[4]);

            String owner = plugin.getChunkManager().getChunkOwner(world, chunkX, chunkZ);

            player.sendMessage(ChatColor.GOLD + "=== Information Admin du Chunk ===");
            player.sendMessage(ChatColor.AQUA + "Localisation: " + ChatColor.WHITE + world + " (" + chunkX + "," + chunkZ + ")");

            if (owner == null) {
                player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.WHITE + "Terre sauvage (non revendiqué)");
            } else {
                Group ownerGroup = plugin.getGroupManager().getGroup(owner);
                if (ownerGroup != null) {
                    player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.WHITE + "Revendiqué");

                    Component groupComponent = Component.text("Propriétaire: ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(owner).color(ownerGroup.getTextColor()));
                    player.sendMessage(groupComponent);

                    // Admin view - show more details
                    String ownerName = Bukkit.getOfflinePlayer(ownerGroup.getOwner()).getName();
                    player.sendMessage(ChatColor.YELLOW + "Chef du groupe: " + ChatColor.WHITE + ownerName);
                    player.sendMessage(ChatColor.YELLOW + "Membres: " + ChatColor.WHITE + ownerGroup.getMemberCount());
                    player.sendMessage(ChatColor.YELLOW + "Total chunks du groupe: " + ChatColor.WHITE +
                                     plugin.getChunkManager().getGroupClaimCount(owner));

                    // Show claim timestamp if available
                    List<com.aspa.aspagroup.models.ChunkClaim> claims = plugin.getChunkManager().getGroupClaims(owner);
                    for (com.aspa.aspagroup.models.ChunkClaim claim : claims) {
                        if (claim.getWorld().equals(world) && claim.getChunkX() == chunkX && claim.getChunkZ() == chunkZ) {
                            Date claimDate = new Date(claim.getClaimTimestamp());
                            player.sendMessage(ChatColor.YELLOW + "Revendiqué le: " + ChatColor.WHITE + claimDate.toString());
                            break;
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.RED + "Revendiqué par un groupe supprimé (" + owner + ")");
                    player.sendMessage(ChatColor.GRAY + "Utilisez /group admin unclaim pour nettoyer");
                }
            }
        } catch (NumberFormatException e) {
            sendErrorMessage(player, "Coordonnées invalides ! Usage: /group admin claiminfo <monde> <x> <z>");
        }
    }

    private void handleAdminUnclaim(Player player, String[] args) {
        if (args.length < 5) {
            sendErrorMessage(player, "Usage: /group admin unclaim <monde> <x> <z>");
            return;
        }

        try {
            String world = args[2];
            int chunkX = Integer.parseInt(args[3]);
            int chunkZ = Integer.parseInt(args[4]);

            String owner = plugin.getChunkManager().getChunkOwner(world, chunkX, chunkZ);
            if (owner == null) {
                sendErrorMessage(player, "Ce chunk n'est revendiqué par aucun groupe !");
                return;
            }

            if (plugin.getChunkManager().forceUnclaimChunk(world, chunkX, chunkZ)) {
                sendSuccessMessage(player, "Chunk forcé à l'abandon ! (" + world + " " + chunkX + "," + chunkZ + ") était revendiqué par '" + owner + "'");
            } else {
                sendErrorMessage(player, "Impossible de forcer l'abandon du chunk !");
            }
        } catch (NumberFormatException e) {
            sendErrorMessage(player, "Coordonnées invalides ! Usage: /group admin unclaim <monde> <x> <z>");
        }
    }

    private void handleAdminClaimList(Player player, String[] args) {
        String targetGroupName = null;

        if (args.length > 2) {
            targetGroupName = args[2];
            Group targetGroup = plugin.getGroupManager().getGroup(targetGroupName);
            if (targetGroup == null) {
                sendErrorMessage(player, "Groupe '" + targetGroupName + "' introuvable !");
                return;
            }
        }

        if (targetGroupName != null) {
            // List specific group's claims
            List<com.aspa.aspagroup.models.ChunkClaim> claims = plugin.getChunkManager().getGroupClaims(targetGroupName);

            if (claims.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Le groupe '" + targetGroupName + "' n'a revendiqué aucun chunk.");
                return;
            }

            player.sendMessage(ChatColor.GOLD + "=== Admin - Chunks de '" + targetGroupName + "' ===");
            player.sendMessage(ChatColor.GRAY + "Total: " + claims.size() + " chunks");

            Map<String, List<com.aspa.aspagroup.models.ChunkClaim>> claimsByWorld = new HashMap<>();
            for (com.aspa.aspagroup.models.ChunkClaim claim : claims) {
                claimsByWorld.computeIfAbsent(claim.getWorld(), k -> new ArrayList<>()).add(claim);
            }

            for (Map.Entry<String, List<com.aspa.aspagroup.models.ChunkClaim>> entry : claimsByWorld.entrySet()) {
                String worldName = entry.getKey();
                List<com.aspa.aspagroup.models.ChunkClaim> worldClaims = entry.getValue();

                player.sendMessage(ChatColor.AQUA + worldName + " (" + worldClaims.size() + " chunks):");
                for (com.aspa.aspagroup.models.ChunkClaim claim : worldClaims) {
                    Date claimDate = new Date(claim.getClaimTimestamp());
                    player.sendMessage(ChatColor.WHITE + "  • " + claim.getChunkX() + "," + claim.getChunkZ() +
                                     ChatColor.GRAY + " (le " + claimDate.toString() + ")");
                }
            }
        } else {
            // List all claims on server
            Map<String, String> allClaims = plugin.getChunkManager().getAllClaims();

            if (allClaims.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Aucun chunk revendiqué sur le serveur.");
                return;
            }

            player.sendMessage(ChatColor.GOLD + "=== Admin - Tous les chunks revendiqués ===");
            player.sendMessage(ChatColor.GRAY + "Total: " + allClaims.size() + " chunks");

            // Group by group name
            Map<String, Integer> claimsByGroup = new HashMap<>();
            for (String groupName : allClaims.values()) {
                claimsByGroup.put(groupName, claimsByGroup.getOrDefault(groupName, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : claimsByGroup.entrySet()) {
                String groupName = entry.getKey();
                int claimCount = entry.getValue();

                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group != null) {
                    Component groupComponent = Component.text("• " + groupName + ": ")
                        .color(group.getTextColor())
                        .append(Component.text(claimCount + " chunks").color(NamedTextColor.WHITE));
                    player.sendMessage(groupComponent);
                } else {
                    player.sendMessage(ChatColor.GRAY + "• " + groupName + ": " + ChatColor.WHITE + claimCount +
                                     " chunks " + ChatColor.RED + "(groupe supprimé)");
                }
            }
        }
    }

    private void handleAdminClaimStats(Player player) {
        Map<String, Object> stats = plugin.getChunkManager().getClaimingStats();

        player.sendMessage(ChatColor.GOLD + "=== Statistiques des Revendications ===");
        player.sendMessage(ChatColor.YELLOW + "Total chunks revendiqués: " + ChatColor.WHITE + stats.get("totalClaims"));
        player.sendMessage(ChatColor.YELLOW + "Groupes actifs: " + ChatColor.WHITE + stats.get("totalGroups"));

        if (stats.containsKey("topGroup")) {
            player.sendMessage(ChatColor.YELLOW + "Groupe le plus actif: " + ChatColor.WHITE +
                             stats.get("topGroup") + " (" + stats.get("topGroupClaims") + " chunks)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Integer> worldCounts = (Map<String, Integer>) stats.get("claimsPerWorld");
        if (worldCounts != null && !worldCounts.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Répartition par monde:");
            for (Map.Entry<String, Integer> entry : worldCounts.entrySet()) {
                player.sendMessage(ChatColor.AQUA + "  • " + entry.getKey() + ": " +
                                 ChatColor.WHITE + entry.getValue() + " chunks");
            }
        }

        // Show limits
        int maxClaims = plugin.getChunkManager().getMaxClaimsPerGroup();
        player.sendMessage(ChatColor.YELLOW + "Limite par groupe: " + ChatColor.WHITE + maxClaims + " chunks");

        // Show potential issues
        int orphanedClaims = 0;
        Map<String, String> allClaims = plugin.getChunkManager().getAllClaims();
        for (String groupName : allClaims.values()) {
            if (plugin.getGroupManager().getGroup(groupName) == null) {
                orphanedClaims++;
            }
        }

        if (orphanedClaims > 0) {
            player.sendMessage(ChatColor.RED + "Chunks orphelins: " + orphanedClaims +
                             " (groupes supprimés - utilisez /group admin unclaim pour nettoyer)");
        }
    }

    // ===== CHUNK CLAIMING COMMANDS =====

    private void handleClaim(Player player) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            sendErrorMessage(player, "Vous devez être dans un groupe pour revendiquer des chunks !");
            return;
        }

        if (!group.isOwner(player.getUniqueId())) {
            sendErrorMessage(player, "Seul le propriétaire du groupe peut revendiquer des chunks !");
            return;
        }

        if (!player.hasPermission("aspagroup.claim")) {
            sendErrorMessage(player, "Vous n'avez pas la permission de revendiquer des chunks !");
            return;
        }

        // Get current chunk
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Check if already claimed
        if (plugin.getChunkManager().isChunkClaimed(world, chunkX, chunkZ)) {
            String owner = plugin.getChunkManager().getChunkOwner(world, chunkX, chunkZ);
            if (owner.equals(group.getName())) {
                sendErrorMessage(player, "Ce chunk est déjà revendiqué par votre groupe !");
            } else {
                sendErrorMessage(player, "Ce chunk est déjà revendiqué par le groupe '" + owner + "' !");
            }
            return;
        }

        // Check claim limit (unless player has unlimited permission)
        if (!plugin.getChunkManager().hasUnlimitedPermission(player) &&
            plugin.getChunkManager().hasReachedClaimLimit(group.getName())) {
            int maxClaims = plugin.getChunkManager().getMaxClaimsPerGroup();
            sendErrorMessage(player, "Votre groupe a atteint la limite de " + maxClaims + " chunks revendiqués !");
            return;
        }

        // Claim the chunk
        if (plugin.getChunkManager().claimChunk(world, chunkX, chunkZ, group.getName())) {
            sendSuccessMessage(player, "Chunk revendiqué avec succès pour le groupe '" + group.getName() + "' ! (" + world + " " + chunkX + "," + chunkZ + ")");
        } else {
            sendErrorMessage(player, "Impossible de revendiquer ce chunk !");
        }
    }

    private void handleUnclaim(Player player) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            sendErrorMessage(player, "Vous devez être dans un groupe pour abandonner des chunks !");
            return;
        }

        if (!group.isOwner(player.getUniqueId())) {
            sendErrorMessage(player, "Seul le propriétaire du groupe peut abandonner des chunks !");
            return;
        }

        // Get current chunk
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Check if claimed by this group
        String owner = plugin.getChunkManager().getChunkOwner(world, chunkX, chunkZ);
        if (owner == null) {
            sendErrorMessage(player, "Ce chunk n'est revendiqué par aucun groupe !");
            return;
        }

        if (!owner.equals(group.getName())) {
            sendErrorMessage(player, "Ce chunk est revendiqué par le groupe '" + owner + "', pas le vôtre !");
            return;
        }

        // Unclaim the chunk
        if (plugin.getChunkManager().unclaimChunk(world, chunkX, chunkZ)) {
            sendSuccessMessage(player, "Chunk abandonné avec succès ! (" + world + " " + chunkX + "," + chunkZ + ")");
        } else {
            sendErrorMessage(player, "Impossible d'abandonner ce chunk !");
        }
    }

    private void handleClaimList(Player player, String[] args) {
        String targetGroupName = null;

        // Check if specific group is specified
        if (args.length > 1) {
            targetGroupName = args[1];
            Group targetGroup = plugin.getGroupManager().getGroup(targetGroupName);
            if (targetGroup == null) {
                sendErrorMessage(player, "Groupe '" + targetGroupName + "' introuvable !");
                return;
            }
        } else {
            // Use player's group
            Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
            if (group == null) {
                sendErrorMessage(player, "Vous devez être dans un groupe ! Usage: /group claimlist [groupe]");
                return;
            }
            targetGroupName = group.getName();
        }

        List<com.aspa.aspagroup.models.ChunkClaim> claims = plugin.getChunkManager().getGroupClaims(targetGroupName);

        if (claims.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Le groupe '" + targetGroupName + "' n'a revendiqué aucun chunk.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "=== Chunks revendiqués par '" + targetGroupName + "' ===");
        player.sendMessage(ChatColor.GRAY + "Total: " + claims.size() + " chunks");

        // Group by world for better display
        Map<String, List<com.aspa.aspagroup.models.ChunkClaim>> claimsByWorld = new HashMap<>();
        for (com.aspa.aspagroup.models.ChunkClaim claim : claims) {
            claimsByWorld.computeIfAbsent(claim.getWorld(), k -> new ArrayList<>()).add(claim);
        }

        for (Map.Entry<String, List<com.aspa.aspagroup.models.ChunkClaim>> entry : claimsByWorld.entrySet()) {
            String worldName = entry.getKey();
            List<com.aspa.aspagroup.models.ChunkClaim> worldClaims = entry.getValue();

            player.sendMessage(ChatColor.AQUA + worldName + " (" + worldClaims.size() + " chunks):");

            // Display up to 10 chunks per world, then show "and X more..."
            int displayed = 0;
            for (com.aspa.aspagroup.models.ChunkClaim claim : worldClaims) {
                if (displayed >= 10) {
                    int remaining = worldClaims.size() - displayed;
                    player.sendMessage(ChatColor.GRAY + "  ... et " + remaining + " chunks de plus");
                    break;
                }
                // Convert chunk coordinates to world coordinates
                int worldX = claim.getChunkX() * 16;
                int worldZ = claim.getChunkZ() * 16;
                player.sendMessage(ChatColor.WHITE + "  • Chunk(" + claim.getChunkX() + "," + claim.getChunkZ() + ") " +
                                 ChatColor.GRAY + "→ Coords(" + worldX + "~" + (worldX + 15) + ", " + worldZ + "~" + (worldZ + 15) + ")");
                displayed++;
            }
        }
    }

    private void handleClaimInfo(Player player, String[] args) {
        String world;
        int chunkX, chunkZ;

        // Check if specific coordinates are provided
        if (args.length >= 4) {
            try {
                world = args[1];
                chunkX = Integer.parseInt(args[2]);
                chunkZ = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sendErrorMessage(player, "Usage: /group claiminfo [monde] [x] [z]");
                return;
            }
        } else {
            // Use current chunk
            org.bukkit.Chunk chunk = player.getLocation().getChunk();
            world = player.getWorld().getName();
            chunkX = chunk.getX();
            chunkZ = chunk.getZ();
        }

        String owner = plugin.getChunkManager().getChunkOwner(world, chunkX, chunkZ);

        player.sendMessage(ChatColor.GREEN + "=== Information du chunk ===");
        player.sendMessage(ChatColor.AQUA + "Localisation: " + ChatColor.WHITE + world + " (" + chunkX + "," + chunkZ + ")");

        if (owner == null) {
            player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.WHITE + "Terre sauvage (non revendiqué)");
        } else {
            Group ownerGroup = plugin.getGroupManager().getGroup(owner);
            if (ownerGroup != null) {
                player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.WHITE + "Revendiqué");

                // Use Adventure Components for colored group name
                Component groupComponent = Component.text("Propriétaire: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(owner).color(ownerGroup.getTextColor()));
                player.sendMessage(groupComponent);

                // Show additional group info
                player.sendMessage(ChatColor.YELLOW + "Membres: " + ChatColor.WHITE + ownerGroup.getMemberCount());
                if (ownerGroup.getDescription() != null && !ownerGroup.getDescription().equals("Aucune description")) {
                    player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + ownerGroup.getDescription());
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "Statut: " + ChatColor.RED + "Revendiqué par un groupe supprimé (" + owner + ")");
            }
        }

        // Add world coordinates info
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;
        player.sendMessage(ChatColor.YELLOW + "Coordonnées monde: " + ChatColor.WHITE +
                         worldX + "~" + (worldX + 15) + ", " + worldZ + "~" + (worldZ + 15));

        // Show surrounding claims map
        showSurroundingClaimsMap(player, world, chunkX, chunkZ);
    }

    /**
     * Shows a visual map of surrounding chunks and their ownership
     */
    private void showSurroundingClaimsMap(Player player, String world, int centerX, int centerZ) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "=== Carte des territoires (15x15) ===");
        player.sendMessage(ChatColor.GRAY + "Légende: " + ChatColor.WHITE + "X = Vous, " +
                         ChatColor.GRAY + "- = Sauvage, " + ChatColor.GREEN + "Lettre = Votre groupe, " +
                         ChatColor.RED + "Lettre = Autres groupes");

        String currentPlayerGroup = null;
        Group playerGroup = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (playerGroup != null) {
            currentPlayerGroup = playerGroup.getName();
        }

        // Create 15x15 map centered on the chunk
        for (int z = centerZ - 7; z <= centerZ + 7; z++) {
            StringBuilder line = new StringBuilder();
            for (int x = centerX - 7; x <= centerX + 7; x++) {
                String chunkOwner = plugin.getChunkManager().getChunkOwner(world, x, z);

                if (x == centerX && z == centerZ) {
                    // Center chunk (where player is)
                    line.append(ChatColor.WHITE).append(ChatColor.YELLOW).append("X");
                } else if (chunkOwner == null) {
                    // Wilderness
                    line.append(ChatColor.GRAY).append("-");
                } else if (chunkOwner.equals(currentPlayerGroup)) {
                    // Player's group
                    line.append(ChatColor.GREEN).append(getGroupInitial(chunkOwner));
                } else {
                    // Other group
                    line.append(ChatColor.RED).append(getGroupInitial(chunkOwner));
                }

                // Add space between characters for better alignment (except last character)
                if (x < centerX + 7) {
                    line.append(" ");
                }
            }
            player.sendMessage(line.toString());
        }

        player.sendMessage(ChatColor.GRAY + "Centre: " + centerX + "," + centerZ + " | Rayon: 7 chunks");
    }

    /**
     * Gets the first letter of a group name for the map display
     */
    private String getGroupInitial(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return "?";
        }
        return groupName.substring(0, 1).toUpperCase();
    }

    // ===== CHAT MODE TOGGLE COMMAND =====

    private void handleToggleChat(Player player) {
        Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());
        if (group == null) {
            sendErrorMessage(player, "Vous devez être dans un groupe pour utiliser le mode chat de groupe !");
            return;
        }

        boolean isNowInGroupMode = plugin.getChatModeManager().toggleChatMode(player);

        if (isNowInGroupMode) {
            sendSuccessMessage(player, "Mode chat de groupe activé ! Tous vos messages iront uniquement au groupe '" + group.getName() + "'.");
            player.sendMessage(ChatColor.GRAY + "Utilisez /group togglechat pour revenir au chat global.");
            player.sendMessage(ChatColor.GRAY + "/group chat <message> fonctionne toujours pour les messages uniques.");
        } else {
            sendSuccessMessage(player, "Mode chat global activé ! Vos messages sont maintenant visibles par tous.");
            player.sendMessage(ChatColor.GRAY + "Utilisez /group togglechat pour revenir au chat de groupe.");
        }
    }

    // ===== HUD TOGGLE COMMAND =====

    private void handleHudToggle(Player player, String[] args) {
        if (!player.hasPermission("aspagroup.hud")) {
            sendErrorMessage(player, "Vous n'avez pas la permission d'utiliser le HUD !");
            return;
        }

        if (args.length > 1) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("toggle")) {
                plugin.getHudManager().toggleHUD(player);
                boolean enabled = plugin.getHudManager().isHudEnabled(player);
                if (enabled) {
                    sendSuccessMessage(player, "HUD des territoires activé ! (Votre préférence sera sauvegardée)");
                } else {
                    sendSuccessMessage(player, "HUD des territoires désactivé. (Votre préférence sera sauvegardée)");
                }
                return;
            } else if (subCommand.equals("on")) {
                plugin.getHudManager().setHudEnabled(player, true);
                sendSuccessMessage(player, "HUD des territoires activé ! (Préférence sauvegardée)");
                return;
            } else if (subCommand.equals("off")) {
                plugin.getHudManager().setHudEnabled(player, false);
                sendSuccessMessage(player, "HUD des territoires désactivé. (Préférence sauvegardée)");
                return;
            } else if (subCommand.equals("status")) {
                boolean enabled = plugin.getHudManager().isHudEnabled(player);
                if (enabled) {
                    sendSuccessMessage(player, "HUD des territoires : ACTIVÉ");
                } else {
                    sendErrorMessage(player, "HUD des territoires : DÉSACTIVÉ");
                }
                return;
            }
        }

        // Default toggle behavior
        plugin.getHudManager().toggleHUD(player);
        boolean enabled = plugin.getHudManager().isHudEnabled(player);
        if (enabled) {
            sendSuccessMessage(player, "HUD des territoires activé ! (Préférence sauvegardée)");
        } else {
            sendSuccessMessage(player, "HUD des territoires désactivé. (Préférence sauvegardée)");
        }
    }
}
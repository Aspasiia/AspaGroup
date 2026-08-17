package com.aspa.aspagroup.managers;

import com.aspa.aspagroup.AspaGroup;
import com.aspa.aspagroup.models.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {
    
    private final AspaGroup plugin;
    private final Map<String, Group> groups;
    private final Map<UUID, String> playerGroups;
    private File groupsFile;
    private FileConfiguration groupsConfig;
    
    public GroupManager(AspaGroup plugin) {
        this.plugin = plugin;
        this.groups = new ConcurrentHashMap<>();
        this.playerGroups = new ConcurrentHashMap<>();
        
        createGroupsFile();
    }
    
    private void createGroupsFile() {
        groupsFile = new File(plugin.getDataFolder(), "groups.yml");
        if (!groupsFile.exists()) {
            groupsFile.getParentFile().mkdirs();
            plugin.saveResource("groups.yml", false);
        }
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
    }
    
    public void loadGroups() {
        int groupCount = 0;
        int totalMembers = 0;
        boolean migrationOccurred = false;
        
        if (groupsConfig.contains("groups")) {
            for (String key : groupsConfig.getConfigurationSection("groups").getKeys(false)) {
                Group group = (Group) groupsConfig.get("groups." + key);
                if (group != null) {
                    groups.put(group.getName().toLowerCase(), group);
                    groupCount++;
                    
                    // Check if this group was migrated from old format
                    if (wasGroupMigrated(key)) {
                        migrationOccurred = true;
                    }
                    
                    for (UUID memberId : group.getMembers()) {
                        playerGroups.put(memberId, group.getName());
                        totalMembers++;
                    }
                }
            }
        }
        
        // Auto-save if migration occurred
        if (migrationOccurred) {
            plugin.getLogger().info("Migrated groups to new RGB color format! Auto-saving...");
            saveGroups();
        }
        
        plugin.getLogger().info("Groupes chargés: " + groupCount + " groupes avec " + totalMembers + " membres au total");
        cleanExpiredRequests();
    }
    
    public void saveGroups() {
        try {
            groupsConfig.set("groups", null);
            
            for (Group group : groups.values()) {
                groupsConfig.set("groups." + group.getName(), group);
            }
            
            groupsConfig.save(groupsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save groups.yml: " + e.getMessage());
        }
    }
    
    public boolean createGroup(String name, UUID ownerId) {
        String lowerName = name.toLowerCase();
        
        if (groups.containsKey(lowerName)) {
            String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            plugin.getLogger().info("Tentative de création d'un groupe existant: " + name + " par " + ownerName);
            return false;
        }
        
        if (playerGroups.containsKey(ownerId)) {
            String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            plugin.getLogger().info("Tentative de création d'un groupe par un joueur déjà membre: " + ownerName);
            return false;
        }
        
        Group group = new Group(name, ownerId);
        groups.put(lowerName, group);
        playerGroups.put(ownerId, name);
        
        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        plugin.getLogger().info("Groupe créé: '" + name + "' par " + ownerName);
        saveGroups();
        return true;
    }
    
    public boolean deleteGroup(String name) {
        String lowerName = name.toLowerCase();
        Group group = groups.remove(lowerName);
        
        if (group == null) {
            plugin.getLogger().warning("Tentative de suppression d'un groupe inexistant: " + name);
            return false;
        }
        
        int memberCount = group.getMemberCount();
        for (UUID memberId : group.getMembers()) {
            playerGroups.remove(memberId);
            // Update online players' display to remove group formatting
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                updatePlayerDisplay(member);
            }
        }
        
        // Clean up the scoreboard team
        cleanupGroupTeam(name);

        // Clean up chunk claims for this group
        if (plugin.getChunkManager() != null) {
            int claimCount = plugin.getChunkManager().getGroupClaimCount(name);
            if (claimCount > 0) {
                plugin.getChunkManager().removeGroupClaims(name);
                plugin.getLogger().info("Supprimé " + claimCount + " revendications de chunks pour le groupe: " + name);
            }
        }

        plugin.getLogger().info("Groupe supprimé: '" + name + "' (" + memberCount + " membres)");
        saveGroups();
        return true;
    }
    
    public Group getGroup(String name) {
        return groups.get(name.toLowerCase());
    }
    
    public Group getPlayerGroup(UUID playerId) {
        String groupName = playerGroups.get(playerId);
        return groupName != null ? getGroup(groupName) : null;
    }
    
    public boolean addPlayerToGroup(UUID playerId, String groupName) {
        Group group = getGroup(groupName);
        if (group == null) {
            plugin.getLogger().warning("Tentative d'ajout de joueur à un groupe inexistant: " + groupName);
            return false;
        }
        
        if (playerGroups.containsKey(playerId)) {
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Tentative d'ajout d'un joueur déjà membre: " + playerName + " au groupe " + groupName);
            return false;
        }
        
        int maxGroupSize = plugin.getConfig().getInt("max-group-size", 10);
        if (group.getMemberCount() >= maxGroupSize) {
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Tentative d'ajout de " + playerName + " au groupe plein '" + groupName + "' (" + maxGroupSize + " membres max)");
            return false;
        }
        
        if (group.addMember(playerId)) {
            playerGroups.put(playerId, group.getName());
            group.removeJoinRequest(playerId);
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Joueur " + playerName + " a rejoint le groupe '" + groupName + "'");
            saveGroups();
            return true;
        }
        
        return false;
    }
    
    public boolean removePlayerFromGroup(UUID playerId) {
        String currentGroupName = playerGroups.get(playerId);
        if (currentGroupName == null) {
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Tentative de retrait d'un joueur non membre: " + playerName);
            return false;
        }
        
        Group group = getGroup(currentGroupName);
        if (group == null) {
            plugin.getLogger().warning("Groupe inexistant lors du retrait du joueur: " + currentGroupName);
            return false;
        }
        
        if (group.removeMember(playerId)) {
            playerGroups.remove(playerId);
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Joueur " + playerName + " a quitté le groupe '" + currentGroupName + "'");
            
            // Update player display and remove from team
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                updatePlayerDisplay(onlinePlayer);
            }
            
            if (group.getMemberCount() == 0) {
                plugin.getLogger().info("Suppression automatique du groupe vide: " + group.getName());
                deleteGroup(group.getName());
            } else if (group.isOwner(playerId)) {
                UUID newOwner = group.getMembers().iterator().next();
                group.setOwner(newOwner);
                String newOwnerName = Bukkit.getOfflinePlayer(newOwner).getName();
                plugin.getLogger().info("Propriété du groupe '" + currentGroupName + "' transférée à " + newOwnerName);
            }
            
            saveGroups();
            return true;
        }
        
        return false;
    }
    
    public boolean addPlayerToGroupBypass(UUID playerId, String groupName) {
        Group group = getGroup(groupName);
        if (group == null) {
            plugin.getLogger().warning("Tentative d'ajout de joueur à un groupe inexistant: " + groupName);
            return false;
        }
        
        if (playerGroups.containsKey(playerId)) {
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("Tentative d'ajout d'un joueur déjà membre: " + playerName + " au groupe " + groupName);
            return false;
        }
        
        // Admin bypass - skip group size check
        if (group.addMember(playerId)) {
            playerGroups.put(playerId, group.getName());
            group.removeJoinRequest(playerId);
            String playerName = Bukkit.getOfflinePlayer(playerId).getName();
            plugin.getLogger().info("ADMIN BYPASS: Joueur " + playerName + " a rejoint le groupe '" + groupName + "' (bypass administrateur)");
            saveGroups();
            return true;
        }
        
        return false;
    }
    
    public boolean transferOwnership(String groupName, UUID currentOwner, UUID newOwner) {
        Group group = getGroup(groupName);
        if (group == null || !group.isOwner(currentOwner) || !group.isMember(newOwner)) {
            return false;
        }
        
        group.setOwner(newOwner);
        saveGroups();
        return true;
    }
    
    public boolean renameGroup(String oldName, String newName, UUID requesterId) {
        Group group = getGroup(oldName);
        if (group == null || !group.isOwner(requesterId)) {
            return false;
        }
        
        String lowerNewName = newName.toLowerCase();
        if (groups.containsKey(lowerNewName)) {
            return false;
        }
        
        groups.remove(oldName.toLowerCase());
        group.setName(newName);
        groups.put(lowerNewName, group);

        for (UUID memberId : group.getMembers()) {
            playerGroups.put(memberId, newName);
        }

        // Update chunk ownership for renamed group
        if (plugin.getChunkManager() != null) {
            plugin.getChunkManager().renameGroupClaims(oldName, newName);
        }

        // Update HUD for all online players to reflect group name change
        if (plugin.getHudManager() != null) {
            plugin.getHudManager().updateAllPlayersHUD();
        }

        saveGroups();
        return true;
    }
    
    public void addJoinRequest(String groupName, UUID playerId) {
        Group group = getGroup(groupName);
        if (group != null && !group.isMember(playerId) && !playerGroups.containsKey(playerId)) {
            group.addJoinRequest(playerId);
            saveGroups();
        }
    }
    
    public boolean acceptJoinRequest(String groupName, UUID ownerId, UUID requesterId) {
        Group group = getGroup(groupName);
        if (group == null || !group.isOwner(ownerId) || !group.hasJoinRequest(requesterId)) {
            return false;
        }
        
        return addPlayerToGroup(requesterId, groupName);
    }
    
    public boolean denyJoinRequest(String groupName, UUID ownerId, UUID requesterId) {
        Group group = getGroup(groupName);
        if (group == null || !group.isOwner(ownerId) || !group.hasJoinRequest(requesterId)) {
            return false;
        }
        
        group.removeJoinRequest(requesterId);
        saveGroups();
        return true;
    }
    
    public Collection<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }
    
    public void cleanExpiredRequests() {
        long expirationTime = plugin.getConfig().getLong("join-request-expire-minutes", 10) * 60 * 1000;
        int totalExpired = 0;
        
        for (Group group : groups.values()) {
            int beforeCount = group.getJoinRequests().size();
            group.cleanExpiredRequests(expirationTime);
            int afterCount = group.getJoinRequests().size();
            totalExpired += (beforeCount - afterCount);
        }
        
        if (totalExpired > 0) {
            plugin.getLogger().info("Nettoyage automatique: " + totalExpired + " demandes d'adhésion expirées supprimées");
            saveGroups();
        }
    }
    
    public void updatePlayerDisplay(Player player) {
        Group group = getPlayerGroup(player.getUniqueId());
        if (group != null) {
            // Use Adventure Components for rich text formatting
            Component displayName = Component.text()
                .append(Component.text("[", group.getTextColor()))
                .append(Component.text(group.getName(), group.getTextColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("] ", group.getTextColor()))
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .build();
            
            player.displayName(displayName);
            player.playerListName(displayName);
            
            // Set tab list ordering: group name + player name for sorting
            // Groups are ordered alphabetically, then players within groups alphabetically
            
            // Use team-based sorting for proper tab list ordering
            updatePlayerTeam(player, group);
        } else {
            // Reset to default - no group players appear at the end
            Component defaultName = Component.text(player.getName(), NamedTextColor.WHITE);
            player.displayName(defaultName);
            player.playerListName(defaultName);
            
            // Remove from team to appear at end of tab list
            removePlayerFromTeam(player);
        }
    }
    
    /**
     * Updates or creates a scoreboard team for proper tab list ordering
     */
    private void updatePlayerTeam(Player player, Group group) {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "group_" + group.getName().toLowerCase();
        
        var team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Always update team prefix and color (in case group color changed)
        team.prefix(Component.text("[" + group.getName() + "] ", group.getTextColor()));
        team.color(NamedTextColor.WHITE);
        
        // Add player to team (automatically handles ordering)
        team.addPlayer(player);
    }
    
    /**
     * Removes player from all teams
     */
    private void removePlayerFromTeam(Player player) {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        var team = scoreboard.getPlayerTeam(player);
        if (team != null) {
            team.removePlayer(player);
            
            // Clean up empty teams
            if (team.getPlayers().isEmpty()) {
                team.unregister();
            }
        }
    }
    
    /**
     * Updates all online players' display and team membership for a specific group
     */
    public void updateGroupDisplay(String groupName) {
        Group group = getGroup(groupName);
        if (group == null) return;

        // Update all online group members
        for (UUID memberId : group.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                updatePlayerDisplay(member);
            }
        }

        // Update HUD for group members (color change affects HUD display)
        if (plugin.getHudManager() != null) {
            plugin.getHudManager().updateGroupMembersHUD(groupName);
        }
    }
    
    /**
     * Cleans up team when a group is deleted
     */
    private void cleanupGroupTeam(String groupName) {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "group_" + groupName.toLowerCase();
        
        var team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }
    
    /**
     * Checks if a group was migrated from old ChatColor format to new hex format
     */
    private boolean wasGroupMigrated(String groupKey) {
        // Check if the group has old "color" field but no "colorHex" field
        return groupsConfig.contains("groups." + groupKey + ".color") && 
               !groupsConfig.contains("groups." + groupKey + ".colorHex");
    }
}
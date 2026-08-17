package com.aspa.aspagroup.managers;

import com.aspa.aspagroup.AspaGroup;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages player chat mode states (global vs group-only chat).
 * All data is stored in memory and cleared on server restart or player logout.
 */
public class ChatModeManager {

    private final AspaGroup plugin;
    private final Set<UUID> groupChatModePlayers;

    public ChatModeManager(AspaGroup plugin) {
        this.plugin = plugin;
        this.groupChatModePlayers = new HashSet<>();
    }

    /**
     * Toggle a player's chat mode between global and group-only
     * @param player The player to toggle
     * @return true if now in group chat mode, false if now in global chat mode
     */
    public boolean toggleChatMode(Player player) {
        UUID playerId = player.getUniqueId();

        if (groupChatModePlayers.contains(playerId)) {
            groupChatModePlayers.remove(playerId);
            plugin.getLogger().info(player.getName() + " switched to global chat mode");
            return false;
        } else {
            groupChatModePlayers.add(playerId);
            plugin.getLogger().info(player.getName() + " switched to group chat mode");
            return true;
        }
    }

    /**
     * Check if a player is in group chat mode
     * @param player The player to check
     * @return true if in group chat mode, false if in global chat mode
     */
    public boolean isInGroupChatMode(Player player) {
        return groupChatModePlayers.contains(player.getUniqueId());
    }

    /**
     * Clear a player's chat mode (e.g., on logout)
     * @param playerId The player UUID to clear
     */
    public void clearPlayerMode(UUID playerId) {
        groupChatModePlayers.remove(playerId);
    }

    /**
     * Clear all chat mode data (e.g., on plugin disable)
     */
    public void clearAllData() {
        groupChatModePlayers.clear();
        plugin.getLogger().info("Cleared all chat mode data");
    }

    /**
     * Get the number of players currently in group chat mode
     * @return Number of players in group chat mode
     */
    public int getGroupChatModeCount() {
        return groupChatModePlayers.size();
    }
}

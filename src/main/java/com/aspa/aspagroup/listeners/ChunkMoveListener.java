package com.aspa.aspagroup.listeners;

import com.aspa.aspagroup.AspaGroup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkMoveListener implements Listener {

    private final AspaGroup plugin;
    private final Map<UUID, String> lastChunkKeys; // playerId -> "world:x:z"
    private final Map<UUID, Long> lastUpdateTimes; // playerId -> timestamp

    public ChunkMoveListener(AspaGroup plugin) {
        this.plugin = plugin;
        this.lastChunkKeys = new ConcurrentHashMap<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Skip if player doesn't have HUD permission
        if (!player.hasPermission("aspagroup.hud")) {
            return;
        }

        // Check update cooldown to prevent spam
        long currentTime = System.currentTimeMillis();
        long lastUpdate = lastUpdateTimes.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = plugin.getHudManager().getUpdateCooldown();

        if (currentTime - lastUpdate < cooldown) {
            return;
        }

        // Check if player moved to a different chunk
        if (hasChangedChunk(player)) {
            // Update HUD on main thread since Player objects aren't thread-safe
            plugin.getHudManager().updatePlayerHUD(player);
            lastUpdateTimes.put(player.getUniqueId(), currentTime);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Skip if player doesn't have HUD permission
        if (!player.hasPermission("aspagroup.hud")) {
            return;
        }

        // Force update on teleport since it can cross chunks/worlds
        // Small delay to ensure teleport completed (on main thread)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getHudManager().forceUpdateHUD(player);
            updatePlayerChunkKey(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Skip if player doesn't have HUD permission
        if (!player.hasPermission("aspagroup.hud")) {
            return;
        }

        // Force update when changing worlds (on main thread)
        plugin.getHudManager().forceUpdateHUD(player);
        updatePlayerChunkKey(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Clean up tracking data
        lastChunkKeys.remove(playerId);
        lastUpdateTimes.remove(playerId);

        // Clean up HUD manager data
        plugin.getHudManager().removePlayer(playerId);
    }

    /**
     * Checks if a player has moved to a different chunk
     */
    private boolean hasChangedChunk(Player player) {
        String currentChunkKey = getCurrentChunkKey(player);
        String lastChunkKey = lastChunkKeys.get(player.getUniqueId());

        // Update stored chunk key
        lastChunkKeys.put(player.getUniqueId(), currentChunkKey);

        // Return true if chunk changed
        return !currentChunkKey.equals(lastChunkKey);
    }

    /**
     * Gets the current chunk key for a player
     */
    private String getCurrentChunkKey(Player player) {
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        return player.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Updates the stored chunk key for a player (used after teleports)
     */
    private void updatePlayerChunkKey(Player player) {
        String currentChunkKey = getCurrentChunkKey(player);
        lastChunkKeys.put(player.getUniqueId(), currentChunkKey);
    }

    /**
     * Forces an update for a specific player (called from other parts of the plugin)
     */
    public void forcePlayerUpdate(Player player) {
        updatePlayerChunkKey(player);
        plugin.getHudManager().forceUpdateHUD(player);
    }

    /**
     * Gets listener statistics for debugging
     */
    public Map<String, Object> getListenerStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("trackedPlayers", lastChunkKeys.size());
        stats.put("playersWithCooldown", lastUpdateTimes.size());

        // Calculate average update frequency
        long currentTime = System.currentTimeMillis();
        long recentUpdates = lastUpdateTimes.values().stream()
                .mapToLong(time -> currentTime - time < 60000 ? 1 : 0) // Updates in last minute
                .sum();

        stats.put("recentUpdates", recentUpdates);
        return stats;
    }

    /**
     * Clears all tracking data (for plugin reload)
     */
    public void clearData() {
        lastChunkKeys.clear();
        lastUpdateTimes.clear();
    }
}
package com.aspa.aspagroup.managers;

import com.aspa.aspagroup.AspaGroup;
import com.aspa.aspagroup.models.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HUDManager {

    private static final String WILDERNESS_MARKER = "__WILDERNESS__"; // Special marker for wilderness

    private final AspaGroup plugin;
    private final Map<UUID, String> playerCurrentZone; // playerId -> current zone (groupName or WILDERNESS_MARKER)
    private final Map<UUID, Boolean> playerHudEnabled; // playerId -> HUD enabled/disabled
    private File hudPrefsFile;
    private FileConfiguration hudPrefsConfig;

    public HUDManager(AspaGroup plugin) {
        this.plugin = plugin;
        this.playerCurrentZone = new ConcurrentHashMap<>();
        this.playerHudEnabled = new ConcurrentHashMap<>();
        createHudPrefsFile();
        loadHudPreferences();
        startHudRefreshTask();
    }

    /**
     * Starts the constant HUD refresh task to keep displays always visible
     */
    private void startHudRefreshTask() {
        // Get refresh interval from config (default 2 seconds)
        long refreshTicks = plugin.getConfig().getLong("hud-refresh-interval-ticks", 40L);

        // Refresh HUD periodically for all players with HUD enabled (ASYNC for performance)
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Collect players to update on async thread
            List<Player> playersToUpdate = new ArrayList<>();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isHudEnabled(player) && playerCurrentZone.containsKey(player.getUniqueId())) {
                    playersToUpdate.add(player);
                }
            }

            // Send updates back to main thread for Action Bar (required)
            if (!playersToUpdate.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Player player : playersToUpdate) {
                        // Double-check player is still online
                        if (player.isOnline()) {
                            refreshPlayerHUD(player);
                        }
                    }
                });
            }
        }, refreshTicks, refreshTicks); // Start after interval, repeat at same interval
    }

    /**
     * Refreshes the HUD display for a player using cached zone data
     */
    private void refreshPlayerHUD(Player player) {
        try {
            // Get cached zone info
            String storedZone = playerCurrentZone.get(player.getUniqueId());
            if (storedZone != null) {
                // Convert marker back to actual owner for display
                String actualOwner = WILDERNESS_MARKER.equals(storedZone) ? null : storedZone;
                Component hudMessage = createHudMessage(actualOwner);
                player.sendActionBar(hudMessage);
            }
        } catch (Exception e) {
            // Silently handle any exceptions (player might have disconnected)
        }
    }

    /**
     * Creates the HUD preferences file
     */
    private void createHudPrefsFile() {
        hudPrefsFile = new File(plugin.getDataFolder(), "hud-preferences.yml");
        if (!hudPrefsFile.exists()) {
            hudPrefsFile.getParentFile().mkdirs();
            try {
                hudPrefsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create hud-preferences.yml file: " + e.getMessage());
            }
        }
        hudPrefsConfig = YamlConfiguration.loadConfiguration(hudPrefsFile);
    }

    /**
     * Loads HUD preferences from file
     */
    public void loadHudPreferences() {
        playerHudEnabled.clear();

        if (hudPrefsConfig.contains("disabled-players")) {
            Set<String> disabledPlayers = hudPrefsConfig.getConfigurationSection("disabled-players").getKeys(false);
            for (String uuidString : disabledPlayers) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    boolean isDisabled = hudPrefsConfig.getBoolean("disabled-players." + uuidString, false);
                    playerHudEnabled.put(playerId, !isDisabled); // Store enabled state (inverse of disabled)
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in hud-preferences.yml: " + uuidString);
                }
            }
        }

        plugin.getLogger().info("Loaded HUD preferences for " + playerHudEnabled.size() + " players");
    }

    /**
     * Saves HUD preferences to file
     */
    public void saveHudPreferences() {
        try {
            // Clear existing data
            hudPrefsConfig.set("disabled-players", null);

            // Save only players who have disabled HUD (to keep file small)
            for (Map.Entry<UUID, Boolean> entry : playerHudEnabled.entrySet()) {
                if (!entry.getValue()) { // Player has HUD disabled
                    hudPrefsConfig.set("disabled-players." + entry.getKey().toString(), true);
                }
            }

            hudPrefsConfig.save(hudPrefsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save hud-preferences.yml: " + e.getMessage());
        }
    }

    /**
     * Updates the HUD for a player based on their current location
     */
    public void updatePlayerHUD(Player player) {
        // Null safety check - player might have disconnected
        if (player == null || !player.isOnline() || player.getUniqueId() == null) {
            return;
        }

        if (!isHudEnabled(player)) {
            return;
        }

        // Check if chunk HUD is globally enabled
        if (!plugin.getConfig().getBoolean("enable-chunk-hud", true)) {
            return;
        }

        // Additional safety checks for async execution
        Location playerLocation = player.getLocation();
        if (playerLocation == null || playerLocation.getWorld() == null) {
            return;
        }

        String currentOwner = plugin.getChunkManager().getChunkOwner(playerLocation);
        String lastOwner = playerCurrentZone.get(player.getUniqueId());

        // Convert null wilderness to marker for comparison
        String currentZone = (currentOwner == null) ? WILDERNESS_MARKER : currentOwner;

        // Only update if zone changed
        if (currentZone.equals(lastOwner)) {
            return;
        }

        // Always store the zone (wilderness gets special marker)
        playerCurrentZone.put(player.getUniqueId(), currentZone);

        // Send HUD update
        Component hudMessage = createHudMessage(currentOwner);
        player.sendActionBar(hudMessage);
    }

    /**
     * Creates the HUD message component based on zone ownership
     */
    private Component createHudMessage(String groupName) {
        if (groupName == null) {
            // Wilderness
            return Component.text("Terre sauvage")
                    .color(NamedTextColor.GRAY);
        } else {
            // Claimed chunk
            Group group = plugin.getGroupManager().getGroup(groupName);
            if (group != null) {
                return Component.text("Zone: ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("[" + groupName + "]")
                                .color(group.getTextColor()));
            } else {
                // Group doesn't exist anymore
                return Component.text("Zone: ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("[" + groupName + "]")
                                .color(NamedTextColor.RED))
                        .append(Component.text(" (groupe supprimé)")
                                .color(NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Forces an immediate HUD update for a player (used when toggling or joining)
     */
    public void forceUpdateHUD(Player player) {
        // Null safety check
        if (player == null || !player.isOnline() || player.getUniqueId() == null) {
            return;
        }

        playerCurrentZone.remove(player.getUniqueId()); // Clear cache to force update
        updatePlayerHUD(player);

        // If HUD is enabled, ensure something is displayed (especially for new players)
        if (isHudEnabled(player)) {
            if (playerCurrentZone.containsKey(player.getUniqueId())) {
                String storedZone = playerCurrentZone.get(player.getUniqueId());
                String actualOwner = WILDERNESS_MARKER.equals(storedZone) ? null : storedZone;
                Component hudMessage = createHudMessage(actualOwner);
                player.sendActionBar(hudMessage);
            } else {
                // Initialize for new player
                updatePlayerHUD(player);
            }
        }
    }

    /**
     * Checks if HUD is enabled for a player
     */
    public boolean isHudEnabled(Player player) {
        return playerHudEnabled.getOrDefault(player.getUniqueId(), true); // Default enabled
    }

    /**
     * Toggles HUD for a player
     */
    public void toggleHUD(Player player) {
        boolean currentState = isHudEnabled(player);
        boolean newState = !currentState;

        playerHudEnabled.put(player.getUniqueId(), newState);
        saveHudPreferences(); // Persist the change

        if (newState) {
            // HUD enabled - show current zone
            forceUpdateHUD(player);
        } else {
            // HUD disabled - clear action bar but keep zone tracking
            player.sendActionBar(Component.empty());
        }
    }

    /**
     * Sets HUD state for a player
     */
    public void setHudEnabled(Player player, boolean enabled) {
        playerHudEnabled.put(player.getUniqueId(), enabled);
        saveHudPreferences(); // Persist the change

        if (enabled) {
            forceUpdateHUD(player);
        } else {
            player.sendActionBar(Component.empty());
        }
    }

    /**
     * Cleans up player data when they leave
     */
    public void removePlayer(UUID playerId) {
        playerCurrentZone.remove(playerId);
        // Don't remove playerHudEnabled - we want to keep preferences across sessions
    }

    /**
     * Updates HUD for all online players (used when chunks change ownership)
     */
    public void updateAllPlayersHUD() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerHUD(player);
        }
    }

    /**
     * Updates HUD for all members of a specific group (used when group is renamed/recolored)
     */
    public void updateGroupMembersHUD(String groupName) {
        Group group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) return;

        for (UUID memberId : group.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                forceUpdateHUD(member);
            }
        }
    }

    /**
     * Gets the update cooldown from config (in milliseconds)
     */
    public long getUpdateCooldown() {
        return plugin.getConfig().getLong("hud-update-cooldown-ms", 100);
    }

    /**
     * Checks if the global HUD system is enabled
     */
    public boolean isHudSystemEnabled() {
        return plugin.getConfig().getBoolean("enable-chunk-hud", true);
    }

    /**
     * Gets HUD statistics for admin purposes
     */
    public Map<String, Object> getHudStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        int enabledPlayers = 0;
        int disabledPlayers = 0;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isHudEnabled(player)) {
                enabledPlayers++;
            } else {
                disabledPlayers++;
            }
        }

        stats.put("enabledPlayers", enabledPlayers);
        stats.put("disabledPlayers", disabledPlayers);
        stats.put("totalTrackedPlayers", playerCurrentZone.size());
        stats.put("globallyEnabled", isHudSystemEnabled());
        stats.put("updateCooldown", getUpdateCooldown());

        return stats;
    }

    /**
     * Clears all HUD data (for plugin reload)
     */
    public void clearAllData() {
        // Clear action bars for all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isHudEnabled(player)) {
                player.sendActionBar(Component.empty());
            }
        }

        playerCurrentZone.clear();
        // Don't clear playerHudEnabled - preserve player preferences
        // Save preferences before shutdown
        saveHudPreferences();
    }
}
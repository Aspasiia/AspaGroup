package com.aspa.aspagroup.listeners;

import com.aspa.aspagroup.AspaGroup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final AspaGroup plugin;

    public PlayerJoinListener(AspaGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getGroupManager().updatePlayerDisplay(event.getPlayer());

        // Initialize HUD for joining player (with small delay to ensure full login)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getHudManager() != null) {
                plugin.getHudManager().forceUpdateHUD(event.getPlayer());
            }
        }, 20L); // 1 second delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear chat mode on logout to free memory
        if (plugin.getChatModeManager() != null) {
            plugin.getChatModeManager().clearPlayerMode(event.getPlayer().getUniqueId());
        }
    }

}
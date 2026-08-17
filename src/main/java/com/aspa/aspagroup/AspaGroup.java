package com.aspa.aspagroup;

import com.aspa.aspagroup.commands.GroupCommand;
import com.aspa.aspagroup.listeners.ChatListener;
import com.aspa.aspagroup.listeners.ChunkMoveListener;
import com.aspa.aspagroup.listeners.PlayerJoinListener;
import com.aspa.aspagroup.managers.ChatModeManager;
import com.aspa.aspagroup.managers.ChunkManager;
import com.aspa.aspagroup.managers.GroupManager;
import com.aspa.aspagroup.managers.HUDManager;
import com.aspa.aspagroup.models.ChunkClaim;
import com.aspa.aspagroup.models.Group;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public final class AspaGroup extends JavaPlugin {

    private GroupManager groupManager;
    private ChunkManager chunkManager;
    private HUDManager hudManager;
    private ChatModeManager chatModeManager;
    private ChunkMoveListener chunkMoveListener;

    @Override
    public void onEnable() {
        ConfigurationSerialization.registerClass(Group.class);
        ConfigurationSerialization.registerClass(ChunkClaim.class);

        saveDefaultConfig();

        this.groupManager = new GroupManager(this);
        this.chunkManager = new ChunkManager(this);
        this.hudManager = new HUDManager(this);
        this.chatModeManager = new ChatModeManager(this);
        this.chunkMoveListener = new ChunkMoveListener(this);

        groupManager.loadGroups();
        chunkManager.loadChunks();

        // Perform data integrity cleanup after loading
        chunkManager.cleanupInvalidClaims();

        // Schedule periodic cleanup task (every hour)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            chunkManager.cleanupInvalidClaims();
        }, 20L * 60 * 60, 20L * 60 * 60); // 1 hour delay, 1 hour repeat

        getCommand("group").setExecutor(new GroupCommand(this));
        
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(chunkMoveListener, this);
        
        getLogger().info("AspaGroup has been enabled!");
    }

    @Override
    public void onDisable() {
        if (groupManager != null) {
            groupManager.saveGroups();
        }

        if (chunkManager != null) {
            chunkManager.saveChunks();
        }

        if (hudManager != null) {
            hudManager.clearAllData();
        }

        if (chatModeManager != null) {
            chatModeManager.clearAllData();
        }

        if (chunkMoveListener != null) {
            chunkMoveListener.clearData();
        }

        getLogger().info("AspaGroup has been disabled!");
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public HUDManager getHudManager() {
        return hudManager;
    }

    public ChatModeManager getChatModeManager() {
        return chatModeManager;
    }

    public ChunkMoveListener getChunkMoveListener() {
        return chunkMoveListener;
    }
}
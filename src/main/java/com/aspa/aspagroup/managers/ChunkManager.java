package com.aspa.aspagroup.managers;

import com.aspa.aspagroup.AspaGroup;
import com.aspa.aspagroup.models.ChunkClaim;
import com.aspa.aspagroup.models.Group;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChunkManager {

    private final AspaGroup plugin;
    private final Map<String, String> chunkClaims; // chunkKey -> groupName
    private File chunksFile;
    private FileConfiguration chunksConfig;

    public ChunkManager(AspaGroup plugin) {
        this.plugin = plugin;
        this.chunkClaims = new ConcurrentHashMap<>();
        createChunksFile();
    }

    private void createChunksFile() {
        chunksFile = new File(plugin.getDataFolder(), "chunks.yml");
        if (!chunksFile.exists()) {
            chunksFile.getParentFile().mkdirs();
            try {
                chunksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create chunks.yml file: " + e.getMessage());
            }
        }
        chunksConfig = YamlConfiguration.loadConfiguration(chunksFile);
    }

    public void loadChunks() {
        chunkClaims.clear();

        for (String key : chunksConfig.getKeys(false)) {
            String groupName = chunksConfig.getString(key);
            if (groupName != null) {
                chunkClaims.put(key, groupName);
            }
        }

        // Clean up claims for groups that no longer exist
        cleanupInvalidClaims();

        plugin.getLogger().info("Loaded " + chunkClaims.size() + " chunk claims");
    }

    public void saveChunks() {
        try {
            // Clear existing data
            for (String key : chunksConfig.getKeys(false)) {
                chunksConfig.set(key, null);
            }

            // Save current claims
            for (Map.Entry<String, String> entry : chunkClaims.entrySet()) {
                chunksConfig.set(entry.getKey(), entry.getValue());
            }

            chunksConfig.save(chunksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chunks.yml: " + e.getMessage());
        }
    }

    /**
     * Claim a chunk for a group
     */
    public boolean claimChunk(String world, int chunkX, int chunkZ, String groupName) {
        String chunkKey = ChunkClaim.createChunkKey(world, chunkX, chunkZ);

        // Check if chunk is already claimed
        if (chunkClaims.containsKey(chunkKey)) {
            return false;
        }

        // Check group claim limit
        if (hasReachedClaimLimit(groupName)) {
            return false;
        }

        chunkClaims.put(chunkKey, groupName);
        saveChunks();
        return true;
    }

    /**
     * Unclaim a chunk
     */
    public boolean unclaimChunk(String world, int chunkX, int chunkZ) {
        String chunkKey = ChunkClaim.createChunkKey(world, chunkX, chunkZ);
        boolean removed = chunkClaims.remove(chunkKey) != null;
        if (removed) {
            saveChunks();
        }
        return removed;
    }

    /**
     * Get the group that owns a chunk
     */
    public String getChunkOwner(String world, int chunkX, int chunkZ) {
        String chunkKey = ChunkClaim.createChunkKey(world, chunkX, chunkZ);
        return chunkClaims.get(chunkKey);
    }

    /**
     * Get the group that owns a chunk at a location
     */
    public String getChunkOwner(Location location) {
        Chunk chunk = location.getChunk();
        return getChunkOwner(location.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Get the group that owns a chunk where a player is standing
     */
    public String getChunkOwner(Player player) {
        return getChunkOwner(player.getLocation());
    }

    /**
     * Check if a chunk is claimed
     */
    public boolean isChunkClaimed(String world, int chunkX, int chunkZ) {
        String chunkKey = ChunkClaim.createChunkKey(world, chunkX, chunkZ);
        return chunkClaims.containsKey(chunkKey);
    }

    /**
     * Get all chunks claimed by a group
     */
    public List<ChunkClaim> getGroupClaims(String groupName) {
        List<ChunkClaim> claims = new ArrayList<>();

        for (Map.Entry<String, String> entry : chunkClaims.entrySet()) {
            if (entry.getValue().equals(groupName)) {
                String[] parts = ChunkClaim.parseChunkKey(entry.getKey());
                if (parts != null) {
                    try {
                        int chunkX = Integer.parseInt(parts[1]);
                        int chunkZ = Integer.parseInt(parts[2]);
                        claims.add(new ChunkClaim(parts[0], chunkX, chunkZ, groupName));
                    } catch (NumberFormatException e) {
                        // Skip invalid chunk keys
                    }
                }
            }
        }

        return claims;
    }

    /**
     * Get the number of chunks claimed by a group
     */
    public int getGroupClaimCount(String groupName) {
        return (int) chunkClaims.values().stream()
                .filter(name -> name.equals(groupName))
                .count();
    }

    /**
     * Check if a group has reached its claim limit
     */
    public boolean hasReachedClaimLimit(String groupName) {
        int maxClaims = plugin.getConfig().getInt("max-claims-per-group", 20);
        int currentClaims = getGroupClaimCount(groupName);
        return currentClaims >= maxClaims;
    }

    /**
     * Check if a player has unlimited claim permission
     */
    public boolean hasUnlimitedPermission(org.bukkit.entity.Player player) {
        return player.hasPermission("aspagroup.claim.unlimited");
    }

    /**
     * Get the maximum number of claims allowed per group
     */
    public int getMaxClaimsPerGroup() {
        return plugin.getConfig().getInt("max-claims-per-group", 20);
    }

    /**
     * Remove all claims for a group (used when group is deleted)
     */
    public void removeGroupClaims(String groupName) {
        boolean changed = false;
        Iterator<Map.Entry<String, String>> iterator = chunkClaims.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue().equals(groupName)) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            saveChunks();
        }
    }

    /**
     * Clean up claims for groups that no longer exist
     */
    public void cleanupInvalidClaims() {
        Set<String> validGroups = plugin.getGroupManager().getAllGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toSet());
        boolean changed = false;

        Iterator<Map.Entry<String, String>> iterator = chunkClaims.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String groupName = entry.getValue();

            if (!validGroups.contains(groupName)) {
                iterator.remove();
                changed = true;
                plugin.getLogger().info("Removed claim for non-existent group: " + groupName);
            }
        }

        // Also clean up claims in worlds that no longer exist
        iterator = chunkClaims.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String[] parts = ChunkClaim.parseChunkKey(entry.getKey());
            if (parts != null) {
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) {
                    iterator.remove();
                    changed = true;
                    plugin.getLogger().info("Removed claim in non-existent world: " + parts[0]);
                }
            }
        }

        if (changed) {
            saveChunks();
        }
    }

    /**
     * Get all chunk claims on the server
     */
    public Map<String, String> getAllClaims() {
        return new HashMap<>(chunkClaims);
    }

    /**
     * Get server-wide claiming statistics
     */
    public Map<String, Object> getClaimingStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalClaims", chunkClaims.size());
        stats.put("totalGroups", chunkClaims.values().stream().collect(Collectors.toSet()).size());

        // Find group with most claims
        Map<String, Integer> groupCounts = new HashMap<>();
        for (String groupName : chunkClaims.values()) {
            groupCounts.put(groupName, groupCounts.getOrDefault(groupName, 0) + 1);
        }

        if (!groupCounts.isEmpty()) {
            String topGroup = groupCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            stats.put("topGroup", topGroup);
            stats.put("topGroupClaims", groupCounts.get(topGroup));
        }

        // Count claims per world
        Map<String, Integer> worldCounts = new HashMap<>();
        for (String chunkKey : chunkClaims.keySet()) {
            String[] parts = ChunkClaim.parseChunkKey(chunkKey);
            if (parts != null) {
                worldCounts.put(parts[0], worldCounts.getOrDefault(parts[0], 0) + 1);
            }
        }
        stats.put("claimsPerWorld", worldCounts);

        return stats;
    }

    /**
     * Force unclaim a specific chunk (admin command)
     */
    public boolean forceUnclaimChunk(String world, int chunkX, int chunkZ) {
        return unclaimChunk(world, chunkX, chunkZ);
    }

    /**
     * Rename group ownership in chunk claims
     */
    public void renameGroupClaims(String oldGroupName, String newGroupName) {
        boolean changed = false;
        for (Map.Entry<String, String> entry : chunkClaims.entrySet()) {
            if (entry.getValue().equals(oldGroupName)) {
                entry.setValue(newGroupName);
                changed = true;
            }
        }
        if (changed) {
            saveChunks();
        }
    }
}
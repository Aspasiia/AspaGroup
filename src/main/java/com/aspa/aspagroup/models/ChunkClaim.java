package com.aspa.aspagroup.models;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("ChunkClaim")
public class ChunkClaim implements ConfigurationSerializable {

    private String world;
    private int chunkX;
    private int chunkZ;
    private String groupName;
    private long claimTimestamp;

    public ChunkClaim(String world, int chunkX, int chunkZ, String groupName) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.groupName = groupName;
        this.claimTimestamp = System.currentTimeMillis();
    }

    public ChunkClaim(Map<String, Object> map) {
        this.world = (String) map.get("world");
        this.chunkX = (Integer) map.get("chunkX");
        this.chunkZ = (Integer) map.get("chunkZ");
        this.groupName = (String) map.get("groupName");
        this.claimTimestamp = (Long) map.getOrDefault("claimTimestamp", System.currentTimeMillis());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", world);
        map.put("chunkX", chunkX);
        map.put("chunkZ", chunkZ);
        map.put("groupName", groupName);
        map.put("claimTimestamp", claimTimestamp);
        return map;
    }

    public String getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getClaimTimestamp() {
        return claimTimestamp;
    }

    /**
     * Creates a unique key for this chunk location
     * Format: "world:x:z"
     */
    public String getChunkKey() {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    /**
     * Creates a chunk key from world name and coordinates
     */
    public static String createChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    /**
     * Parses a chunk key back into components
     * Returns [world, chunkX, chunkZ] or null if invalid
     */
    public static String[] parseChunkKey(String chunkKey) {
        if (chunkKey == null) return null;
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return null;
        try {
            Integer.parseInt(parts[1]); // Validate chunkX
            Integer.parseInt(parts[2]); // Validate chunkZ
            return parts;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ChunkClaim that = (ChunkClaim) obj;
        return chunkX == that.chunkX &&
               chunkZ == that.chunkZ &&
               world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return getChunkKey().hashCode();
    }

    @Override
    public String toString() {
        return String.format("ChunkClaim{world='%s', x=%d, z=%d, group='%s'}",
                           world, chunkX, chunkZ, groupName);
    }
}
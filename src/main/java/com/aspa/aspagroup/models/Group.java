package com.aspa.aspagroup.models;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

@SerializableAs("Group")
public class Group implements ConfigurationSerializable {
    
    private String name;
    private String description;
    private UUID owner;
    private Set<UUID> members;
    private String colorHex;
    private Map<UUID, Long> joinRequests;
    
    public Group(String name, UUID owner) {
        this.name = name;
        this.description = "Aucune description";
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.colorHex = "#ffffff"; // WHITE
        this.joinRequests = new HashMap<>();
    }
    
    public Group(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.description = (String) map.getOrDefault("description", "Aucune description");
        this.owner = UUID.fromString((String) map.get("owner"));
        
        List<String> membersList = (List<String>) map.get("members");
        this.members = new HashSet<>();
        if (membersList != null) {
            for (String memberStr : membersList) {
                this.members.add(UUID.fromString(memberStr));
            }
        }
        
        // Handle both old ChatColor format and new hex format for migration
        if (map.containsKey("colorHex")) {
            // New hex format
            this.colorHex = (String) map.get("colorHex");
        } else if (map.containsKey("color")) {
            // Old ChatColor format - migrate to hex
            String colorName = (String) map.get("color");
            this.colorHex = convertChatColorToHex(colorName);
        } else {
            // Default
            this.colorHex = "#ffffff"; // WHITE
        }
        
        Map<String, Long> requestsMap = (Map<String, Long>) map.getOrDefault("joinRequests", new HashMap<>());
        this.joinRequests = new HashMap<>();
        for (Map.Entry<String, Long> entry : requestsMap.entrySet()) {
            this.joinRequests.put(UUID.fromString(entry.getKey()), entry.getValue());
        }
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("owner", owner.toString());
        
        List<String> membersList = new ArrayList<>();
        for (UUID member : members) {
            membersList.add(member.toString());
        }
        map.put("members", membersList);
        
        map.put("colorHex", colorHex);
        
        Map<String, Long> requestsMap = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : joinRequests.entrySet()) {
            requestsMap.put(entry.getKey().toString(), entry.getValue());
        }
        map.put("joinRequests", requestsMap);
        
        return map;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public void setOwner(UUID owner) {
        this.owner = owner;
    }
    
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    public boolean addMember(UUID playerId) {
        return members.add(playerId);
    }
    
    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }
    
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }
    
    public String getColorHex() {
        return colorHex;
    }
    
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
    
    public TextColor getTextColor() {
        try {
            return TextColor.fromCSSHexString(colorHex);
        } catch (Exception e) {
            return TextColor.fromCSSHexString("#ffffff"); // fallback to white
        }
    }
    
    // Legacy method for backwards compatibility
    @Deprecated
    public ChatColor getColor() {
        return convertHexToChatColor(colorHex);
    }
    
    /**
     * Get ChatColor representation for native Bukkit messaging
     */
    public ChatColor getChatColor() {
        return convertHexToChatColor(colorHex);
    }
    
    // Legacy method for backwards compatibility
    @Deprecated
    public void setColor(ChatColor color) {
        this.colorHex = convertChatColorToHex(color.name());
    }
    
    public void addJoinRequest(UUID playerId) {
        joinRequests.put(playerId, System.currentTimeMillis());
    }
    
    public void removeJoinRequest(UUID playerId) {
        joinRequests.remove(playerId);
    }
    
    public boolean hasJoinRequest(UUID playerId) {
        return joinRequests.containsKey(playerId);
    }
    
    public Set<UUID> getJoinRequests() {
        return new HashSet<>(joinRequests.keySet());
    }
    
    public void cleanExpiredRequests(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        joinRequests.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expirationTime);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * Converts legacy ChatColor names to hex equivalents
     */
    private String convertChatColorToHex(String colorName) {
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("RED", "#ff5555");
        colorMap.put("BLUE", "#5555ff");
        colorMap.put("GREEN", "#55ff55");
        colorMap.put("YELLOW", "#ffff55");
        colorMap.put("PURPLE", "#ff55ff");
        colorMap.put("LIGHT_PURPLE", "#ff55ff");
        colorMap.put("AQUA", "#55ffff");
        colorMap.put("WHITE", "#ffffff");
        colorMap.put("GRAY", "#aaaaaa");
        colorMap.put("DARK_RED", "#aa0000");
        colorMap.put("DARK_BLUE", "#0000aa");
        colorMap.put("DARK_GREEN", "#00aa00");
        colorMap.put("GOLD", "#ffaa00");
        colorMap.put("DARK_PURPLE", "#aa00aa");
        colorMap.put("DARK_AQUA", "#00aaaa");
        return colorMap.getOrDefault(colorName.toUpperCase(), "#ffffff");
    }
    
    /**
     * Converts hex colors back to nearest ChatColor for legacy compatibility
     */
    private ChatColor convertHexToChatColor(String hex) {
        Map<String, ChatColor> hexToColor = new HashMap<>();
        hexToColor.put("#ff5555", ChatColor.RED);
        hexToColor.put("#5555ff", ChatColor.BLUE);
        hexToColor.put("#55ff55", ChatColor.GREEN);
        hexToColor.put("#ffff55", ChatColor.YELLOW);
        hexToColor.put("#ff55ff", ChatColor.LIGHT_PURPLE);
        hexToColor.put("#55ffff", ChatColor.AQUA);
        hexToColor.put("#ffffff", ChatColor.WHITE);
        hexToColor.put("#aaaaaa", ChatColor.GRAY);
        hexToColor.put("#aa0000", ChatColor.DARK_RED);
        hexToColor.put("#0000aa", ChatColor.DARK_BLUE);
        hexToColor.put("#00aa00", ChatColor.DARK_GREEN);
        hexToColor.put("#ffaa00", ChatColor.GOLD);
        hexToColor.put("#aa00aa", ChatColor.DARK_PURPLE);
        hexToColor.put("#00aaaa", ChatColor.DARK_AQUA);
        return hexToColor.getOrDefault(hex.toLowerCase(), ChatColor.WHITE);
    }
}
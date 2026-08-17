package com.aspa.aspagroup.listeners;

import com.aspa.aspagroup.AspaGroup;
import com.aspa.aspagroup.models.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChatListener implements Listener {

    private final AspaGroup plugin;

    public ChatListener(AspaGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is in group chat mode
        if (plugin.getChatModeManager().isInGroupChatMode(player)) {
            // Player is in group-chat-only mode
            Group group = plugin.getGroupManager().getPlayerGroup(player.getUniqueId());

            if (group == null) {
                // Player is in group chat mode but has no group
                player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun groupe ! Utilisez /group togglechat pour revenir au chat global.");
                event.setCancelled(true);
                return;
            }

            // Cancel the global chat event
            event.setCancelled(true);

            // Send message only to group members (reuse group chat formatting)
            String message = event.getMessage();
            Component formattedMessage = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("Chat Groupe", NamedTextColor.GREEN))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text("[", group.getTextColor()))
                .append(Component.text(group.getName(), group.getTextColor()))
                .append(Component.text("] ", group.getTextColor()))
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(": " + message, NamedTextColor.WHITE));

            // Send to all online group members
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
        // If not in group chat mode, let the event proceed normally (global chat)
    }
}
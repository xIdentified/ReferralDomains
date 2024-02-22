package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReferralLinkCommand implements CommandExecutor {
    private final ReferralDomains plugin;
    private final Map<UUID, Long> lastUsedTime;
    private static final long COOLDOWN_TIME = 30000; // 30s

    public ReferralLinkCommand(ReferralDomains plugin) {
        this.plugin = plugin;
        this.lastUsedTime = new HashMap<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Check for cooldown
        if (lastUsedTime.containsKey(playerId) && System.currentTimeMillis() - lastUsedTime.get(playerId) < COOLDOWN_TIME) {
            player.sendMessage(ChatColor.RED + "Please wait before using this command again.");
            return true;
        }

        lastUsedTime.put(playerId, System.currentTimeMillis());

        // Check if the player has the permission to create a domain
        if (!player.hasPermission("referral.create")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to create a referral link.");
            return true;
        }

        String playerName = player.getName().toLowerCase();
        String domain = plugin.getConfig().getString("domain");
        String port = plugin.getConfig().getString("server-port");
        String referralLink = playerName + "." + domain;

        player.sendMessage(ChatColor.YELLOW + "Attempting to create DNS record for " + playerName + "...");

        plugin.checkDNSRecord(playerName).thenAccept(dnsRecordStatus -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (dnsRecordStatus.contains("Online") && plugin.getStorage().hasReferralLink(playerName)) {
                    // If the DNS record is online and the referral link exists in the database
                    player.sendMessage(ChatColor.GREEN + "Link already exists!");
                    String existingLink = plugin.getStorage().getReferralLink(playerName);
                    sendReferralLinkMessage(player, existingLink, port);
                } else {
                    plugin.createDNSRecord(playerName).thenAccept(isCreated -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (isCreated) {
                                plugin.getStorage().saveReferralLink(playerName, referralLink);
                                plugin.debugLog("Successfully created referral link for " + playerName);
                                sendReferralLinkMessage(player, referralLink, port);
                            } else {
                                plugin.getLogger().severe("Failed to create DNS record for " + playerName);
                                player.sendMessage(ChatColor.RED + "There was an error creating your referral link. Please try again later.");
                            }
                        });
                    });
                }
            });
        });

        return true;

    }

    // Method to send referral link message
    private void sendReferralLinkMessage(Player player, String referralLink, String port) {
        TextComponent message = new TextComponent("Your referral link is: ");
        message.setColor(ChatColor.GREEN);

        String fullReferralLink = port.equals("25565") ? referralLink : referralLink + ":" + port;

        TextComponent link = new TextComponent(fullReferralLink);
        link.setColor(ChatColor.YELLOW);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, fullReferralLink));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy!")));

        message.addExtra(link);
        player.spigot().sendMessage(message);
    }

}

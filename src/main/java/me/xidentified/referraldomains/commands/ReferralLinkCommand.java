package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import org.bukkit.ChatColor;
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
    private static final long COOLDOWN_TIME = 30000; // 30 seconds

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
            player.sendMessage("Please wait before using this command again.");
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

        // Check if a DNS record already exists for this player
        String dnsRecordStatus = plugin.checkDNSRecord(playerName);
        if (dnsRecordStatus.contains("DNS Record Found") && plugin.referralLinks.containsKey(playerName)) {
            player.sendMessage(ChatColor.RED + "A referral link already exists for you: " + ChatColor.GREEN + referralLink + ":" + port);
            return true;
        }

        plugin.getLogger().info("Attempting to create DNS record for " + playerName);

        boolean isCreated = plugin.createDNSRecord(playerName);
        if (isCreated) {
            plugin.referralLinks.put(playerName, referralLink);
            plugin.debugLog("Successfully created referral link for " + playerName);
            player.sendMessage(ChatColor.GREEN + "Your new referral link is: " + ChatColor.YELLOW + referralLink + ":" + port);
        } else {
            plugin.getLogger().severe("Failed to create DNS record for " + playerName);
            player.sendMessage(ChatColor.RED + "There was an error creating your referral link. Please try again later.");
        }
        return true;
    }
}

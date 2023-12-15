package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

public class CheckDomainCommand implements CommandExecutor {

    private final ReferralDomains plugin;

    public CheckDomainCommand(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /check-domain <playerName>");
            return true;
        }

        String playerName = args[0].toLowerCase();

        // Retrieve and check the DNS record for the player's custom domain
        String domainStatus = plugin.checkDNSRecord(playerName);
        sender.sendMessage(ChatColor.YELLOW + "Domain status for " + playerName + ": " + domainStatus);
        return true;
    }
}
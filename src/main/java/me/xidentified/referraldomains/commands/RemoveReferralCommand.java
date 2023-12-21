package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

public class RemoveReferralCommand implements CommandExecutor {

    private final ReferralDomains plugin;

    public RemoveReferralCommand(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only admins can run this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /remove-referral-link <playerName>");
            return true;
        }

        String playerName = args[0].toLowerCase();
        boolean isDeleted = plugin.deleteDNSRecord(playerName);

        if (isDeleted) {
            sender.sendMessage(ChatColor.GREEN + "Referral link for " + playerName + " has been removed!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to remove referral link for " + playerName + ".");
        }

        return true;
    }
}

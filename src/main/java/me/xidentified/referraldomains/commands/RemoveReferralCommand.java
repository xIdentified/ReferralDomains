package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import org.bukkit.Bukkit;
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
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /remove-referral-link <playerName>");
            return true;
        }

        String playerName = args[0].toLowerCase();
        sender.sendMessage(ChatColor.YELLOW + "Attempting to remove referral link for " + playerName + "...");

        // Asynchronously delete DNS record
        plugin.deleteDNSRecord(playerName).thenAcceptAsync(isDeleted -> {
            // Since this runs asynchronously, ensure we run the result handling back on the main server thread
            Bukkit.getServer().getScheduler().runTask(plugin, () -> {
                if (isDeleted) {
                    sender.sendMessage(ChatColor.GREEN + "Referral link for " + playerName + " has been removed!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to remove referral link for " + playerName + ".");
                }
            });
        });

        return true;
    }
}
